package in.potenfyr.cdn.damage;

import in.potenfyr.cdn.api.event.DamageNumberSpawnEvent;
import in.potenfyr.cdn.api.impl.ApiEvents;
import in.potenfyr.cdn.config.AnimationSettings;
import in.potenfyr.cdn.config.ConfigManager;
import in.potenfyr.cdn.config.MergeSettings;
import in.potenfyr.cdn.config.StyleSettings;
import in.potenfyr.cdn.packet.EffectService;
import in.potenfyr.cdn.packet.RenderBackend;
import in.potenfyr.cdn.prefs.PlayerPreferences;
import in.potenfyr.cdn.text.TextRenderer;
import in.potenfyr.cdn.util.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The spawn pipeline and per-tick lifecycle for damage numbers.
 *
 * <p>Everything that decides whether a number is shown lives here: filters, caps,
 * viewer selection, the player preference check, merging and the backend call. The
 * listener only classifies an event and hands it over.</p>
 *
 * <p>Main-thread only. State is plain collections rather than copy-on-write lists,
 * and per-player counts are maintained incrementally instead of being recounted with
 * an O(n) scan on every spawn (spec section 10).</p>
 */
public final class DamageService {

    private final ConfigManager config;
    private final DebugLogger debug;
    private final RenderBackend backend;
    private final EffectService effects;
    private final PlayerPreferences preferences;
    private final MergeRegistry mergeRegistry;
    private final StyleResolver styleResolver;

    private final List<FloatingDamage> active = new ArrayList<>();
    private final Map<UUID, Integer> perPlayerCounts = new HashMap<>();

    public DamageService(
            ConfigManager config,
            DebugLogger debug,
            RenderBackend backend,
            EffectService effects,
            PlayerPreferences preferences,
            MergeRegistry mergeRegistry,
            StyleResolver styleResolver
    ) {
        this.config = config;
        this.debug = debug;
        this.backend = backend;
        this.effects = effects;
        this.preferences = preferences;
        this.mergeRegistry = mergeRegistry;
        this.styleResolver = styleResolver == null ? StyleResolver.NONE : styleResolver;
    }

    // ------------------------------------------------------------------
    // Spawning
    // ------------------------------------------------------------------

    /** Displays reporting more than this are administrative (e.g. /kill) and are clamped. */
    public static final double MAX_RENDERED_DAMAGE = 1_000_000.0;

    /**
     * Renders a damage number for a victim.
     *
     * @param victim   the damaged entity; also the animation anchor
     * @param damage   final damage dealt
     * @param type     classified damage category
     * @param critical whether the hit was critical
     * @param attacker the responsible player, or {@code null} for environmental damage
     * @return whether a display was created or an existing one was grown
     */
    public boolean spawn(
            LivingEntity victim,
            double damage,
            DamageType type,
            boolean critical,
            Player attacker
    ) {

        if (!config.isEnabled() || victim == null || !victim.isValid()) {
            return false;
        }

        // /kill and similar administrative damage arrive as Float.MAX_VALUE; rendering
        // that verbatim would print "3.4E38" in front of the player.
        java.util.OptionalDouble sanitised = sanitiseDamage(damage);

        if (sanitised.isEmpty()) {
            return false;
        }

        damage = sanitised.getAsDouble();

        if (!isVictimSupported(victim)) {
            return false;
        }

        if (active.size() >= config.getMaxActiveDisplays()) {

            debug.debug("Display cap reached (%d); skipping %.1f damage on %s",
                    config.getMaxActiveDisplays(), damage, victim.getType());

            return false;
        }

        AnimationSettings animation = config.animation();

        boolean isCritical = critical && config.critical().enabled();

        String profile = attacker == null ? null : styleResolver.profileFor(attacker.getUniqueId());

        DamageType effectiveType = type == null ? DamageType.NORMAL : type;

        // A critical hit is formatted with styles.critical, falling back to its damage
        // category and then to normal if that style is switched off.
        StyleSettings style = resolveStyle(
                config,
                isCritical ? DamageType.CRITICAL : effectiveType,
                effectiveType,
                profile);

        UUID victimId = victim.getUniqueId();

        // ---- merge into an existing display when configured to ----
        MergeSettings merge = config.merge();

        FloatingDamage mergeable = mergeRegistry.find(
                victimId,
                attacker == null ? null : attacker.getUniqueId(),
                merge);

        if (mergeable != null && active.contains(mergeable)) {

            mergeInto(mergeable, damage, merge, profile, isCritical);

            debug.debug("Merged %.1f into display %d (total %.1f)",
                    damage, mergeable.getEntityId(), mergeable.getDamage());

            return true;
        }

        Location anchor = anchorFor(victim, animation);

        if (anchor.getWorld() == null) {
            return false;
        }

        List<Player> viewers = selectViewers(anchor);

        if (viewers.isEmpty()) {
            debug.debug("No viewers for %.1f damage on %s", damage, victim.getType());
            return false;
        }

        // API hook: other plugins may cancel the display or restyle it. Firing
        // here means only displays that are actually about to appear emit the
        // event; merging into an existing display does not.
        DamageNumberSpawnEvent spawnEvent = ApiEvents.fire(
                victim, attacker, effectiveType.styleKey(), isCritical, damage, anchor);

        if (spawnEvent == null || spawnEvent.isCancelled()) {

            if (spawnEvent != null) {
                debug.debug("Display on %s cancelled by an API listener", victim.getType());
            }

            return false;
        }

        java.util.OptionalDouble eventValue = sanitiseDamage(spawnEvent.getValue());

        if (eventValue.isEmpty()) {
            return false;
        }

        damage = eventValue.getAsDouble();
        style = ApiEvents.styledFrom(spawnEvent, style);

        int durationTicks = durationFor(victim, damage, animation);

        AnimationSettings displaySettings = isCritical
                ? animation.withCriticalScale(config.critical().scaleMultiplier())
                : animation;

        int entityId = newEntityId();

        FloatingDamage display = new FloatingDamage(
                entityId,
                victimId,
                anchor.getWorld().getUID(),
                anchor,
                damage,
                effectiveType,
                isCritical,
                durationTicks,
                TextRenderer.toJsonForDamage(damage, style),
                AnimationCurve.angleFor(entityId),
                animation.randomOffset() ? spread(animation) : 0.0,
                animation.randomOffset() ? spread(animation) : 0.0);

        display.setLastScale(1.0f);
        display.setLastOpacity((byte) -1);

        // Critical hits render larger for their whole life; the multiplier is applied
        // to every frame so the curve keeps interpolating between the configured
        // start and end scales.
        AnimationSettings frameSettings = isCritical
                ? animation.withCriticalScale(config.critical().scaleMultiplier())
                : animation;

        display.setScaleMultiplier(isCritical
                ? (float) config.critical().scaleMultiplier()
                : 1.0f);

        activate(display, viewers, frameSettings, true);

        mergeRegistry.record(
                display,
                victimId,
                attacker == null ? null : attacker.getUniqueId(),
                merge);

        debug.debug("Spawned %s display %d for %.1f damage (%d viewers, %d ticks, scale %.2f, victim health %.1f)",
                backend.id(), entityId, damage, viewers.size(), durationTicks,
                displaySettings.startScale(), victim.getHealth());

        return true;
    }

    /**
     * Spawns one display described by the API, already resolved onto concrete
     * style and animation settings.
     *
     * <p>Unlike the damage pipeline this path skips the victim filters, the
     * style profile lookup and hit merging: the caller asked for this exact
     * number, so the plugin renders exactly that. The
     * {@link DamageNumberSpawnEvent} still fires so listeners can restyle or
     * cancel API numbers like any other.</p>
     *
     * @param spawn the resolved request
     * @return whether a display was created
     */
    public boolean spawnCustom(CustomSpawn spawn) {

        if (spawn == null || !config.isEnabled()) {
            return false;
        }

        if (spawn.victim() == null && spawn.location() == null) {
            return false;
        }

        java.util.OptionalDouble sanitised = sanitiseDamage(spawn.value());

        if (sanitised.isEmpty()) {
            return false;
        }

        if (active.size() >= config.getMaxActiveDisplays()) {

            debug.debug("Display cap reached (%d); skipping a custom display",
                    config.getMaxActiveDisplays());

            return false;
        }

        AnimationSettings animation = spawn.animation();

        Location anchor = spawn.victim() != null
                ? anchorFor(spawn.victim(), animation)
                : spawn.location().clone();

        if (anchor.getWorld() == null) {
            return false;
        }

        List<Player> viewers = spawn.viewers() != null
                ? onlineViewers(spawn.viewers())
                : selectViewers(anchor);

        if (viewers.isEmpty()) {
            debug.debug("No viewers for a custom %.1f display", spawn.value());
            return false;
        }

        // API hook: consistent with pipeline spawns.
        DamageNumberSpawnEvent spawnEvent = ApiEvents.fire(
                spawn.victim(), spawn.attacker(), spawn.typeKey(), spawn.critical(),
                sanitised.getAsDouble(), anchor);

        if (spawnEvent == null || spawnEvent.isCancelled()) {
            return false;
        }

        java.util.OptionalDouble eventValue = sanitiseDamage(spawnEvent.getValue());

        if (eventValue.isEmpty()) {
            return false;
        }

        double value = eventValue.getAsDouble();
        StyleSettings style = ApiEvents.styledFrom(spawnEvent, spawn.style());

        boolean critical = spawn.critical();

        AnimationSettings frameSettings = critical
                ? animation.withCriticalScale(config.critical().scaleMultiplier())
                : animation;

        int entityId = newEntityId();

        FloatingDamage display = new FloatingDamage(
                entityId,
                spawn.victim() == null ? null : spawn.victim().getUniqueId(),
                anchor.getWorld().getUID(),
                anchor,
                value,
                spawn.type() == null ? DamageType.NORMAL : spawn.type(),
                critical,
                animation.durationTicks(),
                TextRenderer.toJsonForDamage(value, style),
                AnimationCurve.angleFor(entityId),
                animation.randomOffset() ? spread(animation) : 0.0,
                animation.randomOffset() ? spread(animation) : 0.0);

        display.setLastScale(1.0f);
        display.setLastOpacity((byte) -1);
        display.setScaleMultiplier(critical ? (float) config.critical().scaleMultiplier() : 1.0f);

        activate(display, viewers, frameSettings, !spawn.silent());

        debug.debug("Spawned custom %s display %d for %.1f (%d viewers, %d ticks)",
                backend.id(), entityId, value, viewers.size(), animation.durationTicks());

        return true;
    }

    /**
     * Finishes a spawn: applies the first animation frame, registers the display,
     * attaches viewers, sends the spawn packets and plays the configured effects.
     */
    private void activate(
            FloatingDamage display,
            List<Player> viewers,
            AnimationSettings frameSettings,
            boolean withEffects
    ) {

        AnimationFrame initialFrame = AnimationCurve.frame(0, frameSettings, display.getAngle());

        display.setLastScale(initialFrame.scale());
        display.setLastOpacity(initialFrame.opacityByte());

        active.add(display);

        for (Player viewer : viewers) {
            attach(display, viewer.getUniqueId());
        }

        backend.spawn(display, viewers);

        if (withEffects) {
            effects.particles(display, viewers);

            if (display.isCritical()) {
                effects.criticalSound(display, viewers);
            }
        }
    }

    private void mergeInto(
            FloatingDamage display,
            double damage,
            MergeSettings merge,
            String profile,
            boolean critical
    ) {

        double total = Math.min(merge.maxMergedDamage(), display.getDamage() + damage);

        display.setDamage(total);
        display.setTicksAlive(0);

        if (critical && !display.isCritical()) {
            display.setCritical(true);
        }

        StyleSettings style = resolveStyle(
                config,
                display.isCritical() ? DamageType.CRITICAL : display.getType(),
                display.getType(),
                profile);

        display.setJsonText(TextRenderer.toJsonForDamage(total, style));

        List<Player> viewers = viewersOf(display);

        if (viewers.isEmpty()) {
            return;
        }

        backend.retext(display, viewers);

        if (critical) {
            effects.particles(display, viewers);
            effects.criticalSound(display, viewers);
        }
    }

    // ------------------------------------------------------------------
    // Tick
    // ------------------------------------------------------------------

    /**
     * Advances every live display by one tick.
     *
     * @return how many displays were moved
     */
    public int tick() {

        AnimationSettings animation = config.animation();

        int moved = 0;

        Iterator<FloatingDamage> iterator = active.iterator();

        while (iterator.hasNext()) {

            FloatingDamage display = iterator.next();

            display.setTicksAlive(display.getTicksAlive() + 1);

            if (display.isExpired()) {
                destroy(display);
                iterator.remove();
                continue;
            }

            if (animation.followEntity()) {
                followEntity(display, animation);
            }

            pruneViewers(display, animation);

            if (display.getViewers().isEmpty()) {
                // Nobody is watching any more; drop it without sending packets.
                destroy(display);
                iterator.remove();
                continue;
            }

            AnimationFrame frame = AnimationCurve.frame(
                    display.getTicksAlive(), animation, display.getAngle());

            if (display.getScaleMultiplier() != 1.0f) {
                frame = frame.scaled(display.getScaleMultiplier());
            }

            display.setAnchor(applyFrame(display, frame));

            backend.move(display, frame, viewersOf(display));
            moved++;
        }

        mergeRegistry.prune(Math.max(1_000L, config.merge().windowMs() * 4L));

        return moved;
    }

    /** Recomputes the anchor from the live entity, smoothed toward the previous one. */
    private void followEntity(FloatingDamage display, AnimationSettings animation) {

        UUID victimId = display.getVictimId();

        if (victimId == null) {
            return;
        }

        Entity entity = Bukkit.getEntity(victimId);

        if (entity == null || entity.isDead() || !entity.isValid()) {
            // The entity is gone: freeze the number at its last known position.
            return;
        }

        World world = entity.getWorld();

        if (world == null || !world.getUID().equals(display.getWorldId())) {
            return;
        }

        Location anchor = display.getAnchor();

        double targetX = entity.getLocation().getX() + animation.positionX() + display.getSpawnOffsetX();
        double targetY = entity.getLocation().getY() + animation.anchorHeight();
        double targetZ = entity.getLocation().getZ() + animation.positionZ() + display.getSpawnOffsetZ();

        double smoothing = animation.followSmoothing();

        if (smoothing <= 0.0 || anchor.getWorld() == null
                || !anchor.getWorld().getUID().equals(display.getWorldId())) {

            display.setAnchor(new Location(world, targetX, targetY, targetZ, 0.0f, 0.0f));
            return;
        }

        double factor = 1.0 - smoothing;

        display.setAnchor(new Location(
                world,
                anchor.getX() + (targetX - anchor.getX()) * factor,
                anchor.getY() + (targetY - anchor.getY()) * factor,
                anchor.getZ() + (targetZ - anchor.getZ()) * factor,
                0.0f,
                0.0f));
    }

    /** Applies a frame's animation offsets on top of the current anchor. */
    private Location applyFrame(FloatingDamage display, AnimationFrame frame) {

        Location anchor = display.getAnchor();

        return new Location(
                anchor.getWorld(),
                anchor.getX() + frame.offsetX(),
                anchor.getY() + frame.offsetY(),
                anchor.getZ() + frame.offsetZ(),
                anchor.getYaw(),
                anchor.getPitch());
    }

    /** Detaches viewers that went offline, changed world or moved out of range. */
    private void pruneViewers(FloatingDamage display, AnimationSettings animation) {

        if (!config.isDistanceCulling()) {
            return;
        }

        double maxDistanceSq = (double) config.getViewDistance() * config.getViewDistance();
        Location location = display.getAnchor();
        World world = location.getWorld();

        for (UUID viewerId : List.copyOf(display.getViewers())) {

            Player viewer = Bukkit.getPlayer(viewerId);

            if (viewer == null || !viewer.isOnline()) {
                detach(display, viewerId, false);
                continue;
            }

            if (world == null || viewer.getWorld() != world
                    || viewer.getLocation().distanceSquared(location) > maxDistanceSq) {

                detach(display, viewerId, true);
            }
        }
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    /** Destroys every display for every viewer. */
    public void clear() {

        for (FloatingDamage display : active) {

            if (display.getViewers().isEmpty()) {
                continue;
            }

            backend.destroy(display, viewersOf(display));
        }

        for (FloatingDamage display : active) {
            releaseViewers(display);
        }

        active.clear();
        mergeRegistry.clear();
        perPlayerCounts.clear();
    }

    /** Destroys the displays a specific player can see, leaving them up for others. */
    public int clearFor(UUID playerId) {

        int affected = 0;

        Player viewer = Bukkit.getPlayer(playerId);

        for (FloatingDamage display : active) {

            if (!display.getViewers().contains(playerId)) {
                continue;
            }

            if (viewer != null && viewer.isOnline()) {
                backend.destroy(display, List.of(viewer));
            }

            detach(display, playerId, false);
            affected++;
        }

        return affected;
    }

    /** Called when a player disconnects: they must not be counted as a viewer. */
    public void onQuit(UUID playerId) {

        for (FloatingDamage display : active) {

            if (display.getViewers().contains(playerId)) {
                detach(display, playerId, false);
            }
        }
    }

    /**
     * Drops every display anchored in a world that is no longer loaded.
     *
     * @return how many displays were removed
     */
    public int clearWorld(UUID worldId) {

        if (worldId == null) {
            return 0;
        }

        int removed = 0;

        Iterator<FloatingDamage> iterator = active.iterator();

        while (iterator.hasNext()) {

            FloatingDamage display = iterator.next();

            if (!worldId.equals(display.getWorldId())) {
                continue;
            }

            destroy(display);
            iterator.remove();
            removed++;
        }

        return removed;
    }

    private void destroy(FloatingDamage display) {

        List<Player> viewers = viewersOf(display);

        if (!viewers.isEmpty()) {
            backend.destroy(display, viewers);
        }

        releaseViewers(display);
        mergeRegistry.forget(display);
    }

    // ------------------------------------------------------------------
    // Viewer bookkeeping
    // ------------------------------------------------------------------

    private void attach(FloatingDamage display, UUID viewerId) {

        if (display.getViewers().add(viewerId)) {
            perPlayerCounts.merge(viewerId, 1, Integer::sum);
        }
    }

    private void detach(FloatingDamage display, UUID viewerId, boolean sendDestroy) {

        if (sendDestroy) {

            Player viewer = Bukkit.getPlayer(viewerId);

            if (viewer != null && viewer.isOnline()) {
                backend.destroy(display, List.of(viewer));
            }
        }

        if (display.getViewers().remove(viewerId)) {
            perPlayerCounts.computeIfPresent(viewerId, (key, count) -> count <= 1 ? null : count - 1);
        }
    }

    private void releaseViewers(FloatingDamage display) {

        for (UUID viewerId : List.copyOf(display.getViewers())) {
            display.getViewers().remove(viewerId);
            perPlayerCounts.computeIfPresent(viewerId, (key, count) -> count <= 1 ? null : count - 1);
        }
    }

    private List<Player> viewersOf(FloatingDamage display) {

        if (display.getViewers().isEmpty()) {
            return List.of();
        }

        List<Player> viewers = new ArrayList<>(display.getViewers().size());

        for (UUID viewerId : display.getViewers()) {

            Player viewer = Bukkit.getPlayer(viewerId);

            if (viewer != null && viewer.isOnline()) {
                viewers.add(viewer);
            }
        }

        return viewers;
    }

    /**
     * Filters a forced API viewer set down to players that can actually receive
     * packets: online and still opted in. Explicitly named viewers bypass the
     * nearby-selection and per-player caps.
     */
    private List<Player> onlineViewers(Set<Player> requested) {

        List<Player> viewers = new ArrayList<>(requested.size());

        for (Player player : requested) {

            if (player == null || !player.isOnline()) {
                continue;
            }

            if (!preferences.isEnabled(player.getUniqueId())) {
                continue;
            }

            viewers.add(player);
        }

        return viewers;
    }

    private List<Player> selectViewers(Location location) {

        World world = location.getWorld();

        if (world == null) {
            return List.of();
        }

        boolean nearbyOnly = config.isNearbyViewersOnly();
        double maxDistanceSq = (double) config.getViewDistance() * config.getViewDistance();
        int maxPerPlayer = config.getMaxPerPlayer();
        boolean requirePermission = config.isRequireViewPermission();
        String viewPermission = config.getViewPermission();

        List<Player> viewers = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {

            if (player.getWorld() != world) {
                continue;
            }

            if (nearbyOnly && player.getLocation().distanceSquared(location) > maxDistanceSq) {
                continue;
            }

            // Player-side opt-out: the player has switched damage numbers off.
            if (!preferences.isEnabled(player.getUniqueId())) {
                continue;
            }

            if (requirePermission && !player.hasPermission(viewPermission)) {
                continue;
            }

            if (countFor(player.getUniqueId()) >= maxPerPlayer) {
                continue;
            }

            viewers.add(player);
        }

        return viewers;
    }

    // ------------------------------------------------------------------
    // Filters and helpers
    // ------------------------------------------------------------------

    /** The victim-side filters from {@code general.*}. */
    public boolean isVictimSupported(LivingEntity victim) {

        if (!(victim instanceof Player) && !(victim instanceof ArmorStand) && !victim.isValid()) {
            return false;
        }

        if (config.isIgnoreArmorStands() && victim instanceof ArmorStand) {
            return false;
        }

        if (config.isIgnoreNpcs() && victim.hasMetadata("NPC")) {
            return false;
        }

        if (config.isIgnoreInvisibleEntities() && victim.isInvisible()) {
            return false;
        }

        if (isDisabledWorld(victim.getWorld())) {
            return false;
        }

        if (victim instanceof Player) {
            return config.isPlayers();
        }

        if (isAnimal(victim)) {
            return config.isAnimals();
        }

        return config.isMobs();
    }

    private boolean isDisabledWorld(World world) {

        if (world == null) {
            return true;
        }

        String name = world.getName().toLowerCase(Locale.ROOT);

        for (String disabled : config.getDisabledWorlds()) {

            if (disabled != null && disabled.toLowerCase(Locale.ROOT).equals(name)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isAnimal(LivingEntity entity) {
        return entity instanceof org.bukkit.entity.Animals
                || entity instanceof org.bukkit.entity.WaterMob
                || entity instanceof org.bukkit.entity.Golem;
    }

    private static double spread(AnimationSettings animation) {
        return ThreadLocalRandom.current().nextDouble(
                -animation.spawnSpread(), animation.spawnSpread());
    }

    private static Location anchorFor(LivingEntity victim, AnimationSettings animation) {

        Location base = victim.getLocation().clone();

        return base.add(animation.positionX(), animation.anchorHeight(), animation.positionZ());
    }

    /** Killing blows flash briefly when {@code advanced.remove-on-death} is on. */
    private int durationFor(LivingEntity victim, double damage, AnimationSettings animation) {

        if (!config.isRemoveOnDeath()) {
            return animation.durationTicks();
        }

        boolean fatal = damage >= victim.getHealth();

        return fatal ? Math.min(2, animation.durationTicks()) : animation.durationTicks();
    }

    /**
     * Resolves the style for a display, preferring a critical style for critical hits and
     * falling back to the damage category and finally to normal, so a disabled style
     * still renders something readable.
     */
    public static StyleSettings resolveStyle(
            ConfigManager config,
            DamageType preferred,
            DamageType fallback,
            String profile
    ) {

        StyleSettings style = config.styleFor(preferred, profile);

        if (style.enabled()) {
            return style;
        }

        if (fallback != null && fallback != preferred) {

            style = config.styleFor(fallback, profile);

            if (style.enabled()) {
                return style;
            }
        }

        return config.styleFor(DamageType.NORMAL, profile);
    }

    private static int newEntityId() {
        return io.github.retrooper.packetevents.util.SpigotReflectionUtil.generateEntityId();
    }

    /**
     * Validates a damage value before it is rendered.
     *
     * @return the value to display, or empty when the event should be ignored
     */
    public static java.util.OptionalDouble sanitiseDamage(double damage) {

        if (!Double.isFinite(damage) || damage <= 0.0) {
            return java.util.OptionalDouble.empty();
        }

        return java.util.OptionalDouble.of(Math.min(damage, MAX_RENDERED_DAMAGE));
    }

    // ------------------------------------------------------------------
    // Introspection (used by /cdn stats and the tests)
    // ------------------------------------------------------------------

    /** Live displays. */
    public List<FloatingDamage> active() {
        return active;
    }

    public int activeCount() {
        return active.size();
    }

    /** Live displays a player currently receives. */
    public int countFor(UUID playerId) {

        Integer count = perPlayerCounts.get(playerId);

        return count == null ? 0 : count;
    }

    public int mergeEntries() {
        return mergeRegistry.size();
    }

    public int viewerSlotTotal() {

        int total = 0;

        for (Integer count : perPlayerCounts.values()) {
            total += count;
        }

        return total;
    }

    public RenderBackend backend() {
        return backend;
    }
}

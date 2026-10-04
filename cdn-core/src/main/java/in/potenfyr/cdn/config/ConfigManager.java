package in.potenfyr.cdn.config;

import in.potenfyr.cdn.damage.DamageType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads and validates every key in config.yml.
 *
 * <p>Nothing else in the plugin touches a raw config path: components ask this
 * class for a typed value, so a renamed or missing key is a one-line change and a
 * new option cannot be silently ignored (the 0.2.0 failure mode this release
 * fixes).</p>
 *
 * <p>Every animation/style value is a parsed immutable record, rebuilt on reload,
 * so the hot path never re-reads YAML.</p>
 */
public class ConfigManager {

    public static final String DEFAULT_VIEW_PERMISSION = "cdn.view";
    public static final String DEFAULT_SOUND = "ENTITY_PLAYER_ATTACK_CRIT";
    public static final int DEFAULT_DEBUG_LOG_LIMIT = 256;

    private final JavaPlugin plugin;

    private FileConfiguration config;

    private volatile AnimationSettings animation = AnimationSettings.defaults();
    private volatile Map<String, StyleSettings> styles = new HashMap<>();
    private volatile Map<String, Map<String, StyleSettings>> styleProfiles = new HashMap<>();
    private volatile Map<String, ParticleSettings> particles = new HashMap<>();
    private volatile CriticalSettings critical =
            new CriticalSettings(true, 1.5, true, DEFAULT_SOUND, 1.0f, 1.2f);
    private volatile MergeSettings merge = MergeSettings.disabled();

    /** Live configuration, bound to the plugin's config.yml. */
    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        rebuild();
    }

    /** Test constructor: work against an in-memory configuration. */
    public ConfigManager(FileConfiguration config) {
        this.plugin = null;
        this.config = config;
        rebuild();
    }

    /** Re-reads config.yml from disk and rebuilds every derived value. */
    public void reload() {

        if (plugin != null) {
            plugin.reloadConfig();
            this.config = plugin.getConfig();
        }

        rebuild();
    }

    private void rebuild() {

        animation = readAnimation(config.getConfigurationSection("animation"));

        styles = readStyles(config.getConfigurationSection("styles"));
        styleProfiles = readProfiles(config.getConfigurationSection("style-profiles"));
        particles = readParticles(config.getConfigurationSection("particles"));

        critical = readCritical(config.getConfigurationSection("critical-hits"));
        merge = readMerge(config.getConfigurationSection("merge-system"));
    }

    public FileConfiguration raw() {
        return config;
    }

    // ------------------------------------------------------------------
    // Derived values
    // ------------------------------------------------------------------

    /** Animation settings, already clamped and immutable. */
    public AnimationSettings animation() {
        return animation;
    }

    /** Style for a damage type, honouring the player's style profile. */
    public StyleSettings styleFor(DamageType type, String profile) {

        String key = type.styleKey();
        StyleSettings base = styles.getOrDefault(key, StyleSettings.fallback(key));

        if (profile == null || profile.isBlank() || "default".equalsIgnoreCase(profile)) {
            return base;
        }

        Map<String, StyleSettings> profileStyles =
                styleProfiles.get(profile.toLowerCase(Locale.ROOT));

        if (profileStyles == null) {
            return base;
        }

        return profileStyles.getOrDefault(key, base);
    }

    /** Style for a damage type using the default profile. */
    public StyleSettings styleFor(DamageType type) {
        return styleFor(type, null);
    }

    public Map<String, StyleSettings> styles() {
        return styles;
    }

    public Map<String, Map<String, StyleSettings>> styleProfiles() {
        return styleProfiles;
    }

    public ParticleSettings particleFor(DamageType type) {

        String key = type == DamageType.CRITICAL ? "critical" : type.styleKey();

        return particles.getOrDefault(key, ParticleSettings.disabled());
    }

    public CriticalSettings critical() {
        return critical;
    }

    public MergeSettings merge() {
        return merge;
    }

    // ------------------------------------------------------------------
    // general
    // ------------------------------------------------------------------

    public boolean isEnabled() {
        return config.getBoolean("general.enabled", true);
    }

    public boolean isDebug() {
        return config.getBoolean("general.debug", false);
    }

    /** Whether the ASCII startup banner is printed to the console. */
    public boolean isBannerEnabled() {
        return config.getBoolean("general.banner", true);
    }

    public int getViewDistance() {
        return clamp(config.getInt("general.view-distance", 32), 1, 512);
    }

    public List<String> getDisabledWorlds() {

        List<String> worlds = config.getStringList("general.disabled-worlds");

        return worlds == null ? List.of() : worlds;
    }

    public boolean isIgnoreInvisibleEntities() {
        return config.getBoolean("general.ignore-invisible-entities", true);
    }

    public boolean isIgnoreArmorStands() {
        return config.getBoolean("general.ignore-armor-stands", true);
    }

    public boolean isIgnoreNpcs() {
        return config.getBoolean("general.ignore-npcs", true);
    }

    public boolean isMobs() {
        return config.getBoolean("general.entities.mobs", config.getBoolean("general.mobs", true));
    }

    public boolean isPlayers() {
        return config.getBoolean("general.entities.players", config.getBoolean("general.players", true));
    }

    public boolean isAnimals() {
        return config.getBoolean("general.entities.animals", config.getBoolean("general.animals", true));
    }

    public boolean isSelfDamage() {
        return config.getBoolean("general.self-damage", true);
    }

    public boolean isNearbyViewersOnly() {
        return config.getBoolean("general.nearby-viewers-only", true);
    }

    public int getMaxActiveDisplays() {
        return clamp(config.getInt("general.max-active-displays", 2000), 1, 100_000);
    }

    public boolean isCleanupOrphans() {
        return config.getBoolean("general.cleanup-orphans", true);
    }

    /** Default state of the player-side toggle for players with no stored preference. */
    public boolean isDefaultViewEnabled() {
        return config.getBoolean("general.default-view-enabled", true);
    }

    // ------------------------------------------------------------------
    // performance / permissions / integrations / advanced
    // ------------------------------------------------------------------

    public int getAnimationInterval() {
        return clamp(config.getInt("performance.animation-interval", 1), 1, 20);
    }

    public int getMaxPerPlayer() {
        return clamp(config.getInt("performance.max-per-player", 50), 1, 50_000);
    }

    public boolean isDistanceCulling() {
        return config.getBoolean("performance.distance-culling", true);
    }

    public boolean isRequireViewPermission() {
        return config.getBoolean("permissions.require-view-permission", false);
    }

    public String getViewPermission() {

        String permission = config.getString("permissions.view-permission", DEFAULT_VIEW_PERMISSION);

        return permission == null || permission.isBlank() ? DEFAULT_VIEW_PERMISSION : permission.trim();
    }

    public boolean areStylePermissionsEnabled() {
        return config.getBoolean("permissions.style-permissions", true);
    }

    public boolean isLuckPermsEnabled() {
        return config.getBoolean("integrations.luckperms", true);
    }

    public boolean isPlaceholderApiEnabled() {
        return config.getBoolean("integrations.placeholderapi", true);
    }

    public boolean isMetricsEnabled() {
        return config.getBoolean("advanced.metrics", false);
    }

    /** The operator's bStats plugin id; {@code 0} means metrics are not reported. */
    public int getMetricsId() {
        return Math.max(0, config.getInt("advanced.metrics-id", 0));
    }

    public boolean isRemoveOnDeath() {
        return config.getBoolean("advanced.remove-on-death", true);
    }

    public boolean isIgnoreZeroDamage() {
        return config.getBoolean("advanced.ignore-zero-damage", true);
    }

    public boolean isPacketDebug() {
        return config.getBoolean("advanced.packet-debug", false);
    }

    public int getDebugLogLimit() {
        return clamp(config.getInt("advanced.debug-log-limit", DEFAULT_DEBUG_LOG_LIMIT), 16, 4096);
    }

    /**
     * {@code advanced.folia-mode}: {@code auto} (default) detects Folia, while
     * {@code true}/{@code false} force the scheduler choice.
     *
     * @return {@code null} when detection should decide
     */
    public Boolean getFoliaModeOverride() {

        String value = config.getString("advanced.folia-mode", "auto");

        if (value == null) {
            return null;
        }

        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "true" -> Boolean.TRUE;
            case "false" -> Boolean.FALSE;
            default -> null;
        };
    }

    // ------------------------------------------------------------------
    // Pure readers (unit-tested without a server)
    // ------------------------------------------------------------------

    public static AnimationSettings readAnimation(ConfigurationSection section) {

        AnimationSettings defaults = AnimationSettings.defaults();

        if (section == null) {
            return defaults;
        }

        return new AnimationSettings(
                section.getInt("duration-ticks", defaults.durationTicks()),
                section.getInt("rise-ticks", defaults.riseTicks()),
                section.getDouble("vertical-speed", defaults.verticalSpeed()),
                section.getDouble("horizontal-randomness", defaults.horizontalRandomness()),
                section.getBoolean("scale-animation", defaults.scaleAnimation()),
                section.getDouble("start-scale", defaults.startScale()),
                section.getDouble("end-scale", defaults.endScale()),
                section.getBoolean("fade-out", defaults.fadeOut()),
                section.getDouble("fade-start", defaults.fadeStart()),
                section.getBoolean("bounce", defaults.bounce()),
                section.getDouble("bounce-strength", defaults.bounceStrength()),
                section.getBoolean("rotation", defaults.rotation()),
                section.getDouble("rotation-speed", defaults.rotationSpeed()),
                section.getBoolean("random-offset", defaults.randomOffset()),
                section.getDouble("spawn-spread", defaults.horizontalRandomness()),
                section.getDouble("anchor-height", defaults.anchorHeight()),
                section.getBoolean("follow-entity", defaults.followEntity()),
                section.getDouble("follow-smoothing", defaults.followSmoothing()),
                section.getDouble("critical-scale-multiplier", defaults.criticalScaleMultiplier()));
    }

    public static Map<String, StyleSettings> readStyles(ConfigurationSection section) {

        Map<String, StyleSettings> result = new HashMap<>();

        for (DamageType type : DamageType.values()) {

            String key = type.styleKey();

            result.put(key, readStyle(
                    section == null ? null : section.getConfigurationSection(key), key));
        }

        return result;
    }

    public static StyleSettings readStyle(ConfigurationSection section, String key) {

        if (section == null) {
            return StyleSettings.fallback(key);
        }

        StyleSettings fallback = StyleSettings.fallback(key);

        return new StyleSettings(
                key,
                section.getBoolean("enabled", fallback.enabled()),
                section.getString("format", fallback.format()),
                section.getString("color", fallback.color()),
                section.getBoolean("bold", fallback.bold()),
                section.getBoolean("italic", fallback.italic()),
                section.getBoolean("shadow", fallback.shadow()));
    }

    public static Map<String, Map<String, StyleSettings>> readProfiles(ConfigurationSection section) {

        Map<String, Map<String, StyleSettings>> result = new HashMap<>();

        if (section == null) {
            return result;
        }

        for (String profile : section.getKeys(false)) {

            ConfigurationSection profileSection = section.getConfigurationSection(profile);

            if (profileSection == null) {
                continue;
            }

            Map<String, StyleSettings> profileStyles = new HashMap<>();

            for (String typeKey : profileSection.getKeys(false)) {

                profileStyles.put(
                        typeKey.toLowerCase(Locale.ROOT),
                        readStyle(profileSection.getConfigurationSection(typeKey), typeKey));
            }

            result.put(profile.toLowerCase(Locale.ROOT), profileStyles);
        }

        return result;
    }

    public static Map<String, ParticleSettings> readParticles(ConfigurationSection section) {

        Map<String, ParticleSettings> result = new HashMap<>();

        if (section == null || !section.getBoolean("enabled", true)) {
            return result;
        }

        for (String key : section.getKeys(false)) {

            ConfigurationSection child = section.getConfigurationSection(key);

            if (child == null) {
                continue;
            }

            result.put(key.toLowerCase(Locale.ROOT), new ParticleSettings(
                    child.getBoolean("enabled", true),
                    child.getString("particle", "CRIT"),
                    child.getInt("amount", 5)));
        }

        return result;
    }

    public static CriticalSettings readCritical(ConfigurationSection section) {

        if (section == null) {
            return new CriticalSettings(true, 1.5, true, DEFAULT_SOUND, 1.0f, 1.2f);
        }

        return new CriticalSettings(
                section.getBoolean("enabled", true),
                section.getDouble("scale-multiplier", 1.5),
                section.getBoolean("sound", true),
                section.getString("sound-type", DEFAULT_SOUND),
                (float) section.getDouble("volume", 1.0),
                (float) section.getDouble("pitch", 1.2));
    }

    public static MergeSettings readMerge(ConfigurationSection section) {

        if (section == null) {
            return MergeSettings.disabled();
        }

        return new MergeSettings(
                section.getBoolean("enabled", true),
                section.getLong("merge-window-ms", 150L),
                section.getDouble("max-merged-damage", 9999.0),
                section.getBoolean("same-attacker-only", true));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

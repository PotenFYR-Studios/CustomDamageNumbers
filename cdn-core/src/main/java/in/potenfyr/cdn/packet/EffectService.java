package in.potenfyr.cdn.packet;

import com.github.retrooper.packetevents.protocol.particle.Particle;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleType;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import in.potenfyr.cdn.config.ConfigManager;
import in.potenfyr.cdn.config.CriticalSettings;
import in.potenfyr.cdn.config.ParticleSettings;
import in.potenfyr.cdn.damage.FloatingDamage;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Particle bursts and critical-hit sounds.
 *
 * <p>Both are sent as packets rather than through the Bukkit world API: Bukkit's
 * particle/sound enums were renamed repeatedly between 1.16 and 26.x, while
 * PacketEvents resolves them per viewer protocol. That keeps one code path valid on
 * every supported server.</p>
 *
 * <p>Name normalisation is a pure function so the alias handling is unit-testable
 * without a server.</p>
 */
public final class EffectService {

    private static final Map<String, String> LEGACY_PARTICLE_ALIASES = Map.ofEntries(
            Map.entry("SPELL_MOB", "ENTITY_EFFECT"),
            Map.entry("SPELL_MOB_AMBIENT", "ENTITY_EFFECT"),
            Map.entry("SPELL_WITCH", "WITCH"),
            Map.entry("SPELL_INSTANT", "INSTANT_EFFECT"),
            Map.entry("CRIT_MAGIC", "ENCHANTED_HIT"),
            Map.entry("EXPLOSION_NORMAL", "POOF"),
            Map.entry("EXPLOSION_LARGE", "EXPLOSION"),
            Map.entry("EXPLOSION_HUGE", "EXPLOSION_EMITTER"),
            Map.entry("VILLAGER_HAPPY", "HAPPY_VILLAGER"),
            Map.entry("REDSTONE", "DUST"),
            Map.entry("SMOKE_NORMAL", "SMOKE"),
            Map.entry("SMOKE_LARGE", "LARGE_SMOKE"),
            Map.entry("SLIME", "ITEM_SLIME"),
            Map.entry("SNOWBALL", "ITEM_SNOWBALL"),
            Map.entry("ENCHANTMENT_TABLE", "ENCHANT"),
            Map.entry("WATER_BUBBLE", "BUBBLE"),
            Map.entry("WATER_SPLASH", "SPLASH"),
            Map.entry("WATER_WAKE", "FISHING"),
            Map.entry("TOTEM", "TOTEM_OF_UNDYING"),
            Map.entry("SPELL", "EFFECT"),
            Map.entry("MOB_APPEARANCE", "ELDER_GUARDIAN"),
            Map.entry("DRIP_LAVA", "DRIPPING_LAVA"),
            Map.entry("DRIP_WATER", "DRIPPING_WATER"),
            Map.entry("TOWN_AURA", "MYCELIUM"),
            Map.entry("SUSPENDED", "UNDERWATER"));

    private final ConfigManager config;

    /** Resolved particle handles, keyed by canonical name. */
    private final Map<String, Particle<?>> particleCache = new HashMap<>();

    public EffectService(ConfigManager config) {
        this.config = config;
    }

    /** Sends the configured particle burst for a display. */
    public void particles(FloatingDamage display, Collection<Player> viewers) {

        ParticleSettings settings = config.particleFor(display.getType());

        if (!settings.enabled() || settings.amount() <= 0 || viewers.isEmpty()) {
            return;
        }

        Particle<?> particle = resolveParticle(settings.particle());

        if (particle == null) {
            return;
        }

        Location anchor = display.getAnchor();

        Vector3d position = new Vector3d(anchor.getX(), anchor.getY(), anchor.getZ());
        Vector3f offset = new Vector3f(0.3f, 0.3f, 0.3f);

        for (Player viewer : viewers) {
            PacketUtil.particle(viewer, particle, position, offset, 0.1f, settings.amount());
        }
    }

    /** Plays the configured critical-hit sound to the display's viewers. */
    public void criticalSound(FloatingDamage display, Collection<Player> viewers) {

        CriticalSettings critical = config.critical();

        if (!critical.enabled() || !critical.sound() || viewers.isEmpty()) {
            return;
        }

        Location anchor = display.getAnchor();

        Vector3d position = new Vector3d(anchor.getX(), anchor.getY(), anchor.getZ());
        String sound = normaliseSound(critical.soundType());

        for (Player viewer : viewers) {
            PacketUtil.sound(viewer, sound, position, critical.volume(), critical.pitch());
        }
    }

    /** Resolves a configured particle name to a PacketEvents handle, with caching. */
    public Particle<?> resolveParticle(String configured) {

        String canonical = canonicalParticleName(configured);

        return particleCache.computeIfAbsent(canonical, name -> {
            try {

                ParticleType<?> type = ParticleTypes.getByName(name);

                return type == null ? null : toParticle(type);

            } catch (RuntimeException unknown) {
                return null;
            }
        });
    }

    /** PacketEvents wraps a particle type in a {@link Particle}; the cast is erased. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Particle<?> toParticle(ParticleType<?> type) {
        return new Particle(type);
    }

    /**
     * Canonicalises a configured particle name: strips the namespace, upper-cases,
     * converts dots/hyphens to underscores, and maps Bukkit's pre-1.20 names onto
     * the modern registry names.
     */
    public static String canonicalParticleName(String configured) {

        if (configured == null || configured.isBlank()) {
            return "CRIT";
        }

        String name = configured.trim().toUpperCase(Locale.ROOT);

        if (name.startsWith("MINECRAFT:")) {
            name = name.substring("MINECRAFT:".length());
        }

        name = name.replace('.', '_').replace('-', '_');

        // Strip the "PARTICLE." prefix Bukkit's Particle enum added in 1.20.5.
        if (name.startsWith("PARTICLE_")) {
            name = name.substring("PARTICLE_".length());
        }

        if (name.endsWith("_PARTICLE")) {
            name = name.substring(0, name.length() - "_PARTICLE".length());
        }

        return LEGACY_PARTICLE_ALIASES.getOrDefault(name, name);
    }

    /** Canonicalises a configured sound key into a vanilla sound name. */
    public static String normaliseSound(String configured) {

        if (configured == null || configured.isBlank()) {
            return ConfigManager.DEFAULT_SOUND;
        }

        String name = configured.trim().toUpperCase(Locale.ROOT);

        if (name.startsWith("MINECRAFT:")) {
            name = name.substring("MINECRAFT:".length());
        }

        return name.replace('.', '_').replace('-', '_');
    }
}

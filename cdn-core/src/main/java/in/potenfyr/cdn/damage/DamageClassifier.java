package in.potenfyr.cdn.damage;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;

/**
 * Maps a Bukkit damage event onto a {@link DamageType}, and decides whether a hit
 * is critical.
 *
 * <p>We compile against the 1.16.5 API, where neither
 * {@code EntityDamageEvent#getDamageSource()} nor
 * {@code EntityDamageByEntityEvent#isCritical()} exists. Both are reached by
 * reflection and both have a documented fallback, so the same jar classifies damage
 * on 1.16 and on 26.x (spec section 12).</p>
 */
public final class DamageClassifier {

    /** Cached reflective handles; resolved at most once per JVM. */
    private static volatile boolean modernResolved;

    private static Method getDamageSource;
    private static Method getDamageType;
    private static Method getKey;
    private static Method keyValue;
    private static Method isCritical;

    private DamageClassifier() {
    }

    /**
     * @param event the damage event (any subtype)
     * @return the damage category to render
     */
    public static DamageType classify(EntityDamageEvent event) {

        Optional<String> key = modernDamageKey(event);

        if (key.isPresent()) {

            DamageType modern = fromKey(key.get());

            if (modern != null) {
                return modern;
            }
        }

        return fromCause(event.getCause());
    }

    /**
     * @return whether the hit is a critical hit, using the modern API when present
     *         and falling back to the attacker's fall distance otherwise
     */
    public static boolean isCritical(EntityDamageEvent event, Entity damager) {

        resolve();

        if (isCritical != null && event instanceof EntityDamageByEntityEvent) {

            try {

                Object result = isCritical.invoke(event);

                if (result instanceof Boolean critical) {
                    return critical;
                }

            } catch (ReflectiveOperationException ignored) {
                // Fall through to the heuristic.
            }
        }

        return fallDistanceHeuristic(damager);
    }

    /**
     * The pre-1.20 behaviour: a jumping/falling attacker landing a hit is treated as
     * a critical strike. Mojang's own rule is "falling and not on ground".
     */
    public static boolean fallDistanceHeuristic(Entity damager) {

        if (damager == null) {
            return false;
        }

        return damager.getFallDistance() > 0.0f && !damager.isOnGround();
    }

    /** The vanilla damage type key, e.g. {@code "minecraft:magic"}, when reachable. */
    public static Optional<String> modernDamageKey(EntityDamageEvent event) {

        resolve();

        if (getDamageSource == null || getDamageType == null || getKey == null) {
            return Optional.empty();
        }

        try {

            Object source = getDamageSource.invoke(event);

            if (source == null) {
                return Optional.empty();
            }

            Object damageType = getDamageType.invoke(source);

            if (damageType == null) {
                return Optional.empty();
            }

            Object namespacedKey = getKey.invoke(damageType);

            if (namespacedKey == null) {
                return Optional.empty();
            }

            Object value = keyValue != null
                    ? keyValue.invoke(namespacedKey)
                    : namespacedKey.toString();

            return value == null ? Optional.empty() : Optional.of(value.toString());

        } catch (ReflectiveOperationException | RuntimeException unavailable) {
            return Optional.empty();
        }
    }

    /** Maps a vanilla damage-type key onto our category, or {@code null} if unknown. */
    public static DamageType fromKey(String rawKey) {

        if (rawKey == null) {
            return null;
        }

        String key = rawKey.toLowerCase(Locale.ROOT);

        if (key.startsWith("minecraft:")) {
            key = key.substring("minecraft:".length());
        }

        return switch (key) {

            case "lava", "in_fire", "on_fire", "hot_floor", "fireball",
                 "campfire", "unattended_campfire" -> DamageType.FIRE;

            case "magic", "indirect_magic", "sonic_boom", "dragon_breath",
                 "wither", "thorns" -> DamageType.MAGIC;

            case "poison" -> DamageType.POISON;

            case "explosion", "player_explosion", "fireworks", "bad_respawn_point" -> DamageType.EXPLOSION;

            case "fall", "falling_block", "fly_into_wall", "stalagmite", "ender_pearl" -> DamageType.FALL;

            case "heal" -> DamageType.HEALING;

            default -> null;
        };
    }

    /**
     * Maps a {@link EntityDamageEvent.DamageCause} onto our category.
     *
     * <p>Deliberately name-based rather than a switch over enum constants: we compile
     * against the 1.16.5 API, where causes added later (STALAGMITE, SONIC_BOOM, ...)
     * do not exist, and a name mapping keeps working on every version without a
     * recompile or a reflection hop.</p>
     */
    public static DamageType fromCause(EntityDamageEvent.DamageCause cause) {
        return cause == null ? DamageType.NORMAL : fromCauseName(cause.name());
    }

    /** Name-based damage-cause mapping; unit-tested without a server. */
    public static DamageType fromCauseName(String rawCause) {

        if (rawCause == null) {
            return DamageType.NORMAL;
        }

        return switch (rawCause.toUpperCase(Locale.ROOT)) {

            case "FIRE", "FIRE_TICK", "LAVA", "HOT_FLOOR", "CAMPFIRE",
                 "MELTING" -> DamageType.FIRE;

            case "MAGIC", "SONIC_BOOM", "DRAGON_BREATH", "WITHER", "THORNS" -> DamageType.MAGIC;

            case "POISON" -> DamageType.POISON;

            case "ENTITY_EXPLOSION", "BLOCK_EXPLOSION" -> DamageType.EXPLOSION;

            case "FALL", "FLY_INTO_WALL", "FALLING_BLOCK", "STALAGMITE" -> DamageType.FALL;

            default -> DamageType.NORMAL;
        };
    }

    /** True when the victim is one the config says to ignore. */
    public static boolean isSelfDamage(Entity attacker, LivingEntity victim) {
        return attacker != null && attacker.getUniqueId().equals(victim.getUniqueId());
    }

    private static void resolve() {

        if (modernResolved) {
            return;
        }

        synchronized (DamageClassifier.class) {

            if (modernResolved) {
                return;
            }

            getDamageSource = method(EntityDamageEvent.class, "getDamageSource");
            isCritical = method(EntityDamageByEntityEvent.class, "isCritical");
            keyValue = method(org.bukkit.NamespacedKey.class, "value");

            if (getDamageSource != null) {

                Class<?> sourceType = getDamageSource.getReturnType();

                getDamageType = method(sourceType, "getDamageType");

                if (getDamageType != null) {
                    getKey = method(getDamageType.getReturnType(), "getKey");
                }
            }

            modernResolved = true;
        }
    }

    private static Method method(Class<?> owner, String name) {

        try {
            return owner.getMethod(name);
        } catch (NoSuchMethodException | RuntimeException absent) {
            return null;
        }
    }
}

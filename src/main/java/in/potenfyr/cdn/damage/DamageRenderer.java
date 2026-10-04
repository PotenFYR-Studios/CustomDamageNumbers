package in.potenfyr.cdn.damage;

import in.potenfyr.cdn.CustomDamageNumbersPlugin;
import in.potenfyr.cdn.packet.PacketUtil;
import in.potenfyr.cdn.util.ConfigManager;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class DamageRenderer {

    public static final List<FloatingDamage> ACTIVE =
            new CopyOnWriteArrayList<>();

    public static void spawn(
            LivingEntity victim,
            double damage,
            DamageType type,
            boolean critical
    ) {

        var config =
                CustomDamageNumbersPlugin
                        .getInstance()
                        .getConfigManager();

        if (!config.isEnabled()) return;

        if (ACTIVE.size() >= config.getMaxActiveDisplays()) return;

        // Global critical-hit toggle: when disabled, crits render as normal hits.
        if (critical && !config.areCriticalHitsEnabled()) {
            critical = false;
        }

        // A killing blow ends in a short flash instead of a full animation.
        boolean fatal = config.isRemoveOnDeath()
                && victim.getHealth() > 0
                && damage >= victim.getHealth();

        int durationTicks = fatal
                ? Math.min(2, config.getDurationTicks())
                : config.getDurationTicks();

        // Clone the location so we don't mutate the entity's location.
        Location location = victim.getLocation().clone().add(
                randomOffset(config),
                1.8,  // sits above the entity's head
                randomOffset(config)
        );

        FloatingDamage floatingDamage =
                new FloatingDamage(
                        location,
                        damage,
                        type,
                        critical,
                        durationTicks
                );

        ACTIVE.add(floatingDamage);
        PacketUtil.spawn(floatingDamage);
    }

    /**
     * @return how many live displays currently include the given player as a viewer.
     */
    public static int countActiveFor(UUID playerId) {

        int count = 0;

        for (FloatingDamage damage : ACTIVE) {
            if (damage.getViewers().contains(playerId)) {
                count++;
            }
        }

        return count;
    }

    /**
     * Destroys every live display; used on plugin disable so no viewer is left
     * with ghost entities.
     */
    public static void clear() {

        for (FloatingDamage damage : ACTIVE) {
            PacketUtil.destroy(damage);
        }

        ACTIVE.clear();
    }

    /**
     * Drops displays whose viewers have all gone offline; packet orphans that
     * would otherwise linger until their animation expires. Uses snapshot
     * + removeAll because CopyOnWriteArrayList iterators do not support
     * {@code remove()}.
     */
    public static void cleanupOrphans() {

        List<FloatingDamage> orphans = null;

        for (FloatingDamage damage : ACTIVE) {

            if (damage.getViewers().isEmpty()) {

                if (orphans == null) {
                    orphans = new java.util.ArrayList<>();
                }

                orphans.add(damage);
            }
        }

        if (orphans != null) {
            ACTIVE.removeAll(orphans);
        }
    }

    private static double randomOffset(ConfigManager config) {
        double spread = config.getHorizontalRandomness();
        return ThreadLocalRandom.current().nextDouble(-spread, spread);
    }
}
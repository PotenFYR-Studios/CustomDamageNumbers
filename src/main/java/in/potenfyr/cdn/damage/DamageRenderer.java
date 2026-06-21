package in.potenfyr.cdn.damage;

import in.potenfyr.cdn.CustomDamageNumbersPlugin;
import in.potenfyr.cdn.packet.PacketUtil;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.List;
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

        // Clone the location so we don't mutate the entity's location
        Location location = victim.getLocation().clone().add(
                randomOffset(),
                1.8,  // raised from 1.2 to sit above the mob's head properly
                randomOffset()
        );

        FloatingDamage floatingDamage =
                new FloatingDamage(
                        location,
                        damage,
                        type,
                        critical
                );

        ACTIVE.add(floatingDamage);
        PacketUtil.spawn(floatingDamage);
    }

    private static double randomOffset() {
        return ThreadLocalRandom.current().nextDouble(-0.3, 0.3);
    }
}
package in.potenfyr.cdn.damage;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class DamageListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {

        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }

        double damage = event.getFinalDamage();

        if (damage <= 0) {
            return;
        }

        DamageType type = classify(event);

        boolean critical = isCritical(event);

        DamageRenderer.spawn(
                victim,
                damage,
                type,
                critical
        );
    }

    private DamageType classify(EntityDamageByEntityEvent event) {

        return switch (event.getCause()) {

            case FIRE,
                 FIRE_TICK,
                 LAVA -> DamageType.FIRE;

            case MAGIC -> DamageType.MAGIC;

            case POISON -> DamageType.POISON;

            case ENTITY_EXPLOSION,
                 BLOCK_EXPLOSION -> DamageType.EXPLOSION;

            default -> DamageType.NORMAL;
        };
    }

    private boolean isCritical(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player player)) {
            return false;
        }

        return player.getFallDistance() > 0
                && !player.isOnGround();
    }
}
package in.potenfyr.cdn.damage;

import in.potenfyr.cdn.config.ConfigManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Turns damage events into displays.
 *
 * <p>Listens to the base {@link EntityDamageEvent}, not just the "by entity" subtype,
 * so fall, fire-tick, poison, drowning and the rest render too — 0.2.0 silently
 * dropped everything that was not an entity attack (spec section 10). The attacker is
 * only resolved for the subtype, and environmental damage simply has none.</p>
 */
public final class DamageListener implements Listener {

    private final DamageService service;
    private final ConfigManager config;

    public DamageListener(DamageService service, ConfigManager config) {
        this.service = service;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {

        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }

        double damage = event.getFinalDamage();

        if (damage <= 0.0) {
            return;
        }

        // Virtual hits from plugins (and cancelled-away damage) deal zero "damage"
        // while still reporting final damage; treated as noise unless configured.
        if (config.isIgnoreZeroDamage() && event.getDamage() <= 0.0) {
            return;
        }

        if (victim.isDead() || victim.isInvulnerable()) {
            return;
        }

        Entity damager = event instanceof EntityDamageByEntityEvent byEntity
                ? byEntity.getDamager()
                : null;

        Player attacker = resolveAttackingPlayer(damager);

        if (attacker != null && attacker.getUniqueId().equals(victim.getUniqueId())
                && !config.isSelfDamage()) {
            return;
        }

        DamageType type = DamageClassifier.classify(event);
        boolean critical = DamageClassifier.isCritical(event, damager);

        service.spawn(victim, damage, type, critical, attacker);
    }

    /**
     * Resolves the player responsible for a hit, looking through projectiles so
     * arrow, trident and snowball hits count as player attacks.
     */
    private Player resolveAttackingPlayer(Entity damager) {

        if (damager instanceof Player player) {
            return player;
        }

        if (damager instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }

        return null;
    }
}

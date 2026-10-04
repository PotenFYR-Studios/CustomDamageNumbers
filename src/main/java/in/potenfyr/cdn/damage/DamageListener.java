package in.potenfyr.cdn.damage;

import in.potenfyr.cdn.CustomDamageNumbersPlugin;

import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Locale;

public class DamageListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {

        var config = CustomDamageNumbersPlugin.getInstance().getConfigManager();

        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }

        double damage = event.getFinalDamage();

        // Zero/negative final damage and virtual "fake" hits are never shown.
        if (damage <= 0 || (config.isIgnoreZeroDamage() && event.getDamage() <= 0)) {
            return;
        }

        // Dead or invulnerable victims would produce misleading numbers.
        if (victim.isDead() || victim.isInvulnerable()) {
            return;
        }

        // Documented entity filters (general.* in config.yml).
        if (config.isIgnoreArmorStands() && victim instanceof org.bukkit.entity.ArmorStand) {
            return;
        }

        if (config.isIgnoreNpcs() && victim.hasMetadata("NPC")) {
            return;
        }

        if (config.isIgnoreInvisibleEntities() && victim.isInvisible()) {
            return;
        }

        World world = victim.getWorld();

        for (String name : config.getDisabledWorlds()) {
            if (name.toLowerCase(Locale.ROOT).equals(world.getName().toLowerCase(Locale.ROOT))) {
                return;
            }
        }

        Player attacker = resolveAttackingPlayer(event.getDamager());

        if (attacker == victim && !config.isSelfDamage()) {
            return;
        }

        // Victim-type toggles (general.players / general.animals / general.mobs).
        if (victim instanceof Player) {
            if (!config.isPlayers()) {
                return;
            }
        } else if (isAnimal(victim)) {
            if (!config.isAnimals()) {
                return;
            }
        } else if (!config.isMobs()) {
            return;
        }

        DamageType type = classify(event);

        boolean critical = event.isCritical();

        DamageRenderer.spawn(
                victim,
                damage,
                type,
                critical
        );
    }

    /**
     * Resolves the player responsible for the hit, looking through projectiles
     * to their shooter so arrow/trident hits count as player attacks.
     */
    private Player resolveAttackingPlayer(org.bukkit.entity.Entity damager) {

        if (damager instanceof Player player) {
            return player;
        }

        if (damager instanceof Projectile projectile
                && projectile.getShooter() instanceof Player player) {
            return player;
        }

        return null;
    }

    private boolean isAnimal(LivingEntity entity) {
        return entity instanceof org.bukkit.entity.Animals
                || entity instanceof org.bukkit.entity.WaterMob
                || entity instanceof org.bukkit.entity.Golem;
    }

    /**
     * Classifies damage using the modern damage-source type key where available
     * (stable identifiers such as "minecraft:magic"), falling back to the event
     * cause for anything the damage source does not describe.
     */
    private DamageType classify(EntityDamageByEntityEvent event) {

        var source = event.getDamageSource();

        if (source != null) {

            String key = source.getDamageType().key().value().toLowerCase(Locale.ROOT);

            switch (key) {

                case "lava", "in_fire", "on_fire", "hot_floor", "fireball" -> {
                    return DamageType.FIRE;
                }

                case "magic", "indirect_magic", "sonic_boom", "dragon_breath" -> {
                    return DamageType.MAGIC;
                }

                case "poison", "witch" -> {
                    return DamageType.POISON;
                }

                case "explosion", "fireworks" -> {
                    return DamageType.EXPLOSION;
                }

                default -> {
                }
            }
        }

        return switch (event.getCause()) {

            case FIRE,
                 FIRE_TICK,
                 LAVA,
                 HOT_FLOOR -> DamageType.FIRE;

            case MAGIC,
                 SONIC_BOOM,
                 DRAGON_BREATH -> DamageType.MAGIC;

            case POISON -> DamageType.POISON;

            case ENTITY_EXPLOSION,
                 BLOCK_EXPLOSION -> DamageType.EXPLOSION;

            case FALL -> DamageType.FALL;

            default -> DamageType.NORMAL;
        };
    }
}
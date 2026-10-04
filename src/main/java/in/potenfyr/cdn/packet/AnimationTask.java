package in.potenfyr.cdn.packet;

import in.potenfyr.cdn.CustomDamageNumbersPlugin;
import in.potenfyr.cdn.damage.DamageRenderer;
import in.potenfyr.cdn.damage.FloatingDamage;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

import java.util.ArrayList;
import java.util.List;

public class AnimationTask extends BukkitRunnable {

    @Override
    public void run() {

        var config =
                CustomDamageNumbersPlugin
                        .getInstance()
                        .getConfigManager();

        List<FloatingDamage> toRemove = new ArrayList<>();

        for (FloatingDamage damage : DamageRenderer.ACTIVE) {

            int t = damage.getTicksAlive();

            // Per-display lifetime: killing blows flash briefly instead of
            // playing the full animation (see DamageRenderer#spawn).
            double progress = (double) t / damage.getDurationTicks();

            // Rise fast then fall
            double y = t < 10 ? 0.08 : -0.06;

            // Spray outward in unique direction per entity, slow down as progress increases
            double angle = damage.getEntityId() * 2.399; // golden angle for even spread
            double x = Math.cos(angle) * 0.04 * (1.0 - progress);
            double z = Math.sin(angle) * 0.04 * (1.0 - progress);

            // Location#add mutates in place; publish it back explicitly.
            Location location = damage.getLocation().add(x, y, z);
            damage.setLocation(location);

            damage.setTicksAlive(t + 1);

            // Distance culling: viewers that move out of range are detached
            // from the display and stop receiving update packets.
            if (config.isDistanceCulling()) {
                double maxDistanceSq =
                        (double) config.getViewDistance() * config.getViewDistance();

                for (UUID viewerId : List.copyOf(damage.getViewers())) {

                    Player viewer = org.bukkit.Bukkit.getPlayer(viewerId);

                    if (viewer == null || !viewer.isOnline()
                            || viewer.getWorld() != location.getWorld()
                            || viewer.getLocation().distanceSquared(location) > maxDistanceSq) {
                        PacketUtil.destroyFor(damage, viewerId);
                    }
                }
            }

            PacketUtil.teleport(damage);

            if (damage.getTicksAlive() >= damage.getDurationTicks()) {
                PacketUtil.destroy(damage);
                toRemove.add(damage);
            }
        }

        DamageRenderer.ACTIVE.removeAll(toRemove);

        // Periodically drop displays whose viewers all went offline.
        if (config.isCleanupOrphans()) {
            DamageRenderer.cleanupOrphans();
        }
    }
}
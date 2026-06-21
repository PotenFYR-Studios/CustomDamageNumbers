package in.potenfyr.cdn.packet;

import in.potenfyr.cdn.CustomDamageNumbersPlugin;
import in.potenfyr.cdn.damage.DamageRenderer;
import in.potenfyr.cdn.damage.FloatingDamage;
import org.bukkit.scheduler.BukkitRunnable;

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
            double progress = (double) t / config.getDurationTicks();

            // Rise fast then fall
            double y = t < 10 ? 0.08 : -0.06;

            // Spray outward in unique direction per entity, slow down as progress increases
            double angle = damage.getEntityId() * 2.399; // golden angle for even spread
            double x = Math.cos(angle) * 0.04 * (1.0 - progress);
            double z = Math.sin(angle) * 0.04 * (1.0 - progress);

            damage.getLocation().add(x, y, z);
            damage.setTicksAlive(t + 1);

            PacketUtil.teleport(damage);

            if (damage.getTicksAlive() >= config.getDurationTicks()) {
                PacketUtil.destroy(damage);
                toRemove.add(damage);
            }
        }

        DamageRenderer.ACTIVE.removeAll(toRemove);
    }
}
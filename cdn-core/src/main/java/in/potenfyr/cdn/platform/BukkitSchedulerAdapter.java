package in.potenfyr.cdn.platform;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * The ordinary Bukkit scheduler: used on Spigot, Paper, Purpur and Pufferfish.
 */
public final class BukkitSchedulerAdapter implements SchedulerAdapter {

    private final Plugin plugin;

    public BukkitSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runSync(Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void runSyncLater(Runnable task, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, task, Math.max(0L, delayTicks));
    }

    @Override
    public SchedulerHandle runTimer(Runnable task, long delayTicks, long periodTicks) {

        BukkitTask handle = Bukkit.getScheduler().runTaskTimer(
                plugin,
                task,
                Math.max(0L, delayTicks),
                Math.max(1L, periodTicks));

        return new SchedulerHandle() {

            @Override
            public void cancel() {
                handle.cancel();
            }

            @Override
            public boolean cancelled() {
                return handle.isCancelled();
            }
        };
    }

    @Override
    public void runAsync(Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public String describe() {
        return "bukkit-scheduler";
    }
}

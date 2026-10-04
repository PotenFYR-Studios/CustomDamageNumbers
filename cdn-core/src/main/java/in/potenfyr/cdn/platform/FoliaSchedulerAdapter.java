package in.potenfyr.cdn.platform;

import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * Folia scheduling, reached entirely by reflection.
 *
 * <p>We compile against the 1.16.5 API (see the spec's oldest-common-API rule), so
 * the region scheduler types do not exist at compile time. Reflection here is
 * deliberate and isolated: if Folia's API is missing we fall back to the Bukkit
 * adapter rather than failing to start.</p>
 */
public final class FoliaSchedulerAdapter implements SchedulerAdapter {

    private final Plugin plugin;
    private final Logger logger;
    private final SchedulerAdapter fallback;

    private final Object globalScheduler;
    private final Object asyncScheduler;

    private FoliaSchedulerAdapter(
            Plugin plugin,
            Logger logger,
            SchedulerAdapter fallback,
            Object globalScheduler,
            Object asyncScheduler
    ) {
        this.plugin = plugin;
        this.logger = logger;
        this.fallback = fallback;
        this.globalScheduler = globalScheduler;
        this.asyncScheduler = asyncScheduler;
    }

    /**
     * @return a Folia adapter when the region schedulers are reachable, otherwise
     *         the supplied Bukkit adapter
     */
    public static SchedulerAdapter create(Plugin plugin, SchedulerAdapter fallback) {

        try {

            Method global = plugin.getServer().getClass().getMethod("getGlobalRegionScheduler");
            Method async = plugin.getServer().getClass().getMethod("getAsyncScheduler");

            Object globalScheduler = global.invoke(plugin.getServer());
            Object asyncScheduler = async.invoke(plugin.getServer());

            if (globalScheduler == null || asyncScheduler == null) {
                return fallback;
            }

            return new FoliaSchedulerAdapter(
                    plugin, plugin.getLogger(), fallback, globalScheduler, asyncScheduler);

        } catch (ReflectiveOperationException failure) {
            plugin.getLogger().info(
                    "Folia schedulers unavailable (" + failure.getClass().getSimpleName()
                            + "); using the Bukkit scheduler.");
            return fallback;
        }
    }

    @Override
    public void runSync(Runnable task) {
        runTimerOn(globalScheduler, task, 1L, 0L, true);
    }

    @Override
    public void runSyncLater(Runnable task, long delayTicks) {
        runTimerOn(globalScheduler, task, Math.max(1L, delayTicks), 0L, true);
    }

    @Override
    public SchedulerHandle runTimer(Runnable task, long delayTicks, long periodTicks) {
        return runTimerOn(
                globalScheduler, task, Math.max(1L, delayTicks), Math.max(1L, periodTicks), false);
    }

    @Override
    public void runAsync(Runnable task) {

        try {

            Method method = asyncScheduler.getClass().getMethod(
                    "runNow", Plugin.class, java.util.function.Consumer.class);

            method.invoke(asyncScheduler, plugin, (java.util.function.Consumer<Object>) ignored -> task.run());

        } catch (ReflectiveOperationException failure) {
            logger.fine("Folia async scheduling failed, running inline: " + failure);
            task.run();
        }
    }

    @Override
    public String describe() {
        return "folia-region-scheduler";
    }

    private SchedulerHandle runTimerOn(
            Object scheduler,
            Runnable task,
            long delayTicks,
            long periodTicks,
            boolean once
    ) {

        try {

            Method method = once
                    ? scheduler.getClass().getMethod(
                            "runDelayed", Plugin.class, java.util.function.Consumer.class, long.class)
                    : scheduler.getClass().getMethod(
                            "runAtFixedRate", Plugin.class, java.util.function.Consumer.class,
                            long.class, long.class);

            Object handle = once
                    ? method.invoke(scheduler, plugin, (java.util.function.Consumer<Object>) ignored -> task.run(),
                            delayTicks)
                    : method.invoke(scheduler, plugin, (java.util.function.Consumer<Object>) ignored -> task.run(),
                            delayTicks, periodTicks);

            return new ReflectiveHandle(handle);

        } catch (ReflectiveOperationException failure) {

            logger.warning("Folia scheduling failed, falling back for this task: " + failure);

            if (once) {
                fallback.runSyncLater(task, delayTicks);
                return new SchedulerHandle() {

                    @Override
                    public void cancel() {
                        // Nothing scheduled through Folia.
                    }

                    @Override
                    public boolean cancelled() {
                        return true;
                    }
                };
            }

            return fallback.runTimer(task, delayTicks, periodTicks);
        }
    }

    /** Wraps a Folia {@code ScheduledTask} handle reflectively. */
    private record ReflectiveHandle(Object handle) implements SchedulerHandle {

        @Override
        public void cancel() {

            try {
                handle.getClass().getMethod("cancel").invoke(handle);
            } catch (ReflectiveOperationException ignored) {
                // Already cancelled or the API changed shape; nothing useful to do.
            }
        }

        @Override
        public boolean cancelled() {

            try {
                Object value = handle.getClass().getMethod("isCancelled").invoke(handle);
                return value instanceof Boolean cancelled && cancelled;
            } catch (ReflectiveOperationException ignored) {
                return true;
            }
        }
    }
}

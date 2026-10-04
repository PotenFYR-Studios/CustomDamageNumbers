package in.potenfyr.cdn.platform;

/**
 * The scheduling operations the plugin needs, abstracted so the same code runs on
 * the Bukkit scheduler and on Folia's region/entity schedulers.
 *
 * <p>All methods take the task body; callers never touch a {@code BukkitTask} or a
 * Folia scheduler directly, which is what keeps the Folia path out of the rest of
 * the codebase.</p>
 */
public interface SchedulerAdapter {

    /** Runs the task on the next tick of the main/global thread. */
    void runSync(Runnable task);

    /** Runs the task once, {@code delayTicks} later. */
    void runSyncLater(Runnable task, long delayTicks);

    /**
     * Runs the task repeatedly. The returned handle is owned by the caller and must
     * be cancelled on disable/reload.
     */
    SchedulerHandle runTimer(Runnable task, long delayTicks, long periodTicks);

    /** Runs the task off-thread when the implementation supports it. */
    void runAsync(Runnable task);

    /** Human-readable description for {@code /cdn backend} and debug output. */
    String describe();

    /** A cancellable repeating task. */
    interface SchedulerHandle {

        void cancel();

        boolean cancelled();
    }
}

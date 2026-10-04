package in.potenfyr.cdn.packet;

import in.potenfyr.cdn.damage.DamageService;
import in.potenfyr.cdn.util.DebugLogger;

/**
 * The repeating tick that drives every live display.
 *
 * <p>Deliberately defensive: a failure in one tick is logged and swallowed so a
 * single bad frame can never kill the animation for the rest of the session (and the
 * timing counters give {@code /cdn stats} something real to report).</p>
 */
public final class AnimationTask implements Runnable {

    private final DamageService service;
    private final DebugLogger debug;

    private long executions;
    private long totalNanos;
    private long worstNanos;
    private long lastRunNanos;

    public AnimationTask(DamageService service, DebugLogger debug) {
        this.service = service;
        this.debug = debug;
    }

    @Override
    public void run() {

        long start = System.nanoTime();

        try {

            service.tick();

        } catch (Throwable failure) {

            // Never let one frame take the task down for the rest of the session.
            debug.error("Animation tick failed; continuing.", failure);

        } finally {

            long elapsed = System.nanoTime() - start;

            executions++;
            totalNanos += elapsed;
            worstNanos = Math.max(worstNanos, elapsed);
            lastRunNanos = System.currentTimeMillis();
        }
    }

    public long executions() {
        return executions;
    }

    public double averageMillis() {
        return executions == 0 ? 0.0 : (totalNanos / (double) executions) / 1_000_000.0;
    }

    public double worstMillis() {
        return worstNanos / 1_000_000.0;
    }

    public long lastRunEpochMillis() {
        return lastRunNanos;
    }
}

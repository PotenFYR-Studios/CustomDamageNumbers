package in.potenfyr.cdn.util;

import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Debug logging that costs nothing when disabled.
 *
 * <p>The enabled flag is a live supplier rather than a captured boolean so
 * {@code /cdn debug on} takes effect immediately without re-wiring services.</p>
 */
public final class DebugLogger {

    private final Logger logger;
    private final String prefix;
    private volatile BooleanSupplier enabled;

    public DebugLogger(Logger logger, String prefix, BooleanSupplier enabled) {
        this.logger = logger;
        this.prefix = prefix;
        this.enabled = enabled;
    }

    public void setEnabledSupplier(BooleanSupplier enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled != null && enabled.getAsBoolean();
    }

    public void debug(String message) {

        if (isEnabled()) {
            logger.info("[" + prefix + "] " + message);
        }
    }

    public void debug(String message, Object... arguments) {

        if (isEnabled()) {
            logger.info("[" + prefix + "] " + String.format(message, arguments));
        }
    }

    public void warn(String message) {
        logger.warning("[" + prefix + "] " + message);
    }

    public void error(String message, Throwable failure) {
        logger.log(Level.SEVERE, "[" + prefix + "] " + message, failure);
    }

    public void info(String message) {
        logger.info("[" + prefix + "] " + message);
    }
}

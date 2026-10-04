package in.potenfyr.cdn.metrics;

import in.potenfyr.cdn.util.DebugLogger;
import org.bukkit.plugin.Plugin;
import org.bstats.bukkit.Metrics;

/**
 * bStats wiring, opt-in.
 *
 * <p>Metrics only start when the operator has switched them on <em>and</em> supplied
 * their own bStats plugin id, so this jar never reports into someone else's
 * dashboard by default.</p>
 */
public final class MetricsRegistrar {

    private final Plugin plugin;
    private final DebugLogger debug;

    private Metrics metrics;

    public MetricsRegistrar(Plugin plugin, DebugLogger debug) {
        this.plugin = plugin;
        this.debug = debug;
    }

    /**
     * @param enabled whether {@code advanced.metrics} is on
     * @param pluginId the operator's bStats plugin id; {@code <= 0} disables metrics
     */
    public void start(boolean enabled, int pluginId) {

        if (!enabled) {
            debug.debug("Metrics disabled by config.");
            return;
        }

        if (pluginId <= 0) {
            debug.debug("Metrics enabled but advanced.metrics-id is not set; not reporting.");
            return;
        }

        try {
            metrics = new Metrics(plugin, pluginId);
            debug.debug("Metrics started for bStats id %d.", pluginId);
        } catch (RuntimeException failure) {
            debug.warn("Could not start metrics: " + failure.getMessage());
        }
    }

    public void shutdown() {

        if (metrics != null) {
            metrics.shutdown();
            metrics = null;
        }
    }
}

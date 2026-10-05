package in.potenfyr.cdn.api;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds the {@link CustomDamageNumbersApi} implementation published by the
 * plugin.
 *
 * <p>Registration is restricted to CustomDamageNumbers itself so a rogue plugin
 * cannot impersonate the provider; the implementation is additionally published
 * through Bukkit's {@link org.bukkit.plugin.ServicesManager} for setups that
 * prefer service lookup.</p>
 */
public final class ApiProvider {

    /** The plugin name both shipped jars register under. */
    private static final String OWNER_PLUGIN = "CustomDamageNumbers";

    private static final AtomicReference<CustomDamageNumbersApi> INSTANCE = new AtomicReference<>();

    private ApiProvider() {
    }

    /** The live API, or empty while the plugin is absent or disabled. */
    public static Optional<CustomDamageNumbersApi> get() {
        return Optional.ofNullable(INSTANCE.get());
    }

    /** The live API, or {@code null}; convenience for early startup code. */
    public static CustomDamageNumbersApi getOrNull() {
        return INSTANCE.get();
    }

    /**
     * Publishes the implementation. Called by CustomDamageNumbers on enable;
     * other plugins are rejected.
     *
     * @param plugin         the registering plugin, used for the service registry
     * @param implementation the API implementation
     */
    public static void register(Plugin plugin, CustomDamageNumbersApi implementation) {

        if (plugin == null || implementation == null) {
            throw new IllegalArgumentException("plugin and implementation are required");
        }

        if (!OWNER_PLUGIN.equals(plugin.getName())) {
            throw new IllegalArgumentException(
                    "only " + OWNER_PLUGIN + " may register its API; " + plugin.getName() + " is not allowed to");
        }

        INSTANCE.set(implementation);

        if (Bukkit.getServer() != null) {
            Bukkit.getServicesManager().register(
                    CustomDamageNumbersApi.class, implementation, plugin, ServicePriority.Normal);
        }
    }

    /** Withdraws the implementation. Called by CustomDamageNumbers on disable. */
    public static void unregister(Plugin plugin) {

        if (plugin == null || !OWNER_PLUGIN.equals(plugin.getName())) {
            return;
        }

        INSTANCE.set(null);

        if (Bukkit.getServer() != null) {
            Bukkit.getServicesManager().unregisterAll(plugin);
        }
    }
}

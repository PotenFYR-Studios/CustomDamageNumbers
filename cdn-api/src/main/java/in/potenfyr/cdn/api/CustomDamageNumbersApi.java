package in.potenfyr.cdn.api;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Optional;

/**
 * The public entry point for other plugins: spawn damage numbers with your own
 * text, style and animation, hook the render pipeline, or control per-player
 * state.
 *
 * <p>Obtain an instance from {@link ApiProvider#get()} once this plugin is
 * enabled. Add {@code cdn-api} to your build as a compile-only dependency and
 * declare {@code softdepend: [CustomDamageNumbers]} in your plugin.yml so the
 * provider is registered before your {@code onEnable} runs.</p>
 *
 * <pre>{@code
 * CustomDamageNumbersApi api = CustomDamageNumbersApi.get().orElse(null);
 * if (api != null) {
 *     api.spawn(api.numberBuilder()
 *             .at(victim)
 *             .value(42.5)
 *             .format("<gold>✦ {damage}")
 *             .critical(true));
 * }
 * }</pre>
 *
 * <p>All methods must be called from the main server thread (or the owning
 * region thread on Folia).</p>
 */
public interface CustomDamageNumbersApi {

    /** The version of the running CustomDamageNumbers plugin. */
    String version();

    /** A fresh, empty number description; pass it to {@link #spawn}. */
    DamageNumberBuilder numberBuilder();

    /**
     * Spawns one floating number as described by the builder.
     *
     * <p>Everything is optional except a positive {@code value} and one anchor
     * ({@code at(entity)} or {@code at(location)}): unspecified options fall
     * back to the plugin's configuration, preset overrides win over that, and
     * explicit builder settings win over everything. Builders are mutable and
     * may be reused for repeated spawns.</p>
     *
     * @param number the number description
     * @return whether a display was spawned
     * @throws IllegalStateException    when called off the main thread
     * @throws IllegalArgumentException when the description is invalid
     */
    boolean spawn(DamageNumberBuilder number);

    /** Whether a player currently receives damage numbers. */
    boolean isViewing(Player player);

    /** Turns a player's damage numbers on or off, as if they ran {@code /cdn toggle}. */
    void setViewing(Player player, boolean enabled);

    /** Removes every live display for every viewer. */
    void clearDisplays();

    /**
     * Removes the live displays one player can see, leaving them visible to others.
     *
     * @return how many displays were affected
     */
    int clearDisplays(Player viewer);

    /**
     * Convenience: resolves the API for a plugin at startup.
     *
     * @return empty while CustomDamageNumbers is not installed or still disabled
     */
    static Optional<CustomDamageNumbersApi> get() {
        return ApiProvider.get();
    }
}

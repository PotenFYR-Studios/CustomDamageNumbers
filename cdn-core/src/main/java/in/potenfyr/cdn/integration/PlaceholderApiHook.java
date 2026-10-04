package in.potenfyr.cdn.integration;

import in.potenfyr.cdn.CustomDamageNumbersPlugin;
import in.potenfyr.cdn.damage.DamageService;
import in.potenfyr.cdn.prefs.PlayerPreferences;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

/**
 * PlaceholderAPI expansion, registered only when PlaceholderAPI is installed.
 *
 * <p>Placeholders: {@code %cdn_enabled%}, {@code %cdn_active%},
 * {@code %cdn_shown%} and {@code %cdn_backend%}.</p>
 */
public final class PlaceholderApiHook extends PlaceholderExpansion {

    private final CustomDamageNumbersPlugin plugin;

    public PlaceholderApiHook(CustomDamageNumbersPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "cdn";
    }

    @Override
    public String getAuthor() {
        return "Potenfyr";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {

        DamageService service = plugin.getDamageService();
        PlayerPreferences preferences = plugin.getPreferences();

        if (identifier == null) {
            return "";
        }

        return switch (identifier.toLowerCase(java.util.Locale.ROOT)) {

            case "enabled" -> player == null
                    ? String.valueOf(preferences.defaultEnabled())
                    : String.valueOf(preferences.isEnabled(player.getUniqueId()));

            case "active" -> String.valueOf(service.activeCount());

            case "shown" -> player == null
                    ? "0"
                    : String.valueOf(service.countFor(player.getUniqueId()));

            case "backend" -> service.backend().id();

            default -> null;
        };
    }
}

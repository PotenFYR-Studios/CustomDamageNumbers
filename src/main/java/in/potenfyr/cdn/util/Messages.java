package in.potenfyr.cdn.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * Loads messages.yml and exposes player-facing text as Adventure components.
 * Legacy '&' color codes keep existing messages.yml files fully compatible.
 */
public class Messages {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    private final JavaPlugin plugin;

    private YamlConfiguration config;

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {

        File file = new File(plugin.getDataFolder(), "messages.yml");

        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(file);
    }

    public Component message(String key, String def) {
        return LEGACY.deserialize(config.getString(key, def));
    }

    public Component prefixed(String key, String def) {
        return message("prefix", "").append(message(key, def));
    }
}

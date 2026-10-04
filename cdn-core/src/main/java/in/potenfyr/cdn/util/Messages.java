package in.potenfyr.cdn.util;

import in.potenfyr.cdn.text.TextRenderer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Player-facing text, loaded from {@code messages.yml}.
 *
 * <p>Messages are serialised to a section-sign string before being sent, so no
 * Adventure audience is needed and the same messages render on Spigot and on Paper.
 * MiniMessage tags and legacy {@code &} codes are both accepted.</p>
 */
public final class Messages {

    private static final String DEFAULT_PREFIX = "&6[CDN] &r";

    private final JavaPlugin plugin;

    private volatile FileConfiguration config = new YamlConfiguration();

    /** Rebuilt on every reload so a changed frame width or colour applies at once. */
    private volatile ChatFrame frame;

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    /** Re-reads messages.yml, creating it from the bundled default when missing. */
    public void reload() {

        File file = new File(plugin.getDataFolder(), "messages.yml");

        if (!file.exists()) {

            File parent = file.getParentFile();

            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("Could not create " + parent.getAbsolutePath());
            }

            plugin.saveResource("messages.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(file);
        frame = new ChatFrame(this);
    }

    /** The framed-output renderer, rebuilt on reload. */
    public ChatFrame frame() {

        ChatFrame current = frame;

        if (current == null) {
            current = new ChatFrame(this);
            frame = current;
        }

        return current;
    }

    /** Sends a prefixed, bulleted confirmation. */
    public void feedback(CommandSender sender, String key, String fallback) {
        feedback(sender, key, fallback, null);
    }

    /** Sends a prefixed, bulleted confirmation with placeholder substitutions. */
    public void feedback(
            CommandSender sender,
            String key,
            String fallback,
            Map<String, String> placeholders
    ) {
        ChatFrame.deliver(sender, prefix() + frame().success(apply(plain(key, fallback), placeholders)));
    }

    /** Sends a prefixed, bulleted failure. */
    public void feedbackError(CommandSender sender, String key, String fallback) {
        feedbackError(sender, key, fallback, null);
    }

    /** Sends a prefixed, bulleted failure with placeholder substitutions. */
    public void feedbackError(
            CommandSender sender,
            String key,
            String fallback,
            Map<String, String> placeholders
    ) {
        ChatFrame.deliver(sender, prefix() + frame().error(apply(plain(key, fallback), placeholders)));
    }

    /**
     * Keys that report a refusal rather than a result, so {@link #send} can style them.
     * Keeping the list here means every existing call site gets the new visual without
     * having to be rewritten.
     */
    private static final Set<String> ERROR_KEYS = Set.of(
            "no-permission",
            "players-only",
            "player-not-found",
            "plugin-disabled",
            "unknown-subcommand",
            "unknown-type",
            "invalid-amount",
            "test-failed",
            "style-unknown",
            "backend-unknown");

    /** Sends a configured message, styled as a confirmation or a refusal. */
    public void send(CommandSender sender, String key, String fallback) {

        if (ERROR_KEYS.contains(key)) {
            feedbackError(sender, key, fallback);
        } else {
            feedback(sender, key, fallback);
        }
    }

    /** Raw configured value. */
    public String raw(String key, String fallback) {

        String value = config.getString(key);

        return value == null ? fallback : value;
    }

    public String rawListEntry(String key, int index, String fallback) {

        List<String> values = config.getStringList(key);

        return index < values.size() ? values.get(index) : fallback;
    }

    /** The configured message with the prefix applied, as a section-sign string. */
    public String prefixed(String key, String fallback) {
        return TextRenderer.toLegacy(prefixRaw()) + TextRenderer.toLegacy(raw(key, fallback));
    }

    /** The configured message without the prefix. */
    public String plain(String key, String fallback) {
        return TextRenderer.toLegacy(raw(key, fallback));
    }

    /** Prefix + message with {@code {placeholder}} substitutions applied. */
    public String prefixed(String key, String fallback, Map<String, String> placeholders) {
        return apply(prefixed(key, fallback), placeholders);
    }

    /** Sends a prefixed message with placeholder substitutions. */
    public void send(
            CommandSender sender,
            String key,
            String fallback,
            Map<String, String> placeholders
    ) {
        String text = prefix() + apply(plain(key, fallback), placeholders);

        ChatFrame.deliver(sender, text);
    }

    /** Plain-text form of a configured message, for logs. */
    public String plainText(String key, String fallback) {
        return TextRenderer.toPlainText(raw(key, fallback));
    }

    public String prefix() {
        return TextRenderer.toLegacy(prefixRaw());
    }

    private String prefixRaw() {
        return raw("prefix", DEFAULT_PREFIX);
    }

    private static String apply(String message, Map<String, String> placeholders) {

        if (placeholders == null || placeholders.isEmpty()) {
            return message;
        }

        String result = message;

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {

            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }

            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return result;
    }
}

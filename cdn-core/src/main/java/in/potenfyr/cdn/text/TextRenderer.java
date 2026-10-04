package in.potenfyr.cdn.text;

import in.potenfyr.cdn.config.StyleSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Locale;

/**
 * The single place that turns configured text into something sendable.
 *
 * <p>Two outputs matter:</p>
 * <ul>
 *   <li>{@link #toJson(String, StyleSettings, double)} — a JSON text component for
 *       display metadata. PacketEvents takes a {@code String} here
 *       ({@code EntityDataTypes.COMPONENT}), so Adventure never leaves our
 *       classloader and Spigot support stays safe (spec section 3).</li>
 *   <li>{@link #toLegacy(String)} — a section-sign string for chat messages, which
 *       every supported server accepts without an Adventure audience.</li>
 * </ul>
 *
 * <p>Adventure is shaded and relocated into the jar, so this class is the only
 * boundary between configuration text and the wire.</p>
 */
public final class TextRenderer {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final LegacyComponentSerializer LEGACY_OUT =
            LegacyComponentSerializer.builder()
                    .character(LegacyComponentSerializer.SECTION_CHAR)
                    .hexColors()
                    .useUnusualXRepeatedCharacterHexFormat()
                    .build();

    private TextRenderer() {
    }

    /** Parses configured text, accepting both MiniMessage tags and legacy {@code &} codes. */
    public static Component parse(String raw) {

        String text = raw == null ? "" : raw;

        if (!LegacyToMiniMessage.looksLegacy(text)) {
            return parseMiniMessage(text);
        }

        return parseMiniMessage(LegacyToMiniMessage.convert(text));
    }

    private static Component parseMiniMessage(String text) {

        try {
            return MINI_MESSAGE.deserialize(text);
        } catch (RuntimeException malformed) {
            // A broken tag must never take damage numbers down; render literally.
            return Component.text(text);
        }
    }

    /** Serialises a component to the JSON form display metadata expects. */
    public static String toJson(Component component) {
        return GsonComponentSerializer.gson().serialize(component);
    }

    /**
     * Builds the JSON text component for a damage number.
     *
     * @param damage the damage value substituted into {@code {damage}} / {@code {value}}
     * @param style  the resolved style; {@code null} renders plain white text
     */
    public static String toJsonForDamage(double damage, StyleSettings style) {

        String formatted = formatValue(damage);

        String template = style == null ? "{damage}" : style.format();

        if (template == null || template.isEmpty()) {
            template = "{damage}";
        }

        String rendered = template
                .replace("{damage}", formatted)
                .replace("{value}", formatted);

        return toJson(applyStyle(parse(rendered), style));
    }

    /** Serialises a configured message for {@code CommandSender#sendMessage(String)}. */
    public static String toLegacy(String raw) {
        return LEGACY_OUT.serialize(parse(raw));
    }

    /** Locale-stable decimal formatting; one decimal place, dot separator. */
    public static String formatValue(double damage) {
        return String.format(Locale.ROOT, "%.1f", damage);
    }

    /** Strips all markup, for log lines and plain-text contexts. */
    public static String toPlainText(String raw) {
        return PlainTextComponentSerializer.plainText().serialize(parse(raw));
    }

    private static Component applyStyle(Component component, StyleSettings style) {

        if (style == null) {
            return component;
        }

        Component styled = component;

        TextColor color = style.color() == null || style.color().isBlank()
                ? null
                : TextColor.fromHexString(style.color());

        // A colour written into the format wins over styles.<type>.color: the setting is
        // the default, not an override. Without this, "&c{damage}" would be repainted
        // white by the style's own colour.
        if (color != null && styled.style().color() == null) {
            styled = styled.color(color);
        }

        return styled
                .decoration(TextDecoration.BOLD, state(style.bold()))
                .decoration(TextDecoration.ITALIC, state(style.italic()));
    }

    private static TextDecoration.State state(boolean enabled) {
        return enabled ? TextDecoration.State.TRUE : TextDecoration.State.FALSE;
    }
}

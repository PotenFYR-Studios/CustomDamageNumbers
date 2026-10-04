package in.potenfyr.cdn.util;

import in.potenfyr.cdn.text.TextRenderer;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders framed command output: bordered boxes, aligned label/value rows and accent
 * bullets, all in the same text pipeline as the rest of the plugin (MiniMessage or
 * legacy {@code &} codes, emitted as section-sign strings so Spigot and Paper both
 * render them).
 *
 * <p>Every colour and glyph is read from {@code messages.yml} under {@code frame.*}, so
 * a server owner can restyle the whole command surface without touching code. Width is
 * tracked on the plain text and colours are injected afterwards, which is what keeps the
 * right-hand border aligned regardless of how much colour is in a row.</p>
 */
public final class ChatFrame {

    public static final int MIN_WIDTH = 28;
    public static final int MAX_WIDTH = 80;
    public static final int DEFAULT_WIDTH = 56;

    /** One label/value pair inside a box. */
    public record Row(String label, String value) {

        public static Row of(String label, Object value) {
            return new Row(label, String.valueOf(value));
        }
    }

    private final String border;
    private final String title;
    private final String subtitle;
    private final String label;
    private final String value;
    private final String accent;
    private final String footer;
    private final String glyph;
    private final String bullet;
    private final int width;

    public ChatFrame(Messages messages) {

        this.width = clamp(parseInt(messages.raw("frame.width", String.valueOf(DEFAULT_WIDTH)), DEFAULT_WIDTH),
                MIN_WIDTH, MAX_WIDTH);

        this.border = colour(messages.raw("frame.border", "&8"));
        this.title = colour(messages.raw("frame.title", "&d&l"));
        this.subtitle = colour(messages.raw("frame.subtitle", "&7"));
        this.label = colour(messages.raw("frame.label", "&7"));
        this.value = colour(messages.raw("frame.value", "&f"));
        this.accent = colour(messages.raw("frame.accent", "&b"));
        this.footer = colour(messages.raw("frame.footer", "&8"));
        this.glyph = messages.raw("frame.glyph", "◆");
        this.bullet = messages.raw("frame.bullet", "▪");
    }

    /**
     * Builds a complete box.
     *
     * @param titleText    heading, prefixed with the configured glyph
     * @param subtitleText optional second line (version, server, ...)
     * @param rows         label/value pairs
     * @param footerLines  optional muted lines above the bottom border
     */
    public List<String> box(
            String titleText,
            String subtitleText,
            List<Row> rows,
            List<String> footerLines
    ) {

        int inner = width - 4;
        List<String> lines = new ArrayList<>();

        lines.add(rule('┌', '┐'));
        lines.add(framed(visible(glyph + " " + titleText),
                border + "│ " + "\u00a7r" + title + glyph + " " + titleText));

        if (subtitleText != null && !subtitleText.isBlank()) {
            lines.add(framed(visible(subtitleText),
                    border + "│ " + "\u00a7r" + subtitle + subtitleText));
        }

        if (!rows.isEmpty()) {
            lines.add(rule('├', '┤'));

            int labelColumn = Math.max(10, Math.min(22, inner / 2));

            for (Row row : rows) {
                lines.add(rowLine(row, inner, labelColumn));
            }
        }

        if (footerLines != null && !footerLines.isEmpty()) {

            lines.add(rule('├', '┤'));

            for (String footerLine : footerLines) {
                lines.add(framed(visible(footerLine) + 2,
                        border + "│ " + "\u00a7r" + footer + footerLine));
            }
        }

        lines.add(rule('└', '┘'));

        return lines;
    }

    /** Sends a framed box, adapting every line for the receiving sender. */

    /** A single accent-bulleted line, e.g. for command feedback. */
    public String bullet(String text) {
        return accent + bullet + " " + "\u00a7r" + value + text;
    }

    /** A success line with the accent bullet. */
    public String success(String text) {
        return bullet(text);
    }

    /** A failure line, still bulleted so the shape stays consistent. */
    public String error(String text) {
        return colour("&c") + bullet + " " + "\u00a7r" + colour("&f") + text;
    }

    /** Sends every line of a box to a sender, adapting for the console. */
    public void send(CommandSender sender, List<String> lines) {

        for (String line : lines) {
            deliver(sender, line);
        }
    }

    /**
     * Delivers one line, adapting it for non-player senders.
     *
     * <p>The server console and RCON clients do not render section-sign colours, so they
     * would otherwise show "§6[CDN] §7Backend..." verbatim; the same goes for the box
     * drawing characters on terminals with a narrow codepage. Console output is therefore
     * stripped of colour and framed in ASCII.</p>
     */
    public static void deliver(CommandSender sender, String line) {
        sender.sendMessage(sender instanceof org.bukkit.entity.Player ? line : toConsole(line));
    }

    /** Strips colour codes and transliterates box glyphs, for the console and RCON. */
    public static String toConsole(String line) {

        if (line == null) {
            return "";
        }

        StringBuilder out = new StringBuilder(line.length());
        char[] chars = line.toCharArray();

        for (int index = 0; index < chars.length; index++) {

            char current = chars[index];

            if (current == '\u00a7' && index + 1 < chars.length) {

                // §x§f§f§0§0§0§0 is fourteen characters in total.
                if (Character.toLowerCase(chars[index + 1]) == 'x') {
                    index += 13;
                } else {
                    index++;
                }

                continue;
            }

            out.append(transliterate(current));
        }

        return out.toString();
    }

    private static char transliterate(char character) {

        return switch (character) {
            case '┌', '┐', '└', '┘', '├', '┤', '┬', '┴', '┼' -> '+';
            case '─' -> '-';
            case '│' -> '|';
            case '◆' -> '*';
            case '▪' -> '*';
            case '…' -> '.';
            default -> character;
        };
    }

    public int width() {
        return width;
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    private String rowLine(Row row, int inner, int labelColumn) {

        String labelText = truncate(labelOf(row.label()), labelColumn);
        String valueText = truncate(row.value(), Math.max(1, inner - labelColumn - 1));

        String plain = labelText + " ".repeat(labelColumn - labelText.length()) + " " + valueText;

        String coloured = label + labelText
                + " ".repeat(labelColumn - labelText.length())
                + " " + "\u00a7r" + value + valueText;

        return framed(plain.length(), border + "│ " + "\u00a7r" + coloured);
    }

    /**
     * Compatibility shim for messages.yml files written before the framed layout: those
     * label keys carried the whole line, e.g. {@code "&7Backend: &f{value}"}. Reducing
     * them to their label keeps an existing file rendering correctly instead of printing
     * a stray placeholder.
     */
    static String labelOf(String raw) {

        if (raw == null) {
            return "";
        }

        String label = raw
                .replace("{value}", "")
                .replaceAll(":\\s*$", "")
                .trim();

        return label.isEmpty() ? raw.trim() : label;
    }

    private String rule(char left, char right) {
        return border + left + String.valueOf('─').repeat(width - 2) + right + "\u00a7r";
    }

    /** Wraps pre-coloured content in borders, padding to the frame width. */
    private String framed(int plainLength, String colouredContent) {

        int inner = width - 4;
        int padding = Math.max(0, inner - plainLength);

        return colouredContent + " ".repeat(padding) + " " + border + "│" + "\u00a7r";
    }

    public static String truncate(String text, int limit) {

        if (text == null) {
            return "";
        }

        return text.length() <= limit ? text : text.substring(0, Math.max(0, limit - 1)) + "…";
    }

    static int visible(String text) {
        return stripColour(text).length();
    }

    /**
     * Removes section-sign colour codes (including the fourteen-character hex form) so a
     * line's printed width can be measured even after it has been coloured.
     */
    static String stripColour(String text) {

        if (text == null) {
            return "";
        }

        StringBuilder out = new StringBuilder(text.length());
        char[] chars = text.toCharArray();

        for (int index = 0; index < chars.length; index++) {

            if (chars[index] == '\u00a7' && index + 1 < chars.length) {

                if (Character.toLowerCase(chars[index + 1]) == 'x') {
                    index += 13;
                } else {
                    index++;
                }

                continue;
            }

            out.append(chars[index]);
        }

        return out.toString();
    }

    private static String colour(String raw) {
        return TextRenderer.toLegacy(raw);
    }

    private static int parseInt(String raw, int fallback) {

        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException invalid) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

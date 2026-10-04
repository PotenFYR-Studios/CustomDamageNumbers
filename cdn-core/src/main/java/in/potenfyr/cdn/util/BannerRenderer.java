package in.potenfyr.cdn.util;

import java.util.ArrayList;
import java.util.List;

/**
 * The console banner printed at startup.
 *
 * <p>Deliberately built from plain ASCII: the server console, RCON clients and Windows
 * terminals differ in which glyph sets they render, and a banner that shows as boxes or
 * mojibake is worse than no banner. Width and text live here; the values come from the
 * running server.</p>
 */
public final class BannerRenderer {

    public static final int WIDTH = 64;

    private BannerRenderer() {
    }

    /**
     * Builds the banner as a single multi-line string, ready for one logger call.
     *
     * @param version  plugin version
     * @param tagline  one-line description
     * @param rows     aligned label/value rows
     * @param footer   link or credit line
     */
    public static String render(
            String version,
            String tagline,
            List<ChatFrame.Row> rows,
            String footer
    ) {

        List<String> lines = new ArrayList<>();

        lines.add(rule('='));
        lines.add(centre("CustomDamageNumbers  v" + version));
        lines.add(centre(tagline));
        lines.add(rule('-'));

        int labelWidth = 0;

        for (ChatFrame.Row row : rows) {
            labelWidth = Math.max(labelWidth, row.label().length());
        }

        for (ChatFrame.Row row : rows) {
            lines.add("  " + pad(row.label(), labelWidth) + "   " + row.value());
        }

        lines.add(rule('-'));

        if (footer != null && !footer.isBlank()) {
            lines.add(centre(footer));
            lines.add(rule('='));
        } else {
            lines.add(rule('='));
        }

        return String.join(System.lineSeparator(), lines);
    }

    private static String rule(char character) {
        return String.valueOf(character).repeat(WIDTH);
    }

    private static String centre(String text) {

        String value = text == null ? "" : text;

        if (value.length() >= WIDTH) {
            return value;
        }

        int left = (WIDTH - value.length()) / 2;

        return " ".repeat(left) + value;
    }

    private static String pad(String text, int width) {

        String value = text == null ? "" : text;

        return value + " ".repeat(Math.max(0, width - value.length()));
    }
}

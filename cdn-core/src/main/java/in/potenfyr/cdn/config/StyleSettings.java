package in.potenfyr.cdn.config;

/**
 * Resolved formatting for one damage type ({@code styles.<type>}).
 *
 * @param key     the damage type key this style belongs to, e.g. {@code "critical"}
 * @param enabled whether the type renders at all; disabled types fall back to normal
 * @param format  the template, supporting {@code {damage}} and {@code {value}}
 * @param color   hex colour, e.g. {@code "#FF3333"}
 * @param bold    bold decoration
 * @param italic  italic decoration
 * @param shadow  text shadow; applied to the TextDisplay style flags
 */
public record StyleSettings(
        String key,
        boolean enabled,
        String format,
        String color,
        boolean bold,
        boolean italic,
        boolean shadow
) {

    public static StyleSettings fallback(String key) {
        return new StyleSettings(key, true, "{damage}", "#FFFFFF", true, false, true);
    }
}

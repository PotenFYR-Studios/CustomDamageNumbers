package in.potenfyr.cdn.text;

/**
 * Converts legacy ampersand colour codes to MiniMessage tags, so a config that
 * uses {@code &c} keeps working after the move to MiniMessage parsing.
 *
 * <p>Characters that MiniMessage would otherwise treat as markup are escaped, so
 * a legacy string can never be reinterpreted as a tag.</p>
 */
public final class LegacyToMiniMessage {

    private LegacyToMiniMessage() {
    }

    public static String convert(String input) {

        if (input == null || input.isEmpty()) {
            return "";
        }

        String escaped = input.replace("\\", "\\\\").replace("<", "\\<");

        StringBuilder out = new StringBuilder(escaped.length() + 16);

        for (int index = 0; index < escaped.length(); index++) {

            char current = escaped.charAt(index);

            if (current != '&' || index + 1 >= escaped.length()) {
                out.append(current);
                continue;
            }

            // &#RRGGBB -> <#RRGGBB>
            if (escaped.charAt(index + 1) == '#' && index + 8 < escaped.length()
                    && isHex(escaped, index + 2)) {
                out.append("<#").append(escaped, index + 2, index + 8).append('>');
                index += 7;
                continue;
            }

            char code = Character.toLowerCase(escaped.charAt(index + 1));
            String tag = tagFor(code);

            if (tag == null) {
                // An ampersand that is not a colour code stays literal.
                out.append(current);
                continue;
            }

            out.append(tag);
            index++;
        }

        return out.toString();
    }

    /** True when the input looks like it contains legacy colour codes. */
    public static boolean looksLegacy(String input) {

        if (input == null) {
            return false;
        }

        for (int index = 0; index + 1 < input.length(); index++) {
            if (input.charAt(index) == '&' && tagFor(Character.toLowerCase(input.charAt(index + 1))) != null) {
                return true;
            }
        }

        return false;
    }

    private static String tagFor(char code) {

        return switch (code) {
            case '0' -> "<black>";
            case '1' -> "<dark_blue>";
            case '2' -> "<dark_green>";
            case '3' -> "<dark_aqua>";
            case '4' -> "<dark_red>";
            case '5' -> "<dark_purple>";
            case '6' -> "<gold>";
            case '7' -> "<gray>";
            case '8' -> "<dark_gray>";
            case '9' -> "<blue>";
            case 'a' -> "<green>";
            case 'b' -> "<aqua>";
            case 'c' -> "<red>";
            case 'd' -> "<light_purple>";
            case 'e' -> "<yellow>";
            case 'f' -> "<white>";
            case 'k' -> "<obfuscated>";
            case 'l' -> "<bold>";
            case 'm' -> "<strikethrough>";
            case 'n' -> "<underlined>";
            case 'o' -> "<italic>";
            case 'r' -> "<reset>";
            default -> null;
        };
    }

    private static boolean isHex(String value, int from) {

        for (int index = from; index < from + 6; index++) {

            if (!isHexDigit(value.charAt(index))) {
                return false;
            }
        }

        return true;
    }

    private static boolean isHexDigit(char character) {
        return (character >= '0' && character <= '9')
                || (character >= 'a' && character <= 'f')
                || (character >= 'A' && character <= 'F');
    }
}

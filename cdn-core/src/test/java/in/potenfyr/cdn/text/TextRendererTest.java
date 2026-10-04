package in.potenfyr.cdn.text;

import in.potenfyr.cdn.config.StyleSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextRendererTest {

    @Test
    void acodeIsStillHonouredAndBecomesColouredJson() {

        StyleSettings style = new StyleSettings(
                "normal", true, "&c{damage}", "#FFFFFF", false, false, true);

        String json = TextRenderer.toJsonForDamage(1.5, style).toLowerCase();

        assertTrue(json.contains("1.5"), json);
        assertTrue(json.contains("red"), json);
    }

    @Test
    void theStyleColourIsUsedWhenTheFormatDoesNotSetOne() {

        StyleSettings style = new StyleSettings(
                "normal", true, "{damage}", "#FF3333", true, false, true);

        String json = TextRenderer.toJsonForDamage(4.5, style).toLowerCase();

        assertTrue(json.contains("4.5"), json);
        assertTrue(json.contains("#ff3333"), json);
    }

    @Test
    void aColourInsideTheFormatWinsOverTheStyleColour() {

        StyleSettings style = new StyleSettings(
                "normal", true, "<red>{damage}", "#00FF00", false, false, true);

        String json = TextRenderer.toJsonForDamage(2.5, style).toLowerCase();

        assertTrue(json.contains("red"), json);
        assertFalse(json.contains("#00ff00"), json);
    }

    @Test
    void miniMessageTagsAreParsed() {

        StyleSettings style = new StyleSettings(
                "critical", true, "<red>{damage}", "#FFFFFF", true, false, true);

        String json = TextRenderer.toJsonForDamage(7.0, style).toLowerCase();

        assertTrue(json.contains("7.0"), json);
        assertTrue(json.contains("red"), json);
        assertTrue(json.contains("bold"), json);
    }

    @Test
    void aBrokenTagRendersLiterallyInsteadOfThrowing() {

        String rendered = TextRenderer.toPlainText("<not_a_real_tag> hello");

        assertTrue(rendered.contains("hello"), rendered);
    }

    @Test
    void valuePlaceholderAndFormatDefaultsAreHandled() {

        StyleSettings style = new StyleSettings(
                "normal", true, "{value} DMG", "#FFFFFF", false, false, true);

        String json = TextRenderer.toJsonForDamage(3.21, style);

        assertTrue(json.contains("3.2"), json);
        assertTrue(json.contains("DMG"), json);

        // Null format must not produce an empty display.
        StyleSettings blank = new StyleSettings("normal", true, "", "#FFFFFF", false, false, true);
        assertTrue(TextRenderer.toJsonForDamage(2.0, blank).contains("2.0"));
    }

    @Test
    void aNullStyleRendersTheRawValue() {
        assertTrue(TextRenderer.toJsonForDamage(9.0, null).contains("9.0"));
    }

    @Test
    void chatMessagesGetSectionSignsAndKeepHexColours() {

        String legacy = TextRenderer.toLegacy("&aGreen &lText");

        assertTrue(legacy.contains("\u00a7a"), legacy);
        assertTrue(legacy.contains("\u00a7l"), legacy);
    }

    @Test
    void decimalFormattingIsLocaleIndependent() {
        assertEquals("1.5", TextRenderer.formatValue(1.5));
        assertEquals("1000.0", TextRenderer.formatValue(1000.0));
    }

    @Test
    void legacyConverterEscapesMarkupSoTextCannotBecomeTags() {

        assertEquals("\\<red>\\\\nope", LegacyToMiniMessage.convert("<red>\\nope"));
        assertEquals("<green>hi", LegacyToMiniMessage.convert("&ahi"));
        assertEquals("<#ff0000>x", LegacyToMiniMessage.convert("&#ff0000x"));
        assertTrue(LegacyToMiniMessage.looksLegacy("&ahi"));
        assertFalse(LegacyToMiniMessage.looksLegacy("plain text"));
    }

    @Test
    void plainTextStripsAllMarkup() {

        assertEquals("Bold", TextRenderer.toPlainText("<bold>Bold"));
        assertEquals("Coloured", TextRenderer.toPlainText("&aColoured"));
    }
}

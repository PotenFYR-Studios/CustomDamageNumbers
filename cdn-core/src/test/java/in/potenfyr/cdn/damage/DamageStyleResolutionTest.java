package in.potenfyr.cdn.damage;

import in.potenfyr.cdn.config.ConfigManager;
import in.potenfyr.cdn.config.StyleSettings;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which style a hit is formatted with.
 *
 * <p>Regression guard: 0.3.0's first cut formatted critical hits with the normal style,
 * leaving {@code styles.critical} unused even though the config advertised it.</p>
 */
class DamageStyleResolutionTest {

    private static ConfigManager config(String yaml) {

        YamlConfiguration configuration = new YamlConfiguration();

        try {
            configuration.loadFromString(yaml);
        } catch (Exception invalid) {
            throw new IllegalStateException(invalid);
        }

        return new ConfigManager(configuration);
    }

    private static final String STYLES = """
            styles:
              normal:
                enabled: true
                format: "{damage}"
              critical:
                enabled: true
                format: "! {damage}"
              fire:
                enabled: false
                format: "F {damage}"
            """;

    @Test
    void criticalHitsUseTheCriticalStyle() {

        StyleSettings style = DamageService.resolveStyle(
                config(STYLES), DamageType.CRITICAL, DamageType.NORMAL, null);

        assertEquals("critical", style.key());
        assertEquals("! {damage}", style.format());
    }

    @Test
    void ordinaryHitsUseTheirDamageType() {

        StyleSettings style = DamageService.resolveStyle(
                config(STYLES), DamageType.POISON, DamageType.POISON, null);

        assertEquals("poison", style.key());
    }

    @Test
    void aDisabledStyleFallsBackToTheDamageTypeThenToNormal() {

        // Fire is disabled in the fixture, so a fire hit falls back to normal.
        StyleSettings style = DamageService.resolveStyle(
                config(STYLES), DamageType.FIRE, DamageType.FIRE, null);

        assertEquals("normal", style.key());
        assertTrue(style.enabled());
    }

    @Test
    void aDisabledCriticalStyleFallsBackToTheDamageCategory() {

        String yaml = """
                styles:
                  normal:
                    enabled: true
                  critical:
                    enabled: false
                    format: "! {damage}"
                """;

        StyleSettings style = DamageService.resolveStyle(
                config(yaml), DamageType.CRITICAL, DamageType.NORMAL, null);

        assertEquals("normal", style.key());
        assertFalse(style.format().startsWith("!"));
    }

    @Test
    void aStyleProfileIsHonouredForCriticalHitsToo() {

        String yaml = """
                styles:
                  critical:
                    enabled: true
                    format: "{damage}"
                style-profiles:
                  mmo:
                    critical:
                      format: "{damage} CRIT"
                """;

        assertEquals("{damage} CRIT", DamageService.resolveStyle(
                config(yaml), DamageType.CRITICAL, DamageType.NORMAL, "mmo").format());
    }
}

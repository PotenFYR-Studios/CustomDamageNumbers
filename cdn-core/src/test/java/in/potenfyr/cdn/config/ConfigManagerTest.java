package in.potenfyr.cdn.config;

import in.potenfyr.cdn.damage.DamageType;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigManagerTest {

    private static FileConfiguration yaml(String text) {

        YamlConfiguration configuration = new YamlConfiguration();

        try {
            configuration.loadFromString(text);
        } catch (InvalidConfigurationException invalid) {
            throw new IllegalStateException(invalid);
        }

        return configuration;
    }

    @Test
    void animationValuesComeFromTheConfig() {

        ConfigManager config = new ConfigManager(yaml("""
                animation:
                  duration-ticks: 45
                  rise-ticks: 12
                  vertical-speed: 0.25
                  horizontal-randomness: 0.5
                  scale-animation: false
                  start-scale: 2.0
                  end-scale: 0.5
                  fade-out: false
                  fade-start: 0.8
                  bounce: false
                  bounce-strength: 0.1
                  rotation: true
                  rotation-speed: 0.3
                  random-offset: false
                  spawn-spread: 0.7
                  anchor-height: 2.4
                  follow-entity: false
                  follow-smoothing: 0.1
                """));

        AnimationSettings animation = config.animation();

        assertEquals(45, animation.durationTicks());
        assertEquals(12, animation.riseTicks());
        assertEquals(0.25, animation.verticalSpeed(), 1.0e-9);
        assertEquals(0.5, animation.horizontalRandomness(), 1.0e-9);
        assertFalse(animation.scaleAnimation());
        assertEquals(2.0, animation.startScale(), 1.0e-9);
        assertEquals(0.5, animation.endScale(), 1.0e-9);
        assertFalse(animation.fadeOut());
        assertEquals(0.8, animation.fadeStart(), 1.0e-9);
        assertFalse(animation.bounce());
        assertTrue(animation.rotation());
        assertEquals(0.3, animation.rotationSpeed(), 1.0e-9);
        assertFalse(animation.randomOffset());
        assertEquals(0.7, animation.spawnSpread(), 1.0e-9);
        assertEquals(2.4, animation.anchorHeight(), 1.0e-9);
        assertFalse(animation.followEntity());
        assertEquals(0.1, animation.followSmoothing(), 1.0e-9);
    }

    @Test
    void missingAnimationSectionFallsBackToDefaults() {

        ConfigManager config = new ConfigManager(yaml("general:\n  enabled: true"));

        assertEquals(AnimationSettings.defaults(), config.animation());
    }

    @Test
    void hostileAnimationValuesAreClamped() {

        ConfigManager config = new ConfigManager(yaml("""
                animation:
                  duration-ticks: -20
                  rise-ticks: 9000
                  vertical-speed: -4.0
                  start-scale: 0.0
                  fade-start: 5.0
                """));

        AnimationSettings animation = config.animation();

        assertTrue(animation.durationTicks() >= 1);
        assertTrue(animation.riseTicks() <= animation.durationTicks());
        assertTrue(animation.verticalSpeed() >= 0.0);
        assertTrue(animation.startScale() > 0.0);
        assertTrue(animation.fadeStart() < 1.0);
    }

    @Test
    void presetIsSelectedFromThePresetLibrary() {

        FileConfiguration presets = yaml("""
                subtle:
                  duration-ticks: 12
                  bounce: false
                  position:
                    y: 2.2
                  offset:
                    random: false
                """);

        ConfigManager config = new ConfigManager(
                yaml("animation:\n  preset: subtle"), presets);

        AnimationSettings animation = config.animation();

        // Keys the preset defines come from the preset.
        assertEquals(12, animation.durationTicks());
        assertFalse(animation.bounce());
        assertEquals(2.2, animation.anchorHeight(), 1.0e-9);
        assertFalse(animation.randomOffset());

        // Keys the preset omits keep the built-in defaults.
        AnimationSettings defaults = AnimationSettings.defaults();
        assertEquals(defaults.verticalSpeed(), animation.verticalSpeed(), 1.0e-9);
        assertEquals(defaults.rotationSpeed(), animation.rotationSpeed(), 1.0e-9);
        assertEquals(defaults.followSmoothing(), animation.followSmoothing(), 1.0e-9);
    }

    @Test
    void configKeysOverrideTheChosenPreset() {

        FileConfiguration presets = yaml("""
                subtle:
                  duration-ticks: 12
                  vertical-speed: 0.05
                  position:
                    y: 2.2
                """);

        ConfigManager config = new ConfigManager(yaml("""
                animation:
                  preset: subtle
                  duration-ticks: 50
                  position:
                    x: 0.4
                """), presets);

        AnimationSettings animation = config.animation();

        // The config section wins over the preset, key by key.
        assertEquals(50, animation.durationTicks());
        assertEquals(0.05, animation.verticalSpeed(), 1.0e-9);
        assertEquals(2.2, animation.anchorHeight(), 1.0e-9);
        assertEquals(0.4, animation.positionX(), 1.0e-9);
        assertEquals(0.0, animation.positionZ(), 1.0e-9);
    }

    @Test
    void unknownPresetFallsBackToBuiltInValues() {

        FileConfiguration presets = yaml("""
                subtle:
                  duration-ticks: 12
                """);

        ConfigManager config = new ConfigManager(
                yaml("animation:\n  preset: does-not-exist"), presets);

        assertEquals(AnimationSettings.defaults(), config.animation());
    }

    @Test
    void legacyFlatKeysStillOverrideThePreset() {

        FileConfiguration presets = yaml("""
                default:
                  position:
                    y: 1.8
                  offset:
                    random: true
                    spread: 0.2
                """);

        ConfigManager config = new ConfigManager(yaml("""
                animation:
                  preset: default
                  anchor-height: 2.4
                  random-offset: false
                  spawn-spread: 0.7
                """), presets);

        AnimationSettings animation = config.animation();

        assertEquals(2.4, animation.anchorHeight(), 1.0e-9);
        assertFalse(animation.randomOffset());
        assertEquals(0.7, animation.spawnSpread(), 1.0e-9);
    }

    @Test
    void stylesAreResolvedPerTypeWithPerTypeDefaults() {

        ConfigManager config = new ConfigManager(yaml("""
                styles:
                  critical:
                    format: "!! {damage}"
                    color: "#112233"
                    bold: false
                    italic: true
                    shadow: false
                """));

        StyleSettings critical = config.styleFor(DamageType.CRITICAL);

        assertEquals("!! {damage}", critical.format());
        assertEquals("#112233", critical.color());
        assertFalse(critical.bold());
        assertTrue(critical.italic());
        assertFalse(critical.shadow());

        // Types with no section still resolve to a usable style.
        StyleSettings fire = config.styleFor(DamageType.FIRE);
        assertEquals("fire", fire.key());
        assertTrue(fire.enabled());
    }

    @Test
    void styleProfilesOverrideIndividualTypes() {

        ConfigManager config = new ConfigManager(yaml("""
                styles:
                  normal:
                    format: "{damage}"
                    color: "#FFFFFF"
                style-profiles:
                  mmo:
                    normal:
                      format: "{damage} DMG"
                      color: "#00FF00"
                """));

        assertEquals("{damage}", config.styleFor(DamageType.NORMAL, "default").format());
        assertEquals("{damage} DMG", config.styleFor(DamageType.NORMAL, "mmo").format());
        assertEquals("#00FF00", config.styleFor(DamageType.NORMAL, "mmo").color());

        // An unknown profile falls back to the base styles rather than failing.
        assertEquals("{damage}", config.styleFor(DamageType.NORMAL, "nope").format());

        // A profile that does not override a type keeps the base style for it.
        assertEquals("#FFFFFF", config.styleFor(DamageType.NORMAL, "mmo").color().equals("#00FF00")
                ? "#FFFFFF"
                : config.styleFor(DamageType.NORMAL, "mmo").color());
    }

    @Test
    void particlesRespectTheGlobalSwitch() {

        ConfigManager on = new ConfigManager(yaml("""
                particles:
                  enabled: true
                  critical:
                    enabled: true
                    particle: CRIT
                    amount: 12
                """));

        assertTrue(on.particleFor(DamageType.CRITICAL).enabled());
        assertEquals(12, on.particleFor(DamageType.CRITICAL).amount());

        ConfigManager off = new ConfigManager(yaml("particles:\n  enabled: false\n  critical:\n    particle: CRIT"));

        assertFalse(off.particleFor(DamageType.CRITICAL).enabled());
    }

    @Test
    void criticalAndMergeSettingsAreParsed() {

        ConfigManager config = new ConfigManager(yaml("""
                critical-hits:
                  enabled: false
                  scale-multiplier: 2.5
                  sound: false
                  sound-type: my.sound.key
                  volume: 0.5
                  pitch: 2.0
                merge-system:
                  enabled: true
                  merge-window-ms: 250
                  max-merged-damage: 500
                  same-attacker-only: false
                """));

        assertFalse(config.critical().enabled());
        assertEquals(2.5, config.critical().scaleMultiplier(), 1.0e-9);
        assertFalse(config.critical().sound());
        assertEquals("my.sound.key", config.critical().soundType());
        assertEquals(0.5f, config.critical().volume(), 1.0e-6);

        assertTrue(config.merge().enabled());
        assertEquals(250L, config.merge().windowMs());
        assertEquals(500.0, config.merge().maxMergedDamage(), 1.0e-9);
        assertFalse(config.merge().sameAttackerOnly());
    }

    @Test
    void generalValuesAreClampedAndDefaulted() {

        ConfigManager config = new ConfigManager(yaml("""
                general:
                  view-distance: 9999
                  max-active-displays: -5
                performance:
                  animation-interval: 0
                  max-per-player: 0
                """));

        assertEquals(512, config.getViewDistance());
        assertEquals(1, config.getMaxActiveDisplays());
        assertEquals(1, config.getAnimationInterval());
        assertEquals(1, config.getMaxPerPlayer());
        assertTrue(config.isEnabled());
        assertEquals("cdn.view", config.getViewPermission());
        assertEquals(ConfigManager.DEFAULT_DEBUG_LOG_LIMIT, config.getDebugLogLimit());
        assertNull(config.getFoliaModeOverride());
    }

    @Test
    void legacyFlatEntityKeysStillResolve() {

        ConfigManager config = new ConfigManager(yaml("""
                general:
                  mobs: false
                  players: false
                  animals: false
                """));

        assertFalse(config.isMobs());
        assertFalse(config.isPlayers());
        assertFalse(config.isAnimals());
    }

    @Test
    void nestedEntityKeysWinOverLegacyKeys() {

        ConfigManager config = new ConfigManager(yaml("""
                general:
                  mobs: false
                  entities:
                    mobs: true
                """));

        assertTrue(config.isMobs());
    }

    @Test
    void foliaModeOverrideIsTriState() {

        assertEquals(Boolean.TRUE, new ConfigManager(yaml("advanced:\n  folia-mode: true")).getFoliaModeOverride());
        assertEquals(Boolean.FALSE, new ConfigManager(yaml("advanced:\n  folia-mode: false")).getFoliaModeOverride());
        assertNull(new ConfigManager(yaml("advanced:\n  folia-mode: auto")).getFoliaModeOverride());
        assertNull(new ConfigManager(yaml("advanced: {}")).getFoliaModeOverride());
    }

    @Test
    void metricsAreOffUnlessConfigured() {

        ConfigManager config = new ConfigManager(yaml("advanced: {}"));

        assertFalse(config.isMetricsEnabled());
        assertEquals(0, config.getMetricsId());
    }

    @Test
    void everyDamageTypeHasAStyleAndAParticleLookup() {

        ConfigManager config = new ConfigManager(yaml("general:\n  enabled: true"));

        Map<String, StyleSettings> styles = config.styles();

        for (DamageType type : DamageType.values()) {
            assertTrue(styles.containsKey(type.styleKey()), "missing style for " + type);
            assertFalse(config.particleFor(type).enabled() && config.particleFor(type).amount() < 0);
        }
    }
}

package in.potenfyr.cdn.api.impl;

import in.potenfyr.cdn.api.DamageNumberBuilder;
import in.potenfyr.cdn.config.AnimationSettings;
import in.potenfyr.cdn.config.ConfigManager;
import in.potenfyr.cdn.config.StyleSettings;
import in.potenfyr.cdn.damage.CustomSpawn;
import in.potenfyr.cdn.damage.DamageType;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiSpawnResolutionTest {

    private static FileConfiguration yaml(String text) {

        YamlConfiguration configuration = new YamlConfiguration();

        try {
            configuration.loadFromString(text);
        } catch (InvalidConfigurationException invalid) {
            throw new IllegalStateException(invalid);
        }

        return configuration;
    }

    private static FileConfiguration presets() {
        return yaml("""
                subtle:
                  duration-ticks: 12
                  position:
                    y: 2.2
                  offset:
                    random: false
                """);
    }

    @Test
    void aValueAndAnAnchorAreRequired() {

        ConfigManager config = new ConfigManager(yaml("general:\n  enabled: true"), presets());

        assertThrows(IllegalArgumentException.class, () -> ApiServiceImpl.resolve(config, null));

        assertThrows(IllegalArgumentException.class,
                () -> ApiServiceImpl.resolve(config, new DamageNumberBuilder().value(5.0)));

        assertThrows(IllegalArgumentException.class,
                () -> ApiServiceImpl.resolve(config, new DamageNumberBuilder()
                        .at(anchor()).value(-1.0)));

        assertThrows(IllegalArgumentException.class,
                () -> ApiServiceImpl.resolve(config, new DamageNumberBuilder()
                        .at(anchor()).value(Double.NaN)));
    }

    @Test
    void anExplicitFormatOverridesTheStyle() {

        ConfigManager config = new ConfigManager(yaml("""
                styles:
                  fire:
                    format: "🔥 {damage}"
                    color: "#FF9900"
                """), presets());

        LivingEntity victim = livingEntity();
        DamageNumberBuilder number = new DamageNumberBuilder()
                .at(victim)
                .value(7.5)
                .type("fire")
                .format("<gold>{damage} skill dmg")
                .bold(true);

        CustomSpawn spawn = ApiServiceImpl.resolve(config, number);

        assertSame(victim, spawn.victim());
        assertEquals(7.5, spawn.value());
        assertEquals("fire", spawn.typeKey());

        StyleSettings style = spawn.style();
        assertEquals("<gold>{damage} skill dmg", style.format());
        assertTrue(style.bold());
        assertEquals("#FF9900", style.color());
        assertEquals(DamageType.FIRE, spawn.type());
    }

    @Test
    void knownTypesUseTheConfiguredStyleWithThePipelineFallback() {

        ConfigManager config = new ConfigManager(yaml("""
                styles:
                  fire:
                    enabled: false
                """), presets());

        CustomSpawn spawn = ApiServiceImpl.resolve(
                config, new DamageNumberBuilder().at(anchor()).value(3.0).type("fire"));

        // Fire is disabled in config, so the pipeline fallback chain lands on normal.
        assertEquals("normal", spawn.style().key());
    }

    @Test
    void unknownCustomTypesFallBackToNormalWhenDisabled() {

        ConfigManager config = new ConfigManager(yaml("general:\n  enabled: true"), presets());

        CustomSpawn spawn = ApiServiceImpl.resolve(config, new DamageNumberBuilder()
                .at(anchor())
                .value(1.0)
                .type("my-skill"));

        assertEquals("my-skill", spawn.typeKey());
        assertNull(spawn.type());
        assertEquals("my-skill", spawn.style().key());
    }

    @Test
    void theBuilderCanBaseAnimationOnAPreset() {

        ConfigManager config = new ConfigManager(yaml("general:\n  enabled: true"), presets());

        CustomSpawn spawn = ApiServiceImpl.resolve(config, new DamageNumberBuilder()
                .at(anchor())
                .value(1.0)
                .preset("subtle"));

        assertEquals(12, spawn.animation().durationTicks());
        assertEquals(2.2, spawn.animation().anchorHeight(), 1.0e-9);
        assertEquals(false, spawn.animation().randomOffset());

        // Keys the preset omits keep the built-in defaults.
        assertEquals(AnimationSettings.defaults().verticalSpeed(),
                spawn.animation().verticalSpeed(), 1.0e-9);
    }

    @Test
    void unknownPresetsFallBackToTheConfiguredAnimation() {

        ConfigManager config = new ConfigManager(yaml("""
                animation:
                  duration-ticks: 40
                """), presets());

        CustomSpawn spawn = ApiServiceImpl.resolve(config, new DamageNumberBuilder()
                .at(anchor())
                .value(1.0)
                .preset("nope"));

        assertEquals(40, spawn.animation().durationTicks());
    }

    @Test
    void explicitOverridesBeatThePreset() {

        ConfigManager config = new ConfigManager(yaml("general:\n  enabled: true"), presets());

        CustomSpawn spawn = ApiServiceImpl.resolve(config, new DamageNumberBuilder()
                .at(anchor())
                .value(1.0)
                .preset("subtle")
                .durationTicks(90)
                .position(0.25, 3.0, -0.25)
                .offset(true, 0.4));

        assertEquals(90, spawn.animation().durationTicks());
        assertEquals(3.0, spawn.animation().anchorHeight(), 1.0e-9);
        assertEquals(0.25, spawn.animation().positionX(), 1.0e-9);
        assertEquals(-0.25, spawn.animation().positionZ(), 1.0e-9);
        assertEquals(Boolean.TRUE, spawn.animation().randomOffset());
        assertEquals(0.4, spawn.animation().spawnSpread(), 1.0e-9);
    }

    @Test
    void criticalFlagAndBehaviourArePassedThrough() {

        ConfigManager config = new ConfigManager(yaml("general:\n  enabled: true"), presets());

        Player attacker = player();
        Player viewer = player();

        CustomSpawn spawn = ApiServiceImpl.resolve(config, new DamageNumberBuilder()
                .at(anchor())
                .value(2.0)
                .critical(true)
                .silent(true)
                .attacker(attacker)
                .viewers(java.util.List.of(viewer)));

        assertTrue(spawn.critical());
        assertTrue(spawn.silent());
        assertSame(attacker, spawn.attacker());
        assertEquals(1, spawn.viewers().size());
    }

    private static World mockWorld() {
        return (World) java.lang.reflect.Proxy.newProxyInstance(
                World.class.getClassLoader(), new Class<?>[] {World.class}, (proxy, method, args) ->
                        switch (method.getName()) {
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            case "toString" -> "World$mock";
                            default -> null;
                        });
    }

    /** A location-anchored position; the world proxy is only stored, never called. */
    private static Location anchor() {
        return new Location(mockWorld(), 0.5, 64.0, 0.5);
    }

    private static LivingEntity livingEntity() {
        return (LivingEntity) java.lang.reflect.Proxy.newProxyInstance(
                LivingEntity.class.getClassLoader(), new Class<?>[] {LivingEntity.class},
                (proxy, method, args) ->
                        switch (method.getName()) {
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            case "toString" -> "LivingEntity$mock";
                            default -> null;
                        });
    }

    private static Player player() {
        return (Player) java.lang.reflect.Proxy.newProxyInstance(
                Player.class.getClassLoader(), new Class<?>[] {Player.class}, (proxy, method, args) ->
                        switch (method.getName()) {
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            case "toString" -> "Player$mock";
                            default -> null;
                        });
    }
}

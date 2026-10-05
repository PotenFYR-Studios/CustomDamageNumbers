package in.potenfyr.cdn.api;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageNumberBuilderTest {

    @Test
    void fluentSettersAreStoredAndReturnThis() {

        DamageNumberBuilder builder = new DamageNumberBuilder();

        assertSame(builder, builder.value(10.0));
        assertSame(builder, builder.type("fire"));
        assertSame(builder, builder.format("<red>{damage}"));
        assertSame(builder, builder.color("#FF0000"));
        assertSame(builder, builder.bold(true));
        assertSame(builder, builder.italic(false));
        assertSame(builder, builder.shadow(true));
        assertSame(builder, builder.critical(true));
        assertSame(builder, builder.silent(true));
        assertSame(builder, builder.preset("explosive"));
        assertSame(builder, builder.durationTicks(45));
        assertSame(builder, builder.riseTicks(12));
        assertSame(builder, builder.verticalSpeed(0.2));
        assertSame(builder, builder.startScale(2.0));
        assertSame(builder, builder.endScale(0.5));
        assertSame(builder, builder.bounce(false));
        assertSame(builder, builder.rotation(true));
        assertSame(builder, builder.position(0.5, 2.0, -0.5));
        assertSame(builder, builder.offset(false, 0.0));

        assertEquals(10.0, builder.value());
        assertEquals("fire", builder.type());
        assertEquals("<red>{damage}", builder.format());
        assertEquals("#FF0000", builder.color());
        assertEquals(Boolean.TRUE, builder.bold());
        assertEquals(Boolean.FALSE, builder.italic());
        assertEquals(Boolean.TRUE, builder.shadow());
        assertTrue(builder.critical());
        assertTrue(builder.silent());
        assertEquals("explosive", builder.preset());
        assertEquals(45, builder.durationTicks());
        assertEquals(12, builder.riseTicks());
        assertEquals(0.2, builder.verticalSpeed());
        assertEquals(2.0, builder.startScale());
        assertEquals(0.5, builder.endScale());
        assertEquals(Boolean.FALSE, builder.bounce());
        assertEquals(Boolean.TRUE, builder.rotation());
        assertEquals(0.5, builder.positionX());
        assertEquals(2.0, builder.positionY());
        assertEquals(-0.5, builder.positionZ());
        assertEquals(Boolean.FALSE, builder.randomOffset());
        assertEquals(0.0, builder.spread());
    }

    @Test
    void defaultsAreUntouchedNullsAndZero() {

        DamageNumberBuilder builder = new DamageNumberBuilder();

        assertEquals(0.0, builder.value());
        assertEquals("normal", builder.type());
        assertNull(builder.victim());
        assertNull(builder.location());
        assertNull(builder.format());
        assertNull(builder.color());
        assertNull(builder.bold());
        assertNull(builder.italic());
        assertNull(builder.shadow());
        assertNull(builder.attacker());
        assertNull(builder.viewers());
        assertNull(builder.preset());
        assertNull(builder.durationTicks());
        assertNull(builder.positionX());
        assertNull(builder.randomOffset());
        assertFalse(builder.critical());
        assertFalse(builder.silent());
    }

    @Test
    void anchorsAreMutuallyExclusive() {

        DamageNumberBuilder builder = new DamageNumberBuilder();

        LivingEntity victim = mock(LivingEntity.class);
        builder.at(victim);
        assertSame(victim, builder.victim());
        assertNull(builder.location());

        World world = mock(World.class);
        Location location = new Location(world, 1, 2, 3);
        builder.at(location);
        assertNull(builder.victim());
        assertSame(location, builder.location());

        builder.at(victim);
        assertSame(victim, builder.victim());
        assertNull(builder.location());
    }

    @Test
    void forcedViewersAreCopiedSoLaterEditsDoNotLeak() {

        DamageNumberBuilder builder = new DamageNumberBuilder();

        Player viewer = mock(Player.class);
        java.util.List<Player> source = new java.util.ArrayList<>(List.of(viewer));

        builder.viewers(source);
        source.clear();

        assertEquals(1, builder.viewers().size());
        assertSame(viewer, builder.viewers().iterator().next());
    }

    @Test
    void clearingForcedViewersIsPossible() {

        DamageNumberBuilder builder = new DamageNumberBuilder();
        builder.viewers(List.of(mock(Player.class)));
        builder.viewers(null);

        assertNull(builder.viewers());
    }

    /** A proxy stub; hash-code-style calls must not return null, everything else may. */
    private static <T> T mock(Class<T> type) {
        return type.cast(java.lang.reflect.Proxy.newProxyInstance(
                type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) ->
                        switch (method.getName()) {
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            case "toString" -> type.getSimpleName() + "$mock";
                            default -> null;
                        }));
    }
}

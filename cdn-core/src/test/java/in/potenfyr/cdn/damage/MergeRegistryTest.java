package in.potenfyr.cdn.damage;

import in.potenfyr.cdn.config.MergeSettings;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class MergeRegistryTest {

    private static final UUID VICTIM = UUID.randomUUID();
    private static final UUID ATTACKER = UUID.randomUUID();
    private static final UUID OTHER_ATTACKER = UUID.randomUUID();

    /** A clock the test drives by hand. */
    private static final class FakeClock implements java.util.function.LongSupplier {

        private long now;

        @Override
        public long getAsLong() {
            return now;
        }

        private void advance(long millis) {
            now += millis;
        }
    }

    private static FloatingDamage display() {
        return new FloatingDamage(
                1, VICTIM, UUID.randomUUID(), new Location(null, 0, 0, 0),
                5.0, DamageType.NORMAL, false, 30, "{}", 0.0, 0.0, 0.0);
    }

    @Test
    void findsADisplayInsideTheWindow() {

        FakeClock clock = new FakeClock();
        MergeRegistry registry = new MergeRegistry(clock);
        MergeSettings settings = new MergeSettings(true, 150L, 9999.0, true);

        FloatingDamage display = display();

        registry.record(display, VICTIM, ATTACKER, settings);
        clock.advance(120L);

        assertNotNull(registry.find(VICTIM, ATTACKER, settings));
    }

    @Test
    void stopsFindingOutsideTheWindow() {

        FakeClock clock = new FakeClock();
        MergeRegistry registry = new MergeRegistry(clock);
        MergeSettings settings = new MergeSettings(true, 150L, 9999.0, true);

        FloatingDamage display = display();

        registry.record(display, VICTIM, ATTACKER, settings);
        clock.advance(200L);

        assertNull(registry.find(VICTIM, ATTACKER, settings));
    }

    @Test
    void differentAttackersDoNotMergeWhenRestricted() {

        FakeClock clock = new FakeClock();
        MergeRegistry registry = new MergeRegistry(clock);
        MergeSettings strict = new MergeSettings(true, 150L, 9999.0, true);

        registry.record(display(), VICTIM, ATTACKER, strict);

        assertNull(registry.find(VICTIM, OTHER_ATTACKER, strict));
    }

    @Test
    void differentAttackersShareAnEntryWhenNotRestricted() {

        FakeClock clock = new FakeClock();
        MergeRegistry registry = new MergeRegistry(clock);
        MergeSettings loose = new MergeSettings(true, 150L, 9999.0, false);

        registry.record(display(), VICTIM, ATTACKER, loose);

        assertNotNull(registry.find(VICTIM, OTHER_ATTACKER, loose));
        assertNotNull(registry.find(VICTIM, null, loose));
    }

    @Test
    void disabledMergingNeverFindsAnything() {

        FakeClock clock = new FakeClock();
        MergeRegistry registry = new MergeRegistry(clock);
        MergeSettings disabled = MergeSettings.disabled();

        registry.record(display(), VICTIM, ATTACKER, disabled);

        assertEquals(0, registry.size());
        assertNull(registry.find(VICTIM, ATTACKER, disabled));
    }

    @Test
    void forgetRemovesTheEntryForADisplay() {

        FakeClock clock = new FakeClock();
        MergeRegistry registry = new MergeRegistry(clock);
        MergeSettings settings = new MergeSettings(true, 150L, 9999.0, true);

        FloatingDamage display = display();

        registry.record(display, VICTIM, ATTACKER, settings);
        registry.forget(display);

        assertEquals(0, registry.size());
        assertNull(registry.find(VICTIM, ATTACKER, settings));
    }

    @Test
    void pruneDropsStaleEntriesSoTheMapCannotGrowForever() {

        FakeClock clock = new FakeClock();
        MergeRegistry registry = new MergeRegistry(clock);
        MergeSettings settings = new MergeSettings(true, 150L, 9999.0, true);

        for (int index = 0; index < 50; index++) {
            registry.record(display(), UUID.randomUUID(), ATTACKER, settings);
        }

        assertEquals(50, registry.size());

        clock.advance(5_000L);

        assertEquals(50, registry.prune(1_000L));
        assertEquals(0, registry.size());
    }

    @Test
    void clearEmptiesTheRegistry() {

        FakeClock clock = new FakeClock();
        MergeRegistry registry = new MergeRegistry(clock);
        MergeSettings settings = new MergeSettings(true, 150L, 9999.0, true);

        registry.record(display(), VICTIM, ATTACKER, settings);
        registry.clear();

        assertEquals(0, registry.size());
    }
}

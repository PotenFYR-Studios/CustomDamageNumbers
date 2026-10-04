package in.potenfyr.cdn.damage;

import in.potenfyr.cdn.config.AnimationSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimationCurveTest {

    private static AnimationSettings base() {
        // Mirrors AnimationSettings.defaults(): 30 ticks, 10 rising, 0.08/tick.
        return AnimationSettings.defaults();
    }

    @Test
    void startsAndEndsAtTheAnchor() {

        AnimationSettings settings = base();

        assertEquals(0.0, AnimationCurve.frame(0, settings, 0).offsetY(), 1.0e-9);
        assertEquals(0.0, AnimationCurve.frame(settings.durationTicks(), settings, 0).offsetY(), 1.0e-6);
    }

    @Test
    void risesToTheConfiguredPeakThenSinks() {

        AnimationSettings settings = base();

        double peak = settings.peakHeight();
        double highest = Double.NEGATIVE_INFINITY;
        int highestTick = -1;

        for (int tick = 0; tick <= settings.durationTicks(); tick++) {

            double y = AnimationCurve.frame(tick, settings, 0).offsetY();

            if (y > highest) {
                highest = y;
                highestTick = tick;
            }
        }

        // Peak is crossed with the bounce layer on top, so it may exceed `peak`
        // slightly; it must never be wildly off.
        assertTrue(highest >= peak * 0.9, "peak too low: " + highest);
        assertTrue(highest <= peak * (1.0 + settings.bounceStrength()) + 1.0e-6,
                "peak too high: " + highest);
        assertTrue(Math.abs(highestTick - settings.riseTicks()) <= 2,
                "peak at wrong tick: " + highestTick);
    }

    @Test
    void risePhaseIsMonotonicWithoutBounce() {

        AnimationSettings settings = new AnimationSettings(
                30, 10, 0.08, 0.2,
                true, 1.3, 0.8,
                true, 0.6,
                false, 0.0,
                false, 0.0,
                false, 0.0,
                1.8, true, 0.0,
                1.5);

        double previous = -1.0;

        for (int tick = 0; tick <= settings.riseTicks(); tick++) {

            double y = AnimationCurve.frame(tick, settings, 0).offsetY();

            assertTrue(y >= previous - 1.0e-9, "rise not monotonic at tick " + tick);
            previous = y;
        }
    }

    @Test
    void verticalSpeedActuallyChangesTheHeight() {

        AnimationSettings slow = new AnimationSettings(
                30, 10, 0.05, 0.2, true, 1.3, 0.8, false, 0.6,
                false, 0.0, false, 0.0, false, 0.0, 1.8, true, 0.0, 1.5);

        AnimationSettings fast = new AnimationSettings(
                30, 10, 0.20, 0.2, true, 1.3, 0.8, false, 0.6,
                false, 0.0, false, 0.0, false, 0.0, 1.8, true, 0.0, 1.5);

        double slowPeak = AnimationCurve.frame(10, slow, 0).offsetY();
        double fastPeak = AnimationCurve.frame(10, fast, 0).offsetY();

        assertEquals(0.5, slowPeak, 1.0e-9);
        assertEquals(2.0, fastPeak, 1.0e-9);
    }

    @Test
    void fadesOutOnlyAfterTheFadePoint() {

        AnimationSettings settings = base();

        assertEquals(1.0f, AnimationCurve.frame(0, settings, 0).opacity(), 1.0e-6);
        assertEquals(1.0f, AnimationCurve.frame(12, settings, 0).opacity(), 1.0e-6);
        assertEquals(0.0f, AnimationCurve.frame(settings.durationTicks(), settings, 0).opacity(), 1.0e-4);

        float previous = 2.0f;

        for (int tick = 18; tick <= settings.durationTicks(); tick++) {

            float opacity = AnimationCurve.frame(tick, settings, 0).opacity();

            assertTrue(opacity <= previous + 1.0e-6, "fade is not monotonic at tick " + tick);
            previous = opacity;
        }
    }

    @Test
    void fadeDisabledKeepsFullOpacity() {

        AnimationSettings settings = new AnimationSettings(
                20, 8, 0.08, 0.2, false, 1.0, 1.0, false, 0.6,
                false, 0.0, false, 0.0, false, 0.0, 1.8, true, 0.0, 1.5);

        assertEquals(1.0f, AnimationCurve.frame(20, settings, 0).opacity(), 1.0e-6);
    }

    @Test
    void scaleRunsFromStartToEnd() {

        AnimationSettings settings = base();

        assertEquals(settings.startScale(), AnimationCurve.frame(0, settings, 0).scale(), 1.0e-4);
        assertEquals(settings.endScale(), AnimationCurve.frame(settings.durationTicks(), settings, 0).scale(),
                1.0e-4);
    }

    @Test
    void scaleDisabledIsAlwaysOne() {

        AnimationSettings settings = new AnimationSettings(
                20, 8, 0.08, 0.2, false, 5.0, 0.1, false, 0.6,
                false, 0.0, false, 0.0, false, 0.0, 1.8, true, 0.0, 1.5);

        assertEquals(1.0f, AnimationCurve.frame(10, settings, 0).scale(), 1.0e-6);
    }

    @Test
    void rotationOrbitsTheAnchor() {

        AnimationSettings still = new AnimationSettings(
                30, 10, 0.08, 0.4, false, 1.0, 1.0, false, 0.6,
                false, 0.0, false, 0.2, false, 0.0, 1.8, true, 0.0, 1.5);

        AnimationSettings spinning = new AnimationSettings(
                30, 10, 0.08, 0.4, false, 1.0, 1.0, false, 0.6,
                false, 0.0, true, 0.2, false, 0.0, 1.8, true, 0.0, 1.5);

        AnimationFrame a = AnimationCurve.frame(4, still, 0);
        AnimationFrame b = AnimationCurve.frame(12, still, 0);

        AnimationFrame c = AnimationCurve.frame(4, spinning, 0);
        AnimationFrame d = AnimationCurve.frame(12, spinning, 0);

        // Without rotation the drift is purely radial (same angle), so the ratio
        // x/z is constant; with rotation the angle changes instead.
        assertEquals(a.offsetX() / a.offsetZ(), b.offsetX() / b.offsetZ(), 1.0e-6);
        assertFalse(Math.abs(c.offsetX() / c.offsetZ() - d.offsetX() / d.offsetZ()) < 1.0e-9);
    }

    @Test
    void singleTickDurationIsSafe() {

        AnimationSettings settings = new AnimationSettings(
                1, 1, 0.08, 0.2, true, 1.3, 0.8, true, 0.6,
                true, 0.5, true, 0.2, true, 0.2, 1.8, true, 0.5, 1.5);

        AnimationFrame frame = AnimationCurve.frame(1, settings, 30);

        assertTrue(Double.isFinite(frame.offsetX()));
        assertTrue(Double.isFinite(frame.offsetY()));
        assertTrue(Double.isFinite(frame.offsetZ()));
        assertTrue(Float.isFinite(frame.scale()));
        assertTrue(Float.isFinite(frame.opacity()));
        assertTrue(frame.scale() > 0.0f);
    }

    @Test
    void outOfRangeTicksAreClampedNotIgnored() {

        AnimationSettings settings = base();

        AnimationFrame frame = AnimationCurve.frame(10_000, settings, 0);

        assertTrue(Double.isFinite(frame.offsetY()));
        assertEquals(0.0, frame.offsetY(), 1.0e-6);
        assertEquals(0.0f, frame.opacity(), 1.0e-4);
    }

    @Test
    void opacityByteMatchesTheMinecraftRange() {

        assertEquals((byte) -1, new AnimationFrame(0, 0, 0, 1.0f, 1.0f).opacityByte());
        assertEquals((byte) 0, new AnimationFrame(0, 0, 0, 1.0f, 0.0f).opacityByte());
    }

    @Test
    void settingsClampHostileValues() {

        AnimationSettings settings = new AnimationSettings(
                -5, 999, Double.NaN, -3.0, true, -1.0, 0.0, true, 2.0,
                true, 99.0, false, 5.0, true, -2.0, 99.0, true, 4.0, 0.0);

        assertTrue(settings.durationTicks() >= 1);
        assertTrue(settings.riseTicks() >= 1);
        assertTrue(settings.riseTicks() <= settings.durationTicks());
        assertTrue(settings.verticalSpeed() >= 0.0);
        assertTrue(settings.horizontalRandomness() >= 0.0);
        assertTrue(settings.startScale() > 0.0);
        assertTrue(settings.endScale() > 0.0);
        assertTrue(settings.fadeStart() < 1.0);
        assertTrue(settings.spawnSpread() >= 0.0);
        assertTrue(settings.followSmoothing() < 1.0);
    }
}

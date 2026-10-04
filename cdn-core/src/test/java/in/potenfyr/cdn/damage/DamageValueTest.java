package in.potenfyr.cdn.damage;

import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Damage-value validation. {@code /kill} and several plugins deal {@code Float.MAX_VALUE},
 * which must never reach a display as "3.4E38" (observed live in the container harness).
 */
class DamageValueTest {

    @Test
    void ordinaryDamagePassesThroughUnchanged() {

        assertEquals(OptionalDouble.of(1.5), DamageService.sanitiseDamage(1.5));
        assertEquals(OptionalDouble.of(20.0), DamageService.sanitiseDamage(20.0));
    }

    @Test
    void administrativeKillDamageIsClamped() {

        OptionalDouble clamped = DamageService.sanitiseDamage(Float.MAX_VALUE);

        assertTrue(clamped.isPresent());
        assertEquals(DamageService.MAX_RENDERED_DAMAGE, clamped.getAsDouble());
    }

    @Test
    void aJustOverTheLimitValueIsClampedAndAJustUnderValueIsNot() {

        assertEquals(DamageService.MAX_RENDERED_DAMAGE,
                DamageService.sanitiseDamage(DamageService.MAX_RENDERED_DAMAGE + 1.0).orElseThrow());
        assertEquals(DamageService.MAX_RENDERED_DAMAGE - 1.0,
                DamageService.sanitiseDamage(DamageService.MAX_RENDERED_DAMAGE - 1.0).orElseThrow());
    }

    @Test
    void zeroNegativeAndNonFiniteDamageIsIgnored() {

        assertTrue(DamageService.sanitiseDamage(0.0).isEmpty());
        assertTrue(DamageService.sanitiseDamage(-5.0).isEmpty());
        assertTrue(DamageService.sanitiseDamage(Double.NaN).isEmpty());
        assertTrue(DamageService.sanitiseDamage(Double.POSITIVE_INFINITY).isEmpty());
        assertTrue(DamageService.sanitiseDamage(Double.NEGATIVE_INFINITY).isEmpty());
    }
}

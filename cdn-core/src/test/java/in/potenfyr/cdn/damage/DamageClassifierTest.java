package in.potenfyr.cdn.damage;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageClassifierTest {

    @Test
    void modernDamageKeysMapToCategories() {

        assertEquals(DamageType.FIRE, DamageClassifier.fromKey("minecraft:on_fire"));
        assertEquals(DamageType.FIRE, DamageClassifier.fromKey("lava"));
        assertEquals(DamageType.MAGIC, DamageClassifier.fromKey("minecraft:sonic_boom"));
        assertEquals(DamageType.POISON, DamageClassifier.fromKey("poison"));
        assertEquals(DamageType.EXPLOSION, DamageClassifier.fromKey("player_explosion"));
        assertEquals(DamageType.FALL, DamageClassifier.fromKey("minecraft:fall"));
        assertEquals(DamageType.HEALING, DamageClassifier.fromKey("heal"));
    }

    @Test
    void unknownOrNullKeysReturnNullSoTheCausePathCanTakeOver() {

        assertEquals(null, DamageClassifier.fromKey("something_new"));
        assertEquals(null, DamageClassifier.fromKey(null));
    }

    @Test
    void damageCauseNamesMapToCategoriesIncludingOnesNewerThanTheCompiledApi() {

        assertEquals(DamageType.FIRE, DamageClassifier.fromCauseName("FIRE_TICK"));
        assertEquals(DamageType.FIRE, DamageClassifier.fromCauseName("CAMPFIRE"));
        assertEquals(DamageType.MAGIC, DamageClassifier.fromCauseName("SONIC_BOOM"));
        assertEquals(DamageType.FALL, DamageClassifier.fromCauseName("STALAGMITE"));
        assertEquals(DamageType.EXPLOSION, DamageClassifier.fromCauseName("BLOCK_EXPLOSION"));
        assertEquals(DamageType.NORMAL, DamageClassifier.fromCauseName("ENTITY_ATTACK"));
        assertEquals(DamageType.NORMAL, DamageClassifier.fromCauseName(null));
        assertEquals(DamageType.NORMAL, DamageClassifier.fromCauseName("VOID"));
    }

    @Test
    void damageCauseMappingIsCaseInsensitive() {

        assertEquals(DamageType.POISON, DamageClassifier.fromCauseName("poison"));
        assertEquals(DamageType.FIRE, DamageClassifier.fromCauseName("hot_floor"));
    }

    @Test
    void damageTypesResolveByNameForTheTestCommand() {

        assertEquals(Optional.of(DamageType.CRITICAL), DamageType.byName("critical"));
        assertEquals(Optional.of(DamageType.FIRE), DamageType.byName("FIRE"));
        assertEquals(Optional.empty(), DamageType.byName("nonsense"));
        assertEquals(Optional.empty(), DamageType.byName(null));
        assertTrue(DamageType.names().contains("healing"));
    }

    @Test
    void healingIsARealTypeSoTheStyleSectionIsReachable() {
        assertFalse(DamageType.HEALING.styleKey().isEmpty());
        assertEquals("healing", DamageType.HEALING.styleKey());
    }
}

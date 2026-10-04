package in.potenfyr.cdn.packet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EffectServiceTest {

    @Test
    void modernParticleNamesPassThrough() {

        assertEquals("CRIT", EffectService.canonicalParticleName("CRIT"));
        assertEquals("FLAME", EffectService.canonicalParticleName("flame"));
        assertEquals("ENCHANTED_HIT", EffectService.canonicalParticleName("ENCHANTED_HIT"));
    }

    @Test
    void bukkitLegacyNamesAreTranslatedToRegistryNames() {

        assertEquals("ENTITY_EFFECT", EffectService.canonicalParticleName("SPELL_MOB"));
        assertEquals("WITCH", EffectService.canonicalParticleName("SPELL_WITCH"));
        assertEquals("ENCHANTED_HIT", EffectService.canonicalParticleName("CRIT_MAGIC"));
        assertEquals("POOF", EffectService.canonicalParticleName("EXPLOSION_NORMAL"));
        assertEquals("DUST", EffectService.canonicalParticleName("REDSTONE"));
        assertEquals("HAPPY_VILLAGER", EffectService.canonicalParticleName("VILLAGER_HAPPY"));
        assertEquals("ITEM_SLIME", EffectService.canonicalParticleName("SLIME"));
        assertEquals("ENCHANT", EffectService.canonicalParticleName("ENCHANTMENT_TABLE"));
    }

    @Test
    void namespacesDotsDashesAndTheModernPrefixAreNormalised() {

        assertEquals("CRIT", EffectService.canonicalParticleName("minecraft:crit"));
        assertEquals("FLAME", EffectService.canonicalParticleName("minecraft:flame"));
        assertEquals("ENTITY_EFFECT", EffectService.canonicalParticleName("minecraft:entity_effect"));
        assertEquals("LARGE_SMOKE", EffectService.canonicalParticleName("SMOKE_LARGE"));
        assertEquals("CRIT", EffectService.canonicalParticleName("PARTICLE_CRIT"));
        assertEquals("CRIT", EffectService.canonicalParticleName("crit_particle"));
    }

    @Test
    void blankParticleNamesFallBackToCrit() {

        assertEquals("CRIT", EffectService.canonicalParticleName(null));
        assertEquals("CRIT", EffectService.canonicalParticleName("   "));
    }

    @Test
    void soundKeysAreNormalised() {

        assertEquals("ENTITY_PLAYER_ATTACK_CRIT", EffectService.normaliseSound("entity.player.attack.crit"));
        assertEquals("ENTITY_PLAYER_ATTACK_CRIT", EffectService.normaliseSound("ENTITY_PLAYER_ATTACK_CRIT"));
        assertEquals("ENTITY_PLAYER_ATTACK_CRIT",
                EffectService.normaliseSound("minecraft:entity.player.attack.crit"));
        assertEquals("ENTITY_PLAYER_ATTACK_CRIT", EffectService.normaliseSound(null));
    }
}

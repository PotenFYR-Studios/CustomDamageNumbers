package in.potenfyr.cdn.platform;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftVersionTest {

    @Test
    void parsesBukkitVersionStrings() {

        MinecraftVersion version = MinecraftVersion.parse("1.16.5-R0.1-SNAPSHOT");

        assertEquals(1, version.major());
        assertEquals(16, version.minor());
        assertEquals(5, version.patch());
        assertTrue(version.isKnown());
    }

    @Test
    void parsesDateBasedVersions() {

        MinecraftVersion version = MinecraftVersion.parse("26.3-R0.1-SNAPSHOT");

        assertEquals(26, version.major());
        assertEquals(3, version.minor());
        assertEquals(0, version.patch());
    }

    @Test
    void parsesVersionOutOfAPaperServerString() {

        Optional<MinecraftVersion> version =
                MinecraftVersion.tryParse("git-Paper-196 (MC: 1.21.4)");

        assertTrue(version.isPresent());
        assertTrue(version.get().isAtLeast(1, 21));
    }

    @Test
    void unknownInputIsNotAPanic() {

        assertFalse(MinecraftVersion.tryParse(null).isPresent());
        assertFalse(MinecraftVersion.tryParse("nonsense").isPresent());
        assertEquals(MinecraftVersion.UNKNOWN, MinecraftVersion.parse("nonsense"));
        assertFalse(MinecraftVersion.UNKNOWN.isKnown());
    }

    @Test
    void ordersAcrossTheOneToTwentySixBoundary() {

        assertTrue(MinecraftVersion.parse("26.3").isAtLeast(1, 20, 2));
        assertTrue(MinecraftVersion.parse("1.21.4").isAtLeast(1, 20, 2));
        assertTrue(MinecraftVersion.parse("1.20.2").isAtLeast(1, 20, 2));
        assertFalse(MinecraftVersion.parse("1.20.1").isAtLeast(1, 20, 2));
        assertFalse(MinecraftVersion.parse("1.16.5").isAtLeast(1, 20, 2));
    }

    @Test
    void textDisplayGateMatchesTheDocumentedBoundary() {

        assertFalse(MinecraftVersion.parse("1.19.4").supportsTextDisplay());
        assertFalse(MinecraftVersion.parse("1.20.1").supportsTextDisplay());
        assertTrue(MinecraftVersion.parse("1.20.2").supportsTextDisplay());
        assertTrue(MinecraftVersion.parse("1.21.8").supportsTextDisplay());
        assertTrue(MinecraftVersion.parse("26.3").supportsTextDisplay());
    }

    @Test
    void legacyRenderingCoversOneSixteenUpwards() {

        assertTrue(MinecraftVersion.parse("1.16.5").supportsLegacyRendering());
        assertTrue(MinecraftVersion.parse("1.20.1").supportsLegacyRendering());
        assertFalse(MinecraftVersion.parse("1.15.2").supportsLegacyRendering());
        assertFalse(MinecraftVersion.parse("26.3").supportsLegacyRendering());
    }

    @Test
    void equalityAndHashingIgnoreTheRawString() {

        assertEquals(MinecraftVersion.parse("1.16.5"), MinecraftVersion.parse("1.16.5-R0.1-SNAPSHOT"));
        assertEquals(
                MinecraftVersion.parse("1.16.5").hashCode(),
                MinecraftVersion.parse("1.16.5-R0.1-SNAPSHOT").hashCode());
    }
}

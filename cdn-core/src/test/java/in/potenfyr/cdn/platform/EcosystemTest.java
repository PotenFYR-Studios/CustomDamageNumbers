package in.potenfyr.cdn.platform;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EcosystemTest {

    @Test
    void recognisesPaperFromTheServerNameEvenWhenTheBannerDoesNotSayPaper() {

        // Modern Paper versions no longer include "paper" in getVersion().
        assertEquals(Ecosystem.PAPER,
                Ecosystem.detect("Paper", "26.3-R0.1-SNAPSHOT", className -> false));
    }

    @Test
    void recognisesPaperFromTheBannerOnOlderBuilds() {

        assertEquals(Ecosystem.PAPER,
                Ecosystem.detect(null, "git-Paper-196 (MC: 1.21.4)", className -> false));
    }

    @Test
    void recognisesSpigot() {

        assertEquals(Ecosystem.SPIGOT,
                Ecosystem.detect("CraftBukkit", "git-Spigot-1234 (MC: 1.16.5)", className -> false));
    }

    @Test
    void recognisesPurpurAndPufferfish() {

        assertEquals(Ecosystem.PURPUR,
                Ecosystem.detect("Purpur", "1.21.4-R0.1-SNAPSHOT", className -> false));
        assertEquals(Ecosystem.PUFFERFISH,
                Ecosystem.detect("Pufferfish", "1.21.4-R0.1-SNAPSHOT", className -> false));
    }

    @Test
    void foliaIsDetectedByItsClassesBecauseItsNameLooksLikePaper() {

        assertEquals(Ecosystem.FOLIA,
                Ecosystem.detect("Folia", "26.3-R0.1-SNAPSHOT", className -> true));
        assertEquals(Ecosystem.FOLIA,
                Ecosystem.detect("Paper", "26.3-R0.1-SNAPSHOT",
                        className -> className.contains("threadedregions")));
    }

    @Test
    void unknownServersAreReportedAsUnknownRatherThanGuessed() {

        assertEquals(Ecosystem.UNKNOWN, Ecosystem.detect("Something", "1.20.1", className -> false));
        assertEquals(Ecosystem.UNKNOWN, Ecosystem.detect(null, null, null));
    }

    @Test
    void foliaIsTheOnlyRegionThreadedServerAndPaperForksSupportModernFeatures() {

        assertTrue(Ecosystem.FOLIA.isRegionThreaded());
        assertFalse(Ecosystem.PAPER.isRegionThreaded());
        assertTrue(Ecosystem.PAPER.isPaperDerivative());
        assertTrue(Ecosystem.PURPUR.isPaperDerivative());
        assertFalse(Ecosystem.SPIGOT.isPaperDerivative());
    }
}

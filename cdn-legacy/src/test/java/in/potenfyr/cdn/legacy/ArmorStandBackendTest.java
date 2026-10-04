package in.potenfyr.cdn.legacy;

import in.potenfyr.cdn.packet.MetadataValue;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The legacy (armour stand) metadata payload. Only indices that are identical across
 * 1.16-1.20.1 are used, which is what removes the need for a per-version index table.
 */
class ArmorStandBackendTest {

    private static Map<Integer, MetadataValue> byIndex(List<MetadataValue> metadata) {
        return metadata.stream().collect(Collectors.toMap(MetadataValue::index, data -> data, (a, b) -> b));
    }

    @Test
    void spawnMetadataMakesAnInvisibleStandWithAVisibleName() {

        Map<Integer, MetadataValue> byIndex =
                byIndex(ArmorStandBackend.spawnMetadata("{\"text\":\"1.5\"}"));

        assertEquals(ArmorStandBackend.flagsIndex(), 0);
        assertEquals(ArmorStandBackend.nameIndex(), 2);
        assertEquals(ArmorStandBackend.nameVisibleIndex(), 3);

        assertEquals(ArmorStandBackend.invisibleFlag(), byIndex.get(0).value(), "invisible entity flag");
        assertEquals(Boolean.TRUE, byIndex.get(3).value(), "custom name visible");
        assertEquals(MetadataValue.Kind.BOOLEAN, byIndex.get(3).kind());
    }

    @Test
    void customNameUsesTheOptionalComponentFormValidOnTheseVersions() {

        MetadataValue data = ArmorStandBackend.nameValue("{\"text\":\"7.0\"}");

        assertEquals(2, data.index());
        assertEquals(MetadataValue.Kind.OPTIONAL_COMPONENT_JSON, data.kind());
        assertEquals("{\"text\":\"7.0\"}", data.value());
    }

    @Test
    void aNullNameEncodesAsAbsentRatherThanFailing() {
        assertNull(ArmorStandBackend.nameValue(null).value());
    }

    @Test
    void backendReportsThatUnlikeTextDisplayItCannotFadeOrScale() {

        ArmorStandBackend backend = new ArmorStandBackend();

        assertEquals("armor-stand", backend.id());
        assertFalse(backend.supportsScale(), "scale is only possible from 1.20.2");
        assertFalse(backend.supportsOpacity(), "per-entity opacity is only possible from 1.20.2");
        assertTrue(ArmorStandBackend.spawnMetadata("{}").size() >= 3);
    }
}

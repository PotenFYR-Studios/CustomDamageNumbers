package in.potenfyr.cdn.modern;

import in.potenfyr.cdn.config.ConfigManager;
import in.potenfyr.cdn.packet.MetadataValue;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The TextDisplay metadata payload, asserted without a server.
 *
 * <p>These are the exact indices and values the client reads, so a regression here is
 * a visible rendering bug — this is the test that keeps the animation-config wiring
 * honest.</p>
 */
class TextDisplayBackendTest {

    private static Map<Integer, MetadataValue> byIndex(List<MetadataValue> metadata) {
        return metadata.stream().collect(Collectors.toMap(MetadataValue::index, data -> data, (a, b) -> b));
    }

    @Test
    void spawnMetadataCarriesEveryFieldTheRendererNeeds() {

        List<MetadataValue> metadata =
                TextDisplayBackend.spawnMetadata(1.3f, (byte) -1, true, "{\"text\":\"1.5\"}");

        Map<Integer, MetadataValue> byIndex = byIndex(metadata);

        assertTrue(byIndex.containsKey(23), "text index");
        assertTrue(byIndex.containsKey(12), "scale index");
        assertTrue(byIndex.containsKey(15), "billboard index");
        assertTrue(byIndex.containsKey(26), "opacity index");
        assertTrue(byIndex.containsKey(27), "style flags index");
        assertTrue(byIndex.containsKey(10), "teleport duration index");
        assertTrue(byIndex.containsKey(24), "line width index");
        assertTrue(byIndex.containsKey(25), "background colour index");

        assertEquals(TextDisplayBackend.textIndex(), 23);
        assertEquals(TextDisplayBackend.scaleIndex(), 12);
        assertEquals(TextDisplayBackend.opacityIndex(), 26);
        assertEquals(TextDisplayBackend.billboardIndex(), 15);
        assertEquals(TextDisplayBackend.styleFlagsIndex(), 27);
    }

    @Test
    void textIsSentAsAJsonString() {

        MetadataValue text = TextDisplayBackend.textValue("{\"text\":\"crit\"}");

        assertEquals(23, text.index());
        assertEquals(MetadataValue.Kind.COMPONENT_JSON, text.kind());
        assertEquals("{\"text\":\"crit\"}", text.value());
    }

    @Test
    void opacityIsCarriedAsTheSignedByteTheClientExpects() {

        assertEquals((byte) -1, TextDisplayBackend.opacityValue((byte) -1).value());
        assertEquals(26, TextDisplayBackend.opacityValue((byte) -1).index());
        assertEquals(MetadataValue.Kind.BYTE, TextDisplayBackend.opacityValue((byte) 0).kind());
    }

    @Test
    void scaleIsSentAsAUniformVector() {

        MetadataValue scale = TextDisplayBackend.scaleValue(2.5f);

        assertEquals(12, scale.index());
        assertEquals(MetadataValue.Kind.VECTOR3F, scale.kind());

        float[] components = scale.vector();

        assertEquals(2.5f, components[0], 1.0e-6);
        assertEquals(2.5f, components[1], 1.0e-6);
        assertEquals(2.5f, components[2], 1.0e-6);
    }

    @Test
    void styleFlagsFollowTheConfiguredShadow() {

        assertEquals(1, TextDisplayBackend.styleFlags(true));
        assertEquals(0, TextDisplayBackend.styleFlags(false));

        Map<Integer, MetadataValue> withoutShadow =
                byIndex(TextDisplayBackend.spawnMetadata(1.0f, (byte) -1, false, "{}"));

        assertEquals((byte) 0, withoutShadow.get(27).value());
    }

    @Test
    void backendAdvertisesTheCapabilitiesItsVersionsActuallyHave() {

        TextDisplayBackend backend = new TextDisplayBackend(new ConfigManager(new YamlConfiguration()));

        assertEquals("text-display", backend.id());
        assertTrue(backend.supportsScale());
        assertTrue(backend.supportsOpacity());
        assertFalse(backend.id().isBlank());
    }
}

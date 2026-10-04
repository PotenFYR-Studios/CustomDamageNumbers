package in.potenfyr.cdn.legacy;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import in.potenfyr.cdn.damage.AnimationFrame;
import in.potenfyr.cdn.damage.FloatingDamage;
import in.potenfyr.cdn.packet.MetadataValue;
import in.potenfyr.cdn.packet.PacketUtil;
import in.potenfyr.cdn.packet.RenderBackend;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The 1.16.x - 1.20.1 renderer: a client-side armour stand whose custom name carries
 * the number.
 *
 * <p>Still packet-only — nothing is spawned into the world — which is the promise the
 * plugin makes on every version. Only metadata indices that are identical across
 * 1.16-1.20.1 are touched (entity flags at 0, custom name at 2, name visibility at 3),
 * which is why this backend needs no per-version index table.</p>
 *
 * <p>Per-entity opacity and scale do not exist before 1.20.2, so {@code fade-out} and
 * {@code scale-animation} degrade to position-only here: {@link #supportsOpacity()}
 * and {@link #supportsScale()} report that honestly, and {@code /cdn backend} surfaces
 * it.</p>
 */
public final class ArmorStandBackend implements RenderBackend {

    static final int INDEX_ENTITY_FLAGS = 0;
    static final int INDEX_CUSTOM_NAME = 2;
    static final int INDEX_CUSTOM_NAME_VISIBLE = 3;

    /** Entity flag bit 0x20 marks the entity invisible to clients. */
    private static final byte FLAG_INVISIBLE = 0x20;

    @Override
    public String id() {
        return "armor-stand";
    }

    @Override
    public boolean supportsScale() {
        return false;
    }

    @Override
    public boolean supportsOpacity() {
        return false;
    }

    @Override
    public void spawn(FloatingDamage display, Collection<Player> viewers) {

        if (viewers.isEmpty()) {
            return;
        }

        Vector3d position = position(display);

        List<MetadataValue> metadata = spawnMetadata(display.getJsonText());

        for (Player viewer : viewers) {

            PacketUtil.spawnEntity(
                    viewer, display.getEntityId(), display.getUuid(), EntityTypes.ARMOR_STAND, position);

            PacketUtil.entityMetadata(viewer, display.getEntityId(), metadata);
        }
    }

    @Override
    public void move(FloatingDamage display, AnimationFrame frame, Collection<Player> viewers) {

        // No per-entity scale or opacity exists on these versions: the animation is
        // carried entirely by the anchor position.
        if (viewers.isEmpty()) {
            return;
        }

        Vector3d position = position(display);

        for (Player viewer : viewers) {
            PacketUtil.teleport(viewer, display.getEntityId(), position, 0.0f, 0.0f);
        }
    }

    @Override
    public void retext(FloatingDamage display, Collection<Player> viewers) {

        if (viewers.isEmpty()) {
            return;
        }

        List<MetadataValue> metadata = List.of(nameValue(display.getJsonText()));

        for (Player viewer : viewers) {
            PacketUtil.entityMetadata(viewer, display.getEntityId(), metadata);
        }
    }

    @Override
    public void destroy(FloatingDamage display, Collection<Player> viewers) {

        for (Player viewer : viewers) {
            PacketUtil.destroy(viewer, display.getEntityId());
        }
    }

    // ------------------------------------------------------------------
    // Metadata payloads (pure: unit-tested without a server or PacketEvents)
    // ------------------------------------------------------------------

    /** Invisible armour stand with a visible custom name and nothing else set. */
    public static List<MetadataValue> spawnMetadata(String jsonText) {

        List<MetadataValue> metadata = new ArrayList<>(3);

        metadata.add(MetadataValue.ofByte(INDEX_ENTITY_FLAGS, FLAG_INVISIBLE));
        metadata.add(nameValue(jsonText));
        metadata.add(MetadataValue.ofBoolean(INDEX_CUSTOM_NAME_VISIBLE, true));

        return metadata;
    }

    /**
     * The custom name field. It is an optional chat component from 1.13 onwards, so it
     * is sent as an optional component carrying the JSON string form of the text;
     * a {@code null} name encodes as absent.
     */
    public static MetadataValue nameValue(String jsonText) {
        return MetadataValue.optionalComponent(INDEX_CUSTOM_NAME, jsonText);
    }

    public static int nameIndex() {
        return INDEX_CUSTOM_NAME;
    }

    public static int flagsIndex() {
        return INDEX_ENTITY_FLAGS;
    }

    public static int nameVisibleIndex() {
        return INDEX_CUSTOM_NAME_VISIBLE;
    }

    static byte invisibleFlag() {
        return FLAG_INVISIBLE;
    }

    private static Vector3d position(FloatingDamage display) {

        Location anchor = display.getAnchor();

        return new Vector3d(anchor.getX(), anchor.getY(), anchor.getZ());
    }
}

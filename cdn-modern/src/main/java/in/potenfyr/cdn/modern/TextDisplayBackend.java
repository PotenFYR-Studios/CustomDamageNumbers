package in.potenfyr.cdn.modern;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import in.potenfyr.cdn.config.ConfigManager;
import in.potenfyr.cdn.damage.AnimationFrame;
import in.potenfyr.cdn.damage.DamageType;
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
 * The 1.20.2+ renderer: one client-side TextDisplay per damage number.
 *
 * <p>Metadata indices are the display-entity layout that has been stable since
 * 1.20.2 — Display base 8-22, TextDisplay 23-27 — and are declared in exactly one
 * place here. Version 1.19.4-1.20.1 used a different field layout and is served by
 * the legacy jar rather than by an unverified index table.</p>
 *
 * <p>Text is sent as the JSON string form, so no Adventure type crosses into
 * PacketEvents.</p>
 */
public final class TextDisplayBackend implements RenderBackend {

    static final int INDEX_TELEPORT_DURATION = 10;
    static final int INDEX_SCALE = 12;
    static final int INDEX_BILLBOARD = 15;
    static final int INDEX_TEXT = 23;
    static final int INDEX_LINE_WIDTH = 24;
    static final int INDEX_BACKGROUND_COLOR = 25;
    static final int INDEX_TEXT_OPACITY = 26;
    static final int INDEX_STYLE_FLAGS = 27;

    private static final byte BILLBOARD_CENTER = 3;
    private static final int LINE_WIDTH = 200;
    private static final int TRANSPARENT_BACKGROUND = 0;
    private static final byte STYLE_FLAG_SHADOW = 0x01;

    /** Client-side interpolation over 3 ticks smooths the per-tick teleports. */
    private static final int TELEPORT_DURATION_TICKS = 3;

    /** Scale changes smaller than this are not worth a metadata packet. */
    private static final float SCALE_EPSILON = 0.01f;

    private final ConfigManager config;

    public TextDisplayBackend(ConfigManager config) {
        this.config = config;
    }

    @Override
    public String id() {
        return "text-display";
    }

    @Override
    public boolean supportsScale() {
        return true;
    }

    @Override
    public boolean supportsOpacity() {
        return true;
    }

    @Override
    public void spawn(FloatingDamage display, Collection<Player> viewers) {

        if (viewers.isEmpty()) {
            return;
        }

        Vector3d position = position(display);

        List<MetadataValue> metadata = spawnMetadata(
                display.getLastScale(),
                display.getLastOpacity(),
                config.styleFor(display.isCritical() ? DamageType.CRITICAL : display.getType()).shadow(),
                display.getJsonText());

        for (Player viewer : viewers) {

            PacketUtil.spawnEntity(
                    viewer, display.getEntityId(), display.getUuid(), EntityTypes.TEXT_DISPLAY, position);

            PacketUtil.entityMetadata(viewer, display.getEntityId(), metadata);
        }
    }

    @Override
    public void move(FloatingDamage display, AnimationFrame frame, Collection<Player> viewers) {

        if (viewers.isEmpty()) {
            return;
        }

        Vector3d position = position(display);

        for (Player viewer : viewers) {
            PacketUtil.teleport(viewer, display.getEntityId(), position, 0.0f, 0.0f);
        }

        float scale = frame.scale();
        byte opacity = frame.opacityByte();

        boolean scaleChanged = Float.isNaN(display.getLastScale())
                || Math.abs(scale - display.getLastScale()) >= SCALE_EPSILON;

        boolean opacityChanged = opacity != display.getLastOpacity();

        if (!scaleChanged && !opacityChanged) {
            return;
        }

        List<MetadataValue> metadata = new ArrayList<>(2);

        if (scaleChanged) {
            metadata.add(scaleValue(scale));
        }

        if (opacityChanged) {
            metadata.add(opacityValue(opacity));
        }

        for (Player viewer : viewers) {
            PacketUtil.entityMetadata(viewer, display.getEntityId(), metadata);
        }

        display.setLastScale(scale);
        display.setLastOpacity(opacity);
    }

    @Override
    public void retext(FloatingDamage display, Collection<Player> viewers) {

        if (viewers.isEmpty()) {
            return;
        }

        List<MetadataValue> metadata = List.of(textValue(display.getJsonText()));

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

    /** The full TextDisplay metadata sent at spawn. */
    public static List<MetadataValue> spawnMetadata(
            float scale,
            byte opacity,
            boolean shadow,
            String jsonText
    ) {

        List<MetadataValue> metadata = new ArrayList<>(8);

        metadata.add(MetadataValue.ofInt(INDEX_TELEPORT_DURATION, TELEPORT_DURATION_TICKS));
        metadata.add(scaleValue(scale));
        metadata.add(MetadataValue.ofByte(INDEX_BILLBOARD, BILLBOARD_CENTER));
        metadata.add(textValue(jsonText));
        metadata.add(MetadataValue.ofInt(INDEX_LINE_WIDTH, LINE_WIDTH));
        metadata.add(MetadataValue.ofInt(INDEX_BACKGROUND_COLOR, TRANSPARENT_BACKGROUND));
        metadata.add(opacityValue(opacity));
        metadata.add(MetadataValue.ofByte(INDEX_STYLE_FLAGS, styleFlags(shadow)));

        return metadata;
    }

    /** Uniform scale vector; the client applies it to all three axes. */
    public static MetadataValue scaleValue(float scale) {
        return MetadataValue.uniformScale(INDEX_SCALE, scale);
    }

    /** The text payload: a JSON string, which is what PacketEvents' COMPONENT type takes. */
    public static MetadataValue textValue(String jsonText) {
        return MetadataValue.component(INDEX_TEXT, jsonText);
    }

    /** Text opacity; {@code -1} (255) is fully opaque. */
    public static MetadataValue opacityValue(byte opacity) {
        return MetadataValue.ofByte(INDEX_TEXT_OPACITY, opacity);
    }

    /** Style flags: bit 0 is the text shadow. */
    public static byte styleFlags(boolean shadow) {
        return shadow ? STYLE_FLAG_SHADOW : 0;
    }

    public static int textIndex() {
        return INDEX_TEXT;
    }

    public static int scaleIndex() {
        return INDEX_SCALE;
    }

    public static int opacityIndex() {
        return INDEX_TEXT_OPACITY;
    }

    public static int styleFlagsIndex() {
        return INDEX_STYLE_FLAGS;
    }

    public static int billboardIndex() {
        return INDEX_BILLBOARD;
    }

    private static Vector3d position(FloatingDamage display) {

        Location anchor = display.getAnchor();

        return new Vector3d(anchor.getX(), anchor.getY(), anchor.getZ());
    }
}

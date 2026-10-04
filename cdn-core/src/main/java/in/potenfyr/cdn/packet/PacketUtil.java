package in.potenfyr.cdn.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.particle.Particle;
import com.github.retrooper.packetevents.protocol.sound.Sound;
import com.github.retrooper.packetevents.protocol.sound.SoundCategory;
import com.github.retrooper.packetevents.protocol.sound.Sounds;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerParticle;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSoundEffect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Thin wrapper over the PacketEvents send calls used by the renderers.
 *
 * <p>Exists so the backends describe <em>what</em> they render while this class owns
 * <em>how</em> it goes on the wire; it is also the single seam where a packet failure
 * would be handled.</p>
 */
public final class PacketUtil {

    private PacketUtil() {
    }

    public static void send(Player viewer, PacketWrapper<?> packet) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
    }

    /** Spawns a fake entity for one viewer. */
    public static void spawnEntity(
            Player viewer,
            int entityId,
            UUID uuid,
            EntityType type,
            Vector3d position
    ) {
        send(viewer, new WrapperPlayServerSpawnEntity(
                entityId,
                Optional.of(uuid),
                type,
                position,
                0.0f,
                0.0f,
                0.0f,
                0,
                Optional.empty()));
    }

    /** Applies entity metadata to one viewer. */
    public static void entityMetadata(
            Player viewer,
            int entityId,
            List<MetadataValue> metadata
    ) {

        List<EntityData<?>> encoded = new ArrayList<>(metadata.size());

        for (MetadataValue entry : metadata) {
            encoded.add(encode(entry));
        }

        send(viewer, new WrapperPlayServerEntityMetadata(entityId, encoded));
    }

    /**
     * The single place PacketEvents' metadata types are touched: reading
     * {@code EntityDataTypes} initialises its versioned registry, so it happens when a
     * packet is sent rather than while a payload is being described.
     */
    private static EntityData<?> encode(MetadataValue entry) {

        return switch (entry.kind()) {

            case BYTE -> new EntityData<>(
                    entry.index(), EntityDataTypes.BYTE, ((Number) entry.value()).byteValue());

            case INT -> new EntityData<>(
                    entry.index(), EntityDataTypes.INT, ((Number) entry.value()).intValue());

            case FLOAT -> new EntityData<>(
                    entry.index(), EntityDataTypes.FLOAT, ((Number) entry.value()).floatValue());

            case BOOLEAN -> new EntityData<>(
                    entry.index(), EntityDataTypes.BOOLEAN, (Boolean) entry.value());

            case COMPONENT_JSON -> new EntityData<>(
                    entry.index(), EntityDataTypes.COMPONENT, (String) entry.value());

            case OPTIONAL_COMPONENT_JSON -> new EntityData<>(
                    entry.index(), EntityDataTypes.OPTIONAL_COMPONENT,
                    Optional.ofNullable((String) entry.value()));

            case VECTOR3F -> {
                float[] components = entry.vector();
                yield new EntityData<>(
                        entry.index(), EntityDataTypes.VECTOR3F,
                        new Vector3f(components[0], components[1], components[2]));
            }
        };
    }

    /** Moves a fake entity for one viewer. */
    public static void teleport(
            Player viewer,
            int entityId,
            Vector3d position,
            float yaw,
            float pitch
    ) {
        send(viewer, new WrapperPlayServerEntityTeleport(entityId, position, yaw, pitch, false));
    }

    /** Removes a fake entity for one viewer. */
    public static void destroy(Player viewer, int entityId) {
        send(viewer, new WrapperPlayServerDestroyEntities(entityId));
    }

    public static void particle(
            Player viewer,
            Particle<?> particle,
            Vector3d position,
            Vector3f offset,
            float speed,
            int count
    ) {
        send(viewer, new WrapperPlayServerParticle(particle, true, position, offset, speed, count));
    }

    /** Plays a sound client-side; the key is a vanilla sound name such as {@code ENTITY_PLAYER_ATTACK_CRIT}. */
    public static void sound(
            Player viewer,
            String soundKey,
            Vector3d position,
            float volume,
            float pitch
    ) {

        Sound sound = Sounds.getByNameOrCreate(soundKey);

        send(viewer, new WrapperPlayServerSoundEffect(
                sound, SoundCategory.PLAYER, position, volume, pitch));
    }
}

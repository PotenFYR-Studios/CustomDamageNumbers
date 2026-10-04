package in.potenfyr.cdn.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;

import in.potenfyr.cdn.CustomDamageNumbersPlugin;
import in.potenfyr.cdn.damage.DamageFormatter;
import in.potenfyr.cdn.damage.DamageRenderer;
import in.potenfyr.cdn.damage.FloatingDamage;

import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PacketUtil {


    public static void spawn(FloatingDamage damage) {

        int entityId = damage.getEntityId();

        WrapperPlayServerSpawnEntity spawnPacket =
                new WrapperPlayServerSpawnEntity(
                        entityId,
                        Optional.of(damage.getUuid()),
                        EntityTypes.TEXT_DISPLAY,
                        new Vector3d(
                                damage.getLocation().getX(),
                                damage.getLocation().getY(),
                                damage.getLocation().getZ()
                        ),
                        0f, 0f, 0f,
                        0,
                        Optional.empty()
                );

        Component text = DamageFormatter.format(
                damage.getDamage(),
                damage.getType(),
                damage.isCritical()
        );

        List<EntityData<?>> metadata =
                buildMetadata(text);

        WrapperPlayServerEntityMetadata metadataPacket =
                new WrapperPlayServerEntityMetadata(
                        entityId,
                        metadata
                );

        var config =
                CustomDamageNumbersPlugin
                        .getInstance()
                        .getConfigManager();

        boolean nearbyOnly = config.isNearbyViewersOnly();
        int viewDistance = config.getViewDistance();
        double maxDistanceSq = (double) viewDistance * viewDistance;
        int maxPerPlayer = config.getMaxPerPlayer();

        for (Player player : Bukkit.getOnlinePlayers()) {

            if (player.getWorld() != damage.getLocation().getWorld()) {
                continue;
            }

            if (nearbyOnly
                    && player.getLocation().distanceSquared(damage.getLocation()) > maxDistanceSq) {
                continue;
            }

            // Per-player cap so one busy combat area cannot flood a single viewer.
            if (DamageRenderer.countActiveFor(player.getUniqueId()) >= maxPerPlayer) {
                continue;
            }

            // Optional per-player view permission (permissions.require-view-permission).
            if (config.isRequireViewPermission()
                    && !player.hasPermission(config.getViewPermission())) {
                continue;
            }

            PacketEvents.getAPI().getPlayerManager().sendPacket(player, spawnPacket);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, metadataPacket);

            damage.addViewer(player.getUniqueId());
        }
    }

    /*
     * TextDisplay metadata indices, valid for the 26.3 protocol (unchanged
     * since 1.20.2; Display base spans 8-22, Text Display spans 23-27).
     * PacketEvents 2.x intentionally exposes no typed constants for these,
     * so the indices are kept here, documented, in one place.
     */
    private static final int INDEX_POSITION_INTERPOLATION = 10;
    private static final int INDEX_BILLBOARD = 15;
    private static final byte BILLBOARD_CENTER = 3;

    private static final int INDEX_TEXT = 23;
    private static final int INDEX_LINE_WIDTH = 24;
    private static final int INDEX_BACKGROUND_COLOR = 25;
    private static final int INDEX_TEXT_OPACITY = 26;
    private static final int INDEX_STYLE_FLAGS = 27;

    private static final byte STYLE_FLAG_SHADOW = 0x01;

    private static final int LINE_WIDTH = 200;
    private static final byte FULLY_OPAQUE = (byte) -1;

    /**
     * Builds TextDisplay metadata. Entries are encoded per viewer protocol by
     * PacketEvents, keeping 26.3 clients and protocol-translated viewers
     * (ViaVersion/Geyser) correct.
     */
    private static List<EntityData<?>> buildMetadata(Component text) {

        List<EntityData<?>> metadata = new ArrayList<>();

        // Smooth the per-tick teleport movement client-side over 3 ticks.
        metadata.add(new EntityData<>(
                INDEX_POSITION_INTERPOLATION, EntityDataTypes.INT, 3));

        // Billboard CENTER: the number always faces the viewer.
        metadata.add(new EntityData<>(
                INDEX_BILLBOARD, EntityDataTypes.BYTE, BILLBOARD_CENTER));

        metadata.add(new EntityData<>(
                INDEX_TEXT, EntityDataTypes.ADV_COMPONENT, text));

        metadata.add(new EntityData<>(
                INDEX_LINE_WIDTH, EntityDataTypes.INT, LINE_WIDTH));

        // Fully transparent background.
        metadata.add(new EntityData<>(
                INDEX_BACKGROUND_COLOR, EntityDataTypes.INT, 0));

        metadata.add(new EntityData<>(
                INDEX_TEXT_OPACITY, EntityDataTypes.BYTE, FULLY_OPAQUE));

        metadata.add(new EntityData<>(
                INDEX_STYLE_FLAGS, EntityDataTypes.BYTE, STYLE_FLAG_SHADOW));

        return metadata;
    }

    public static void teleport(FloatingDamage damage) {

        WrapperPlayServerEntityTeleport packet =
                new WrapperPlayServerEntityTeleport(
                        damage.getEntityId(),
                        new Vector3d(
                                damage.getLocation().getX(),
                                damage.getLocation().getY(),
                                damage.getLocation().getZ()
                        ),
                        0f, 0f,
                        false
                );

        for (UUID viewerId : List.copyOf(damage.getViewers())) {

            Player viewer = Bukkit.getPlayer(viewerId);

            if (viewer == null || !viewer.isOnline()) {
                continue;
            }

            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
        }
    }

    /**
     * Detaches a single viewer from a display (distance culling / logout).
     */
    public static void destroyFor(FloatingDamage damage, UUID viewerId) {

        Player viewer = Bukkit.getPlayer(viewerId);

        if (viewer != null && viewer.isOnline()) {

            PacketEvents.getAPI().getPlayerManager().sendPacket(
                    viewer,
                    new WrapperPlayServerDestroyEntities(damage.getEntityId()));
        }

        damage.getViewers().remove(viewerId);
    }

    public static void destroy(FloatingDamage damage) {

        WrapperPlayServerDestroyEntities packet =
                new WrapperPlayServerDestroyEntities(damage.getEntityId());

        // Only players that actually received the spawn get the despawn packet.
        for (UUID viewerId : List.copyOf(damage.getViewers())) {

            Player viewer = Bukkit.getPlayer(viewerId);

            if (viewer == null || !viewer.isOnline()) {
                continue;
            }

            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
        }

        damage.clearViewers();
    }
}
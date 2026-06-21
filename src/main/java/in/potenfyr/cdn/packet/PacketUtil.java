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
import in.potenfyr.cdn.damage.FloatingDamage;

import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PacketUtil {


    public static void spawn(FloatingDamage damage) {

        WrapperPlayServerSpawnEntity spawnPacket =
                new WrapperPlayServerSpawnEntity(
                        damage.getEntityId(),
                        Optional.of(UUID.randomUUID()),
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

        String componentJson =
                GsonComponentSerializer.gson()
                        .serialize(
                                DamageFormatter.format(
                                        damage.getDamage(),
                                        damage.getType(),
                                        damage.isCritical()
                                )
                        );

        List<EntityData<?>> metadata = new ArrayList<>();


        metadata.add(new EntityData<>(10, EntityDataTypes.INT, 3));

        // Text component
        // Index 15 - Billboard = CENTER (always faces player)
        metadata.add(new EntityData<>(15, EntityDataTypes.BYTE, (byte) 3));

        // Index 23 - Text component
        metadata.add(new EntityData<>(23, EntityDataTypes.COMPONENT, componentJson));

        // Index 24 - Line width
        metadata.add(new EntityData<>(24, EntityDataTypes.INT, 200));

        // Index 25 - Background color (fully transparent = 0)
        metadata.add(new EntityData<>(25, EntityDataTypes.INT, 0));

        // Index 26 - Text opacity (fully opaque = -1)
        metadata.add(new EntityData<>(26, EntityDataTypes.BYTE, (byte) -1));

        // Index 27 - Style flags (0x01 = shadow)
        metadata.add(new EntityData<>(27, EntityDataTypes.BYTE, (byte) 0x01));
        WrapperPlayServerEntityMetadata metadataPacket =
                new WrapperPlayServerEntityMetadata(
                        damage.getEntityId(),
                        metadata
                );

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!canSee(player, damage)) continue;
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, spawnPacket);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, metadataPacket);
        }
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

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!canSee(player, damage)) continue;
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
        }
    }

    public static void destroy(FloatingDamage damage) {

        WrapperPlayServerDestroyEntities packet =
                new WrapperPlayServerDestroyEntities(damage.getEntityId());

        for (Player player : Bukkit.getOnlinePlayers()) {
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
        }
    }

    private static boolean canSee(Player player, FloatingDamage damage) {

        int distance =
                CustomDamageNumbersPlugin
                        .getInstance()
                        .getConfigManager()
                        .getViewDistance();

        if (!player.getWorld().equals(damage.getLocation().getWorld())) {
            return false;
        }

        return player.getLocation().distanceSquared(damage.getLocation())
                <= distance * distance;
    }
}
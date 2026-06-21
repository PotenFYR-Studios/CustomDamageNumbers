package in.potenfyr.cdn.damage;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;

import java.util.HashSet;
import java.util.Set;

public class MetadataDebugListener extends PacketListenerAbstract {

    // Track entity IDs we know are TextDisplays
    private final Set<Integer> textDisplayIds = new HashSet<>();

    @Override
    public void onPacketSend(PacketSendEvent event) {

        // Track spawned TextDisplays by entity ID
        if (event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
            WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(event);
            if (spawn.getEntityType() ==
                    com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.TEXT_DISPLAY) {
                textDisplayIds.add(spawn.getEntityId());
            }
            return;
        }

        if (event.getPacketType() != PacketType.Play.Server.ENTITY_METADATA) return;

        WrapperPlayServerEntityMetadata packet =
                new WrapperPlayServerEntityMetadata(event);

        // Only log TextDisplay entities
        if (!textDisplayIds.contains(packet.getEntityId())) return;

        System.out.println("[CDN-DEBUG] TextDisplay metadata (entityId="
                + packet.getEntityId() + "):");

        for (EntityData<?> data : packet.getEntityMetadata()) {
            System.out.println("  index=" + data.getIndex()
                    + "  type=" + data.getType()
                    + "  value=" + data.getValue());
        }
    }
}
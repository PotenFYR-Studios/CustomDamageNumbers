package in.potenfyr.cdn.packet;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import in.potenfyr.cdn.util.DebugLogger;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Opt-in packet dump for diagnosing display issues ({@code advanced.packet-debug}).
 *
 * <p>The 0.2.0 version of this listener kept every TextDisplay entity id it ever saw
 * in a plain {@code HashSet} and never removed any, which is an unbounded leak on a
 * long-running server. This version keys ids in a bounded LRU and drops them when the
 * entity is destroyed, and the whole listener is unregistered on disable.</p>
 */
public final class MetadataDebugListener extends PacketListenerAbstract {

    private final DebugLogger debug;
    private final int maxTracked;

    /** Bounded insertion-ordered set: oldest id is evicted first. */
    private final Deque<Integer> order = new ArrayDeque<>();
    private final Set<Integer> textDisplayIds = new HashSet<>();

    public MetadataDebugListener(DebugLogger debug, int maxTracked) {
        this.debug = debug;
        this.maxTracked = Math.max(16, maxTracked);
    }

    @Override
    public synchronized void onPacketSend(PacketSendEvent event) {

        if (event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {

            WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(event);

            if (spawn.getEntityType() == EntityTypes.TEXT_DISPLAY) {
                track(spawn.getEntityId());
            }

            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.DESTROY_ENTITIES) {

            WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(event);

            for (int entityId : destroy.getEntityIds()) {
                untrack(entityId);
            }

            return;
        }

        if (event.getPacketType() != PacketType.Play.Server.ENTITY_METADATA) {
            return;
        }

        WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(event);

        if (!isTracked(packet.getEntityId())) {
            return;
        }

        debug.info("TextDisplay metadata (entityId=" + packet.getEntityId() + "):");

        for (EntityData<?> data : packet.getEntityMetadata()) {
            debug.info("  index=" + data.getIndex()
                    + " type=" + data.getType()
                    + " value=" + data.getValue());
        }
    }

    /** Number of ids currently tracked; exposed for the leak regression test. */
    public synchronized int trackedCount() {
        return textDisplayIds.size();
    }

    private synchronized void track(int entityId) {

        if (!textDisplayIds.add(entityId)) {
            return;
        }

        order.addLast(entityId);

        while (order.size() > maxTracked) {
            textDisplayIds.remove(order.removeFirst());
        }
    }

    private synchronized void untrack(int entityId) {

        if (textDisplayIds.remove(entityId)) {
            order.remove(entityId);
        }
    }

    private synchronized boolean isTracked(int entityId) {
        return textDisplayIds.contains(entityId);
    }
}

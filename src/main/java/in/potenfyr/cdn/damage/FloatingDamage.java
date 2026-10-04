package in.potenfyr.cdn.damage;

import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import org.bukkit.Location;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FloatingDamage {

    private final int entityId;

    /**
     * Stable fake-entity UUID shared by every viewer; using one UUID per
     * spawn keeps client-side state consistent and avoids allocating a
     * random UUID per player in the hot path.
     */
    private final UUID uuid = UUID.randomUUID();

    /**
     * Players that actually received the spawn packet. Teleport and destroy
     * packets are only sent to these viewers, so players who never saw the
     * display never receive packets for it.
     */
    private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();

    private Location location;

    private final double damage;

    private final DamageType type;

    private final boolean critical;

    private int ticksAlive;

    /**
     * Lifetime for this specific display. Killing blows flash briefly
     * (remove-on-death) instead of playing the full animation.
     */
    private final int durationTicks;

    public FloatingDamage(
            Location location,
            double damage,
            DamageType type,
            boolean critical,
            int durationTicks
    ) {

        this.entityId =
                SpigotReflectionUtil.generateEntityId();

        this.location = location;
        this.damage = damage;
        this.type = type;
        this.critical = critical;
        this.durationTicks = durationTicks;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public int getEntityId() {
        return entityId;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Set<UUID> getViewers() {
        return viewers;
    }

    public void addViewer(UUID playerId) {
        viewers.add(playerId);
    }

    public void clearViewers() {
        viewers.clear();
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public double getDamage() {
        return damage;
    }

    public DamageType getType() {
        return type;
    }

    public boolean isCritical() {
        return critical;
    }

    public int getTicksAlive() {
        return ticksAlive;
    }

    public void setTicksAlive(int ticksAlive) {
        this.ticksAlive = ticksAlive;
    }
}
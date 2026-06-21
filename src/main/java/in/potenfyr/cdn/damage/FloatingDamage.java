package in.potenfyr.cdn.damage;

import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import org.bukkit.Location;

public class FloatingDamage {

    private final int entityId;

    private Location location;

    private final double damage;

    private final DamageType type;

    private final boolean critical;

    private int ticksAlive;

    public FloatingDamage(
            Location location,
            double damage,
            DamageType type,
            boolean critical
    ) {

        this.entityId =
                SpigotReflectionUtil.generateEntityId();

        this.location = location;
        this.damage = damage;
        this.type = type;
        this.critical = critical;
    }

    public int getEntityId() {
        return entityId;
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
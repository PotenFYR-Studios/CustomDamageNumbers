package in.potenfyr.cdn.damage;

import org.bukkit.Location;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One live damage number.
 *
 * <p>Owned and mutated by {@link DamageService} on the main thread only; the viewer
 * set is concurrent because a viewer may be removed from a culling pass while the
 * set is being read.</p>
 *
 * <p>The display stores the <em>victim</em> rather than a fixed position: the
 * animation anchor is recomputed from the live entity every tick, which is what
 * makes the number stay next to the mob (spec section 6).</p>
 */
public final class FloatingDamage {

    private final int entityId;
    private final UUID uuid = UUID.randomUUID();

    /** The damaged entity this number belongs to; {@code null} for a free-standing test display. */
    private final UUID victimId;

    private final UUID worldId;

    /** Last known anchor (entity feet + anchor height); advanced every tick. */
    private Location anchor;

    private double damage;

    private DamageType type;

    private boolean critical;

    private int ticksAlive;

    private int durationTicks;

    /** Current JSON text component, refreshed when the value changes (merging). */
    private String jsonText;

    /** Purely horizontal angle, so simultaneous numbers fan out instead of overlapping. */
    private final double angle;

    /**
     * Per-display random spawn offset, re-applied to the anchor every tick so the
     * offset survives the per-tick anchor recomputation that keeps the number next
     * to a moving entity.
     */
    private final double spawnOffsetX;

    private final double spawnOffsetZ;

    private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();

    /** Last values sent, so redundant metadata packets are skipped. */
    private float lastScale = Float.NaN;

    private byte lastOpacity = Byte.MIN_VALUE;

    /** Critical hits render larger; 1.0 for ordinary hits. */
    private float scaleMultiplier = 1.0f;

    public FloatingDamage(
            int entityId,
            UUID victimId,
            UUID worldId,
            Location anchor,
            double damage,
            DamageType type,
            boolean critical,
            int durationTicks,
            String jsonText,
            double angle,
            double spawnOffsetX,
            double spawnOffsetZ
    ) {
        this.entityId = entityId;
        this.victimId = victimId;
        this.worldId = worldId;
        this.anchor = anchor;
        this.damage = damage;
        this.type = type;
        this.critical = critical;
        this.durationTicks = Math.max(1, durationTicks);
        this.jsonText = jsonText;
        this.angle = angle;
        this.spawnOffsetX = spawnOffsetX;
        this.spawnOffsetZ = spawnOffsetZ;
    }

    public double getSpawnOffsetX() {
        return spawnOffsetX;
    }

    public double getSpawnOffsetZ() {
        return spawnOffsetZ;
    }

    public int getEntityId() {
        return entityId;
    }

    public UUID getUuid() {
        return uuid;
    }

    public UUID getVictimId() {
        return victimId;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public Location getAnchor() {
        return anchor;
    }

    public void setAnchor(Location anchor) {
        this.anchor = anchor;
    }

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public DamageType getType() {
        return type;
    }

    public void setType(DamageType type) {
        this.type = type;
    }

    public boolean isCritical() {
        return critical;
    }

    public void setCritical(boolean critical) {
        this.critical = critical;
    }

    public int getTicksAlive() {
        return ticksAlive;
    }

    public void setTicksAlive(int ticksAlive) {
        this.ticksAlive = ticksAlive;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public void setDurationTicks(int durationTicks) {
        this.durationTicks = Math.max(1, durationTicks);
    }

    public String getJsonText() {
        return jsonText;
    }

    public void setJsonText(String jsonText) {
        this.jsonText = jsonText;
    }

    public double getAngle() {
        return angle;
    }

    public Set<UUID> getViewers() {
        return viewers;
    }

    public void addViewer(UUID playerId) {
        viewers.add(playerId);
    }

    public void removeViewer(UUID playerId) {
        viewers.remove(playerId);
    }

    public float getLastScale() {
        return lastScale;
    }

    public void setLastScale(float lastScale) {
        this.lastScale = lastScale;
    }

    public byte getLastOpacity() {
        return lastOpacity;
    }

    public void setLastOpacity(byte lastOpacity) {
        this.lastOpacity = lastOpacity;
    }

    public float getScaleMultiplier() {
        return scaleMultiplier;
    }

    public void setScaleMultiplier(float scaleMultiplier) {
        this.scaleMultiplier = scaleMultiplier <= 0.0f ? 1.0f : scaleMultiplier;
    }

    public boolean isExpired() {
        return ticksAlive >= durationTicks;
    }
}

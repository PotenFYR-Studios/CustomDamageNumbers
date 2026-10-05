package in.potenfyr.cdn.api;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Describes one damage number for {@link CustomDamageNumbersApi#spawn}.
 *
 * <p>Every option is optional unless stated otherwise. Resolution order, loosest
 * to tightest: the plugin's configuration, an animation {@link #preset}, then the
 * explicit settings on this builder. Text falls back to
 * {@code styles.<type>} when no {@link #format} is given.</p>
 *
 * <p>Builders are mutable and not thread-safe; reusing one instance for repeated
 * spawns is fine and cheap. Create one with
 * {@link CustomDamageNumbersApi#numberBuilder()}.</p>
 */
public final class DamageNumberBuilder {

    // ---- anchor: exactly one of these ----
    private LivingEntity victim;
    private Location location;

    // ---- content ----
    private double value;
    private String type = "normal";
    private String format;
    private String color;
    private Boolean bold;
    private Boolean italic;
    private Boolean shadow;

    // ---- behaviour ----
    private boolean critical;
    private Player attacker;
    private boolean silent;
    private Set<Player> viewers;

    // ---- animation overrides ----
    private String preset;
    private Integer durationTicks;
    private Integer riseTicks;
    private Double verticalSpeed;
    private Double startScale;
    private Double endScale;
    private Boolean bounce;
    private Boolean rotation;
    private Double positionX;
    private Double positionY;
    private Double positionZ;
    private Boolean randomOffset;
    private Double spread;

    /** Anchors the number to a living entity; it follows the entity while it lives. */
    public DamageNumberBuilder at(LivingEntity victim) {
        this.victim = victim;
        this.location = null;
        return this;
    }

    /** Anchors the number to a fixed world position; it does not follow anything. */
    public DamageNumberBuilder at(Location location) {
        this.location = location;
        this.victim = null;
        return this;
    }

    /** The number to render, substituted into {@code {damage}} / {@code {value}}. Required. */
    public DamageNumberBuilder value(double value) {
        this.value = value;
        return this;
    }

    /**
     * The {@code styles.<key>} section to format with, e.g. {@code fire} or a
     * custom key your plugin wrote into config.yml. Defaults to {@code normal}.
     * Ignored when {@link #format} is set.
     */
    public DamageNumberBuilder type(String type) {
        this.type = type;
        return this;
    }

    /**
     * A full format string replacing the style's own, e.g.
     * {@code "&6✦ {damage}"} or MiniMessage {@code "<gold>{damage} dmg"}.
     * {@code {damage}} and {@code {value}} are substituted.
     */
    public DamageNumberBuilder format(String format) {
        this.format = format;
        return this;
    }

    /** Default colour as {@code #RRGGBB} or a MiniMessage colour name; format-embedded colours win. */
    public DamageNumberBuilder color(String color) {
        this.color = color;
        return this;
    }

    public DamageNumberBuilder bold(boolean bold) {
        this.bold = bold;
        return this;
    }

    public DamageNumberBuilder italic(boolean italic) {
        this.italic = italic;
        return this;
    }

    /** Text shadow; only applies on the TextDisplay renderer (1.20.2+). */
    public DamageNumberBuilder shadow(boolean shadow) {
        this.shadow = shadow;
        return this;
    }

    /** Marks the display as a critical hit: larger scale, crit particles and sound. */
    public DamageNumberBuilder critical(boolean critical) {
        this.critical = critical;
        return this;
    }

    /** The player responsible; influences style profiles and crit ownership. Optional. */
    public DamageNumberBuilder attacker(Player attacker) {
        this.attacker = attacker;
        return this;
    }

    /** Suppresses the configured particles and critical sound for this number. */
    public DamageNumberBuilder silent(boolean silent) {
        this.silent = silent;
        return this;
    }

    /**
     * Forces exactly these viewers instead of the plugin's automatic nearby
     * selection. Players who opted out are still skipped unless you also manage
     * their preference; offline players are ignored.
     */
    public DamageNumberBuilder viewers(Collection<Player> viewers) {
        this.viewers = viewers == null ? null : new LinkedHashSet<>(viewers);
        return this;
    }

    /** An animation preset from presets.yml to base this number on. */
    public DamageNumberBuilder preset(String preset) {
        this.preset = preset;
        return this;
    }

    /** Total lifetime in ticks (20 = 1s). */
    public DamageNumberBuilder durationTicks(int durationTicks) {
        this.durationTicks = durationTicks;
        return this;
    }

    /** Ticks spent rising before the number sinks. */
    public DamageNumberBuilder riseTicks(int riseTicks) {
        this.riseTicks = riseTicks;
        return this;
    }

    /** Blocks per tick during the rise; rise-ticks × speed is the peak height. */
    public DamageNumberBuilder verticalSpeed(double verticalSpeed) {
        this.verticalSpeed = verticalSpeed;
        return this;
    }

    /** Scale at spawn; critical hits multiply this further. */
    public DamageNumberBuilder startScale(double startScale) {
        this.startScale = startScale;
        return this;
    }

    /** Scale at expiry. */
    public DamageNumberBuilder endScale(double endScale) {
        this.endScale = endScale;
        return this;
    }

    /** Layers a damped bounce onto the rise. */
    public DamageNumberBuilder bounce(boolean bounce) {
        this.bounce = bounce;
        return this;
    }

    /** Orbits the anchor instead of drifting straight out. */
    public DamageNumberBuilder rotation(boolean rotation) {
        this.rotation = rotation;
        return this;
    }

    /**
     * Where the number sits relative to the anchor: horizontal offset from the
     * entity's centre and height above its feet. Only the given axes change;
     * the rest keep the configured values.
     */
    public DamageNumberBuilder position(double x, double y, double z) {
        this.positionX = x;
        this.positionY = y;
        this.positionZ = z;
        return this;
    }

    /** Random spawn offset for readability, and its half-range in blocks. */
    public DamageNumberBuilder offset(boolean random, double spread) {
        this.randomOffset = random;
        this.spread = spread;
        return this;
    }

    // ------------------------------------------------------------------
    // Accessors for the implementation
    // ------------------------------------------------------------------

    public LivingEntity victim() {
        return victim;
    }

    public Location location() {
        return location;
    }

    public double value() {
        return value;
    }

    public String type() {
        return type;
    }

    public String format() {
        return format;
    }

    public String color() {
        return color;
    }

    public Boolean bold() {
        return bold;
    }

    public Boolean italic() {
        return italic;
    }

    public Boolean shadow() {
        return shadow;
    }

    public boolean critical() {
        return critical;
    }

    public Player attacker() {
        return attacker;
    }

    public boolean silent() {
        return silent;
    }

    public Set<Player> viewers() {
        return viewers;
    }

    public String preset() {
        return preset;
    }

    public Integer durationTicks() {
        return durationTicks;
    }

    public Integer riseTicks() {
        return riseTicks;
    }

    public Double verticalSpeed() {
        return verticalSpeed;
    }

    public Double startScale() {
        return startScale;
    }

    public Double endScale() {
        return endScale;
    }

    public Boolean bounce() {
        return bounce;
    }

    public Boolean rotation() {
        return rotation;
    }

    public Double positionX() {
        return positionX;
    }

    public Double positionY() {
        return positionY;
    }

    public Double positionZ() {
        return positionZ;
    }

    public Boolean randomOffset() {
        return randomOffset;
    }

    public Double spread() {
        return spread;
    }
}

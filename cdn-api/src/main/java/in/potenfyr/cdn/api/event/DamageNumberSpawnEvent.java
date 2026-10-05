package in.potenfyr.cdn.api.event;

import in.potenfyr.cdn.api.DamageNumberBuilder;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired once for every damage number the plugin is about to render - both its
 * own pipeline and numbers other plugins spawned through
 * {@link DamageNumberBuilder}.
 *
 * <p>Listeners can cancel the display, change the rendered value, or restyle
 * it. Any field left at its default ({@code null} / unset) keeps the value the
 * plugin resolved from its configuration, so a listener only needs to touch
 * what it cares about.</p>
 *
 * <pre>{@code
 * @EventHandler
 * public void onNumber(DamageNumberSpawnEvent event) {
 *     if (event.getTypeKey().equals("fire")) {
 *         event.setFormat("<dark_red>🔥 {damage}");
 *         event.setColor(null); // let the format's own colour win
 *     }
 * }
 * }</pre>
 *
 * <p>Merging a hit into an already-live display does not fire this event; it is
 * a per-display spawn hook.</p>
 */
public class DamageNumberSpawnEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final LivingEntity victim;
    private final Player attacker;
    private final String typeKey;
    private final boolean critical;
    private final Location anchor;

    private double value;
    private String format;
    private String color;
    private Boolean bold;
    private Boolean italic;
    private Boolean shadow;
    private boolean cancelled;

    /**
     * @param victim   the damaged entity, or {@code null} for a fixed-position
     *                 number spawned through the API
     * @param attacker the responsible player, or {@code null} for environmental
     *                 or unattributed damage
     * @param typeKey  the {@code styles.<key>} section the display is formatted with
     * @param critical whether the display is flagged as a critical hit
     * @param value    the value about to be rendered
     * @param anchor   where the display will appear
     */
    public DamageNumberSpawnEvent(
            LivingEntity victim,
            Player attacker,
            String typeKey,
            boolean critical,
            double value,
            Location anchor
    ) {
        this.victim = victim;
        this.attacker = attacker;
        this.typeKey = typeKey;
        this.critical = critical;
        this.value = value;
        this.anchor = anchor == null ? null : anchor.clone();
    }

    /** The damaged entity the number is anchored to, or {@code null} for a fixed position. */
    public LivingEntity getVictim() {
        return victim;
    }

    /** The player responsible for the number, or {@code null}. */
    public Player getAttacker() {
        return attacker;
    }

    /** The {@code styles.<key>} section this display is formatted with. */
    public String getTypeKey() {
        return typeKey;
    }

    /** Whether the display is flagged as a critical hit. */
    public boolean isCritical() {
        return critical;
    }

    /** Where the display will appear. */
    public Location getAnchor() {
        return anchor == null ? null : anchor.clone();
    }

    /** The value that will be substituted into {@code {damage}} / {@code {value}}. */
    public double getValue() {
        return value;
    }

    /** Changes the rendered value; must be finite and positive to render at all. */
    public void setValue(double value) {
        this.value = value;
    }

    /** The explicit format override, or {@code null} when the style's format is kept. */
    public String getFormat() {
        return format;
    }

    /** Replaces the format string ({@code {damage}} is substituted); MiniMessage and {@code &} codes both work. */
    public void setFormat(String format) {
        this.format = format;
    }

    /** The explicit colour override, or {@code null} when the style's colour is kept. */
    public String getColor() {
        return color;
    }

    /** Overrides the colour ({@code #RRGGBB}); {@code null} keeps the style's. */
    public void setColor(String color) {
        this.color = color;
    }

    /** The bold override, or {@code null} when the style's setting is kept. */
    public Boolean getBold() {
        return bold;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    /** The italic override, or {@code null} when the style's setting is kept. */
    public Boolean getItalic() {
        return italic;
    }

    public void setItalic(boolean italic) {
        this.italic = italic;
    }

    /** The shadow override, or {@code null} when the style's setting is kept. */
    public Boolean getShadow() {
        return shadow;
    }

    /** Overrides the text shadow (TextDisplay renderer only); {@code null} keeps the style's. */
    public void setShadow(boolean shadow) {
        this.shadow = shadow;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /** Cancels the display: it is not spawned and nothing is sent to any viewer. */
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

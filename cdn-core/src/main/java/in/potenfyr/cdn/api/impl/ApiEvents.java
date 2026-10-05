package in.potenfyr.cdn.api.impl;

import in.potenfyr.cdn.api.event.DamageNumberSpawnEvent;
import in.potenfyr.cdn.config.StyleSettings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Fires the spawn event for displays and folds listener overrides back into the
 * resolved style.
 *
 * <p>Kept as a separate class so the damage pipeline depends only on the API
 * surface, and so the "no event bus" case (unit tests, shutdown) is handled in
 * exactly one place.</p>
 */
public final class ApiEvents {

    private ApiEvents() {
    }

    /**
     * Fires {@link DamageNumberSpawnEvent}.
     *
     * @return the event with listener changes applied, or {@code null} when
     *         there is no event bus (tests, shutdown) or firing failed
     */
    public static DamageNumberSpawnEvent fire(
            LivingEntity victim,
            Player attacker,
            String typeKey,
            boolean critical,
            double value,
            Location anchor
    ) {

        Server server = Bukkit.getServer();

        if (server == null || server.getPluginManager() == null) {
            return null;
        }

        DamageNumberSpawnEvent event =
                new DamageNumberSpawnEvent(victim, attacker, typeKey, critical, value, anchor);

        try {
            server.getPluginManager().callEvent(event);
        } catch (Throwable failure) {
            // A broken event bus (wrong thread, shutdown race) must never stop
            // rendering; the spawn continues with its resolved style.
            return null;
        }

        return event;
    }

    /**
     * Applies a listener's non-null overrides on top of the resolved style,
     * leaving everything the listener did not touch untouched.
     */
    public static StyleSettings styledFrom(DamageNumberSpawnEvent event, StyleSettings style) {

        if (event == null) {
            return style;
        }

        String format = event.getFormat() != null ? event.getFormat() : style.format();
        String color = event.getColor() != null ? event.getColor() : style.color();
        boolean bold = event.getBold() != null ? event.getBold() : style.bold();
        boolean italic = event.getItalic() != null ? event.getItalic() : style.italic();
        boolean shadow = event.getShadow() != null ? event.getShadow() : style.shadow();

        return new StyleSettings(style.key(), style.enabled(), format, color, bold, italic, shadow);
    }
}

package in.potenfyr.cdn.damage;

import in.potenfyr.cdn.config.AnimationSettings;
import in.potenfyr.cdn.config.StyleSettings;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Set;

/**
 * A fully resolved spawn request from the API: the
 * {@link in.potenfyr.cdn.api.DamageNumberBuilder} has already been mapped onto
 * configured styles and animation settings, so {@link DamageService#spawnCustom}
 * can treat it as data.
 *
 * @param victim   the entity to anchor to, or {@code null} when {@code location} is set
 * @param location the fixed anchor, or {@code null} when {@code victim} is set
 * @param value    the validated, positive value to render
 * @param typeKey  the {@code styles.<key>} section the style was resolved from
 * @param type     the matching {@link DamageType}, or {@code null} for a custom key
 * @param style    the resolved text style
 * @param animation the resolved animation settings
 * @param critical whether the display is flagged as a critical hit
 * @param attacker the responsible player, or {@code null}
 * @param viewers  the forced viewer set, or {@code null} for automatic selection
 * @param silent   whether particles and the critical sound are suppressed
 */
public record CustomSpawn(
        LivingEntity victim,
        Location location,
        double value,
        String typeKey,
        DamageType type,
        StyleSettings style,
        AnimationSettings animation,
        boolean critical,
        Player attacker,
        Set<Player> viewers,
        boolean silent
) {
}

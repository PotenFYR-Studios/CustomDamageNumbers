package in.potenfyr.cdn.damage;

import java.util.UUID;

/**
 * Supplies the style profile (a named set of style overrides) for a player.
 *
 * <p>Implemented by the LuckPerms hook, which is what finally makes the
 * {@code style-profiles} config section reachable rather than dead code.</p>
 */
@FunctionalInterface
public interface StyleResolver {

    /** A resolver that never yields a profile. */
    StyleResolver NONE = playerId -> null;

    /**
     * @return the profile name for the player, or {@code null} for the default styles
     */
    String profileFor(UUID playerId);
}

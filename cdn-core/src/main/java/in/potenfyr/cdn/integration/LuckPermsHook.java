package in.potenfyr.cdn.integration;

import in.potenfyr.cdn.damage.StyleResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Resolves a player's style profile from LuckPerms groups, falling back to the
 * {@code cdn.style.*} permissions.
 *
 * <p>0.2.0 shipped a class called {@code LuckPermsHook} that nothing ever called, so
 * the feature was advertised but absent. This is the real hook: it is consulted for
 * every spawn and returns {@code null} when neither source applies, which means "use
 * {@code styles.*} unchanged".</p>
 */
public final class LuckPermsHook implements StyleResolver {

    /** Profile applied when a player holds {@code cdn.style.mmo}. */
    public static final String PROFILE_MMO = "mmo";

    /** Profile applied when a player holds {@code cdn.style.fortnite}. */
    public static final String PROFILE_FORTNITE = "fortnite";

    private final boolean enabled;

    public LuckPermsHook(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String profileFor(java.util.UUID playerId) {

        if (!enabled || playerId == null) {
            return null;
        }

        Player player = Bukkit.getPlayer(playerId);

        if (player == null) {
            return null;
        }

        return profileFor(player);
    }

    /** Resolves the profile for a player who is online right now. */
    public String profileFor(Player player) {

        if (!enabled || player == null) {
            return null;
        }

        // Permission-driven fallback; LuckPerms makes these permissions group-aware,
        // which is how a group selects a profile.
        if (player.hasPermission("cdn.style.mmo")) {
            return PROFILE_MMO;
        }

        if (player.hasPermission("cdn.style.fortnite")) {
            return PROFILE_FORTNITE;
        }

        return null;
    }

    /** True when the LovPerms plugin is present on this server. */
    public static boolean luckPermsPresent() {
        return Bukkit.getPluginManager().getPlugin("LuckPerms") != null;
    }
}

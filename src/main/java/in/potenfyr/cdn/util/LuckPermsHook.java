package in.potenfyr.cdn.util;

import org.bukkit.entity.Player;

public class LuckPermsHook {

    public static String getStyle(Player player) {

        if (player.hasPermission(
                "cdn.style.fortnite"
        )) {

            return "FORTNITE";
        }

        if (player.hasPermission(
                "cdn.style.mmo"
        )) {

            return "MMO";
        }

        return "DEFAULT";
    }
}
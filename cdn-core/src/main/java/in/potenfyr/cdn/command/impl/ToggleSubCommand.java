package in.potenfyr.cdn.command.impl;

import in.potenfyr.cdn.CustomDamageNumbersPlugin;
import in.potenfyr.cdn.command.SubCommand;
import in.potenfyr.cdn.prefs.PlayerPreferences;
import in.potenfyr.cdn.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * {@code /cdn toggle [on|off] [player]} — the player-side on/off switch.
 *
 * <p>Switching off also clears the player's live displays immediately, so the change
 * is visible at once rather than at the end of the current animation.</p>
 */
public final class ToggleSubCommand implements SubCommand {

    private final CustomDamageNumbersPlugin plugin;
    private final Messages messages;

    public ToggleSubCommand(CustomDamageNumbersPlugin plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessages();
    }

    @Override
    public String name() {
        return "toggle";
    }

    @Override
    public List<String> aliases() {
        return List.of("t");
    }

    @Override
    public String permission() {
        return "cdn.toggle";
    }

    @Override
    public boolean playerOnly() {
        return true;
    }

    @Override
    public String usage() {
        return "/cdn toggle [on|off] [player]";
    }

    @Override
    public String description() {
        return "Turn damage numbers on or off for yourself";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        PlayerPreferences preferences = plugin.getPreferences();

        Player target = (Player) sender;

        Boolean requested = null;
        int argumentIndex = 0;

        if (args.length >= 1) {

            Boolean parsed = parseState(args[0]);

            if (parsed != null) {
                requested = parsed;
                argumentIndex = 1;
            }
        }

        if (args.length > argumentIndex) {

            String name = args[argumentIndex];

            Player other = Bukkit.getPlayerExact(name);

            if (other == null) {
                messages.send(sender, "player-not-found", "&cPlayer &e{player} &cis not online.",
                        Map.of("player", name));
                return true;
            }

            if (!sender.hasPermission("cdn.toggle.others")) {
                messages.send(sender, "no-permission", "&cYou do not have permission.");
                return true;
            }

            target = other;
        }

        boolean desired = requested != null ? requested : !preferences.isEnabled(target.getUniqueId());

        preferences.setEnabled(target.getUniqueId(), desired);

        if (!desired) {
            plugin.getDamageService().clearFor(target.getUniqueId());
        }

        boolean self = target.getUniqueId().equals(((Player) sender).getUniqueId());

        if (self) {

            messages.send(sender, desired ? "toggle-on-self" : "toggle-off-self",
                    desired
                            ? "&aDamage numbers are now &fon&a for you."
                            : "&cDamage numbers are now &foff&c for you.");

        } else {

            messages.send(sender, "toggle-other",
                    "&aDamage numbers for &e{player}&a are now &f{state}&a.",
                    Map.of("player", target.getName(), "state", desired ? "on" : "off"));

            messages.send(target, desired ? "toggle-on-self" : "toggle-off-self",
                    desired
                            ? "&aDamage numbers are now &fon&a for you."
                            : "&cDamage numbers are now &foff&c for you.");
        }

        return true;
    }

    /** Parses an on/off/true/false/yes/no argument; {@code null} when it is not a state. */
    public static Boolean parseState(String raw) {

        if (raw == null) {
            return null;
        }

        return switch (raw.toLowerCase(Locale.ROOT)) {

            case "on", "true", "yes", "enable", "enabled", "1" -> Boolean.TRUE;
            case "off", "false", "no", "disable", "disabled", "0" -> Boolean.FALSE;
            default -> null;
        };
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {

        if (args.length == 1) {
            return List.of("on", "off");
        }

        if (args.length == 2 && sender.hasPermission("cdn.toggle.others")) {

            List<String> names = new ArrayList<>();

            for (Player player : Bukkit.getOnlinePlayers()) {

                if (player.getName().toLowerCase(Locale.ROOT)
                        .startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    names.add(player.getName());
                }
            }

            return names;
        }

        return List.of();
    }
}

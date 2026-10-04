package in.potenfyr.cdn.command.impl;

import in.potenfyr.cdn.CustomDamageNumbersPlugin;
import in.potenfyr.cdn.command.SubCommand;
import in.potenfyr.cdn.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * {@code /cdn style [player] <profile>} — assigns a style profile from
 * {@code style-profiles.<name>} to a player, overriding the permission-derived one.
 */
public final class StyleSubCommand implements SubCommand {

    private final CustomDamageNumbersPlugin plugin;
    private final Messages messages;

    public StyleSubCommand(CustomDamageNumbersPlugin plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessages();
    }

    @Override
    public String name() {
        return "style";
    }

    @Override
    public String permission() {
        return "cdn.style";
    }

    @Override
    public boolean playerOnly() {
        return true;
    }

    @Override
    public String usage() {
        return "/cdn style [player] <profile|default>";
    }

    @Override
    public String description() {
        return "Choose a damage number style profile";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        if (args.length == 0) {
            messages.send(sender, "style-usage", "&eUsage: &f" + usage());
            return true;
        }

        Player target = (Player) sender;
        String profile = args[0];

        if (args.length >= 2) {

            Player other = Bukkit.getPlayerExact(args[0]);

            if (other == null) {
                messages.send(sender, "player-not-found", "&cPlayer &e{player} &cis not online.",
                        Map.of("player", args[0]));
                return true;
            }

            if (!sender.hasPermission("cdn.style.others")) {
                messages.send(sender, "no-permission", "&cYou do not have permission.");
                return true;
            }

            target = other;
            profile = args[1];
        }

        boolean clearing = "default".equalsIgnoreCase(profile) || "none".equalsIgnoreCase(profile);

        if (!clearing && !plugin.getConfigManager().styleProfiles().containsKey(profile.toLowerCase(Locale.ROOT))) {

            messages.send(sender, "style-unknown", "&cNo style profile named &e{profile}&c.",
                    Map.of("profile", profile));

            return true;
        }

        plugin.getPreferences().setStyle(target.getUniqueId(), clearing ? null : profile);

        messages.send(sender, "style-set", "&aStyle for &e{player}&a set to &f{profile}&a.",
                Map.of("player", target.getName(),
                        "profile", clearing ? "default" : profile.toLowerCase(Locale.ROOT)));

        return true;
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {

        List<String> profiles = new ArrayList<>();
        profiles.add("default");
        profiles.addAll(plugin.getConfigManager().styleProfiles().keySet());

        if (args.length == 1) {

            List<String> completions = new ArrayList<>(profiles);

            if (sender.hasPermission("cdn.style.others")) {

                String prefix = args[0].toLowerCase(Locale.ROOT);

                for (Player player : Bukkit.getOnlinePlayers()) {

                    if (player.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                        completions.add(player.getName());
                    }
                }
            }

            return completions;
        }

        return args.length == 2 ? profiles : List.of();
    }
}

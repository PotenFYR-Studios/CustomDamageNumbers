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

/** {@code /cdn clear [player|all]} — removes live displays on demand. */
public final class ClearSubCommand implements SubCommand {

    private final CustomDamageNumbersPlugin plugin;
    private final Messages messages;

    public ClearSubCommand(CustomDamageNumbersPlugin plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessages();
    }

    @Override
    public String name() {
        return "clear";
    }

    @Override
    public String permission() {
        return "cdn.clear";
    }

    @Override
    public String usage() {
        return "/cdn clear [player|all]";
    }

    @Override
    public String description() {
        return "Remove live damage displays";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        if (args.length == 0 || args[0].equalsIgnoreCase("all")) {

            int before = plugin.getDamageService().activeCount();

            plugin.getDamageService().clear();

            messages.send(sender, "clear-all", "&aCleared &e{count} &adisplay(s) for every viewer.",
                    Map.of("count", String.valueOf(before)));

            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);

        if (target == null) {
            messages.send(sender, "player-not-found", "&cPlayer &e{player} &cis not online.",
                    Map.of("player", args[0]));
            return true;
        }

        int affected = plugin.getDamageService().clearFor(target.getUniqueId());

        messages.send(sender, "clear-player", "&aCleared &e{count} &adisplay(s) for &e{player}&a.",
                Map.of("count", String.valueOf(affected), "player", target.getName()));

        return true;
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {

        if (args.length != 1) {
            return List.of();
        }

        List<String> completions = new ArrayList<>();
        completions.add("all");

        String prefix = args[0].toLowerCase(Locale.ROOT);

        for (Player player : Bukkit.getOnlinePlayers()) {

            if (player.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                completions.add(player.getName());
            }
        }

        return completions;
    }
}

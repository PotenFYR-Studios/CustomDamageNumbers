package in.potenfyr.cdn.command;

import in.potenfyr.cdn.CustomDamageNumbersPlugin;
import in.potenfyr.cdn.damage.DamageRenderer;
import in.potenfyr.cdn.damage.DamageType;
import in.potenfyr.cdn.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public class CDNCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUB_COMMANDS = List.of("reload", "test");

    private final Messages messages;

    public CDNCommand(Messages messages) {
        this.messages = messages;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (args.length == 0) {

            sender.sendMessage(messages.message("prefix", "&6[CDN] ")
                    .append(messages.message("help", "&e/cdn reload &7- &fReload config")
                            .appendNewline()
                            .append(messages.message("help-test", "&e/cdn test &7- &fSpawn a test display"))));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {

            if (!sender.hasPermission("cdn.reload")) {

                sender.sendMessage(messages.prefixed(
                        "no-permission", "&cYou do not have permission."));
                return true;
            }

            var plugin = CustomDamageNumbersPlugin.getInstance();

            plugin.reloadConfig();
            messages.reload();

            // A changed animation interval only applies once the task restarts.
            plugin.restartAnimationTask();

            sender.sendMessage(messages.prefixed(
                    "reload", "&aConfiguration reloaded."));

            return true;
        }

        if (args[0].equalsIgnoreCase("test")) {

            if (!sender.hasPermission("cdn.test")) {

                sender.sendMessage(messages.prefixed(
                        "no-permission", "&cYou do not have permission."));
                return true;
            }

            if (!(sender instanceof Player player)) {

                sender.sendMessage(messages.prefixed(
                        "players-only", "&cOnly players can use this."));
                return true;
            }

            DamageRenderer.spawn(
                    player,
                    25.0,
                    DamageType.CRITICAL,
                    true
            );

            sender.sendMessage(messages.prefixed(
                    "test", "&aSpawned test damage."));

            return true;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (args.length == 1) {

            String prefix = args[0].toLowerCase(Locale.ROOT);

            return SUB_COMMANDS.stream()
                    .filter(sub -> sub.startsWith(prefix))
                    .toList();
        }

        return List.of();
    }
}
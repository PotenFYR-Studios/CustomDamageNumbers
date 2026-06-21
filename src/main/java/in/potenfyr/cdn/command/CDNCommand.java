package in.potenfyr.cdn.command;

import in.potenfyr.cdn.CustomDamageNumbersPlugin;
import in.potenfyr.cdn.damage.DamageRenderer;
import in.potenfyr.cdn.damage.DamageType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CDNCommand implements CommandExecutor {

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (args.length == 0) {

            sender.sendMessage("§6CustomDamageNumbers");
            sender.sendMessage("§e/cdn reload");
            sender.sendMessage("§e/cdn test");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {

            if (!sender.hasPermission("cdn.reload")) {

                sender.sendMessage("§cNo permission.");
                return true;
            }

            CustomDamageNumbersPlugin.getInstance()
                    .reloadConfig();

            sender.sendMessage(
                    "§aConfiguration reloaded."
            );

            return true;
        }

        if (args[0].equalsIgnoreCase("test")) {

            if (!(sender instanceof Player player)) {
                return true;
            }

            DamageRenderer.spawn(
                    player,
                    25.0,
                    DamageType.CRITICAL,
                    true
            );

            sender.sendMessage(
                    "§aSpawned test damage."
            );

            return true;
        }

        return true;
    }
}
package in.potenfyr.cdn.command;

import in.potenfyr.cdn.CustomDamageNumbersPlugin;
import in.potenfyr.cdn.command.impl.BackendSubCommand;
import in.potenfyr.cdn.command.impl.ClearSubCommand;
import in.potenfyr.cdn.command.impl.DebugSubCommand;
import in.potenfyr.cdn.command.impl.HelpSubCommand;
import in.potenfyr.cdn.command.impl.ReloadSubCommand;
import in.potenfyr.cdn.command.impl.StatsSubCommand;
import in.potenfyr.cdn.command.impl.StyleSubCommand;
import in.potenfyr.cdn.command.impl.TestSubCommand;
import in.potenfyr.cdn.command.impl.ToggleSubCommand;
import in.potenfyr.cdn.command.impl.VersionSubCommand;
import in.potenfyr.cdn.util.ChatFrame;
import in.potenfyr.cdn.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Routes {@code /cdn <subcommand>} to its {@link SubCommand}, applying permission and
 * player-only checks in one place, and drives tab completion from the same table.
 */
public final class CommandDispatcher implements CommandExecutor, TabCompleter {

    private final CustomDamageNumbersPlugin plugin;
    private final Messages messages;

    private final SubCommandTable table = new SubCommandTable();

    public CommandDispatcher(CustomDamageNumbersPlugin plugin) {

        this.plugin = plugin;
        this.messages = plugin.getMessages();

        register(new HelpSubCommand(this));
        register(new ReloadSubCommand(plugin));
        register(new TestSubCommand(plugin));
        register(new ToggleSubCommand(plugin));
        register(new DebugSubCommand(plugin));
        register(new StatsSubCommand(plugin));
        register(new ClearSubCommand(plugin));
        register(new StyleSubCommand(plugin));
        register(new BackendSubCommand(plugin));
        register(new VersionSubCommand(plugin));
    }

    private void register(SubCommand subCommand) {
        table.register(subCommand);
    }

    /** Subcommands in help order. */
    public List<SubCommand> subCommands() {
        return table.ordered();
    }

    public Optional<SubCommand> find(String name) {
        return table.find(name);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        SubCommand subCommand = table.find(args[0]).orElse(null);

        if (subCommand == null) {

            messages.send(sender, "unknown-subcommand", "&cUnknown subcommand. Try &e/cdn help&c.");

            return true;
        }

        if (!table.mayUse(sender::hasPermission, subCommand)) {

            messages.send(sender, "no-permission", "&cYou do not have permission.");

            return true;
        }

        if (subCommand.playerOnly() && !(sender instanceof Player)) {

            messages.send(sender, "players-only", "&cOnly players can use this command.");

            return true;
        }

        return subCommand.execute(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 1) {
            return table.completions(args[0], sender::hasPermission);
        }

        SubCommand subCommand = table.find(args[0]).orElse(null);

        if (subCommand == null || !table.mayUse(sender::hasPermission, subCommand)) {
            return List.of();
        }

        return subCommand.complete(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    /** Sends the help panel as a framed box, filtered to what the sender may use. */
    public void sendHelp(CommandSender sender) {

        ChatFrame frame = messages.frame();
        List<ChatFrame.Row> rows = new ArrayList<>();

        for (SubCommand subCommand : table.ordered()) {

            if (!table.mayUse(sender::hasPermission, subCommand)) {
                continue;
            }

            String permission = subCommand.permission();

            rows.add(new ChatFrame.Row(
                    "/cdn " + subCommand.name(),
                    subCommand.description() + (permission == null
                            ? ""
                            : " " + messages.plain("help-label-permission", "&8[{permission}]")
                                    .replace("{permission}", permission))));
        }

        String subtitle = messages.plain("help-subtitle", "&7{version} &8· &7{backend} renderer")
                .replace("{version}", plugin.getDescription().getVersion())
                .replace("{backend}", plugin.getBackend() == null ? "none" : plugin.getBackend().id());

        frame.send(sender, frame.box(
                messages.plain("help-title", "CustomDamageNumbers"),
                subtitle,
                rows,
                List.of(messages.plain("help-footer", "&7Run &f/cdn <command> &7for usage"))));
    }

    /** The owning plugin; subcommands use it for their services. */
    public CustomDamageNumbersPlugin plugin() {
        return plugin;
    }

    public Messages messages() {
        return messages;
    }
}

package in.potenfyr.cdn.command.impl;

import in.potenfyr.cdn.command.CommandDispatcher;
import in.potenfyr.cdn.command.SubCommand;
import org.bukkit.command.CommandSender;

import java.util.List;

/** {@code /cdn help} — lists the subcommands the sender may use. */
public final class HelpSubCommand implements SubCommand {

    private final CommandDispatcher dispatcher;

    public HelpSubCommand(CommandDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public String name() {
        return "help";
    }

    @Override
    public List<String> aliases() {
        return List.of("?");
    }

    @Override
    public String usage() {
        return "/cdn help";
    }

    @Override
    public String description() {
        return "Show this command list";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        dispatcher.sendHelp(sender);
        return true;
    }
}

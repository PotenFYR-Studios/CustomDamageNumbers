package in.potenfyr.cdn.command.impl;

import in.potenfyr.cdn.CustomDamageNumbersPlugin;
import in.potenfyr.cdn.command.SubCommand;
import in.potenfyr.cdn.util.Messages;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

/** {@code /cdn debug [on|off]} — toggles verbose logging and persists the change. */
public final class DebugSubCommand implements SubCommand {

    private final CustomDamageNumbersPlugin plugin;
    private final Messages messages;

    public DebugSubCommand(CustomDamageNumbersPlugin plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessages();
    }

    @Override
    public String name() {
        return "debug";
    }

    @Override
    public String permission() {
        return "cdn.debug";
    }

    @Override
    public String usage() {
        return "/cdn debug [on|off]";
    }

    @Override
    public String description() {
        return "Toggle verbose logging";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        boolean current = plugin.getConfigManager().isDebug();

        Boolean requested = args.length >= 1 ? ToggleSubCommand.parseState(args[0]) : null;

        boolean desired = requested != null ? requested : !current;

        plugin.getConfig().set("general.debug", desired);
        plugin.saveConfig();
        plugin.getConfigManager().reload();

        messages.send(sender, "debug", "&aDebug logging is now &f{state}&a.",
                Map.of("state", desired ? "on" : "off"));

        return true;
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        return args.length == 1 ? List.of("on", "off") : List.of();
    }
}

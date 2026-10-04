package in.potenfyr.cdn.command.impl;

import in.potenfyr.cdn.CustomDamageNumbersPlugin;
import in.potenfyr.cdn.command.SubCommand;
import in.potenfyr.cdn.util.Messages;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Locale;

/** {@code /cdn reload [config|messages|all]} — re-reads the configuration in place. */
public final class ReloadSubCommand implements SubCommand {

    private final CustomDamageNumbersPlugin plugin;
    private final Messages messages;

    public ReloadSubCommand(CustomDamageNumbersPlugin plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessages();
    }

    @Override
    public String name() {
        return "reload";
    }

    @Override
    public List<String> aliases() {
        return List.of("rl");
    }

    @Override
    public String permission() {
        return "cdn.reload";
    }

    @Override
    public String usage() {
        return "/cdn reload [config|messages|all]";
    }

    @Override
    public String description() {
        return "Reload config.yml and messages.yml";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        String target = args.length == 0 ? "all" : args[0].toLowerCase(Locale.ROOT);

        switch (target) {

            case "config" -> {
                plugin.getConfigManager().reload();
                plugin.restartAnimationTask();
                messages.send(sender, "reload-config", "&aConfiguration reloaded from config.yml.");
            }

            case "messages" -> {
                messages.reload();
                messages.send(sender, "reload-messages", "&aMessages reloaded from messages.yml.");
            }

            case "all" -> {
                plugin.reloadEverything();
                messages.send(sender, "reload", "&aConfiguration and messages reloaded.");
            }

            default -> messages.send(sender, "unknown-subcommand",
                    "&cUnknown reload target. Use config, messages or all.");
        }

        return true;
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {

        if (args.length == 1) {
            return List.of("config", "messages", "all");
        }

        return List.of();
    }
}

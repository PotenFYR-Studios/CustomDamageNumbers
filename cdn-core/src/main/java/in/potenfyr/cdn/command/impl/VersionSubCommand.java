package in.potenfyr.cdn.command.impl;

import in.potenfyr.cdn.CustomDamageNumbersPlugin;
import in.potenfyr.cdn.command.SubCommand;
import in.potenfyr.cdn.packet.RenderBackend;
import in.potenfyr.cdn.util.ChatFrame;
import in.potenfyr.cdn.util.Messages;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * {@code /cdn version} — a framed panel naming what is running and what this jar
 * serves, because "which jar do I install" is the question this release exists to
 * answer.
 */
public final class VersionSubCommand implements SubCommand {

    private final CustomDamageNumbersPlugin plugin;
    private final Messages messages;

    public VersionSubCommand(CustomDamageNumbersPlugin plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessages();
    }

    @Override
    public String name() {
        return "version";
    }

    @Override
    public List<String> aliases() {
        return List.of("ver");
    }

    @Override
    public String usage() {
        return "/cdn version";
    }

    @Override
    public String description() {
        return "Show version information";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        RenderBackend backend = plugin.getBackend();
        ChatFrame frame = messages.frame();

        List<ChatFrame.Row> rows = List.of(
                row("version-label-plugin", "Plugin", plugin.getDescription().getVersion()),
                row("version-label-renderer", "Renderer", backend == null ? "none" : backend.id()),
                row("version-label-server", "Server",
                        plugin.getEcosystem().displayName() + " " + plugin.getServerVersion().raw()),
                row("version-label-support", "Supports", "1.20.2 - 26.x (this jar)"));

        frame.send(sender, frame.box(
                messages.plain("version-title", "Version"),
                null,
                rows,
                List.of(messages.plain("version-support",
                        "&7This jar serves &f1.20.2 - 26.x&7; install &fCustomDamageNumbers-Legacy"
                                + "&7 for &f1.17.x - 1.20.1&7."),
                        messages.plain("version-footer",
                                "&8Legacy jar for 1.17.x - 1.20.1"))));

        return true;
    }

    private ChatFrame.Row row(String key, String fallbackLabel, Object value) {
        return new ChatFrame.Row(messages.plain(key, fallbackLabel), String.valueOf(value));
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        return List.of();
    }
}

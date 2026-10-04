package in.potenfyr.cdn.command.impl;

import in.potenfyr.cdn.CustomDamageNumbersPlugin;
import in.potenfyr.cdn.command.SubCommand;
import in.potenfyr.cdn.packet.AnimationTask;
import in.potenfyr.cdn.packet.RenderBackend;
import in.potenfyr.cdn.util.ChatFrame;
import in.potenfyr.cdn.util.Messages;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Locale;

/**
 * {@code /cdn backend} — which renderer is active and, honestly, what it can do:
 * the legacy renderer cannot fade or scale per entity and says so here.
 */
public final class BackendSubCommand implements SubCommand {

    private final CustomDamageNumbersPlugin plugin;
    private final Messages messages;

    public BackendSubCommand(CustomDamageNumbersPlugin plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessages();
    }

    @Override
    public String name() {
        return "backend";
    }

    @Override
    public String usage() {
        return "/cdn backend";
    }

    @Override
    public String description() {
        return "Show the active renderer and its capabilities";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        RenderBackend backend = plugin.getBackend();
        ChatFrame frame = messages.frame();

        if (backend == null) {
            messages.feedbackError(sender, "backend-unknown", "&cNo renderer is active.");
            return true;
        }

        String yes = messages.plain("value-yes", "&ayes");
        String no = messages.plain("value-no", "&cno");

        AnimationTask task = plugin.getAnimationTask();

        List<ChatFrame.Row> rows = List.of(
                row("stats-backend", "Renderer", backend.id()),
                row("backend-label-scale", "Scale animation", backend.supportsScale() ? yes : no),
                row("backend-label-fade", "Fade animation", backend.supportsOpacity() ? yes : no),
                row("backend-label-scheduler", "Scheduler",
                        plugin.getScheduler() == null ? "none" : plugin.getScheduler().describe()),
                row("backend-label-cost", "Tick cost", String.format(Locale.ROOT, "avg %.3f ms",
                        task == null ? 0.0 : task.averageMillis())));

        frame.send(sender, frame.box(
                messages.plain("backend-title", "Renderer"),
                messages.plain("stats-subtitle", "&7{server} &8· &7{scheduler}")
                        .replace("{server}", plugin.getEcosystem().displayName() + " "
                                + plugin.getServerVersion().raw())
                        .replace("{scheduler}", plugin.getScheduler() == null
                                ? "none" : plugin.getScheduler().describe()),
                rows,
                List.of(messages.plain("backend-footer",
                        "&8Fade and scale need the modern jar (1.20.2+)."))));

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

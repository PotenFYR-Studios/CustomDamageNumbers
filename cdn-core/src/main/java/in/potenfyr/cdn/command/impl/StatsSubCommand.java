package in.potenfyr.cdn.command.impl;

import in.potenfyr.cdn.CustomDamageNumbersPlugin;
import in.potenfyr.cdn.command.SubCommand;
import in.potenfyr.cdn.damage.DamageService;
import in.potenfyr.cdn.packet.AnimationTask;
import in.potenfyr.cdn.util.ChatFrame;
import in.potenfyr.cdn.util.Messages;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Locale;

/**
 * {@code /cdn stats} — a framed panel of live counters, so the audit claims are
 * checkable at runtime rather than taken on trust.
 */
public final class StatsSubCommand implements SubCommand {

    private final CustomDamageNumbersPlugin plugin;
    private final Messages messages;

    public StatsSubCommand(CustomDamageNumbersPlugin plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessages();
    }

    @Override
    public String name() {
        return "stats";
    }

    @Override
    public String permission() {
        return "cdn.stats";
    }

    @Override
    public String usage() {
        return "/cdn stats";
    }

    @Override
    public String description() {
        return "Show live counters and tick cost";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        DamageService service = plugin.getDamageService();
        AnimationTask task = plugin.getAnimationTask();
        ChatFrame frame = messages.frame();

        String server = plugin.getEcosystem().displayName() + " " + plugin.getServerVersion().raw();
        String scheduler = plugin.getScheduler() == null ? "none" : plugin.getScheduler().describe();

        List<ChatFrame.Row> rows = List.of(
                row("stats-backend", "Backend", plugin.getBackend() == null ? "none" : plugin.getBackend().id()),
                row("stats-active", "Active displays", service.activeCount()),
                row("stats-viewers", "Viewer slots", service.viewerSlotTotal()),
                row("stats-merges", "Merge entries", service.mergeEntries()),
                row("stats-ticks", "Animation ticks", task == null ? 0L : task.executions()),
                row("stats-tick-cost", "Tick cost", String.format(Locale.ROOT, "avg %.3f ms · worst %.3f ms",
                        task == null ? 0.0 : task.averageMillis(),
                        task == null ? 0.0 : task.worstMillis())),
                row("stats-optouts", "Players opted out", plugin.getPreferences().overrideCount()),
                row("stats-server", "Server", server));

        frame.send(sender, frame.box(
                messages.plain("stats-title", "Statistics"),
                messages.plain("stats-subtitle", "&7{server} &8· &7{scheduler}")
                        .replace("{server}", server)
                        .replace("{scheduler}", scheduler),
                rows,
                List.of(messages.plain("stats-footer", "&7Tick cost is the average time one animation step takes."))));

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

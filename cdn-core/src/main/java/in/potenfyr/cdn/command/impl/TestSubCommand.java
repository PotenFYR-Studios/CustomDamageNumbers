package in.potenfyr.cdn.command.impl;

import in.potenfyr.cdn.CustomDamageNumbersPlugin;
import in.potenfyr.cdn.command.SubCommand;
import in.potenfyr.cdn.damage.DamageService;
import in.potenfyr.cdn.damage.DamageType;
import in.potenfyr.cdn.util.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@code /cdn test [type] [amount]} — spawns a test display anchored to the
 * sender, which is also how the container harness exercises the render path.
 */
public final class TestSubCommand implements SubCommand {

    private final CustomDamageNumbersPlugin plugin;
    private final Messages messages;

    public TestSubCommand(CustomDamageNumbersPlugin plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessages();
    }

    @Override
    public String name() {
        return "test";
    }

    @Override
    public String permission() {
        return "cdn.test";
    }

    @Override
    public boolean playerOnly() {
        return true;
    }

    @Override
    public String usage() {
        return "/cdn test [type] [amount]";
    }

    @Override
    public String description() {
        return "Spawn a test damage display at your position";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        Player player = (Player) sender;

        if (!plugin.getConfigManager().isEnabled()) {

            messages.send(sender, "plugin-disabled",
                    "&cDamage numbers are disabled in config.yml (general.enabled).");

            return true;
        }

        DamageType type = args.length >= 1
                ? DamageType.byName(args[0]).orElse(null)
                : DamageType.NORMAL;

        if (type == null) {
            messages.send(sender, "unknown-type", "&cUnknown damage type: &e{type}", Map.of("type", args[0]));
            return true;
        }

        double amount = 1.5;

        if (args.length >= 2) {

            Optional<Double> parsed = parseAmount(args[1]);

            if (parsed.isEmpty()) {
                messages.send(sender, "invalid-amount", "&cInvalid amount: &e{amount}", Map.of("amount", args[1]));
                return true;
            }

            amount = parsed.get();
        }

        DamageService service = plugin.getDamageService();

        boolean spawned = service.spawn(player, amount, type, type == DamageType.CRITICAL, player);

        if (spawned) {
            messages.send(sender, "test", "&aSpawned a &e{type} &adisplay for &e{amount}&a.",
                    Map.of("type", type.name().toLowerCase(java.util.Locale.ROOT),
                            "amount", String.valueOf(amount)));
        } else {
            messages.send(sender, "test-failed",
                    "&cNothing was spawned: check general.enabled, view distance and your display cap.");
        }

        return true;
    }

    /** Parses a positive damage amount; exposed for the unit tests. */
    public static Optional<Double> parseAmount(String raw) {

        try {

            double value = Double.parseDouble(raw);

            return value > 0.0 && Double.isFinite(value) ? Optional.of(value) : Optional.empty();

        } catch (NumberFormatException invalid) {
            return Optional.empty();
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {

        if (args.length == 1) {

            List<String> completions = new ArrayList<>();

            for (String type : DamageType.names()) {
                if (type.startsWith(args[0].toLowerCase(java.util.Locale.ROOT))) {
                    completions.add(type);
                }
            }

            return completions;
        }

        if (args.length == 2) {
            return List.of("1.5", "10", "999");
        }

        return List.of();
    }
}

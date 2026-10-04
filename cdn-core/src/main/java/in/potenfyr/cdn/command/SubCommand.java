package in.potenfyr.cdn.command;

import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * One {@code /cdn <name>} subcommand.
 *
 * <p>Kept as a small interface so the dispatcher, permissions, tab completion and
 * help listing are all driven from one table rather than a chain of if-else blocks,
 * and so routing can be unit-tested without a server.</p>
 */
public interface SubCommand {

    /** The primary name, e.g. {@code "toggle"}. */
    String name();

    default List<String> aliases() {
        return List.of();
    }

    /** Permission required, or {@code null} for everyone. */
    default String permission() {
        return null;
    }

    /** Whether only players may run it. */
    default boolean playerOnly() {
        return false;
    }

    /** Short usage shown in help, e.g. {@code /cdn toggle [on|off] [player]}. */
    String usage();

    /** One-line description shown in help. */
    String description();

    /**
     * Runs the subcommand.
     *
     * @param args everything after the subcommand name
     * @return whether the command was handled successfully
     */
    boolean execute(CommandSender sender, String[] args);

    /** Tab completions for {@code args}, with {@code args.length - 1} being the partial word. */
    default List<String> complete(CommandSender sender, String[] args) {
        return List.of();
    }
}

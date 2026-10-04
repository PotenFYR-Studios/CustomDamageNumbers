package in.potenfyr.cdn.command;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * The subcommand lookup table: registration, name/alias resolution, permission-aware
 * listing and tab completion.
 *
 * <p>Split out of {@link CommandDispatcher} so the routing rules can be unit-tested
 * without a server, a sender or a plugin instance — the dispatcher keeps only the
 * Bukkit plumbing.</p>
 */
public final class SubCommandTable {

    private final Map<String, SubCommand> byName = new LinkedHashMap<>();
    private final List<SubCommand> ordered = new ArrayList<>();

    /** Registers a subcommand under its name and every alias. */
    public void register(SubCommand subCommand) {

        if (subCommand == null || subCommand.name() == null) {
            return;
        }

        ordered.add(subCommand);
        byName.put(subCommand.name().toLowerCase(Locale.ROOT), subCommand);

        for (String alias : subCommand.aliases()) {

            if (alias != null && !alias.isBlank()) {
                byName.putIfAbsent(alias.toLowerCase(Locale.ROOT), subCommand);
            }
        }
    }

    /** Registered subcommands in help order. */
    public List<SubCommand> ordered() {
        return List.copyOf(ordered);
    }

    /** Resolves a name or alias. */
    public Optional<SubCommand> find(String name) {
        return name == null
                ? Optional.empty()
                : Optional.ofNullable(byName.get(name.toLowerCase(Locale.ROOT)));
    }

    /**
     * Subcommand names the permission test accepts, filtered by prefix.
     *
     * @param permissionTest returns whether a permission string is granted
     */
    public List<String> completions(String prefix, Predicate<String> permissionTest) {

        String normalised = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> names = new ArrayList<>();

        for (SubCommand subCommand : ordered) {

            if (!mayUse(permissionTest, subCommand)) {
                continue;
            }

            if (subCommand.name().startsWith(normalised)) {
                names.add(subCommand.name());
            }
        }

        return names;
    }

    /** Whether the sender may see and run a subcommand. */
    public boolean mayUse(Predicate<String> permissionTest, SubCommand subCommand) {

        String permission = subCommand.permission();

        return permission == null || permissionTest == null || permissionTest.test(permission);
    }

    public int size() {
        return ordered.size();
    }
}

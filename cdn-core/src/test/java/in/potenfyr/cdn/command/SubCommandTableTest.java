package in.potenfyr.cdn.command;

import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubCommandTableTest {

    private static SubCommand sub(String name, String permission, boolean playerOnly, List<String> aliases) {

        return new SubCommand() {

            @Override
            public String name() {
                return name;
            }

            @Override
            public List<String> aliases() {
                return aliases;
            }

            @Override
            public String permission() {
                return permission;
            }

            @Override
            public boolean playerOnly() {
                return playerOnly;
            }

            @Override
            public String usage() {
                return "/cdn " + name;
            }

            @Override
            public String description() {
                return "the " + name + " command";
            }

            @Override
            public boolean execute(CommandSender sender, String[] args) {
                return true;
            }
        };
    }

    private static SubCommandTable table() {

        SubCommandTable table = new SubCommandTable();

        table.register(sub("help", null, false, List.of("?")));
        table.register(sub("reload", "cdn.reload", false, List.of("rl")));
        table.register(sub("toggle", "cdn.toggle", true, List.of("t")));
        table.register(sub("clear", "cdn.clear", false, List.of()));

        return table;
    }

    @Test
    void resolvesNamesCaseInsensitively() {

        assertEquals("reload", table().find("RELOAD").orElseThrow().name());
        assertEquals("reload", table().find("Reload").orElseThrow().name());
    }

    @Test
    void resolvesAliasesToTheirSubcommand() {

        assertEquals("reload", table().find("rl").orElseThrow().name());
        assertEquals("help", table().find("?").orElseThrow().name());
        assertEquals("toggle", table().find("t").orElseThrow().name());
    }

    @Test
    void unknownNamesResolveToNothing() {

        assertEquals(Optional.empty(), table().find("nope"));
        assertEquals(Optional.empty(), table().find(null));
    }

    @Test
    void registrationOrderIsPreservedForHelp() {

        List<String> names = table().ordered().stream().map(SubCommand::name).toList();

        assertEquals(List.of("help", "reload", "toggle", "clear"), names);
    }

    @Test
    void duplicateAliasesDoNotShadowAnExistingName() {

        SubCommandTable table = new SubCommandTable();

        table.register(sub("reload", "cdn.reload", false, List.of("t")));
        table.register(sub("toggle", "cdn.toggle", true, List.of()));

        assertEquals("reload", table.find("t").orElseThrow().name());
        assertEquals("toggle", table.find("toggle").orElseThrow().name());
    }

    @Test
    void completionHidesCommandsTheSenderMayNotUse() {

        Set<String> withoutPermissions = Set.copyOf(table().completions("", permission -> false));

        assertEquals(Set.of("help"), withoutPermissions);
    }

    @Test
    void completionFiltersByPrefix() {

        // Only canonical names are offered, not aliases: completing both "toggle" and
        // its alias "t" would be noise.
        assertEquals(List.of("reload"), table().completions("re", permission -> true));
        assertEquals(List.of("toggle"), table().completions("t", permission -> true));
        assertEquals(List.of(), table().completions("zzz", permission -> true));
        assertEquals(List.of("help", "reload", "toggle", "clear"), table().completions("", permission -> true));
    }

    @Test
    void permissionlessCommandsAreAlwaysUsable() {

        SubCommand help = table().find("help").orElseThrow();

        assertTrue(table().mayUse(permission -> false, help));
        assertFalse(table().mayUse(permission -> false, table().find("clear").orElseThrow()));
        assertTrue(table().mayUse(permission -> true, table().find("clear").orElseThrow()));
    }
}

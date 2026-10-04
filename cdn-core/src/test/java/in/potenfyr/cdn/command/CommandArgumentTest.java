package in.potenfyr.cdn.command;

import in.potenfyr.cdn.command.impl.TestSubCommand;
import in.potenfyr.cdn.command.impl.ToggleSubCommand;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The argument parsing that the toggle and test commands rely on, exercised without a
 * server. Command routing itself is covered end-to-end by the container harness.
 */
class CommandArgumentTest {

    @Test
    void onAndOffWordsAreUnderstood() {

        assertEquals(Boolean.TRUE, ToggleSubCommand.parseState("on"));
        assertEquals(Boolean.TRUE, ToggleSubCommand.parseState("ENABLED"));
        assertEquals(Boolean.TRUE, ToggleSubCommand.parseState("yes"));
        assertEquals(Boolean.TRUE, ToggleSubCommand.parseState("true"));
        assertEquals(Boolean.TRUE, ToggleSubCommand.parseState("1"));

        assertEquals(Boolean.FALSE, ToggleSubCommand.parseState("off"));
        assertEquals(Boolean.FALSE, ToggleSubCommand.parseState("DISABLED"));
        assertEquals(Boolean.FALSE, ToggleSubCommand.parseState("no"));
        assertEquals(Boolean.FALSE, ToggleSubCommand.parseState("false"));
        assertEquals(Boolean.FALSE, ToggleSubCommand.parseState("0"));
    }

    @Test
    void aWordThatIsNotAStateReturnsNullSoItIsTreatedAsAPlayerName() {

        assertNull(ToggleSubCommand.parseState("Notch"));
        assertNull(ToggleSubCommand.parseState(null));
    }

    @Test
    void damageAmountsMustBePositiveNumbers() {

        assertEquals(Optional.of(12.5), TestSubCommand.parseAmount("12.5"));
        assertEquals(Optional.of(1.0), TestSubCommand.parseAmount("1"));
        assertEquals(Optional.empty(), TestSubCommand.parseAmount("0"));
        assertEquals(Optional.empty(), TestSubCommand.parseAmount("-3"));
        assertEquals(Optional.empty(), TestSubCommand.parseAmount("abc"));
        assertEquals(Optional.empty(), TestSubCommand.parseAmount("NaN"));
    }
}

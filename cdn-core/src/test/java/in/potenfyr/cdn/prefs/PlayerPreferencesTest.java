package in.potenfyr.cdn.prefs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerPreferencesTest {

    @TempDir
    File folder;

    private PlayerPreferences preferences(boolean defaultEnabled) {

        PlayerPreferences preferences = new PlayerPreferences(
                new File(folder, "players.yml"), Logger.getLogger("test"), null, defaultEnabled);

        preferences.load();

        return preferences;
    }

    @Test
    void unknownPlayersGetTheDefault() {

        PlayerPreferences preferences = preferences(true);

        assertTrue(preferences.isEnabled(UUID.randomUUID()));
        assertFalse(preferences.hasOverride(UUID.randomUUID()));
        assertTrue(preferences.defaultEnabled());
    }

    @Test
    void aStoredChoiceSurvivesAReload() {

        UUID player = UUID.randomUUID();

        PlayerPreferences first = preferences(true);
        first.setEnabled(player, false);

        PlayerPreferences reloaded = preferences(true);

        assertFalse(reloaded.isEnabled(player));
        assertTrue(reloaded.hasOverride(player));
        assertEquals(1, reloaded.overrideCount());
    }

    @Test
    void choosingTheDefaultClearsTheOverrideSoTheFileStaysSparse() {

        UUID player = UUID.randomUUID();

        PlayerPreferences preferences = preferences(true);

        preferences.setEnabled(player, false);
        assertEquals(1, preferences.overrideCount());

        preferences.setEnabled(player, true);

        assertEquals(0, preferences.overrideCount());
        assertTrue(preferences.isEnabled(player));
    }

    @Test
    void defaultsArePersistedToo() {

        PlayerPreferences preferences = preferences(true);
        preferences.setDefaultEnabled(false);

        PlayerPreferences reloaded = preferences(true);

        assertFalse(reloaded.defaultEnabled());
        assertFalse(reloaded.isEnabled(UUID.randomUUID()));
    }

    @Test
    void styleOverridesRoundTrip() {

        UUID player = UUID.randomUUID();

        PlayerPreferences preferences = preferences(true);
        preferences.setStyle(player, "MMO");

        assertEquals("mmo", preferences.styleFor(player));

        PlayerPreferences reloaded = preferences(true);
        assertEquals("mmo", reloaded.styleFor(player));
        assertEquals(1, reloaded.styleCount());

        reloaded.setStyle(player, null);

        assertNull(reloaded.styleFor(player));
        assertEquals(0, preferences(true).styleCount());
    }

    @Test
    void malformedPlayerIdsAreIgnoredRatherThanThrowing() throws Exception {

        File file = new File(folder, "players.yml");

        java.nio.file.Files.writeString(file.toPath(), """
                default-enabled: true
                players:
                  not-a-uuid: false
                """);

        PlayerPreferences preferences = new PlayerPreferences(
                file, Logger.getLogger("test"), null, true);

        preferences.load();

        assertEquals(0, preferences.overrideCount());
        assertTrue(preferences.isEnabled(UUID.randomUUID()));
    }

    @Test
    void loadedFileIsReadableYaml() {

        UUID player = UUID.randomUUID();

        PlayerPreferences preferences = preferences(true);
        preferences.setEnabled(player, false);
        preferences.setStyle(player, "fortnite");

        org.bukkit.configuration.file.YamlConfiguration configuration =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(preferences.file());

        assertEquals(Boolean.FALSE, configuration.getBoolean("players." + player));
        assertEquals("fortnite", configuration.getString("styles." + player));
        assertTrue(configuration.getBoolean("default-enabled"));
    }
}

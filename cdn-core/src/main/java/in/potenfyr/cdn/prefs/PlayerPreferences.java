package in.potenfyr.cdn.prefs;

import in.potenfyr.cdn.platform.SchedulerAdapter;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Per-player damage-indicator preference, backed by {@code players.yml}.
 *
 * <p>Storage is sparse: only players whose choice differs from the configured
 * default are written, so the file stays proportional to the number of players who
 * actually opted out rather than to the number who ever joined.</p>
 *
 * <p>Reads are served from an in-memory map on the main thread; writes are debounced
 * and flushed off-thread through the scheduler so a toggle never stalls a tick.</p>
 */
public final class PlayerPreferences {

    private static final String FILE_NAME = "players.yml";
    private static final String ROOT = "players";

    private final File file;
    private final Logger logger;

    /** {@code null} in tests, where writes are performed inline. */
    private final SchedulerAdapter scheduler;

    private final Map<UUID, Boolean> overrides = new ConcurrentHashMap<>();

    /** Per-player style profile overrides set by {@code /cdn style}. */
    private final Map<UUID, String> styles = new ConcurrentHashMap<>();

    private volatile boolean defaultEnabled;
    private volatile boolean dirty;

    public static PlayerPreferences inDataFolder(
            File dataFolder,
            Logger logger,
            SchedulerAdapter scheduler,
            boolean defaultEnabled
    ) {
        return new PlayerPreferences(new File(dataFolder, FILE_NAME), logger, scheduler, defaultEnabled);
    }

    /** Constructor taking the storage file directly, which is also the test seam. */
    public PlayerPreferences(
            File file,
            Logger logger,
            SchedulerAdapter scheduler,
            boolean defaultEnabled
    ) {
        this.file = file;
        this.logger = logger;
        this.scheduler = scheduler;
        this.defaultEnabled = defaultEnabled;
    }

    /** Loads stored preferences; safe to call when the file does not exist yet. */
    public void load() {

        overrides.clear();
        styles.clear();

        if (!file.exists()) {
            return;
        }

        try {

            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);

            defaultEnabled = configuration.getBoolean("default-enabled", defaultEnabled);

            var section = configuration.getConfigurationSection(ROOT);

            if (section != null) {

                for (String key : section.getKeys(false)) {

                    try {
                        overrides.put(UUID.fromString(key), section.getBoolean(key));
                    } catch (IllegalArgumentException malformed) {
                        logger.warning("Ignoring malformed player id in " + FILE_NAME + ": " + key);
                    }
                }
            }

            var styleSection = configuration.getConfigurationSection("styles");

            if (styleSection != null) {

                for (String key : styleSection.getKeys(false)) {

                    try {
                        styles.put(UUID.fromString(key), styleSection.getString(key, ""));
                    } catch (IllegalArgumentException malformed) {
                        logger.warning("Ignoring malformed player id in " + FILE_NAME + " styles: " + key);
                    }
                }
            }

        } catch (RuntimeException failure) {
            logger.log(Level.WARNING, "Could not read " + FILE_NAME + "; using defaults.", failure);
        }
    }

    /** Whether the given player currently sees damage numbers. */
    public boolean isEnabled(UUID playerId) {

        if (playerId == null) {
            return false;
        }

        Boolean stored = overrides.get(playerId);

        return stored == null ? defaultEnabled : stored;
    }

    /** Whether the given player has an explicit stored preference. */
    public boolean hasOverride(UUID playerId) {
        return playerId != null && overrides.containsKey(playerId);
    }

    /**
     * Stores a player's choice.
     *
     * @return the new state
     */
    public boolean setEnabled(UUID playerId, boolean enabled) {

        if (playerId == null) {
            return defaultEnabled;
        }

        if (enabled == defaultEnabled) {
            overrides.remove(playerId);
        } else {
            overrides.put(playerId, enabled);
        }

        markDirty();
        saveNow();

        return enabled;
    }

    /** Clears an explicit preference, returning the player to the server default. */
    public boolean clearOverride(UUID playerId) {
        boolean removed = playerId != null && overrides.remove(playerId) != null;
        markDirty();
        saveNow();
        return removed;
    }

    public boolean defaultEnabled() {
        return defaultEnabled;
    }

    public void setDefaultEnabled(boolean defaultEnabled) {

        if (this.defaultEnabled == defaultEnabled) {
            return;
        }

        this.defaultEnabled = defaultEnabled;
        markDirty();
        saveNow();
    }

    public int overrideCount() {
        return overrides.size();
    }

    // ------------------------------------------------------------------
    // Style overrides (/cdn style)
    // ------------------------------------------------------------------

    /** The player's explicit style profile, or {@code null} when they have none. */
    public String styleFor(UUID playerId) {
        return playerId == null ? null : styles.get(playerId);
    }

    /**
     * Sets (or clears) a player's style profile.
     *
     * @param profile profile name, or {@code null}/blank to clear
     */
    public void setStyle(UUID playerId, String profile) {

        if (playerId == null) {
            return;
        }

        if (profile == null || profile.isBlank() || "default".equalsIgnoreCase(profile)) {
            styles.remove(playerId);
        } else {
            styles.put(playerId, profile.toLowerCase(java.util.Locale.ROOT));
        }

        markDirty();
        saveNow();
    }

    public int styleCount() {
        return styles.size();
    }

    private void markDirty() {
        dirty = true;
    }

    /** Flushes pending changes, off-thread when a scheduler is available. */
    public void saveNow() {

        if (scheduler == null) {
            write();
            return;
        }

        scheduler.runAsync(this::write);
    }

    /** Writes the current state synchronously; called by {@link #saveNow()} and on disable. */
    public synchronized void write() {

        if (!dirty) {
            return;
        }

        YamlConfiguration configuration = new YamlConfiguration();

        configuration.set("default-enabled", defaultEnabled);

        for (Map.Entry<UUID, Boolean> entry : overrides.entrySet()) {
            configuration.set(ROOT + "." + entry.getKey(), entry.getValue());
        }

        for (Map.Entry<UUID, String> entry : styles.entrySet()) {
            configuration.set("styles." + entry.getKey(), entry.getValue());
        }

        try {

            File parent = file.getParentFile();

            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                logger.warning("Could not create " + parent.getAbsolutePath());
            }

            configuration.save(file);
            dirty = false;

        } catch (IOException failure) {
            logger.log(Level.WARNING, "Could not save " + FILE_NAME + " (changes kept in memory).", failure);
        }
    }

    /** Test seam: current on-disk state, or {@code null} when nothing was written. */
    public File file() {
        return file;
    }
}

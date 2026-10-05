package in.potenfyr.cdn.config;

import in.potenfyr.cdn.damage.DamageType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads and validates every key in config.yml.
 *
 * <p>Nothing else in the plugin touches a raw config path: components ask this
 * class for a typed value, so a renamed or missing key is a one-line change and a
 * new option cannot be silently ignored (the 0.2.0 failure mode this release
 * fixes).</p>
 *
 * <p>Every animation/style value is a parsed immutable record, rebuilt on reload,
 * so the hot path never re-reads YAML.</p>
 */
public class ConfigManager {

    public static final String DEFAULT_VIEW_PERMISSION = "cdn.view";
    public static final String DEFAULT_SOUND = "entity.player.attack.crit";
    public static final int DEFAULT_DEBUG_LOG_LIMIT = 256;

    private final JavaPlugin plugin;

    private FileConfiguration config;

    /** The preset library from presets.yml; empty in tests. */
    private FileConfiguration presets;

    private volatile AnimationSettings animation = AnimationSettings.defaults();
    private volatile Map<String, StyleSettings> styles = new HashMap<>();
    private volatile Map<String, Map<String, StyleSettings>> styleProfiles = new HashMap<>();
    private volatile Map<String, ParticleSettings> particles = new HashMap<>();
    private volatile CriticalSettings critical =
            new CriticalSettings(true, 1.5, true, DEFAULT_SOUND, 1.0f, 1.2f);
    private volatile MergeSettings merge = MergeSettings.disabled();

    /** Live configuration, bound to the plugin's config.yml. */
    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        loadPresets();
        rebuild();
    }

    /** Test constructor: work against an in-memory configuration. */
    public ConfigManager(FileConfiguration config) {
        this.plugin = null;
        this.config = config;
        loadPresets();
        rebuild();
    }

    /** Test constructor: in-memory configuration with an in-memory preset library. */
    public ConfigManager(FileConfiguration config, FileConfiguration presets) {
        this.plugin = null;
        this.config = config;
        this.presets = presets;
        rebuild();
    }

    /** Re-reads config.yml and presets.yml from disk and rebuilds every derived value. */
    public void reload() {

        if (plugin != null) {
            plugin.reloadConfig();
            this.config = plugin.getConfig();
            loadPresets();
        }

        rebuild();
    }

    /** Reads presets.yml, creating it from the bundled default when missing. */
    private void loadPresets() {

        if (plugin == null) {
            presets = new YamlConfiguration();
            return;
        }

        File file = new File(plugin.getDataFolder(), "presets.yml");

        if (!file.exists()) {
            plugin.saveResource("presets.yml", false);
        }

        presets = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
    }

    private void rebuild() {

        ConfigurationSection animationSection = config.getConfigurationSection("animation");

        animation = readAnimation(animationSection, presetSection(requestedPreset(animationSection)));

        styles = readStyles(config.getConfigurationSection("styles"));
        styleProfiles = readProfiles(config.getConfigurationSection("style-profiles"));
        particles = readParticles(config.getConfigurationSection("particles"));

        critical = readCritical(config.getConfigurationSection("critical-hits"));
        merge = readMerge(config.getConfigurationSection("merge-system"));
    }

    /** The preset name requested by {@code animation.preset} in config.yml. */
    private static String requestedPreset(ConfigurationSection animationSection) {
        return animationSection == null ? "default" : animationSection.getString("preset", "default");
    }

    /**
     * Looks the requested preset up in presets.yml.
     *
     * @return {@code null} when the preset does not exist, which makes
     *         {@link #readAnimation} fall back to the built-in values; a missing
     *         non-default name is reported once per reload
     */
    private ConfigurationSection presetSection(String name) {

        if (presets == null) {
            return null;
        }

        ConfigurationSection preset = presets.getConfigurationSection(
                name == null ? "default" : name);

        if (preset == null && plugin != null && name != null && !"default".equalsIgnoreCase(name.trim())) {
            plugin.getLogger().warning("animation.preset '" + name
                    + "' is not defined in presets.yml; using the built-in animation values.");
        }

        return preset;
    }

    public FileConfiguration raw() {
        return config;
    }

    // ------------------------------------------------------------------
    // Derived values
    // ------------------------------------------------------------------

    /** Animation settings, already clamped and immutable. */
    public AnimationSettings animation() {
        return animation;
    }

    /** Style for a damage type, honouring the player's style profile. */
    public StyleSettings styleFor(DamageType type, String profile) {

        String key = type.styleKey();
        StyleSettings base = styles.getOrDefault(key, StyleSettings.fallback(key));

        if (profile == null || profile.isBlank() || "default".equalsIgnoreCase(profile)) {
            return base;
        }

        Map<String, StyleSettings> profileStyles =
                styleProfiles.get(profile.toLowerCase(Locale.ROOT));

        if (profileStyles == null) {
            return base;
        }

        return profileStyles.getOrDefault(key, base);
    }

    /** Style for a damage type using the default profile. */
    public StyleSettings styleFor(DamageType type) {
        return styleFor(type, null);
    }

    public Map<String, StyleSettings> styles() {
        return styles;
    }

    public Map<String, Map<String, StyleSettings>> styleProfiles() {
        return styleProfiles;
    }

    public ParticleSettings particleFor(DamageType type) {

        String key = type == DamageType.CRITICAL ? "critical" : type.styleKey();

        return particles.getOrDefault(key, ParticleSettings.disabled());
    }

    public CriticalSettings critical() {
        return critical;
    }

    public MergeSettings merge() {
        return merge;
    }

    // ------------------------------------------------------------------
    // general
    // ------------------------------------------------------------------

    public boolean isEnabled() {
        return config.getBoolean("general.enabled", true);
    }

    public boolean isDebug() {
        return config.getBoolean("general.debug", false);
    }

    /** Whether the ASCII startup banner is printed to the console. */
    public boolean isBannerEnabled() {
        return config.getBoolean("general.banner", true);
    }

    public int getViewDistance() {
        return clamp(config.getInt("general.view-distance", 32), 1, 512);
    }

    public List<String> getDisabledWorlds() {

        List<String> worlds = config.getStringList("general.disabled-worlds");

        return worlds == null ? List.of() : worlds;
    }

    public boolean isIgnoreInvisibleEntities() {
        return config.getBoolean("general.ignore-invisible-entities", true);
    }

    public boolean isIgnoreArmorStands() {
        return config.getBoolean("general.ignore-armor-stands", true);
    }

    public boolean isIgnoreNpcs() {
        return config.getBoolean("general.ignore-npcs", true);
    }

    public boolean isMobs() {
        return config.getBoolean("general.entities.mobs", config.getBoolean("general.mobs", true));
    }

    public boolean isPlayers() {
        return config.getBoolean("general.entities.players", config.getBoolean("general.players", true));
    }

    public boolean isAnimals() {
        return config.getBoolean("general.entities.animals", config.getBoolean("general.animals", true));
    }

    public boolean isSelfDamage() {
        return config.getBoolean("general.self-damage", true);
    }

    public boolean isNearbyViewersOnly() {
        return config.getBoolean("general.nearby-viewers-only", true);
    }

    public int getMaxActiveDisplays() {
        return clamp(config.getInt("general.max-active-displays", 2000), 1, 100_000);
    }

    public boolean isCleanupOrphans() {
        return config.getBoolean("general.cleanup-orphans", true);
    }

    /** Default state of the player-side toggle for players with no stored preference. */
    public boolean isDefaultViewEnabled() {
        return config.getBoolean("general.default-view-enabled", true);
    }

    // ------------------------------------------------------------------
    // performance / permissions / integrations / advanced
    // ------------------------------------------------------------------

    public int getAnimationInterval() {
        return clamp(config.getInt("performance.animation-interval", 1), 1, 20);
    }

    public int getMaxPerPlayer() {
        return clamp(config.getInt("performance.max-per-player", 50), 1, 50_000);
    }

    public boolean isDistanceCulling() {
        return config.getBoolean("performance.distance-culling", true);
    }

    public boolean isRequireViewPermission() {
        return config.getBoolean("permissions.require-view-permission", false);
    }

    public String getViewPermission() {

        String permission = config.getString("permissions.view-permission", DEFAULT_VIEW_PERMISSION);

        return permission == null || permission.isBlank() ? DEFAULT_VIEW_PERMISSION : permission.trim();
    }

    public boolean areStylePermissionsEnabled() {
        return config.getBoolean("permissions.style-permissions", true);
    }

    public boolean isLuckPermsEnabled() {
        return config.getBoolean("integrations.luckperms", true);
    }

    public boolean isPlaceholderApiEnabled() {
        return config.getBoolean("integrations.placeholderapi", true);
    }

    public boolean isMetricsEnabled() {
        return config.getBoolean("advanced.metrics", false);
    }

    /** The operator's bStats plugin id; {@code 0} means metrics are not reported. */
    public int getMetricsId() {
        return Math.max(0, config.getInt("advanced.metrics-id", 0));
    }

    public boolean isRemoveOnDeath() {
        return config.getBoolean("advanced.remove-on-death", true);
    }

    public boolean isIgnoreZeroDamage() {
        return config.getBoolean("advanced.ignore-zero-damage", true);
    }

    public boolean isPacketDebug() {
        return config.getBoolean("advanced.packet-debug", false);
    }

    public int getDebugLogLimit() {
        return clamp(config.getInt("advanced.debug-log-limit", DEFAULT_DEBUG_LOG_LIMIT), 16, 4096);
    }

    /**
     * {@code advanced.folia-mode}: {@code auto} (default) detects Folia, while
     * {@code true}/{@code false} force the scheduler choice.
     *
     * @return {@code null} when detection should decide
     */
    public Boolean getFoliaModeOverride() {

        String value = config.getString("advanced.folia-mode", "auto");

        if (value == null) {
            return null;
        }

        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "true" -> Boolean.TRUE;
            case "false" -> Boolean.FALSE;
            default -> null;
        };
    }

    // ------------------------------------------------------------------
    // Pure readers (unit-tested without a server)
    // ------------------------------------------------------------------

    public static AnimationSettings readAnimation(ConfigurationSection section) {
        return readAnimation(section, null);
    }

    /**
     * Reads the animation settings, layering sources over each other:
     * built-in defaults ← preset ← config.yml.
     *
     * <p>{@code position.x/y/z} and {@code offset.random/spread} are the nested
     * forms a preset uses. The flat legacy keys {@code anchor-height},
     * {@code random-offset} and {@code spawn-spread} keep working; within one
     * source the nested form wins, and anything written in config.yml wins over
     * the preset.</p>
     *
     * @param section the {@code animation} section of config.yml, may be {@code null}
     * @param preset  the chosen preset section of presets.yml, may be {@code null}
     */
    public static AnimationSettings readAnimation(ConfigurationSection section, ConfigurationSection preset) {

        AnimationSettings defaults = AnimationSettings.defaults();

        if (section == null && preset == null) {
            return defaults;
        }

        return new AnimationSettings(
                intOrDefault(section, preset, "duration-ticks", defaults.durationTicks()),
                intOrDefault(section, preset, "rise-ticks", defaults.riseTicks()),
                doubleOrDefault(section, preset, "vertical-speed", defaults.verticalSpeed()),
                doubleOrDefault(section, preset, "horizontal-randomness", defaults.horizontalRandomness()),
                boolOrDefault(section, preset, "scale-animation", defaults.scaleAnimation()),
                doubleOrDefault(section, preset, "start-scale", defaults.startScale()),
                doubleOrDefault(section, preset, "end-scale", defaults.endScale()),
                boolOrDefault(section, preset, "fade-out", defaults.fadeOut()),
                doubleOrDefault(section, preset, "fade-start", defaults.fadeStart()),
                boolOrDefault(section, preset, "bounce", defaults.bounce()),
                doubleOrDefault(section, preset, "bounce-strength", defaults.bounceStrength()),
                boolOrDefault(section, preset, "rotation", defaults.rotation()),
                doubleOrDefault(section, preset, "rotation-speed", defaults.rotationSpeed()),
                boolOrDefault(section, preset, "offset.random", "random-offset", defaults.randomOffset()),
                doubleOrDefault(section, preset, "offset.spread", "spawn-spread", defaults.spawnSpread()),
                doubleOrDefault(section, preset, "position.y", "anchor-height", defaults.anchorHeight()),
                doubleOrDefault(section, preset, "position.x", defaults.positionX()),
                doubleOrDefault(section, preset, "position.z", defaults.positionZ()),
                boolOrDefault(section, preset, "follow-entity", defaults.followEntity()),
                doubleOrDefault(section, preset, "follow-smoothing", defaults.followSmoothing()),
                doubleOrDefault(section, preset, "critical-scale-multiplier",
                        defaults.criticalScaleMultiplier()));
    }

    public static Map<String, StyleSettings> readStyles(ConfigurationSection section) {

        Map<String, StyleSettings> result = new HashMap<>();

        for (DamageType type : DamageType.values()) {

            String key = type.styleKey();

            result.put(key, readStyle(
                    section == null ? null : section.getConfigurationSection(key), key));
        }

        return result;
    }

    public static StyleSettings readStyle(ConfigurationSection section, String key) {

        if (section == null) {
            return StyleSettings.fallback(key);
        }

        StyleSettings fallback = StyleSettings.fallback(key);

        return new StyleSettings(
                key,
                section.getBoolean("enabled", fallback.enabled()),
                section.getString("format", fallback.format()),
                section.getString("color", fallback.color()),
                section.getBoolean("bold", fallback.bold()),
                section.getBoolean("italic", fallback.italic()),
                section.getBoolean("shadow", fallback.shadow()));
    }

    public static Map<String, Map<String, StyleSettings>> readProfiles(ConfigurationSection section) {

        Map<String, Map<String, StyleSettings>> result = new HashMap<>();

        if (section == null) {
            return result;
        }

        for (String profile : section.getKeys(false)) {

            ConfigurationSection profileSection = section.getConfigurationSection(profile);

            if (profileSection == null) {
                continue;
            }

            Map<String, StyleSettings> profileStyles = new HashMap<>();

            for (String typeKey : profileSection.getKeys(false)) {

                profileStyles.put(
                        typeKey.toLowerCase(Locale.ROOT),
                        readStyle(profileSection.getConfigurationSection(typeKey), typeKey));
            }

            result.put(profile.toLowerCase(Locale.ROOT), profileStyles);
        }

        return result;
    }

    public static Map<String, ParticleSettings> readParticles(ConfigurationSection section) {

        Map<String, ParticleSettings> result = new HashMap<>();

        if (section == null || !section.getBoolean("enabled", true)) {
            return result;
        }

        for (String key : section.getKeys(false)) {

            ConfigurationSection child = section.getConfigurationSection(key);

            if (child == null) {
                continue;
            }

            result.put(key.toLowerCase(Locale.ROOT), new ParticleSettings(
                    child.getBoolean("enabled", true),
                    child.getString("particle", "CRIT"),
                    child.getInt("amount", 5)));
        }

        return result;
    }

    public static CriticalSettings readCritical(ConfigurationSection section) {

        if (section == null) {
            return new CriticalSettings(true, 1.5, true, DEFAULT_SOUND, 1.0f, 1.2f);
        }

        return new CriticalSettings(
                section.getBoolean("enabled", true),
                section.getDouble("scale-multiplier", 1.5),
                section.getBoolean("sound", true),
                section.getString("sound-type", DEFAULT_SOUND),
                (float) section.getDouble("volume", 1.0),
                (float) section.getDouble("pitch", 1.2));
    }

    public static MergeSettings readMerge(ConfigurationSection section) {

        if (section == null) {
            return MergeSettings.disabled();
        }

        return new MergeSettings(
                section.getBoolean("enabled", true),
                section.getLong("merge-window-ms", 150L),
                section.getDouble("max-merged-damage", 9999.0),
                section.getBoolean("same-attacker-only", true));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    // ------------------------------------------------------------------
    // Layered reads (config.yml over the chosen preset)
    // ------------------------------------------------------------------

    private static int intOrDefault(
            ConfigurationSection section, ConfigurationSection preset, String path, int fallback) {

        if (section != null && section.contains(path)) {
            return section.getInt(path);
        }

        if (preset != null && preset.contains(path)) {
            return preset.getInt(path);
        }

        return fallback;
    }

    private static double doubleOrDefault(
            ConfigurationSection section, ConfigurationSection preset, String path, double fallback) {

        if (section != null && section.contains(path)) {
            return section.getDouble(path);
        }

        if (preset != null && preset.contains(path)) {
            return preset.getDouble(path);
        }

        return fallback;
    }

    /**
     * Reads a value that has both a nested form ({@code path}) and a legacy flat
     * form ({@code legacyPath}); within one source the nested form wins, and
     * config.yml wins over the preset.
     */
    private static double doubleOrDefault(
            ConfigurationSection section, ConfigurationSection preset,
            String path, String legacyPath, double fallback) {

        Double value = doubleOrNull(section, path, legacyPath);

        if (value == null) {
            value = doubleOrNull(preset, path, legacyPath);
        }

        return value == null ? fallback : value;
    }

    private static boolean boolOrDefault(
            ConfigurationSection section, ConfigurationSection preset, String path, boolean fallback) {

        if (section != null && section.contains(path)) {
            return section.getBoolean(path);
        }

        if (preset != null && preset.contains(path)) {
            return preset.getBoolean(path);
        }

        return fallback;
    }

    /** See {@link #doubleOrDefault(ConfigurationSection, ConfigurationSection, String, String, double)}. */
    private static boolean boolOrDefault(
            ConfigurationSection section, ConfigurationSection preset,
            String path, String legacyPath, boolean fallback) {

        Boolean value = boolOrNull(section, path, legacyPath);

        if (value == null) {
            value = boolOrNull(preset, path, legacyPath);
        }

        return value == null ? fallback : value;
    }

    private static Double doubleOrNull(ConfigurationSection section, String path, String legacyPath) {

        if (section == null) {
            return null;
        }

        if (section.contains(path)) {
            return section.getDouble(path);
        }

        if (legacyPath != null && section.contains(legacyPath)) {
            return section.getDouble(legacyPath);
        }

        return null;
    }

    private static Boolean boolOrNull(ConfigurationSection section, String path, String legacyPath) {

        if (section == null) {
            return null;
        }

        if (section.contains(path)) {
            return section.getBoolean(path);
        }

        if (legacyPath != null && section.contains(legacyPath)) {
            return section.getBoolean(legacyPath);
        }

        return null;
    }
}

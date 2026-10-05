package in.potenfyr.cdn.api.impl;

import in.potenfyr.cdn.api.CustomDamageNumbersApi;
import in.potenfyr.cdn.api.DamageNumberBuilder;
import in.potenfyr.cdn.config.AnimationSettings;
import in.potenfyr.cdn.config.ConfigManager;
import in.potenfyr.cdn.config.StyleSettings;
import in.potenfyr.cdn.damage.CustomSpawn;
import in.potenfyr.cdn.damage.DamageService;
import in.potenfyr.cdn.damage.DamageType;
import in.potenfyr.cdn.prefs.PlayerPreferences;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.Optional;

/**
 * The API implementation backing {@link CustomDamageNumbersApi}.
 *
 * <p>Translation from the builder onto core records lives in {@link #resolve},
 * a pure static function against a {@link ConfigManager}, so the mapping rules
 * are unit-testable without a server.</p>
 */
public final class ApiServiceImpl implements CustomDamageNumbersApi {

    private final JavaPlugin plugin;
    private final ConfigManager config;
    private final DamageService damageService;
    private final PlayerPreferences preferences;

    public ApiServiceImpl(
            JavaPlugin plugin,
            ConfigManager config,
            DamageService damageService,
            PlayerPreferences preferences
    ) {
        this.plugin = plugin;
        this.config = config;
        this.damageService = damageService;
        this.preferences = preferences;
    }

    @Override
    public String version() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public DamageNumberBuilder numberBuilder() {
        return new DamageNumberBuilder();
    }

    @Override
    public boolean spawn(DamageNumberBuilder number) {

        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException(
                    "CustomDamageNumbers API must be used on the main thread (or the owning region thread on Folia)");
        }

        return damageService.spawnCustom(resolve(config, number));
    }

    @Override
    public boolean isViewing(Player player) {
        return preferences.isEnabled(player.getUniqueId());
    }

    @Override
    public void setViewing(Player player, boolean enabled) {
        preferences.setEnabled(player.getUniqueId(), enabled);
    }

    @Override
    public void clearDisplays() {
        damageService.clear();
    }

    @Override
    public int clearDisplays(Player viewer) {
        return damageService.clearFor(viewer.getUniqueId());
    }

    /**
     * Maps a builder onto a resolved {@link CustomSpawn}.
     *
     * @throws IllegalArgumentException when the description cannot render
     */
    public static CustomSpawn resolve(ConfigManager config, DamageNumberBuilder number) {

        if (number == null) {
            throw new IllegalArgumentException("number is required");
        }

        double value = number.value();

        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException("value must be finite and positive, got " + value);
        }

        if (number.victim() == null && number.location() == null) {
            throw new IllegalArgumentException("an anchor is required: at(entity) or at(location)");
        }

        String typeKey = number.type() == null || number.type().isBlank()
                ? "normal"
                : number.type().trim().toLowerCase(Locale.ROOT);

        return new CustomSpawn(
                number.victim(),
                number.location(),
                value,
                typeKey,
                DamageType.byName(typeKey).orElse(null),
                resolveStyle(config, typeKey, number),
                resolveAnimation(config, number),
                number.critical(),
                number.attacker(),
                number.viewers(),
                number.silent());
    }

    /**
     * Resolves the text style: {@code styles.<type>} when the builder set no
     * format, with the pipeline's fallback chain for known damage types; then
     * the builder's explicit look on top.
     */
    public static StyleSettings resolveStyle(
            ConfigManager config, String typeKey, DamageNumberBuilder number) {

        StyleSettings style;
        Optional<DamageType> known = DamageType.byName(typeKey);

        if (known.isPresent()) {
            style = DamageService.resolveStyle(config, known.get(), known.get(), null);
        } else {

            style = config.styles().getOrDefault(typeKey, StyleSettings.fallback(typeKey));

            if (!style.enabled()) {
                style = config.styles().getOrDefault("normal", StyleSettings.fallback("normal"));
            }
        }

        String format = number.format();
        String color = number.color();

        return new StyleSettings(
                style.key(),
                style.enabled(),
                format != null && !format.isBlank() ? format : style.format(),
                color != null && !color.isBlank() ? color : style.color(),
                number.bold() != null ? number.bold() : style.bold(),
                number.italic() != null ? number.italic() : style.italic(),
                number.shadow() != null ? number.shadow() : style.shadow());
    }

    /**
     * Resolves the animation: the configured animation, or the builder's preset
     * when one is named, then the builder's explicit overrides on top.
     */
    public static AnimationSettings resolveAnimation(ConfigManager config, DamageNumberBuilder number) {

        AnimationSettings base = config.animation();

        if (number.preset() != null && !number.preset().isBlank()) {

            // An unknown preset name keeps the configured animation rather than
            // the built-in defaults: the dev asked for a variant of what is live.
            AnimationSettings preset = config.preset(number.preset().trim());

            if (preset != null) {
                base = preset;
            }
        }

        return new AnimationSettings(
                orDefault(number.durationTicks(), base.durationTicks()),
                orDefault(number.riseTicks(), base.riseTicks()),
                orDefault(number.verticalSpeed(), base.verticalSpeed()),
                base.horizontalRandomness(),
                base.scaleAnimation(),
                orDefault(number.startScale(), base.startScale()),
                orDefault(number.endScale(), base.endScale()),
                base.fadeOut(),
                base.fadeStart(),
                orDefault(number.bounce(), base.bounce()),
                base.bounceStrength(),
                orDefault(number.rotation(), base.rotation()),
                base.rotationSpeed(),
                orDefault(number.randomOffset(), base.randomOffset()),
                orDefault(number.spread(), base.spawnSpread()),
                orDefault(number.positionY(), base.anchorHeight()),
                orDefault(number.positionX(), base.positionX()),
                orDefault(number.positionZ(), base.positionZ()),
                base.followEntity(),
                base.followSmoothing(),
                base.criticalScaleMultiplier());
    }

    private static int orDefault(Integer value, int fallback) {
        return value != null ? value : fallback;
    }

    private static double orDefault(Double value, double fallback) {
        return value != null ? value : fallback;
    }

    private static boolean orDefault(Boolean value, boolean fallback) {
        return value != null ? value : fallback;
    }
}

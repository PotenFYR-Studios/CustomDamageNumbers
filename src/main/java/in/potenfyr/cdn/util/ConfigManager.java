package in.potenfyr.cdn.util;

import in.potenfyr.cdn.CustomDamageNumbersPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigManager {

    private final CustomDamageNumbersPlugin plugin;

    public ConfigManager(
            CustomDamageNumbersPlugin plugin
    ) {

        this.plugin = plugin;
    }

    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    public boolean isEnabled() {

        return getConfig().getBoolean(
                "general.enabled",
                true
        );
    }

    public boolean isDebug() {

        return getConfig().getBoolean(
                "general.debug",
                false
        );
    }

    public int getViewDistance() {

        return getConfig().getInt(
                "general.view-distance",
                32
        );
    }

    public List<String> getDisabledWorlds() {

        return getConfig().getStringList(
                "general.disabled-worlds"
        );
    }

    public boolean isMobs() {

        return getConfig().getBoolean("general.mobs", true);
    }

    public boolean isPlayers() {

        return getConfig().getBoolean("general.players", true);
    }

    public boolean isAnimals() {

        return getConfig().getBoolean("general.animals", true);
    }

    public boolean isSelfDamage() {

        return getConfig().getBoolean("general.self-damage", true);
    }

    public boolean isRemoveOnDeath() {

        return getConfig().getBoolean(
                "advanced.remove-on-death",
                true
        );
    }

    public boolean isCleanupOrphans() {

        return getConfig().getBoolean(
                "general.cleanup-orphans",
                true
        );
    }

    public boolean isIgnoreInvisibleEntities() {

        return getConfig().getBoolean(
                "general.ignore-invisible-entities",
                true
        );
    }

    public boolean isIgnoreArmorStands() {

        return getConfig().getBoolean(
                "general.ignore-armor-stands",
                true
        );
    }

    public boolean isIgnoreNpcs() {

        return getConfig().getBoolean(
                "general.ignore-npcs",
                true
        );
    }

    public boolean isRequireViewPermission() {

        return getConfig().getBoolean(
                "permissions.require-view-permission",
                false
        );
    }

    public String getViewPermission() {

        return getConfig().getString(
                "permissions.view-permission",
                "cdn.view"
        );
    }

    public boolean isNearbyViewersOnly() {

        return getConfig().getBoolean(
                "general.nearby-viewers-only",
                true
        );
    }

    public int getMaxActiveDisplays() {

        return getConfig().getInt(
                "general.max-active-displays",
                2000
        );
    }

    public int getDurationTicks() {

        return getConfig().getInt(
                "animation.duration-ticks",
                30
        );
    }

    public double getVerticalSpeed() {

        return getConfig().getDouble(
                "animation.vertical-speed",
                0.03
        );
    }

    public double getHorizontalRandomness() {

        return getConfig().getDouble(
                "animation.horizontal-randomness",
                0.20
        );
    }

    public boolean isFadeOut() {

        return getConfig().getBoolean(
                "animation.fade-out",
                true
        );
    }

    public boolean isScaleAnimation() {

        return getConfig().getBoolean(
                "animation.scale-animation",
                true
        );
    }

    public double getStartScale() {

        return getConfig().getDouble(
                "animation.start-scale",
                1.3
        );
    }

    public double getEndScale() {

        return getConfig().getDouble(
                "animation.end-scale",
                0.8
        );
    }

    public boolean isMergeEnabled() {

        return getConfig().getBoolean(
                "merge-system.enabled",
                true
        );
    }

    public long getMergeWindow() {

        return getConfig().getLong(
                "merge-system.merge-window-ms",
                150
        );
    }

    public boolean areCriticalHitsEnabled() {

        return getConfig().getBoolean(
                "critical-hits.enabled",
                true
        );
    }

    public double getCriticalScaleMultiplier() {

        return getConfig().getDouble(
                "critical-hits.scale-multiplier",
                1.5
        );
    }

    public String getCriticalSymbol() {

        return getConfig().getString(
                "critical-hits.symbol",
                "✧"
        );
    }

    public String getFormat(String type) {

        return getConfig().getString(
                "styles." + type + ".format",
                "{damage}"
        );
    }

    public String getColor(String type) {

        return getConfig().getString(
                "styles." + type + ".color",
                "#FFFFFF"
        );
    }

    public boolean isBold(String type) {

        return getConfig().getBoolean(
                "styles." + type + ".bold",
                true
        );
    }

    public boolean isItalic(String type) {

        return getConfig().getBoolean(
                "styles." + type + ".italic",
                false
        );
    }

    public boolean isShadow(String type) {

        return getConfig().getBoolean(
                "styles." + type + ".shadow",
                true
        );
    }

    public int getAnimationInterval() {

        return getConfig().getInt(
                "performance.animation-interval",
                1
        );
    }

    public int getMaxPerPlayer() {

        return getConfig().getInt(
                "performance.max-per-player",
                50
        );
    }

    public boolean isPacketBatching() {

        return getConfig().getBoolean(
                "performance.packet-batching",
                true
        );
    }

    public boolean isPacketDebug() {

        return getConfig().getBoolean(
                "advanced.packet-debug",
                false
        );
    }

    public int getEntityIdStart() {

        return getConfig().getInt(
                "advanced.entity-id-start",
        500000
        );
    }

    public boolean isDistanceCulling() {

        return getConfig().getBoolean(
                "performance.distance-culling",
                true
        );
    }

    public boolean isIgnoreZeroDamage() {

        return getConfig().getBoolean(
                "advanced.ignore-zero-damage",
        true
        );
    }
}
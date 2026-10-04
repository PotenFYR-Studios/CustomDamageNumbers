package in.potenfyr.cdn.damage;

import in.potenfyr.cdn.CustomDamageNumbersPlugin;
import in.potenfyr.cdn.util.ColorUtil;
import in.potenfyr.cdn.util.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.Locale;

/**
 * Renders the damage value using the per-type style configured under
 * {@code styles.<type>} (format, color, bold, italic). Critical hits use the
 * {@code styles.critical} section, so server owners can fully restyle them.
 */
public class DamageFormatter {

    public static Component format(
            double damage,
            DamageType type,
            boolean critical
    ) {

        ConfigManager config =
                CustomDamageNumbersPlugin.getInstance().getConfigManager();

        // Locale.ROOT keeps the decimal point stable on non-English servers.
        String value = String.format(Locale.ROOT, "%.1f", damage);

        String styleKey = critical
                ? "critical"
                : type.name().toLowerCase(Locale.ROOT);

        String text = config.getFormat(styleKey)
                .replace("{damage}", value)
                .replace("{symbol}", config.getCriticalSymbol());

        return Component.text(text)
                .color(ColorUtil.fromHex(config.getColor(styleKey)))
                .decoration(TextDecoration.BOLD,
                        config.isBold(styleKey)
                                ? TextDecoration.State.TRUE
                                : TextDecoration.State.FALSE)
                .decoration(TextDecoration.ITALIC,
                        config.isItalic(styleKey)
                                ? TextDecoration.State.TRUE
                                : TextDecoration.State.FALSE);
    }
}
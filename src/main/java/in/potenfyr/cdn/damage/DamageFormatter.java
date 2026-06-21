package in.potenfyr.cdn.damage;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class DamageFormatter {

    public static Component format(
            double damage,
            DamageType type,
            boolean critical
    ) {

        String value =
                String.format("%.1f", damage);

        if (critical) {

            return Component.text("✦ " + value)
                    .color(NamedTextColor.RED)
                    .decorate(TextDecoration.BOLD);
        }

        return switch (type) {

            case FIRE -> Component.text(value)
                    .color(NamedTextColor.GOLD)
                    .decorate(TextDecoration.BOLD);

            case MAGIC -> Component.text(value)
                    .color(NamedTextColor.LIGHT_PURPLE)
                    .decorate(TextDecoration.BOLD);

            case POISON -> Component.text(value)
                    .color(NamedTextColor.GREEN)
                    .decorate(TextDecoration.BOLD);

            case EXPLOSION -> Component.text(value)
                    .color(NamedTextColor.YELLOW)
                    .decorate(TextDecoration.BOLD);

            default -> Component.text(value)
                    .color(NamedTextColor.WHITE)
                    .decorate(TextDecoration.BOLD);
        };
    }
}
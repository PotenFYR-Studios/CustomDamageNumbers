package in.potenfyr.cdn.util;

import net.kyori.adventure.text.format.TextColor;

public class ColorUtil {

    public static TextColor fromHex(
            String hex
    ) {

        TextColor color =
                TextColor.fromHexString(hex);

        if (color == null) {
            return TextColor.color(255, 255, 255);
        }

        return color;
    }
}
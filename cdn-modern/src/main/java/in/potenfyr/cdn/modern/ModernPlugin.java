package in.potenfyr.cdn.modern;

import in.potenfyr.cdn.CustomDamageNumbersPlugin;
import in.potenfyr.cdn.packet.RenderBackend;
import in.potenfyr.cdn.platform.MinecraftVersion;

/**
 * The modern platform entry point (1.20.2 - 26.x).
 *
 * <p>Refuses to run on a server without the TextDisplay entity and names the jar the
 * operator should install instead, rather than failing silently at render time.</p>
 */
public final class ModernPlugin extends CustomDamageNumbersPlugin {

    @Override
    protected RenderBackend createBackend() {
        return new TextDisplayBackend(getConfigManager());
    }

    @Override
    protected boolean isServerSupported(MinecraftVersion version) {
        return version.supportsTextDisplay();
    }

    @Override
    protected String unsupportedServerHint(MinecraftVersion version) {
        return "Minecraft " + version.raw() + " predates the TextDisplay entity (1.19.4) and uses "
                + "the older display metadata layout. Install CustomDamageNumbers-Legacy-"
                + getDescription().getVersion() + ".jar instead; it serves 1.17.x - 1.20.1.";
    }
}

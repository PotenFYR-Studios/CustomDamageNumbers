package in.potenfyr.cdn.legacy;

import in.potenfyr.cdn.CustomDamageNumbersPlugin;
import in.potenfyr.cdn.packet.RenderBackend;
import in.potenfyr.cdn.platform.MinecraftVersion;

/**
 * The legacy platform entry point (1.16.x - 1.20.1).
 *
 * <p>Also runs on newer servers — armour stands exist everywhere — but the modern jar
 * is the better choice there because only it can animate fade and scale, so the
 * startup log says so.</p>
 */
public final class LegacyPlugin extends CustomDamageNumbersPlugin {

    @Override
    protected RenderBackend createBackend() {
        return new ArmorStandBackend();
    }

    @Override
    protected boolean isServerSupported(MinecraftVersion version) {
        return version.supportsLegacyRendering();
    }

    @Override
    protected String unsupportedServerHint(MinecraftVersion version) {
        return "Minecraft " + version.raw() + " is older than 1.16, which this plugin does not support."
                + " The shipped build is Java 17 bytecode, so a 1.16.5 server needs a rebuild with"
                + " javaRelease=16 in gradle.properties and a Java 16 server.";
    }

    @Override
    protected void onEnabled() {

        if (isEnabled() && getServerVersion().supportsTextDisplay()) {

            getLogger().info("This server can use the modern jar (CustomDamageNumbers-"
                    + getDescription().getVersion() + ".jar) for fade and scale animation.");
        }
    }
}

package in.potenfyr.cdn.platform;

import java.util.Locale;
import java.util.function.Predicate;

/**
 * The server software we are running on. Detected once at startup so the rest of
 * the plugin can branch on capabilities instead of string-sniffing.
 */
public enum Ecosystem {

    FOLIA("Folia"),
    PAPER("Paper"),
    PURPUR("Purpur"),
    PUFFERFISH("Pufferfish"),
    SPIGOT("Spigot"),
    UNKNOWN("Unknown");

    private final String displayName;

    Ecosystem(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /** Folia needs region-aware scheduling; everything else uses the Bukkit scheduler. */
    public boolean isRegionThreaded() {
        return this == FOLIA;
    }

    /** Paper and its forks provide Adventure, hex colours and modern events natively. */
    public boolean isPaperDerivative() {
        return this == PAPER || this == PURPUR || this == PUFFERFISH || this == FOLIA;
    }

    /**
     * Classifies the server from its name and version banner.
     *
     * <p>Both inputs matter: newer Paper builds report a version string without
     * "paper" in it, while {@code Bukkit.getName()} answers "Paper"/"Purpur"/"Folia"
     * reliably. Either signal alone mis-detects some modern server.</p>
     *
     * @param serverName     {@code Bukkit.getName()}
     * @param serverVersion  {@code Bukkit.getVersion()}, e.g. {@code "git-Paper-196 (MC: 1.21.4)"}
     * @param classIsPresent lets callers inject a classloader probe for Folia, which
     *                       is only distinguishable from Paper by its classes
     */
    public static Ecosystem detect(String serverName, String serverVersion, Predicate<String> classIsPresent) {

        String name = serverName == null ? "" : serverName.toLowerCase(Locale.ROOT);
        String version = serverVersion == null ? "" : serverVersion.toLowerCase(Locale.ROOT);

        // Folia reports itself as "Paper" in some banners, so its classes decide first.
        if (name.contains("folia") || version.contains("folia")
                || (classIsPresent != null && classIsPresent.test(FOLIA_MARKER))) {
            return FOLIA;
        }

        if (name.contains("purpur") || version.contains("purpur")) {
            return PURPUR;
        }

        if (name.contains("pufferfish") || version.contains("pufferfish")) {
            return PUFFERFISH;
        }

        if (name.contains("paper") || version.contains("paper")) {
            return PAPER;
        }

        if (name.contains("spigot") || version.contains("spigot")
                || name.contains("craftbukkit") || version.contains("craftbukkit")) {
            return SPIGOT;
        }

        return UNKNOWN;
    }

    private static final String FOLIA_MARKER = "io.papermc.paper.threadedregions.RegionizedServer";
}

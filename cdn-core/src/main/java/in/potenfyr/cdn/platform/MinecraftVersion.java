package in.potenfyr.cdn.platform;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Minecraft release version, parsed tolerantly from strings such as
 * {@code "1.16.5-R0.1-SNAPSHOT"}, {@code "26.3"} or {@code "git-Paper-123 (MC: 1.21.4)"}.
 *
 * <p>Pure value type: no Bukkit dependency, so the version gates that decide which
 * renderer runs are unit-testable without a server.</p>
 */
public final class MinecraftVersion implements Comparable<MinecraftVersion> {

    /** First token that looks like a dotted release version, e.g. 1.16.5 or 26.3. */
    private static final Pattern RELEASE = Pattern.compile("(\\d{1,3})\\.(\\d{1,3})(?:\\.(\\d{1,3}))?");

    public static final MinecraftVersion UNKNOWN = new MinecraftVersion(0, 0, 0, "unknown");

    private final int major;
    private final int minor;
    private final int patch;
    private final String raw;

    private MinecraftVersion(int major, int minor, int patch, String raw) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.raw = raw;
    }

    /**
     * Parses the first release-looking token in {@code raw}.
     *
     * @return the parsed version, or {@link #UNKNOWN} when nothing parseable is present
     */
    public static MinecraftVersion parse(String raw) {
        return tryParse(raw).orElse(UNKNOWN);
    }

    public static Optional<MinecraftVersion> tryParse(String raw) {

        if (raw == null) {
            return Optional.empty();
        }

        Matcher matcher = RELEASE.matcher(raw);

        if (!matcher.find()) {
            return Optional.empty();
        }

        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));

        return Optional.of(new MinecraftVersion(major, minor, patch, matcher.group()));
    }

    public int major() {
        return major;
    }

    public int minor() {
        return minor;
    }

    public int patch() {
        return patch;
    }

    public String raw() {
        return raw;
    }

    public boolean isKnown() {
        return major > 0;
    }

    /** True when this version is at least {@code major.minor} (patch ignored). */
    public boolean isAtLeast(int major, int minor) {
        return compareTo(new MinecraftVersion(major, minor, 0, major + "." + minor)) >= 0;
    }

    /** True when this version is at least {@code major.minor.patch}. */
    public boolean isAtLeast(int major, int minor, int patch) {
        return compareTo(new MinecraftVersion(major, minor, patch, major + "." + minor + "." + patch)) >= 0;
    }

    public boolean isBelow(int major, int minor) {
        return !isAtLeast(major, minor);
    }

    /**
     * The modern renderer needs the TextDisplay entity, whose metadata layout is
     * only validated from 1.20.2 onwards (see spec section 4).
     */
    public boolean supportsTextDisplay() {
        return isAtLeast(1, 20, 2);
    }

    /**
     * Legacy rendering covers everything from 1.16 up to the point the modern
     * renderer takes over.
     */
    public boolean supportsLegacyRendering() {
        return major == 1 && (minor >= 16);
    }

    @Override
    public int compareTo(MinecraftVersion other) {

        int result = Integer.compare(major, other.major);

        if (result == 0) {
            result = Integer.compare(minor, other.minor);
        }

        if (result == 0) {
            result = Integer.compare(patch, other.patch);
        }

        return result;
    }

    @Override
    public boolean equals(Object other) {

        if (this == other) {
            return true;
        }

        if (!(other instanceof MinecraftVersion version)) {
            return false;
        }

        return major == version.major && minor == version.minor && patch == version.patch;
    }

    @Override
    public int hashCode() {
        return (major * 31 + minor) * 31 + patch;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}

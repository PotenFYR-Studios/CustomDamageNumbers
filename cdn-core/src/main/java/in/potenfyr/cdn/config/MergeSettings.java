package in.potenfyr.cdn.config;

/**
 * Hit merging ({@code merge-system.*}): rapid hits on the same victim collapse into
 * one growing number instead of spawning a stack of displays.
 *
 * @param enabled           whether merging is active
 * @param windowMs          how long after a hit another hit may merge into it
 * @param maxMergedDamage   cap for the accumulated value
 * @param sameAttackerOnly  only merge hits from the same attacker
 */
public record MergeSettings(
        boolean enabled,
        long windowMs,
        double maxMergedDamage,
        boolean sameAttackerOnly
) {

    public MergeSettings {
        windowMs = Math.max(0L, Math.min(5_000L, windowMs));
        maxMergedDamage = Math.max(1.0, maxMergedDamage);
    }

    public static MergeSettings disabled() {
        return new MergeSettings(false, 0L, 9999.0, true);
    }
}

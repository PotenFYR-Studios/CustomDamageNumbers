package in.potenfyr.cdn.config;

/**
 * Immutable, validated animation settings built from {@code animation.*} in
 * config.yml.
 *
 * <p>The compact constructor clamps every value into a range the animation maths
 * can safely use, so a hostile or broken config can never produce NaN offsets or
 * a display stuck at zero scale.</p>
 *
 * @param durationTicks         total lifetime of a display, in ticks
 * @param riseTicks             ticks spent rising before the display starts to sink
 * @param verticalSpeed         blocks per tick of the rising phase; sets the peak height
 * @param horizontalRandomness  peak horizontal drift in blocks
 * @param scaleAnimation        whether the display scales over its lifetime
 * @param startScale            scale at spawn
 * @param endScale              scale at expiry
 * @param fadeOut               whether the display fades near the end of its life
 * @param fadeStart             progress point (0-1) where fading begins
 * @param bounce                whether a damped bounce is layered onto the rise
 * @param bounceStrength        bounce amplitude as a fraction of the peak height
 * @param rotation              whether the display orbits its anchor
 * @param rotationSpeed         orbit radians added per tick
 * @param randomOffset          whether each display gets a random spawn offset
 * @param spawnSpread           half-range of the random spawn offset, in blocks
 * @param anchorHeight          blocks above the entity's feet the number anchors to
 * @param followEntity          whether the anchor tracks the live entity
 * @param followSmoothing       anchor smoothing factor in {@code [0,1)}, 0 = snap
 * @param criticalScaleMultiplier extra scale applied to critical hits
 */
public record AnimationSettings(
        int durationTicks,
        int riseTicks,
        double verticalSpeed,
        double horizontalRandomness,
        boolean scaleAnimation,
        double startScale,
        double endScale,
        boolean fadeOut,
        double fadeStart,
        boolean bounce,
        double bounceStrength,
        boolean rotation,
        double rotationSpeed,
        boolean randomOffset,
        double spawnSpread,
        double anchorHeight,
        boolean followEntity,
        double followSmoothing,
        double criticalScaleMultiplier
) {

    public static final int MAX_DURATION_TICKS = 20 * 60;

    public AnimationSettings {

        durationTicks = clampInt(durationTicks, 1, MAX_DURATION_TICKS);
        riseTicks = clampInt(riseTicks, 1, durationTicks);
        verticalSpeed = clampDouble(verticalSpeed, 0.0, 1.0);
        horizontalRandomness = clampDouble(horizontalRandomness, 0.0, 8.0);
        startScale = clampDouble(startScale, 0.05, 16.0);
        endScale = clampDouble(endScale, 0.05, 16.0);
        fadeStart = clampDouble(fadeStart, 0.0, 0.99);
        bounceStrength = clampDouble(bounceStrength, 0.0, 4.0);
        rotationSpeed = clampDouble(rotationSpeed, -1.0, 1.0);
        spawnSpread = clampDouble(spawnSpread, 0.0, 8.0);
        anchorHeight = clampDouble(anchorHeight, -4.0, 8.0);
        followSmoothing = clampDouble(followSmoothing, 0.0, 0.95);
        criticalScaleMultiplier = clampDouble(criticalScaleMultiplier, 0.1, 8.0);
    }

    /** Peak height in blocks reached at {@code riseTicks}. */
    public double peakHeight() {
        return verticalSpeed * riseTicks;
    }

    /** Settings scaled up for a critical hit, keeping the peak height unchanged. */
    public AnimationSettings withCriticalScale(double multiplier) {
        return new AnimationSettings(
                durationTicks, riseTicks, verticalSpeed, horizontalRandomness,
                scaleAnimation, startScale * multiplier, endScale * multiplier,
                fadeOut, fadeStart, bounce, bounceStrength, rotation, rotationSpeed,
                randomOffset, spawnSpread, anchorHeight, followEntity, followSmoothing,
                criticalScaleMultiplier);
    }

    /**
     * Sensible defaults, matching the shipped config.yml; used by unit tests and
     * as the fallback when a config file is missing keys.
     */
    public static AnimationSettings defaults() {
        return new AnimationSettings(
                30, 10, 0.08, 0.20,
                true, 1.3, 0.8,
                true, 0.6,
                true, 0.35,
                false, 0.10,
                true, 0.20,
                1.8, true, 0.35,
                1.5);
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {

        if (Double.isNaN(value)) {
            return min;
        }

        return Math.max(min, Math.min(max, value));
    }
}

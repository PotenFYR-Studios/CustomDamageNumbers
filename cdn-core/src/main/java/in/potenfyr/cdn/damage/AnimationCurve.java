package in.potenfyr.cdn.damage;

import in.potenfyr.cdn.config.AnimationSettings;

/**
 * The animation maths, kept deliberately free of Bukkit types so it can be unit
 * tested exactly and reasoned about at a glance.
 *
 * <p>Every frame is an <em>absolute offset from the anchor</em>, not a delta: the
 * anchor follows the damaged entity, so recomputing the frame each tick keeps the
 * number next to the entity no matter how it moves.</p>
 */
public final class AnimationCurve {

    private AnimationCurve() {
    }

    /**
     * Computes the frame for one tick of a display's life.
     *
     * @param tick   ticks since spawn, {@code >= 0}
     * @param settings validated animation settings
     * @param angle  the display's own horizontal angle in degrees; callers derive
     *               it from the entity id so simultaneous numbers fan out
     */
    public static AnimationFrame frame(int tick, AnimationSettings settings, double angle) {

        int duration = settings.durationTicks();
        int riseTicks = settings.riseTicks();

        int clampedTick = Math.max(0, Math.min(tick, duration));

        double progress = (double) clampedTick / duration;

        // ---- vertical: rise to the peak, then sink back to the anchor ----
        double peak = settings.peakHeight();
        double y;

        if (clampedTick < riseTicks) {
            y = peak * easeOutCubic((double) clampedTick / riseTicks);
        } else {
            double descend = (double) (clampedTick - riseTicks) / Math.max(1, duration - riseTicks);
            y = peak * (1.0 - easeInCubic(clamp(descend, 0.0, 1.0)));
        }

        if (settings.bounce()) {
            // Damped sine layered on the rise; decays to nothing by the end.
            double decay = 1.0 - progress;
            y += settings.bounceStrength() * peak * Math.sin(Math.PI * 2.0 * progress) * decay * decay;
        }

        // ---- horizontal: drift outward and slow down, optionally orbiting ----
        double spread = settings.horizontalRandomness() * (1.0 - progress);
        double theta = Math.toRadians(angle)
                + (settings.rotation() ? settings.rotationSpeed() * clampedTick : 0.0);
        double x = Math.cos(theta) * spread;
        double z = Math.sin(theta) * spread;

        // ---- scale ----
        float scale = settings.scaleAnimation()
                ? (float) lerp(settings.startScale(), settings.endScale(), easeInOut(progress))
                : 1.0f;

        if (!Float.isFinite(scale) || scale <= 0.0f) {
            scale = 0.05f;
        }

        // ---- opacity ----
        float opacity = 1.0f;

        if (settings.fadeOut() && progress > settings.fadeStart()) {
            double faded = (progress - settings.fadeStart()) / (1.0 - settings.fadeStart());
            opacity = (float) clamp(1.0 - faded, 0.0, 1.0);
        }

        return new AnimationFrame(x, y, z, scale, opacity);
    }

    /** A stable per-display angle so numbers for different entities fan out evenly. */
    public static double angleFor(int entityId) {
        return (entityId * 137.508) % 360.0;
    }

    public static double lerp(double from, double to, double t) {
        return from + (to - from) * t;
    }

    public static double easeOutCubic(double t) {
        double clamped = clamp(t, 0.0, 1.0);
        double inverse = 1.0 - clamped;
        return 1.0 - inverse * inverse * inverse;
    }

    public static double easeInCubic(double t) {
        double clamped = clamp(t, 0.0, 1.0);
        return clamped * clamped * clamped;
    }

    public static double easeInOut(double t) {
        double clamped = clamp(t, 0.0, 1.0);
        return clamped < 0.5
                ? 4.0 * clamped * clamped * clamped
                : 1.0 - Math.pow(-2.0 * clamped + 2.0, 3.0) / 2.0;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

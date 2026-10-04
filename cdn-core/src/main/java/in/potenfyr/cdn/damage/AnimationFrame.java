package in.potenfyr.cdn.damage;

/**
 * One frame of a damage-number animation: an offset from the anchor position
 * (which follows the damaged entity) plus the display scale and opacity.
 *
 * @param offsetX horizontal X offset in blocks, relative to the anchor
 * @param offsetY vertical offset in blocks, relative to the anchor
 * @param offsetZ horizontal Z offset in blocks, relative to the anchor
 * @param scale   display scale multiplier, always &gt; 0
 * @param opacity display opacity in {@code [0,1]}, where 1 is fully opaque
 */
public record AnimationFrame(double offsetX, double offsetY, double offsetZ, float scale, float opacity) {

    public static final AnimationFrame IDENTITY = new AnimationFrame(0.0, 0.0, 0.0, 1.0f, 1.0f);

    /** A copy of this frame with the scale multiplied (critical hits). */
    public AnimationFrame scaled(float multiplier) {
        return new AnimationFrame(offsetX, offsetY, offsetZ, scale * multiplier, opacity);
    }

    /** Opacity as the signed byte a TextDisplay metadata packet expects. */
    public byte opacityByte() {
        int value = Math.round(opacity * 255.0f);
        return (byte) Math.max(0, Math.min(255, value));
    }
}

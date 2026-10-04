package in.potenfyr.cdn.config;

/**
 * Critical-hit behaviour ({@code critical-hits.*}).
 *
 * @param enabled         whether critical hits get their own treatment
 * @param scaleMultiplier extra display scale for a critical hit
 * @param sound           whether a critical hit plays a sound
 * @param soundType       the sound key, e.g. {@code "ENTITY_PLAYER_ATTACK_CRIT"}
 * @param volume          sound volume
 * @param pitch           sound pitch
 */
public record CriticalSettings(
        boolean enabled,
        double scaleMultiplier,
        boolean sound,
        String soundType,
        float volume,
        float pitch
) {

    public CriticalSettings {
        scaleMultiplier = Math.max(0.1, Math.min(8.0, scaleMultiplier));
        volume = (float) Math.max(0.0, Math.min(4.0, volume));
        pitch = (float) Math.max(0.1, Math.min(4.0, pitch));
        soundType = soundType == null || soundType.isBlank()
                ? "ENTITY_PLAYER_ATTACK_CRIT"
                : soundType.trim();
    }
}

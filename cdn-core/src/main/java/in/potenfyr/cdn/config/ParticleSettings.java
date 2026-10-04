package in.potenfyr.cdn.config;

/**
 * Particle burst settings for one damage type ({@code particles.<type>}).
 *
 * @param enabled  whether the burst is sent
 * @param particle the PacketEvents particle name, e.g. {@code "CRIT"}
 * @param amount   particle count, clamped to a sane maximum by {@link ConfigManager}
 */
public record ParticleSettings(boolean enabled, String particle, int amount) {

    public static final int MAX_AMOUNT = 200;

    public ParticleSettings {
        amount = Math.max(0, Math.min(MAX_AMOUNT, amount));
        particle = particle == null || particle.isBlank() ? "CRIT" : particle.trim();
    }

    public static ParticleSettings disabled() {
        return new ParticleSettings(false, "CRIT", 0);
    }
}

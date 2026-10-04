package in.potenfyr.cdn.damage;

/**
 * Damage categories the plugin can render differently. {@code HEALING} exists so
 * the {@code styles.healing} config section is reachable instead of being dead
 * code (see spec section 10).
 */
public enum DamageType {

    NORMAL,
    CRITICAL,
    FIRE,
    MAGIC,
    POISON,
    EXPLOSION,
    FALL,
    HEALING;

    /** The {@code styles.<key>} section used to format this type. */
    public String styleKey() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    /** Tolerant lookup used by {@code /cdn test <type>}; empty when unknown. */
    public static java.util.Optional<DamageType> byName(String raw) {

        if (raw == null || raw.isBlank()) {
            return java.util.Optional.empty();
        }

        String name = raw.trim().toUpperCase(java.util.Locale.ROOT);

        for (DamageType type : values()) {
            if (type.name().equals(name)) {
                return java.util.Optional.of(type);
            }
        }

        return java.util.Optional.empty();
    }

    /** Type names for tab completion. */
    public static java.util.List<String> names() {

        java.util.List<String> names = new java.util.ArrayList<>(values().length);

        for (DamageType type : values()) {
            names.add(type.name().toLowerCase(java.util.Locale.ROOT));
        }

        return names;
    }
}

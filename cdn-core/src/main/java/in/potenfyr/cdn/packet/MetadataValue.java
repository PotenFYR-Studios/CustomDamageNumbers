package in.potenfyr.cdn.packet;

/**
 * One entity-metadata entry, described without any PacketEvents type.
 *
 * <p>Why this exists: PacketEvents' {@code EntityDataTypes} constants are not plain
 * fields — reading one runs a static initialiser that loads versioned registries and
 * therefore requires a live {@code PacketEventsAPI}. Building payloads out of this
 * JDK-only record means the exact index/kind/value triple each renderer sends can be
 * unit-tested, and {@link PacketUtil} is the single place that touches PacketEvents
 * when the packet is actually sent.</p>
 *
 * @param index the metadata index (the wire contract the client reads)
 * @param kind  how the value is encoded on the wire
 * @param value the value itself; a {@code float[]} for {@link Kind#VECTOR3F}, a JSON
 *              string for the component kinds, {@code null} for an empty optional
 */
public record MetadataValue(int index, Kind kind, Object value) {

    public enum Kind {

        /** Single byte, e.g. entity flags. */
        BYTE,

        /** Signed 32-bit integer. */
        INT,

        /** 32-bit float. */
        FLOAT,

        /** Boolean. */
        BOOLEAN,

        /** A JSON text component. */
        COMPONENT_JSON,

        /** An optional JSON text component; a {@code null} value means empty. */
        OPTIONAL_COMPONENT_JSON,

        /** Three floats, stored as a {@code float[3]}. */
        VECTOR3F
    }

    public static MetadataValue ofByte(int index, byte value) {
        return new MetadataValue(index, Kind.BYTE, value);
    }

    public static MetadataValue ofInt(int index, int value) {
        return new MetadataValue(index, Kind.INT, value);
    }

    public static MetadataValue ofFloat(int index, float value) {
        return new MetadataValue(index, Kind.FLOAT, value);
    }

    public static MetadataValue ofBoolean(int index, boolean value) {
        return new MetadataValue(index, Kind.BOOLEAN, value);
    }

    /** A JSON text component. */
    public static MetadataValue component(int index, String json) {
        return new MetadataValue(index, Kind.COMPONENT_JSON, json);
    }

    /** An optional JSON text component; {@code null} encodes as absent. */
    public static MetadataValue optionalComponent(int index, String json) {
        return new MetadataValue(index, Kind.OPTIONAL_COMPONENT_JSON, json);
    }

    /** A uniform or per-axis vector; stored as a defensive three-element array. */
    public static MetadataValue vector3f(int index, float x, float y, float z) {
        return new MetadataValue(index, Kind.VECTOR3F, new float[] {x, y, z});
    }

    /** A uniform vector, e.g. display scale. */
    public static MetadataValue uniformScale(int index, float scale) {
        return vector3f(index, scale, scale, scale);
    }

    /** The stored vector components; only valid for {@link Kind#VECTOR3F}. */
    public float[] vector() {

        if (value instanceof float[] components && components.length == 3) {
            return components.clone();
        }

        return new float[] {0.0f, 0.0f, 0.0f};
    }
}

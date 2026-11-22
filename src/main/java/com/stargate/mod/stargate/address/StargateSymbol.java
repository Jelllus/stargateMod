package com.stargate.mod.stargate.address;

/**
 * One glyph on the gate ring.
 *
 * <p>A gate carries {@link #COUNT} symbols, numbered {@code 0..COUNT-1}. Symbol {@code 0} is the
 * <em>point of origin</em>: it is never part of an address, it is the glyph dialed last to tell the
 * gate "start from here" and lock the connection.
 *
 * <p>Instances are interned, so {@code ==} works, but prefer {@link #equals(Object)}.
 */
public final class StargateSymbol {
    /** Number of glyphs on the ring, point of origin included. */
    public static final int COUNT = 39;

    private static final StargateSymbol[] VALUES = new StargateSymbol[COUNT];

    static {
        for (int i = 0; i < COUNT; i++) {
            VALUES[i] = new StargateSymbol(i);
        }
    }

    /** The glyph that terminates a dial sequence. */
    public static final StargateSymbol POINT_OF_ORIGIN = VALUES[0];

    private final int id;

    private StargateSymbol(int id) {
        this.id = id;
    }

    /**
     * @throws IllegalArgumentException if {@code id} is outside {@code 0..COUNT-1}
     */
    public static StargateSymbol of(int id) {
        if (!isValidId(id)) {
            throw new IllegalArgumentException("Symbol id out of range [0, " + (COUNT - 1) + "]: " + id);
        }
        return VALUES[id];
    }

    public static boolean isValidId(int id) {
        return id >= 0 && id < COUNT;
    }

    public int id() {
        return id;
    }

    public boolean isPointOfOrigin() {
        return id == 0;
    }

    /** Translation key for the glyph's display name, e.g. {@code stargate.symbol.12}. */
    public String translationKey() {
        return "stargate.symbol." + id;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof StargateSymbol other && other.id == id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return Integer.toString(id);
    }
}

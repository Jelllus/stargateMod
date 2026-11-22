package com.stargate.mod.stargate.address;

/**
 * One glyph on the gate ring.
 *
 * <p>A gate carries {@link #COUNT} symbols, numbered {@code 0..COUNT-1}. Symbol {@code 0} is the
 * <em>point of origin</em>: it is never part of an address, it is the glyph dialed last to tell the
 * gate "start from here" and lock the connection.
 *
 * <p>Instances are interned, so {@code ==} works, but prefer {@link #equals(Object)}.
 *
 * <p><i>FR —</i> Un glyphe de l'anneau de la porte.
 *
 * <p>Une porte porte {@link #COUNT} symboles, numérotés de {@code 0} à {@code COUNT-1}. Le symbole
 * {@code 0} est le <em>point d'origine</em> : il ne fait jamais partie d'une adresse, c'est le glyphe
 * composé en dernier pour dire à la porte « on part d'ici » et verrouiller la connexion.
 *
 * <p>Les instances sont uniques par identifiant, donc {@code ==} fonctionne, mais préférez
 * {@link #equals(Object)}.
 */
public final class StargateSymbol {
    /**
     * Number of glyphs on the ring, point of origin included.
     *
     * <p><i>FR —</i> Nombre de glyphes sur l'anneau, point d'origine compris.
     */
    public static final int COUNT = 39;

    private static final StargateSymbol[] VALUES = new StargateSymbol[COUNT];

    static {
        for (int i = 0; i < COUNT; i++) {
            VALUES[i] = new StargateSymbol(i);
        }
    }

    /**
     * The glyph that terminates a dial sequence.
     *
     * <p><i>FR —</i> Le glyphe qui termine une séquence de numérotation.
     */
    public static final StargateSymbol POINT_OF_ORIGIN = VALUES[0];

    private final int id;

    private StargateSymbol(int id) {
        this.id = id;
    }

    /**
     * Returns the symbol with the given id.
     *
     * <p><i>FR —</i> Renvoie le symbole portant cet identifiant.
     *
     * @param id symbol id, {@code 0..COUNT-1} / identifiant du symbole, de {@code 0} à {@code COUNT-1}
     * @return the interned symbol / le symbole unique correspondant
     * @throws IllegalArgumentException if {@code id} is out of range / si {@code id} est hors limites
     */
    public static StargateSymbol of(int id) {
        if (!isValidId(id)) {
            throw new IllegalArgumentException("Symbol id out of range [0, " + (COUNT - 1) + "]: " + id);
        }
        return VALUES[id];
    }

    /**
     * Whether {@code id} names an existing symbol.
     *
     * <p><i>FR —</i> Indique si {@code id} désigne un symbole existant.
     *
     * @param id candidate id / identifiant à tester
     * @return {@code true} if {@code 0 <= id < COUNT} / {@code true} si {@code 0 <= id < COUNT}
     */
    public static boolean isValidId(int id) {
        return id >= 0 && id < COUNT;
    }

    /**
     * Numeric id of this symbol.
     *
     * <p><i>FR —</i> Identifiant numérique de ce symbole.
     *
     * @return the id / l'identifiant
     */
    public int id() {
        return id;
    }

    /**
     * Whether this is the point of origin (id {@code 0}).
     *
     * <p><i>FR —</i> Indique s'il s'agit du point d'origine (identifiant {@code 0}).
     *
     * @return {@code true} for the point of origin / {@code true} pour le point d'origine
     */
    public boolean isPointOfOrigin() {
        return id == 0;
    }

    /**
     * Translation key for the glyph's display name, e.g. {@code stargate.symbol.12}.
     *
     * <p><i>FR —</i> Clé de traduction du nom affiché du glyphe, par exemple {@code stargate.symbol.12}.
     *
     * @return the translation key / la clé de traduction
     */
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

    /**
     * The id as text, e.g. {@code "12"}.
     *
     * <p><i>FR —</i> L'identifiant sous forme de texte, par exemple {@code "12"}.
     */
    @Override
    public String toString() {
        return Integer.toString(id);
    }
}

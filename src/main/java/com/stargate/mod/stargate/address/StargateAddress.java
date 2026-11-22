package com.stargate.mod.stargate.address;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * An immutable, validated gate address: the destination glyphs, <em>without</em> the point of origin.
 *
 * <p>Dialing an address encodes its symbols in order, then the point of origin. The number of chevrons
 * engaged is therefore {@code length() + 1}:
 * <ul>
 *     <li>{@link Type#STANDARD} — 6 symbols, 7 chevrons: the address every gate is given.</li>
 *     <li>{@link Type#EXTENDED} — 7 symbols, 8 chevrons: reserved for long-range links.</li>
 *     <li>{@link Type#SPECIAL} — 8 symbols, 9 chevrons: reserved for unique destinations.</li>
 * </ul>
 * Only {@code STANDARD} addresses are assigned today; the other lengths are accepted by the validator so
 * dialing logic doesn't need to change when they get a meaning.
 *
 * <p>Text form is the symbol ids joined by {@code -}, e.g. {@code 27-7-15-32-12-30}.
 *
 * <p><i>FR —</i> Une adresse de porte immuable et validée : les glyphes de destination, <em>sans</em> le
 * point d'origine.
 *
 * <p>Composer une adresse encode ses symboles dans l'ordre, puis le point d'origine. Le nombre de chevrons
 * engagés est donc {@code length() + 1} :
 * <ul>
 *     <li>{@link Type#STANDARD} — 6 symboles, 7 chevrons : l'adresse attribuée à chaque porte.</li>
 *     <li>{@link Type#EXTENDED} — 7 symboles, 8 chevrons : réservée aux liaisons longue distance.</li>
 *     <li>{@link Type#SPECIAL} — 8 symboles, 9 chevrons : réservée aux destinations uniques.</li>
 * </ul>
 * Seules les adresses {@code STANDARD} sont attribuées pour l'instant ; les autres longueurs sont acceptées
 * par le validateur pour que la logique de numérotation n'ait pas à changer quand elles auront un sens.
 *
 * <p>La forme texte est la suite des identifiants séparés par {@code -}, par exemple
 * {@code 27-7-15-32-12-30}.
 */
public final class StargateAddress {
    /**
     * Address kind, by length.
     *
     * <p><i>FR —</i> Type d'adresse, selon sa longueur.
     */
    public enum Type {
        /**
         * 6 symbols, 7 chevrons. / <i>FR —</i> 6 symboles, 7 chevrons.
         */
        STANDARD(6),
        /**
         * 7 symbols, 8 chevrons. / <i>FR —</i> 7 symboles, 8 chevrons.
         */
        EXTENDED(7),
        /**
         * 8 symbols, 9 chevrons. / <i>FR —</i> 8 symboles, 9 chevrons.
         */
        SPECIAL(8);

        private final int length;

        Type(int length) {
            this.length = length;
        }

        /**
         * Number of symbols, point of origin excluded.
         *
         * <p><i>FR —</i> Nombre de symboles, point d'origine exclu.
         *
         * @return the length / la longueur
         */
        public int length() {
            return length;
        }

        /**
         * Number of chevrons engaged when dialing, point of origin included.
         *
         * <p><i>FR —</i> Nombre de chevrons engagés à la numérotation, point d'origine compris.
         *
         * @return {@code length() + 1}
         */
        public int chevrons() {
            return length + 1;
        }

        /**
         * The type matching an address length.
         *
         * <p><i>FR —</i> Le type correspondant à une longueur d'adresse.
         *
         * @param length number of symbols / nombre de symboles
         * @return the type, or empty if no type has that length / le type, ou vide si aucun ne correspond
         */
        public static Optional<Type> forLength(int length) {
            for (Type t : values()) {
                if (t.length == length) {
                    return Optional.of(t);
                }
            }
            return Optional.empty();
        }
    }

    /**
     * Shortest valid address length. / <i>FR —</i> Longueur minimale d'une adresse valide.
     */
    public static final int MIN_LENGTH = Type.STANDARD.length();
    /**
     * Longest valid address length. / <i>FR —</i> Longueur maximale d'une adresse valide.
     */
    public static final int MAX_LENGTH = Type.SPECIAL.length();

    private final List<StargateSymbol> symbols;

    private StargateAddress(List<StargateSymbol> symbols) {
        this.symbols = List.copyOf(symbols);
    }

    /**
     * Builds an address from symbols.
     *
     * <p><i>FR —</i> Construit une adresse à partir de symboles.
     *
     * @param symbols the destination glyphs, without point of origin / les glyphes de destination, sans
     *                point d'origine
     * @return the address / l'adresse
     * @throws IllegalArgumentException if the symbols don't form a valid address (see
     *                                  {@link AddressValidator}) / si les symboles ne forment pas une adresse
     *                                  valide
     */
    public static StargateAddress of(List<StargateSymbol> symbols) {
        AddressValidator.Result result = AddressValidator.validate(symbols);
        if (!result.isValid()) {
            throw new IllegalArgumentException("Invalid address " + symbols + ": " + result);
        }
        return new StargateAddress(symbols);
    }

    /**
     * Builds an address from symbol ids.
     *
     * <p><i>FR —</i> Construit une adresse à partir d'identifiants de symboles.
     *
     * @param ids symbol ids / identifiants des symboles
     * @return the address / l'adresse
     * @throws IllegalArgumentException if an id is out of range or the address is invalid / si un
     *                                  identifiant est hors limites ou si l'adresse est invalide
     */
    public static StargateAddress of(int... ids) {
        List<StargateSymbol> list = new ArrayList<>(ids.length);
        for (int id : ids) {
            list.add(StargateSymbol.of(id));
        }
        return of(list);
    }

    /**
     * Like {@link #of(List)} but returns empty instead of throwing.
     *
     * <p><i>FR —</i> Comme {@link #of(List)}, mais renvoie vide au lieu de lever une exception.
     *
     * @param symbols the destination glyphs / les glyphes de destination
     * @return the address, or empty if invalid / l'adresse, ou vide si elle est invalide
     */
    public static Optional<StargateAddress> tryOf(List<StargateSymbol> symbols) {
        return AddressValidator.validate(symbols).isValid()
                ? Optional.of(new StargateAddress(symbols))
                : Optional.empty();
    }

    /**
     * Parses the {@code 1-2-3-4-5-6} text form. Whitespace around ids is ignored.
     *
     * <p><i>FR —</i> Lit la forme texte {@code 1-2-3-4-5-6}. Les espaces autour des identifiants sont
     * ignorés.
     *
     * @param text the text to parse / le texte à lire
     * @return the address, or empty if the text is not a valid address / l'adresse, ou vide si le texte
     *         n'est pas une adresse valide
     */
    public static Optional<StargateAddress> parse(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String[] parts = text.trim().split("\\s*-\\s*");
        List<StargateSymbol> list = new ArrayList<>(parts.length);
        for (String part : parts) {
            int id;
            try {
                id = Integer.parseInt(part.trim());
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
            if (!StargateSymbol.isValidId(id)) {
                return Optional.empty();
            }
            list.add(StargateSymbol.of(id));
        }
        return tryOf(list);
    }

    /**
     * Draws a random {@link Type#STANDARD} address: distinct symbols, never the point of origin.
     *
     * <p><i>FR —</i> Tire au hasard une adresse {@link Type#STANDARD} : symboles distincts, jamais le
     * point d'origine.
     *
     * @param random the random source / la source d'aléa
     * @return a new valid address / une nouvelle adresse valide
     */
    public static StargateAddress random(RandomGenerator random) {
        return random(random, Type.STANDARD);
    }

    /**
     * Draws a random address of the given type.
     *
     * <p><i>FR —</i> Tire au hasard une adresse du type donné.
     *
     * @param random the random source / la source d'aléa
     * @param type   the address type / le type d'adresse
     * @return a new valid address / une nouvelle adresse valide
     */
    public static StargateAddress random(RandomGenerator random, Type type) {
        Set<Integer> used = new HashSet<>();
        List<StargateSymbol> list = new ArrayList<>(type.length());
        while (list.size() < type.length()) {
            int id = 1 + random.nextInt(StargateSymbol.COUNT - 1);
            if (used.add(id)) {
                list.add(StargateSymbol.of(id));
            }
        }
        return new StargateAddress(list);
    }

    /**
     * The destination glyphs, in dialing order.
     *
     * <p><i>FR —</i> Les glyphes de destination, dans l'ordre de numérotation.
     *
     * @return an unmodifiable list / une liste non modifiable
     */
    public List<StargateSymbol> symbols() {
        return symbols;
    }

    /**
     * Number of symbols, point of origin excluded.
     *
     * <p><i>FR —</i> Nombre de symboles, point d'origine exclu.
     *
     * @return the length / la longueur
     */
    public int length() {
        return symbols.size();
    }

    /**
     * The address type, deduced from its length.
     *
     * <p><i>FR —</i> Le type d'adresse, déduit de sa longueur.
     *
     * @return the type / le type
     */
    public Type type() {
        return Type.forLength(symbols.size()).orElseThrow();
    }

    /**
     * Chevrons engaged when this address is dialed (point of origin included).
     *
     * <p><i>FR —</i> Nombre de chevrons engagés quand on compose cette adresse (point d'origine compris).
     *
     * @return {@code length() + 1}
     */
    public int chevrons() {
        return symbols.size() + 1;
    }

    /**
     * The full dial sequence: the address symbols followed by the point of origin.
     *
     * <p><i>FR —</i> La séquence de numérotation complète : les symboles de l'adresse suivis du point
     * d'origine.
     *
     * @return an unmodifiable list / une liste non modifiable
     */
    public List<StargateSymbol> dialSequence() {
        List<StargateSymbol> seq = new ArrayList<>(symbols);
        seq.add(StargateSymbol.POINT_OF_ORIGIN);
        return List.copyOf(seq);
    }

    /**
     * The symbol ids, in order.
     *
     * <p><i>FR —</i> Les identifiants des symboles, dans l'ordre.
     *
     * @return a new array / un nouveau tableau
     */
    public int[] toIds() {
        return symbols.stream().mapToInt(StargateSymbol::id).toArray();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof StargateAddress other && other.symbols.equals(symbols);
    }

    @Override
    public int hashCode() {
        return symbols.hashCode();
    }

    /**
     * The text form, e.g. {@code 27-7-15-32-12-30}; {@link #parse(String)} reads it back.
     *
     * <p><i>FR —</i> La forme texte, par exemple {@code 27-7-15-32-12-30} ; {@link #parse(String)} la
     * relit.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < symbols.size(); i++) {
            if (i > 0) {
                sb.append('-');
            }
            sb.append(symbols.get(i).id());
        }
        return sb.toString();
    }
}

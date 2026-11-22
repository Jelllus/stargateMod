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
 */
public final class StargateAddress {
    public enum Type {
        STANDARD(6), EXTENDED(7), SPECIAL(8);

        private final int length;

        Type(int length) {
            this.length = length;
        }

        public int length() {
            return length;
        }

        public int chevrons() {
            return length + 1;
        }

        public static Optional<Type> forLength(int length) {
            for (Type t : values()) {
                if (t.length == length) {
                    return Optional.of(t);
                }
            }
            return Optional.empty();
        }
    }

    public static final int MIN_LENGTH = Type.STANDARD.length();
    public static final int MAX_LENGTH = Type.SPECIAL.length();

    private final List<StargateSymbol> symbols;

    private StargateAddress(List<StargateSymbol> symbols) {
        this.symbols = List.copyOf(symbols);
    }

    /**
     * @throws IllegalArgumentException if the symbols don't form a valid address (see {@link AddressValidator})
     */
    public static StargateAddress of(List<StargateSymbol> symbols) {
        AddressValidator.Result result = AddressValidator.validate(symbols);
        if (!result.isValid()) {
            throw new IllegalArgumentException("Invalid address " + symbols + ": " + result);
        }
        return new StargateAddress(symbols);
    }

    public static StargateAddress of(int... ids) {
        List<StargateSymbol> list = new ArrayList<>(ids.length);
        for (int id : ids) {
            list.add(StargateSymbol.of(id));
        }
        return of(list);
    }

    /** Like {@link #of(List)} but returns empty instead of throwing. */
    public static Optional<StargateAddress> tryOf(List<StargateSymbol> symbols) {
        return AddressValidator.validate(symbols).isValid()
                ? Optional.of(new StargateAddress(symbols))
                : Optional.empty();
    }

    /** Parses the {@code 1-2-3-4-5-6} text form. Whitespace around ids is ignored. */
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

    /** Draws a random {@link Type#STANDARD} address: distinct symbols, never the point of origin. */
    public static StargateAddress random(RandomGenerator random) {
        return random(random, Type.STANDARD);
    }

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

    public List<StargateSymbol> symbols() {
        return symbols;
    }

    public int length() {
        return symbols.size();
    }

    public Type type() {
        return Type.forLength(symbols.size()).orElseThrow();
    }

    /** Chevrons engaged when this address is dialed (point of origin included). */
    public int chevrons() {
        return symbols.size() + 1;
    }

    /** The full dial sequence: the address symbols followed by the point of origin. */
    public List<StargateSymbol> dialSequence() {
        List<StargateSymbol> seq = new ArrayList<>(symbols);
        seq.add(StargateSymbol.POINT_OF_ORIGIN);
        return List.copyOf(seq);
    }

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

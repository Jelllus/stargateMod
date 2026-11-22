package com.stargate.mod.stargate.address;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Rules a sequence of symbols must satisfy to be an address:
 * <ul>
 *     <li>length between {@link StargateAddress#MIN_LENGTH} and {@link StargateAddress#MAX_LENGTH};</li>
 *     <li>no point of origin (it is only ever the terminating glyph of a dial);</li>
 *     <li>no symbol repeated — a chevron can only lock one glyph.</li>
 * </ul>
 */
public final class AddressValidator {
    public enum Result {
        VALID,
        TOO_SHORT,
        TOO_LONG,
        CONTAINS_POINT_OF_ORIGIN,
        DUPLICATE_SYMBOL,
        NULL_SYMBOL;

        public boolean isValid() {
            return this == VALID;
        }
    }

    private AddressValidator() {
    }

    public static Result validate(List<StargateSymbol> symbols) {
        if (symbols == null || symbols.size() < StargateAddress.MIN_LENGTH) {
            return Result.TOO_SHORT;
        }
        if (symbols.size() > StargateAddress.MAX_LENGTH) {
            return Result.TOO_LONG;
        }
        Set<StargateSymbol> seen = new HashSet<>();
        for (StargateSymbol s : symbols) {
            if (s == null) {
                return Result.NULL_SYMBOL;
            }
            if (s.isPointOfOrigin()) {
                return Result.CONTAINS_POINT_OF_ORIGIN;
            }
            if (!seen.add(s)) {
                return Result.DUPLICATE_SYMBOL;
            }
        }
        return Result.VALID;
    }

    /**
     * Whether {@code next} may be encoded after the {@code alreadyEncoded} symbols of a dial in progress.
     * The point of origin is always accepted once at least {@link StargateAddress#MIN_LENGTH} symbols are
     * in; whether the result resolves to a gate is decided when it locks.
     */
    public static boolean canEncode(List<StargateSymbol> alreadyEncoded, StargateSymbol next) {
        if (next == null) {
            return false;
        }
        if (next.isPointOfOrigin()) {
            return alreadyEncoded.size() >= StargateAddress.MIN_LENGTH;
        }
        return alreadyEncoded.size() < StargateAddress.MAX_LENGTH && !alreadyEncoded.contains(next);
    }
}

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
 *
 * <p><i>FR —</i> Règles qu'une suite de symboles doit respecter pour être une adresse :
 * <ul>
 *     <li>longueur comprise entre {@link StargateAddress#MIN_LENGTH} et {@link StargateAddress#MAX_LENGTH} ;</li>
 *     <li>pas de point d'origine (il ne sert qu'à terminer une numérotation) ;</li>
 *     <li>aucun symbole répété : un chevron ne verrouille qu'un seul glyphe.</li>
 * </ul>
 */
public final class AddressValidator {
    /**
     * Outcome of {@link #validate(List)}.
     *
     * <p><i>FR —</i> Résultat de {@link #validate(List)}.
     */
    public enum Result {
        /** The symbols form a valid address. / <i>FR —</i> Les symboles forment une adresse valide. */
        VALID,
        /** Fewer symbols than {@link StargateAddress#MIN_LENGTH}, or {@code null}. / <i>FR —</i> Moins de symboles que {@link StargateAddress#MIN_LENGTH}, ou {@code null}. */
        TOO_SHORT,
        /** More symbols than {@link StargateAddress#MAX_LENGTH}. / <i>FR —</i> Plus de symboles que {@link StargateAddress#MAX_LENGTH}. */
        TOO_LONG,
        /** The point of origin appears in the address. / <i>FR —</i> Le point d'origine figure dans l'adresse. */
        CONTAINS_POINT_OF_ORIGIN,
        /** A symbol appears twice. / <i>FR —</i> Un symbole apparaît deux fois. */
        DUPLICATE_SYMBOL,
        /** The list contains {@code null}. / <i>FR —</i> La liste contient {@code null}. */
        NULL_SYMBOL;

        /**
         * Whether this is {@link #VALID}.
         *
         * <p><i>FR —</i> Indique s'il s'agit de {@link #VALID}.
         *
         * @return {@code true} if valid / {@code true} si valide
         */
        public boolean isValid() {
            return this == VALID;
        }
    }

    private AddressValidator() {
    }

    /**
     * Checks whether {@code symbols} form a valid address.
     *
     * <p><i>FR —</i> Vérifie si {@code symbols} forment une adresse valide.
     *
     * @param symbols the candidate address, without point of origin / l'adresse candidate, sans point
     *                d'origine
     * @return {@link Result#VALID} or the first rule broken / {@link Result#VALID} ou la première règle
     *         enfreinte
     */
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
     *
     * <p><i>FR —</i> Indique si {@code next} peut être encodé après les symboles {@code alreadyEncoded}
     * d'une numérotation en cours. Le point d'origine est toujours accepté dès qu'au moins
     * {@link StargateAddress#MIN_LENGTH} symboles sont entrés ; savoir si le résultat mène à une porte se
     * décide au verrouillage.
     *
     * @param alreadyEncoded symbols locked so far / symboles déjà verrouillés
     * @param next           symbol to encode / symbole à encoder
     * @return {@code true} if the symbol can be encoded now / {@code true} si le symbole peut être encodé
     *         maintenant
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

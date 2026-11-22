package com.stargate.mod.stargate.connection;

/**
 * Outcome of a dialing request, or the reason a dial in progress failed.
 *
 * <p><i>FR —</i> Résultat d'une demande de numérotation, ou raison de l'échec d'une numérotation en cours.
 */
public enum DialResult {
    /**
     * An automatic dial was queued.
     *
     * <p><i>FR —</i> Une numérotation automatique a été mise en file.
     */
    STARTED(true),
    /**
     * A symbol was encoded; more are needed (or accepted) before the point of origin.
     *
     * <p><i>FR —</i> Un symbole a été encodé ; d'autres sont nécessaires (ou acceptés) avant le point
     * d'origine.
     */
    ENCODED(true),
    /**
     * The point of origin locked and a wormhole is forming.
     *
     * <p><i>FR —</i> Le point d'origine s'est verrouillé et un trou de ver se forme.
     */
    LOCKED(true),

    /**
     * The dialing gate is not registered.
     *
     * <p><i>FR —</i> La porte qui compose n'est pas enregistrée.
     */
    UNKNOWN_GATE(false),
    /**
     * The dialing gate is not idle (already dialing, connected, closing, or receiving).
     *
     * <p><i>FR —</i> La porte qui compose n'est pas inactive (déjà en numérotation, connectée, en
     * fermeture ou en réception).
     */
    GATE_BUSY(false),
    /**
     * The symbol cannot be encoded now (repeated, too many symbols, point of origin too early).
     *
     * <p><i>FR —</i> Le symbole ne peut pas être encodé maintenant (répété, trop de symboles, point
     * d'origine trop tôt).
     */
    INVALID_SYMBOL(false),
    /**
     * The encoded symbols don't form a valid address.
     *
     * <p><i>FR —</i> Les symboles encodés ne forment pas une adresse valide.
     */
    INVALID_ADDRESS(false),
    /**
     * The address is valid but no gate answers to it.
     *
     * <p><i>FR —</i> L'adresse est valide mais aucune porte n'y répond.
     */
    NO_SUCH_ADDRESS(false),
    /**
     * The gate dialed its own address.
     *
     * <p><i>FR —</i> La porte a composé sa propre adresse.
     */
    SELF_DIAL(false),
    /**
     * The dialed gate is busy.
     *
     * <p><i>FR —</i> La porte appelée est occupée.
     */
    TARGET_BUSY(false),
    /**
     * A manual dial stayed incomplete for too long.
     *
     * <p><i>FR —</i> Une numérotation manuelle est restée incomplète trop longtemps.
     */
    TIMED_OUT(false),
    /**
     * The dial was cancelled.
     *
     * <p><i>FR —</i> La numérotation a été annulée.
     */
    ABORTED(false);

    private final boolean success;

    DialResult(boolean success) {
        this.success = success;
    }

    /**
     * Whether the request was accepted.
     *
     * <p><i>FR —</i> Indique si la demande a été acceptée.
     *
     * @return {@code true} for {@link #STARTED}, {@link #ENCODED} and {@link #LOCKED} / {@code true} pour
     *         {@link #STARTED}, {@link #ENCODED} et {@link #LOCKED}
     */
    public boolean isSuccess() {
        return success;
    }
}

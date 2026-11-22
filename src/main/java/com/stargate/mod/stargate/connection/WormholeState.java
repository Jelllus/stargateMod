package com.stargate.mod.stargate.connection;

/**
 * Lifecycle of a single gate.
 *
 * <pre>
 * IDLE ──dial/encode──▶ DIALING ──point of origin locks──▶ ENGAGING ──▶ OPEN ──▶ CLOSING ──▶ IDLE
 *                          │                                  ▲
 *                          └── fail / abort / timeout ──▶ IDLE│ (dialed gate jumps straight here)
 * </pre>
 *
 * The dialed (incoming) gate goes {@code IDLE → ENGAGING} when the dialing gate locks, and follows the
 * same {@code OPEN → CLOSING → IDLE} path. Whether a gate is the outgoing or incoming end is held by its
 * {@link StargateConnection}.
 *
 * <p><i>FR —</i> Cycle de vie d'une porte.
 *
 * <pre>
 * IDLE ──dial/encode──▶ DIALING ──verrouillage du point d'origine──▶ ENGAGING ──▶ OPEN ──▶ CLOSING ──▶ IDLE
 *                          │                                            ▲
 *                          └── échec / annulation / délai ──▶ IDLE      │ (la porte appelée arrive directement ici)
 * </pre>
 *
 * La porte appelée (entrante) passe de {@code IDLE} à {@code ENGAGING} quand la porte qui compose se
 * verrouille, puis suit le même chemin {@code OPEN → CLOSING → IDLE}. C'est sa {@link StargateConnection}
 * qui indique si une porte est l'extrémité sortante ou entrante.
 */
public enum WormholeState {
    /**
     * Inactive, ready to dial or be dialed.
     *
     * <p><i>FR —</i> Inactive, prête à composer ou à être appelée.
     */
    IDLE,
    /**
     * Chevrons are being encoded; no target yet.
     *
     * <p><i>FR —</i> Les chevrons sont en cours d'encodage ; pas encore de cible.
     */
    DIALING,
    /**
     * Address locked, vortex forming. Unstable: entering the event horizon now is lethal.
     *
     * <p><i>FR —</i> Adresse verrouillée, vortex en formation. Instable : entrer dans l'horizon des
     * événements à ce moment est mortel.
     */
    ENGAGING,
    /**
     * Stable wormhole. Matter travels from the outgoing gate to the incoming gate only.
     *
     * <p><i>FR —</i> Trou de ver stable. La matière voyage uniquement de la porte sortante vers la porte
     * entrante.
     */
    OPEN,
    /**
     * Event horizon collapsing; the gate cannot be used until it returns to {@link #IDLE}.
     *
     * <p><i>FR —</i> L'horizon des événements s'effondre ; la porte est inutilisable jusqu'à son retour à
     * {@link #IDLE}.
     */
    CLOSING;

    /**
     * Whether the gate refuses new dials, incoming or outgoing.
     *
     * <p><i>FR —</i> Indique si la porte refuse toute nouvelle numérotation, entrante ou sortante.
     *
     * @return {@code true} unless {@link #IDLE} / {@code true} sauf pour {@link #IDLE}
     */
    public boolean isBusy() {
        return this != IDLE;
    }

    /**
     * Whether the gate is linked to another gate (vortex forming or open).
     *
     * <p><i>FR —</i> Indique si la porte est reliée à une autre porte (vortex en formation ou ouvert).
     *
     * @return {@code true} for {@link #ENGAGING} and {@link #OPEN} / {@code true} pour {@link #ENGAGING}
     *         et {@link #OPEN}
     */
    public boolean isConnected() {
        return this == ENGAGING || this == OPEN;
    }
}

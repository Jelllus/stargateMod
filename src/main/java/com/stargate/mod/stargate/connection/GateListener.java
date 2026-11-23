package com.stargate.mod.stargate.connection;

import com.stargate.mod.stargate.address.StargateSymbol;

import java.util.UUID;

/**
 * Hooks for whatever renders or sounds a gate (block entity, network sync, sounds). All methods are
 * called on the server thread, after the manager's state is updated.
 *
 * <p><i>FR —</i> Points d'accroche pour ce qui affiche ou fait sonner une porte (block entity, synchro
 * réseau, sons). Toutes les méthodes sont appelées sur le thread serveur, après la mise à jour de l'état
 * du gestionnaire.
 */
public interface GateListener {
    /**
     * A listener that does nothing.
     *
     * <p><i>FR —</i> Un écouteur qui ne fait rien.
     */
    GateListener NONE = new GateListener() {
    };

    /**
     * A gate changed state.
     *
     * <p><i>FR —</i> Une porte a changé d'état.
     *
     * @param gate the gate / la porte
     * @param from previous state / état précédent
     * @param to   new state / nouvel état
     */
    default void onStateChanged(UUID gate, WormholeState from, WormholeState to) {
    }

    /**
     * A chevron locked on a gate.
     *
     * <p><i>FR —</i> Un chevron s'est verrouillé sur une porte.
     *
     * @param gate     the gate / la porte
     * @param chevron  1-based chevron index that just locked / numéro du chevron verrouillé, à partir de 1
     * @param symbol   the symbol it locked / le symbole verrouillé
     * @param incoming {@code true} when the gate is being dialed rather than dialing / {@code true} quand
     *                 la porte est appelée plutôt qu'elle ne compose
     */
    default void onChevronEncoded(UUID gate, int chevron, StargateSymbol symbol, boolean incoming) {
    }

    /**
     * The ring started spinning towards a symbol, or the top chevron started locking it (see
     * {@link ConnectionManager#getRing}), or a symbol was queued.
     *
     * <p><i>FR —</i> L'anneau a commencé à tourner vers un symbole, ou le chevron du haut a commencé à le
     * verrouiller (voir {@link ConnectionManager#getRing}), ou un symbole a été mis en file.
     *
     * @param gate the dialing gate / la porte qui compose
     */
    default void onDialProgress(UUID gate) {
    }

    /**
     * A dial failed; the gate is back to {@link WormholeState#IDLE}.
     *
     * <p><i>FR —</i> Une numérotation a échoué ; la porte est revenue à {@link WormholeState#IDLE}.
     *
     * @param gate   the dialing gate / la porte qui composait
     * @param reason why it failed / la raison de l'échec
     */
    default void onDialFailed(UUID gate, DialResult reason) {
    }

    /**
     * The vortex stabilised; both gates are {@link WormholeState#OPEN}.
     *
     * <p><i>FR —</i> Le vortex s'est stabilisé ; les deux portes sont {@link WormholeState#OPEN}.
     *
     * @param connection the wormhole / le trou de ver
     */
    default void onConnectionOpened(StargateConnection connection) {
    }

    /**
     * A wormhole closed; its gates are {@link WormholeState#CLOSING}.
     *
     * <p><i>FR —</i> Un trou de ver s'est fermé ; ses portes sont {@link WormholeState#CLOSING}.
     *
     * @param connection the wormhole / le trou de ver
     * @param reason     why it closed / la raison de la fermeture
     */
    default void onConnectionClosed(StargateConnection connection, CloseReason reason) {
    }

    /**
     * Why a wormhole closed.
     *
     * <p><i>FR —</i> Pourquoi un trou de ver s'est fermé.
     */
    enum CloseReason {
        /**
         * The dialing gate shut the wormhole.
         *
         * <p><i>FR —</i> La porte qui a composé a fermé le trou de ver.
         */
        DISCONNECTED,
        /**
         * The wormhole reached its maximum duration.
         *
         * <p><i>FR —</i> Le trou de ver a atteint sa durée maximale.
         */
        TIMED_OUT,
        /**
         * One of the two gates was destroyed or unregistered.
         *
         * <p><i>FR —</i> L'une des deux portes a été détruite ou désenregistrée.
         */
        GATE_REMOVED
    }
}

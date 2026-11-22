package com.stargate.mod.stargate.connection;

import com.stargate.mod.stargate.address.StargateSymbol;

import java.util.UUID;

/**
 * Hooks for whatever renders or sounds a gate (block entity, network sync, sounds). All methods are
 * called on the server thread, after the manager's state is updated.
 */
public interface GateListener {
    GateListener NONE = new GateListener() {
    };

    default void onStateChanged(UUID gate, WormholeState from, WormholeState to) {
    }

    /**
     * @param chevron  1-based chevron index that just locked
     * @param incoming {@code true} when the gate is being dialed rather than dialing
     */
    default void onChevronEncoded(UUID gate, int chevron, StargateSymbol symbol, boolean incoming) {
    }

    default void onDialFailed(UUID gate, DialResult reason) {
    }

    default void onConnectionOpened(StargateConnection connection) {
    }

    default void onConnectionClosed(StargateConnection connection, CloseReason reason) {
    }

    enum CloseReason {
        /** The dialing gate shut the wormhole. */
        DISCONNECTED,
        /** The wormhole reached its maximum duration. */
        TIMED_OUT,
        /** One of the two gates was destroyed or unregistered. */
        GATE_REMOVED
    }
}

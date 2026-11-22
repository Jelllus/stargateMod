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
 */
public enum WormholeState {
    /** Inactive, ready to dial or be dialed. */
    IDLE,
    /** Chevrons are being encoded; no target yet. */
    DIALING,
    /** Address locked, vortex forming. Unstable: entering the event horizon now is lethal. */
    ENGAGING,
    /** Stable wormhole. Matter travels from the outgoing gate to the incoming gate only. */
    OPEN,
    /** Event horizon collapsing; the gate cannot be used until it returns to {@link #IDLE}. */
    CLOSING;

    /** Whether the gate refuses new dials, incoming or outgoing. */
    public boolean isBusy() {
        return this != IDLE;
    }

    /** Whether the gate is linked to another gate (vortex forming or open). */
    public boolean isConnected() {
        return this == ENGAGING || this == OPEN;
    }
}

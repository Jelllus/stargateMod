package com.stargate.mod.stargate.connection;

/** Outcome of a dialing request, or the reason a dial in progress failed. */
public enum DialResult {
    /** An automatic dial was queued. */
    STARTED(true),
    /** A symbol was encoded; more are needed (or accepted) before the point of origin. */
    ENCODED(true),
    /** The point of origin locked and a wormhole is forming. */
    LOCKED(true),

    UNKNOWN_GATE(false),
    /** The dialing gate is not idle (already dialing, connected, closing, or receiving). */
    GATE_BUSY(false),
    /** The symbol cannot be encoded now (repeated, too many symbols, point of origin too early). */
    INVALID_SYMBOL(false),
    /** The encoded symbols don't form a valid address. */
    INVALID_ADDRESS(false),
    /** The address is valid but no gate answers to it. */
    NO_SUCH_ADDRESS(false),
    /** The gate dialed its own address. */
    SELF_DIAL(false),
    /** The dialed gate is busy. */
    TARGET_BUSY(false),
    /** A manual dial stayed incomplete for too long. */
    TIMED_OUT(false),
    /** The dial was cancelled. */
    ABORTED(false);

    private final boolean success;

    DialResult(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }
}

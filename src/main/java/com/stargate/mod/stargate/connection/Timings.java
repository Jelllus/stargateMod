package com.stargate.mod.stargate.connection;

/**
 * Durations, in server ticks (20 per second), that drive the gate state machine.
 *
 * @param chevronDelayTicks ticks between two chevrons during an automatic dial
 * @param engageTicks       how long the vortex takes to stabilise after locking
 * @param maxOpenTicks      maximum lifetime of an open wormhole
 * @param closeTicks        how long the event horizon takes to collapse
 * @param dialTimeoutTicks  a manual dial is dropped after this long without a new symbol
 */
public record Timings(int chevronDelayTicks, int engageTicks, int maxOpenTicks, int closeTicks, int dialTimeoutTicks) {
    /** 1 s per chevron, 2 s vortex, 38 min open, 1 s close, 30 s manual-dial timeout. */
    public static final Timings DEFAULT = new Timings(20, 40, 38 * 60 * 20, 20, 30 * 20);

    public Timings {
        if (chevronDelayTicks < 0 || engageTicks < 0 || closeTicks < 0) {
            throw new IllegalArgumentException("Durations must be >= 0");
        }
        if (maxOpenTicks <= 0 || dialTimeoutTicks <= 0) {
            throw new IllegalArgumentException("maxOpenTicks and dialTimeoutTicks must be > 0");
        }
    }
}

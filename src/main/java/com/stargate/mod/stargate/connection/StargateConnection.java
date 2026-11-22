package com.stargate.mod.stargate.connection;

import com.stargate.mod.stargate.address.StargateAddress;

import java.util.Objects;
import java.util.UUID;

/**
 * A wormhole between two gates. Travel is one-way: from {@link #source()} (the gate that dialed) to
 * {@link #target()} (the gate that was dialed).
 *
 * @param source         the dialing gate
 * @param target         the dialed gate
 * @param address        the address that was dialed to reach {@code target}
 * @param establishedTick manager tick at which the address locked
 */
public record StargateConnection(UUID source, UUID target, StargateAddress address, long establishedTick) {
    public StargateConnection {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(address, "address");
        if (source.equals(target)) {
            throw new IllegalArgumentException("A gate cannot connect to itself");
        }
    }

    public boolean involves(UUID gate) {
        return source.equals(gate) || target.equals(gate);
    }

    public boolean isSource(UUID gate) {
        return source.equals(gate);
    }

    /** The gate at the other end from {@code gate}. */
    public UUID other(UUID gate) {
        if (source.equals(gate)) {
            return target;
        }
        if (target.equals(gate)) {
            return source;
        }
        throw new IllegalArgumentException(gate + " is not part of " + this);
    }
}

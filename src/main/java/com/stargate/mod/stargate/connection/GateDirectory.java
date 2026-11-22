package com.stargate.mod.stargate.connection;

import com.stargate.mod.stargate.address.StargateAddress;

import java.util.Optional;
import java.util.UUID;

/** Lets the {@link ConnectionManager} look up gates without depending on how they are stored. */
public interface GateDirectory {
    Optional<UUID> resolve(StargateAddress address);

    boolean exists(UUID gate);
}

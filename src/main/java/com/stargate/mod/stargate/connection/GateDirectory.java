package com.stargate.mod.stargate.connection;

import com.stargate.mod.stargate.address.StargateAddress;

import java.util.Optional;
import java.util.UUID;

/**
 * Lets the {@link ConnectionManager} look up gates without depending on how they are stored.
 *
 * <p><i>FR —</i> Permet au {@link ConnectionManager} de retrouver les portes sans dépendre de la façon dont
 * elles sont stockées.
 */
public interface GateDirectory {
    /**
     * Finds the gate that answers to {@code address}.
     *
     * <p><i>FR —</i> Trouve la porte qui répond à {@code address}.
     *
     * @param address the dialed address / l'adresse composée
     * @return the gate id, or empty if none / l'identifiant de la porte, ou vide s'il n'y en a pas
     */
    Optional<UUID> resolve(StargateAddress address);

    /**
     * Whether {@code gate} is registered.
     *
     * <p><i>FR —</i> Indique si {@code gate} est enregistrée.
     *
     * @param gate the gate id / l'identifiant de la porte
     * @return {@code true} if it exists / {@code true} si elle existe
     */
    boolean exists(UUID gate);
}

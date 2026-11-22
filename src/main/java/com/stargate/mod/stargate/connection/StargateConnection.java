package com.stargate.mod.stargate.connection;

import com.stargate.mod.stargate.address.StargateAddress;

import java.util.Objects;
import java.util.UUID;

/**
 * A wormhole between two gates. Travel is one-way: from {@link #source()} (the gate that dialed) to
 * {@link #target()} (the gate that was dialed).
 *
 * <p><i>FR —</i> Un trou de ver entre deux portes. Le voyage est à sens unique : de {@link #source()} (la
 * porte qui a composé) vers {@link #target()} (la porte appelée).
 *
 * @param source          the dialing gate / la porte qui compose
 * @param target          the dialed gate / la porte appelée
 * @param address         the address that was dialed to reach {@code target} / l'adresse composée pour
 *                        joindre {@code target}
 * @param establishedTick manager tick at which the address locked / tick du gestionnaire auquel
 *                        l'adresse s'est verrouillée
 */
public record StargateConnection(UUID source, UUID target, StargateAddress address, long establishedTick) {
    /**
     * Validates the components.
     *
     * <p><i>FR —</i> Valide les composants.
     *
     * @throws NullPointerException     if a gate or the address is {@code null} / si une porte ou
     *                                  l'adresse vaut {@code null}
     * @throws IllegalArgumentException if {@code source} equals {@code target} / si {@code source} est
     *                                  égale à {@code target}
     */
    public StargateConnection {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(address, "address");
        if (source.equals(target)) {
            throw new IllegalArgumentException("A gate cannot connect to itself");
        }
    }

    /**
     * Whether {@code gate} is one of the two ends.
     *
     * <p><i>FR —</i> Indique si {@code gate} est l'une des deux extrémités.
     *
     * @param gate the gate to test / la porte à tester
     * @return {@code true} if it is the source or the target / {@code true} si c'est la source ou la cible
     */
    public boolean involves(UUID gate) {
        return source.equals(gate) || target.equals(gate);
    }

    /**
     * Whether {@code gate} is the dialing (outgoing) end.
     *
     * <p><i>FR —</i> Indique si {@code gate} est l'extrémité qui compose (sortante).
     *
     * @param gate the gate to test / la porte à tester
     * @return {@code true} if it is the source / {@code true} si c'est la source
     */
    public boolean isSource(UUID gate) {
        return source.equals(gate);
    }

    /**
     * The gate at the other end from {@code gate}.
     *
     * <p><i>FR —</i> La porte à l'autre bout par rapport à {@code gate}.
     *
     * @param gate one end of this connection / une extrémité de cette connexion
     * @return the other end / l'autre extrémité
     * @throws IllegalArgumentException if {@code gate} is not part of this connection / si {@code gate}
     *                                  ne fait pas partie de cette connexion
     */
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

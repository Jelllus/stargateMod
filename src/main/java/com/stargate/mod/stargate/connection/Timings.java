package com.stargate.mod.stargate.connection;

/**
 * Durations, in server ticks (20 per second), that drive the gate state machine.
 *
 * <p><i>FR —</i> Durées, en ticks serveur (20 par seconde), qui pilotent la machine à états des portes.
 *
 * @param chevronDelayTicks ticks between two chevrons during an automatic dial / ticks entre deux
 *                          chevrons pendant une numérotation automatique
 * @param engageTicks       how long the vortex takes to stabilise after locking / temps de stabilisation
 *                          du vortex après le verrouillage
 * @param maxOpenTicks      maximum lifetime of an open wormhole / durée de vie maximale d'un trou de ver
 *                          ouvert
 * @param closeTicks        how long the event horizon takes to collapse / temps d'effondrement de
 *                          l'horizon des événements
 * @param dialTimeoutTicks  a manual dial is dropped after this long without a new symbol / une
 *                          numérotation manuelle est abandonnée après ce délai sans nouveau symbole
 */
public record Timings(int chevronDelayTicks, int engageTicks, int maxOpenTicks, int closeTicks, int dialTimeoutTicks) {
    /**
     * 1 s per chevron, 2 s vortex, 38 min open, 1 s close, 30 s manual-dial timeout.
     *
     * <p><i>FR —</i> 1 s par chevron, 2 s de vortex, 38 min d'ouverture, 1 s de fermeture, 30 s de délai
     * pour une numérotation manuelle.
     */
    public static final Timings DEFAULT = new Timings(20, 40, 38 * 60 * 20, 20, 30 * 20);

    /**
     * Validates the durations.
     *
     * <p><i>FR —</i> Valide les durées.
     *
     * @throws IllegalArgumentException if a duration is negative, or {@code maxOpenTicks} /
     *                                  {@code dialTimeoutTicks} is not positive / si une durée est négative,
     *                                  ou si {@code maxOpenTicks} / {@code dialTimeoutTicks} n'est pas
     *                                  strictement positif
     */
    public Timings {
        if (chevronDelayTicks < 0 || engageTicks < 0 || closeTicks < 0) {
            throw new IllegalArgumentException("Durations must be >= 0");
        }
        if (maxOpenTicks <= 0 || dialTimeoutTicks <= 0) {
            throw new IllegalArgumentException("maxOpenTicks and dialTimeoutTicks must be > 0");
        }
    }
}

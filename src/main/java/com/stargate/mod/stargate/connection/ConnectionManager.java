package com.stargate.mod.stargate.connection;

import com.stargate.mod.stargate.address.AddressValidator;
import com.stargate.mod.stargate.address.StargateAddress;
import com.stargate.mod.stargate.address.StargateSymbol;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Runtime state of every gate: dialing progress, wormholes, and the timers between states.
 *
 * <p>Pure game logic, no Minecraft types: gates are identified by {@link UUID}, looked up through a
 * {@link GateDirectory}, and time advances only through {@link #tick()}. Not thread-safe; call from the
 * server thread.
 *
 * <h2>Two ways to dial</h2>
 * <ul>
 *     <li>{@link #dial(UUID, StargateAddress)} — automatic (dialing computer): one chevron every
 *     {@link Timings#chevronDelayTicks()}, then the point of origin.</li>
 *     <li>{@link #encode(UUID, StargateSymbol)} — manual (DHD): each symbol locks immediately; encoding
 *     the point of origin attempts the connection.</li>
 * </ul>
 * A failure at lock time resets the dialing gate to {@link WormholeState#IDLE} and is reported both as
 * the return value (manual) and through {@link GateListener#onDialFailed} (always).
 *
 * <p><i>FR —</i> État d'exécution de toutes les portes : progression de la numérotation, trous de ver, et
 * minuteries entre les états.
 *
 * <p>Logique de jeu pure, sans type Minecraft : les portes sont identifiées par un {@link UUID},
 * retrouvées via un {@link GateDirectory}, et le temps n'avance que par {@link #tick()}. Non thread-safe ;
 * à appeler depuis le thread serveur.
 *
 * <h2>Deux façons de composer</h2>
 * <ul>
 *     <li>{@link #dial(UUID, StargateAddress)} — automatique (ordinateur de numérotation) : un chevron
 *     toutes les {@link Timings#chevronDelayTicks()}, puis le point d'origine.</li>
 *     <li>{@link #encode(UUID, StargateSymbol)} — manuelle (DHD) : chaque symbole se verrouille aussitôt ;
 *     encoder le point d'origine tente la connexion.</li>
 * </ul>
 * Un échec au verrouillage remet la porte qui compose à {@link WormholeState#IDLE} ; il est signalé par la
 * valeur de retour (en manuel) et par {@link GateListener#onDialFailed} (toujours).
 */
public final class ConnectionManager {
    private final GateDirectory directory;
    private final Timings timings;
    private GateListener listener = GateListener.NONE;

    private final Map<UUID, Gate> gates = new HashMap<>();
    private long tick;

    /**
     * Creates a manager with no gate active.
     *
     * <p><i>FR —</i> Crée un gestionnaire sans aucune porte active.
     *
     * @param directory where gates are looked up / où les portes sont recherchées
     * @param timings   durations of each phase / durées de chaque phase
     */
    public ConnectionManager(GateDirectory directory, Timings timings) {
        this.directory = Objects.requireNonNull(directory);
        this.timings = Objects.requireNonNull(timings);
    }

    /**
     * Sets the listener notified of every state change; {@code null} removes it.
     *
     * <p><i>FR —</i> Définit l'écouteur prévenu de chaque changement d'état ; {@code null} le retire.
     *
     * @param listener the listener / l'écouteur
     */
    public void setListener(GateListener listener) {
        this.listener = listener == null ? GateListener.NONE : listener;
    }

    /**
     * The durations in use.
     *
     * <p><i>FR —</i> Les durées utilisées.
     *
     * @return the timings / les durées
     */
    public Timings timings() {
        return timings;
    }

    /**
     * Number of {@link #tick()} calls so far.
     *
     * <p><i>FR —</i> Nombre d'appels à {@link #tick()} jusqu'ici.
     *
     * @return the current tick / le tick courant
     */
    public long currentTick() {
        return tick;
    }

    // ---------------------------------------------------------------- queries / requêtes

    /**
     * Current state of a gate; {@link WormholeState#IDLE} for a gate the manager doesn't track.
     *
     * <p><i>FR —</i> État actuel d'une porte ; {@link WormholeState#IDLE} pour une porte que le
     * gestionnaire ne suit pas.
     *
     * @param gate the gate / la porte
     * @return its state / son état
     */
    public WormholeState getState(UUID gate) {
        Gate g = gates.get(gate);
        return g == null ? WormholeState.IDLE : g.state;
    }

    /**
     * Symbols locked so far (point of origin included once encoded).
     *
     * <p><i>FR —</i> Symboles verrouillés jusqu'ici (point d'origine compris une fois encodé).
     *
     * @param gate the gate / la porte
     * @return an unmodifiable copy / une copie non modifiable
     */
    public List<StargateSymbol> getEncoded(UUID gate) {
        Gate g = gates.get(gate);
        return g == null ? List.of() : List.copyOf(g.encoded);
    }

    /**
     * The wormhole a gate is part of, outgoing or incoming.
     *
     * <p><i>FR —</i> Le trou de ver dont la porte fait partie, sortant ou entrant.
     *
     * @param gate the gate / la porte
     * @return the connection, or empty / la connexion, ou vide
     */
    public Optional<StargateConnection> getConnection(UUID gate) {
        Gate g = gates.get(gate);
        return g == null ? Optional.empty() : Optional.ofNullable(g.connection);
    }

    /**
     * Where something entering {@code gate}'s event horizon ends up: the far gate, only if {@code gate} is
     * the outgoing end of an {@link WormholeState#OPEN} wormhole.
     *
     * <p><i>FR —</i> Où arrive ce qui entre dans l'horizon des événements de {@code gate} : la porte
     * distante, seulement si {@code gate} est l'extrémité sortante d'un trou de ver
     * {@link WormholeState#OPEN}.
     *
     * @param gate the gate being entered / la porte dans laquelle on entre
     * @return the destination gate, or empty / la porte de destination, ou vide
     */
    public Optional<UUID> getTravelDestination(UUID gate) {
        Gate g = gates.get(gate);
        if (g == null || g.state != WormholeState.OPEN || g.connection == null || !g.connection.isSource(gate)) {
            return Optional.empty();
        }
        return Optional.of(g.connection.target());
    }

    /**
     * Ticks left before the wormhole through {@code gate} closes on its own, if it is open.
     *
     * <p><i>FR —</i> Ticks restants avant que le trou de ver de {@code gate} se ferme tout seul, s'il est
     * ouvert.
     *
     * @param gate either end / l'une ou l'autre extrémité
     * @return remaining ticks, or empty if not open / ticks restants, ou vide s'il n'est pas ouvert
     */
    public Optional<Integer> getRemainingOpenTicks(UUID gate) {
        Gate g = gates.get(gate);
        if (g == null || g.state != WormholeState.OPEN) {
            return Optional.empty();
        }
        return Optional.of((int) Math.max(0, timings.maxOpenTicks() - g.ticksInState()));
    }

    // ---------------------------------------------------------------- commands / commandes

    /**
     * Starts an automatic dial of {@code address} from {@code gate}.
     *
     * <p><i>FR —</i> Lance une numérotation automatique de {@code address} depuis {@code gate}.
     *
     * @param gate    the dialing gate / la porte qui compose
     * @param address the address to dial / l'adresse à composer
     * @return {@link DialResult#STARTED}, {@link DialResult#UNKNOWN_GATE} or {@link DialResult#GATE_BUSY};
     *         lock failures arrive later through the listener / les échecs de verrouillage arrivent plus
     *         tard via l'écouteur
     */
    public DialResult dial(UUID gate, StargateAddress address) {
        Objects.requireNonNull(address, "address");
        if (!directory.exists(gate)) {
            return DialResult.UNKNOWN_GATE;
        }
        Gate g = gate(gate);
        if (g.state.isBusy()) {
            return DialResult.GATE_BUSY;
        }
        g.pending.addAll(address.dialSequence());
        setState(g, WormholeState.DIALING);
        return DialResult.STARTED;
    }

    /**
     * Encodes one symbol manually. Not allowed while an automatic dial is running on that gate.
     *
     * <p><i>FR —</i> Encode un symbole à la main. Interdit pendant une numérotation automatique sur cette
     * porte.
     *
     * @param gate   the dialing gate / la porte qui compose
     * @param symbol the symbol to lock / le symbole à verrouiller
     * @return {@link DialResult#ENCODED}, {@link DialResult#LOCKED} after the point of origin, or the
     *         failure / {@link DialResult#ENCODED}, {@link DialResult#LOCKED} après le point d'origine, ou
     *         l'échec
     */
    public DialResult encode(UUID gate, StargateSymbol symbol) {
        Objects.requireNonNull(symbol, "symbol");
        if (!directory.exists(gate)) {
            return DialResult.UNKNOWN_GATE;
        }
        Gate g = gate(gate);
        if (g.state != WormholeState.IDLE && g.state != WormholeState.DIALING) {
            return DialResult.GATE_BUSY;
        }
        if (!g.pending.isEmpty()) {
            return DialResult.GATE_BUSY;
        }
        if (!AddressValidator.canEncode(g.encoded, symbol)) {
            return DialResult.INVALID_SYMBOL;
        }
        if (g.state == WormholeState.IDLE) {
            setState(g, WormholeState.DIALING);
        }
        return lockChevron(g, symbol);
    }

    /**
     * Cancels a dial in progress.
     *
     * <p><i>FR —</i> Annule une numérotation en cours.
     *
     * @param gate the dialing gate / la porte qui compose
     * @return {@code false} if the gate wasn't dialing / {@code false} si la porte ne composait pas
     */
    public boolean abortDial(UUID gate) {
        Gate g = gates.get(gate);
        if (g == null || g.state != WormholeState.DIALING) {
            return false;
        }
        failDial(g, DialResult.ABORTED);
        return true;
    }

    /**
     * Shuts the wormhole. Only the dialing gate can do this, while the vortex is forming or open.
     *
     * <p><i>FR —</i> Ferme le trou de ver. Seule la porte qui a composé peut le faire, pendant que le vortex
     * se forme ou est ouvert.
     *
     * @param gate the dialing gate / la porte qui a composé
     * @return {@code false} if the gate has no outgoing wormhole / {@code false} si la porte n'a pas de
     *         trou de ver sortant
     */
    public boolean disconnect(UUID gate) {
        Gate g = gates.get(gate);
        if (g == null || g.connection == null || !g.state.isConnected() || !g.connection.isSource(gate)) {
            return false;
        }
        close(g.connection, GateListener.CloseReason.DISCONNECTED);
        return true;
    }

    /**
     * Forgets a gate that was destroyed or unregistered: collapses its wormhole (the far gate goes to
     * {@link WormholeState#CLOSING}) and drops its state.
     *
     * <p><i>FR —</i> Oublie une porte détruite ou désenregistrée : effondre son trou de ver (la porte
     * distante passe à {@link WormholeState#CLOSING}) et supprime son état.
     *
     * @param gate the removed gate / la porte retirée
     */
    public void onGateRemoved(UUID gate) {
        Gate g = gates.get(gate);
        if (g == null) {
            return;
        }
        if (g.connection != null) {
            close(g.connection, GateListener.CloseReason.GATE_REMOVED);
        }
        gates.remove(gate);
    }

    /**
     * Drops all state without firing events (server shutdown).
     *
     * <p><i>FR —</i> Supprime tout l'état sans déclencher d'événement (arrêt du serveur).
     */
    public void clear() {
        gates.clear();
    }

    // ---------------------------------------------------------------- time / temps

    /**
     * Advances time by one tick: encodes queued chevrons, stabilises vortices, closes expired wormholes
     * and finishes closings. Call once per server tick.
     *
     * <p><i>FR —</i> Fait avancer le temps d'un tick : encode les chevrons en attente, stabilise les
     * vortex, ferme les trous de ver expirés et termine les fermetures. À appeler une fois par tick serveur.
     */
    public void tick() {
        tick++;
        for (Gate g : List.copyOf(gates.values())) {
            if (gates.get(g.id) != g) {
                continue; // removed earlier this tick / retirée plus tôt dans ce tick
            }
            switch (g.state) {
                case DIALING -> tickDialing(g);
                case ENGAGING -> {
                    // The dialing end drives the shared timeline so both ends switch on the same tick.
                    // La porte qui compose pilote la chronologie commune : les deux bouts changent au même tick.
                    if (g.connection.isSource(g.id) && g.ticksInState() >= timings.engageTicks()) {
                        open(g.connection);
                    }
                }
                case OPEN -> {
                    if (g.connection.isSource(g.id) && g.ticksInState() >= timings.maxOpenTicks()) {
                        close(g.connection, GateListener.CloseReason.TIMED_OUT);
                    }
                }
                case CLOSING -> {
                    if (g.ticksInState() >= timings.closeTicks()) {
                        setState(g, WormholeState.IDLE);
                    }
                }
                case IDLE -> {
                }
            }
            if (g.state == WormholeState.IDLE && g.encoded.isEmpty() && g.pending.isEmpty()) {
                gates.remove(g.id);
            }
        }
    }

    /**
     * Encodes the next queued chevron when its delay is up, or drops a stalled manual dial.
     *
     * <p><i>FR —</i> Encode le prochain chevron en attente quand son délai est écoulé, ou abandonne une
     * numérotation manuelle bloquée.
     */
    private void tickDialing(Gate g) {
        if (!g.pending.isEmpty()) {
            if (tick - g.lastChevronTick >= timings.chevronDelayTicks()) {
                StargateSymbol next = g.pending.poll();
                if (!AddressValidator.canEncode(g.encoded, next)) {
                    failDial(g, DialResult.INVALID_SYMBOL);
                    return;
                }
                lockChevron(g, next);
            }
        } else if (tick - g.lastChevronTick >= timings.dialTimeoutTicks()) {
            failDial(g, DialResult.TIMED_OUT);
        }
    }

    // ---------------------------------------------------------------- transitions

    /**
     * Locks one chevron; the point of origin triggers {@link #engage(Gate)}.
     *
     * <p><i>FR —</i> Verrouille un chevron ; le point d'origine déclenche {@link #engage(Gate)}.
     */
    private DialResult lockChevron(Gate g, StargateSymbol symbol) {
        g.encoded.add(symbol);
        g.lastChevronTick = tick;
        listener.onChevronEncoded(g.id, g.encoded.size(), symbol, false);
        if (symbol.isPointOfOrigin()) {
            return engage(g);
        }
        return DialResult.ENCODED;
    }

    /**
     * Resolves the encoded address and, if the target is free, links both gates in
     * {@link WormholeState#ENGAGING}.
     *
     * <p><i>FR —</i> Résout l'adresse encodée et, si la cible est libre, relie les deux portes en
     * {@link WormholeState#ENGAGING}.
     */
    private DialResult engage(Gate g) {
        List<StargateSymbol> addressSymbols = g.encoded.subList(0, g.encoded.size() - 1);
        Optional<StargateAddress> address = StargateAddress.tryOf(new ArrayList<>(addressSymbols));
        if (address.isEmpty()) {
            return failDial(g, DialResult.INVALID_ADDRESS);
        }
        Optional<UUID> targetId = directory.resolve(address.get());
        if (targetId.isEmpty() || !directory.exists(targetId.get())) {
            return failDial(g, DialResult.NO_SUCH_ADDRESS);
        }
        if (targetId.get().equals(g.id)) {
            return failDial(g, DialResult.SELF_DIAL);
        }
        Gate target = gate(targetId.get());
        if (target.state.isBusy()) {
            return failDial(g, DialResult.TARGET_BUSY);
        }

        StargateConnection connection = new StargateConnection(g.id, target.id, address.get(), tick);
        g.connection = connection;
        target.connection = connection;
        g.pending.clear();
        setState(g, WormholeState.ENGAGING);

        // The dialed gate lights the same chevrons, all at once.
        // La porte appelée allume les mêmes chevrons, tous d'un coup.
        for (StargateSymbol s : address.get().dialSequence()) {
            target.encoded.add(s);
            listener.onChevronEncoded(target.id, target.encoded.size(), s, true);
        }
        setState(target, WormholeState.ENGAGING);
        return DialResult.LOCKED;
    }

    /** Both ends become {@link WormholeState#OPEN}. / <i>FR —</i> Les deux bouts passent à {@link WormholeState#OPEN}. */
    private void open(StargateConnection c) {
        setState(gates.get(c.source()), WormholeState.OPEN);
        setState(gates.get(c.target()), WormholeState.OPEN);
        listener.onConnectionOpened(c);
    }

    /** Both remaining ends become {@link WormholeState#CLOSING}. / <i>FR —</i> Les bouts restants passent à {@link WormholeState#CLOSING}. */
    private void close(StargateConnection c, GateListener.CloseReason reason) {
        for (UUID id : List.of(c.source(), c.target())) {
            Gate g = gates.get(id);
            if (g != null && g.connection == c) {
                g.connection = null;
                g.encoded.clear();
                setState(g, WormholeState.CLOSING);
            }
        }
        listener.onConnectionClosed(c, reason);
    }

    /** Resets a dialing gate to idle and reports why. / <i>FR —</i> Remet une porte qui compose à l'arrêt et signale pourquoi. */
    private DialResult failDial(Gate g, DialResult reason) {
        g.encoded.clear();
        g.pending.clear();
        setState(g, WormholeState.IDLE);
        listener.onDialFailed(g.id, reason);
        return reason;
    }

    /** Changes state and restarts the timers. / <i>FR —</i> Change d'état et relance les minuteries. */
    private void setState(Gate g, WormholeState state) {
        WormholeState from = g.state;
        g.state = state;
        g.stateSince = tick;
        g.lastChevronTick = tick;
        if (from != state) {
            listener.onStateChanged(g.id, from, state);
        }
    }

    private Gate gate(UUID id) {
        return gates.computeIfAbsent(id, Gate::new);
    }

    /**
     * Mutable per-gate state, created on demand and dropped once idle.
     *
     * <p><i>FR —</i> État modifiable d'une porte, créé à la demande et supprimé une fois inactif.
     */
    private final class Gate {
        final UUID id;
        WormholeState state = WormholeState.IDLE;
        final List<StargateSymbol> encoded = new ArrayList<>();
        final Deque<StargateSymbol> pending = new ArrayDeque<>();
        StargateConnection connection;
        /**
         * Manager tick of the last state change / last locked chevron; timers are derived from these.
         * <i>FR —</i> Tick du dernier changement d'état / du dernier chevron ; les minuteries en découlent.
         */
        long stateSince;
        long lastChevronTick;

        Gate(UUID id) {
            this.id = id;
        }

        long ticksInState() {
            return tick - stateSince;
        }
    }
}

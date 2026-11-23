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
 * <h2>Dialing</h2>
 * Each symbol is dialed like on the show: the inner ring spins (alternating direction) until the symbol is
 * under the top chevron ({@link Timings#ticksPerSymbol()} per symbol), then the top chevron locks it
 * ({@link Timings#chevronDelayTicks()}) and the next chevron lights. Symbols wait in a queue while the ring
 * is busy. The connection is only attempted once the point of origin is locked, i.e. when every chevron is
 * locked. The ring's movement is available through {@link #getRing(UUID)}.
 * <ul>
 *     <li>{@link #dial(UUID, StargateAddress)} — automatic (dialing computer): queues the whole address and
 *     the point of origin.</li>
 *     <li>{@link #encode(UUID, StargateSymbol)} — manual (DHD): queues one symbol.</li>
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
 * <h2>Numérotation</h2>
 * Chaque symbole est composé comme dans la série : l'anneau intérieur tourne (sens alterné) jusqu'à amener
 * le symbole sous le chevron du haut ({@link Timings#ticksPerSymbol()} par symbole), puis le chevron du haut
 * le verrouille ({@link Timings#chevronDelayTicks()}) et le chevron suivant s'allume. Les symboles attendent
 * dans une file tant que l'anneau est occupé. La connexion n'est tentée qu'une fois le point d'origine
 * verrouillé, c'est-à-dire quand tous les chevrons sont verrouillés. Le mouvement de l'anneau est accessible
 * via {@link #getRing(UUID)}.
 * <ul>
 *     <li>{@link #dial(UUID, StargateAddress)} — automatique (ordinateur de numérotation) : met en file toute
 *     l'adresse et le point d'origine.</li>
 *     <li>{@link #encode(UUID, StargateSymbol)} — manuelle (DHD) : met un symbole en file.</li>
 * </ul>
 * Un échec au verrouillage remet la porte qui compose à {@link WormholeState#IDLE} ; il est signalé par la
 * valeur de retour (en manuel) et par {@link GateListener#onDialFailed} (toujours).
 */
public final class ConnectionManager {
    private final GateDirectory directory;
    private final Timings timings;
    private GateListener listener = GateListener.NONE;

    private final Map<UUID, Gate> gates = new HashMap<>();
    /** Kept while idle, so the ring stays where it stopped. / <i>FR —</i> Gardé à l'arrêt : l'anneau reste où il s'est arrêté. */
    private final Map<UUID, RingMotion> rings = new HashMap<>();
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
     * Every symbol dialed so far, in order: locked, then the one the ring is bringing up, then the queued
     * ones. This is what a DHD shows as pressed.
     *
     * <p><i>FR —</i> Tous les symboles composés jusqu'ici, dans l'ordre : verrouillés, puis celui que l'anneau
     * amène, puis ceux en file. C'est ce qu'un DHD montre comme enfoncé.
     *
     * @param gate the gate / la porte
     * @return a new list / une nouvelle liste
     */
    public List<StargateSymbol> getDialed(UUID gate) {
        Gate g = gates.get(gate);
        return g == null ? List.of() : g.dialed();
    }

    /**
     * Movement of the gate's inner ring, in manager ticks ({@link #currentTick()}); at rest when not
     * dialing.
     *
     * <p><i>FR —</i> Mouvement de l'anneau intérieur de la porte, en ticks du gestionnaire
     * ({@link #currentTick()}) ; au repos hors numérotation.
     *
     * @param gate the gate / la porte
     * @return the motion / le mouvement
     */
    public RingMotion getRing(UUID gate) {
        return rings.getOrDefault(gate, RingMotion.rest(0));
    }

    /**
     * Restores where a gate's ring stopped (e.g. after a server restart), unless the manager already knows.
     *
     * <p><i>FR —</i> Rétablit où l'anneau d'une porte s'est arrêté (par exemple après un redémarrage du
     * serveur), sauf si le gestionnaire le sait déjà.
     *
     * @param gate     the gate / la porte
     * @param position symbol under the top chevron / symbole sous le chevron du haut
     */
    public void restoreRing(UUID gate, double position) {
        rings.putIfAbsent(gate, RingMotion.rest(position));
    }

    /**
     * Whether the top chevron is locking a symbol right now.
     *
     * <p><i>FR —</i> Indique si le chevron du haut est en train de verrouiller un symbole.
     *
     * @param gate the gate / la porte
     * @return {@code true} while locking / {@code true} pendant le verrouillage
     */
    public boolean isLocking(UUID gate) {
        Gate g = gates.get(gate);
        return g != null && g.inFlight != null && getRing(gate).isLocking(tick);
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

    /**
     * Ticks since the gate entered its current state.
     *
     * <p><i>FR —</i> Ticks écoulés depuis l'entrée de la porte dans son état actuel.
     *
     * @param gate the gate / la porte
     * @return ticks, or empty for an idle gate the manager doesn't track / ticks, ou vide pour une porte
     *         inactive que le gestionnaire ne suit pas
     */
    public Optional<Long> getTicksInState(UUID gate) {
        Gate g = gates.get(gate);
        return g == null ? Optional.empty() : Optional.of(g.ticksInState());
    }

    /**
     * Every wormhole currently linked (vortex forming or open), once each.
     *
     * <p><i>FR —</i> Tous les trous de ver actuellement reliés (vortex en formation ou ouverts), une fois
     * chacun.
     *
     * @return a new list / une nouvelle liste
     */
    public List<StargateConnection> getConnections() {
        List<StargateConnection> out = new ArrayList<>();
        for (Gate g : gates.values()) {
            if (g.connection != null && g.connection.isSource(g.id) && g.state.isConnected()) {
                out.add(g.connection);
            }
        }
        return out;
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
        g.automatic = true;
        startNext(g);
        return DialResult.STARTED;
    }

    /**
     * Dials one symbol manually: it is queued, and locked once the ring has brought it to the top. Not
     * allowed while an automatic dial is running on that gate. Failures found when the point of origin
     * locks (unknown address, busy target...) arrive later through {@link GateListener#onDialFailed}.
     *
     * <p><i>FR —</i> Compose un symbole à la main : il est mis en file, puis verrouillé une fois amené en
     * haut par l'anneau. Interdit pendant une numérotation automatique sur cette porte. Les échecs détectés au
     * verrouillage du point d'origine (adresse inconnue, cible occupée...) arrivent plus tard via
     * {@link GateListener#onDialFailed}.
     *
     * @param gate   the dialing gate / la porte qui compose
     * @param symbol the symbol to dial / le symbole à composer
     * @return {@link DialResult#ENCODED} if queued, or the failure / {@link DialResult#ENCODED} si mis en
     *         file, ou l'échec
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
        if (g.automatic) {
            return DialResult.GATE_BUSY;
        }
        List<StargateSymbol> dialed = g.dialed();
        // Nothing comes after the point of origin. / Rien ne vient après le point d'origine.
        if (dialed.contains(StargateSymbol.POINT_OF_ORIGIN) || !AddressValidator.canEncode(dialed, symbol)) {
            return DialResult.INVALID_SYMBOL;
        }
        if (g.state == WormholeState.IDLE) {
            setState(g, WormholeState.DIALING);
        }
        g.pending.add(symbol);
        g.lastChevronTick = tick;
        listener.onDialProgress(g.id);
        startNext(g);
        return DialResult.ENCODED;
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
        rings.remove(gate);
    }

    /**
     * Drops all state without firing events (server shutdown).
     *
     * <p><i>FR —</i> Supprime tout l'état sans déclencher d'événement (arrêt du serveur).
     */
    public void clear() {
        gates.clear();
        rings.clear();
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
            if (g.state == WormholeState.IDLE && g.encoded.isEmpty() && g.pending.isEmpty() && g.inFlight == null) {
                gates.remove(g.id);
            }
        }
    }

    /**
     * Follows the symbol being dialed: signals the start of the lock, locks it when done and starts the next
     * one; or drops a stalled manual dial.
     *
     * <p><i>FR —</i> Suit le symbole en cours : signale le début du verrouillage, le verrouille une fois
     * terminé et lance le suivant ; ou abandonne une numérotation manuelle bloquée.
     */
    private void tickDialing(Gate g) {
        if (g.inFlight != null) {
            RingMotion ring = getRing(g.id);
            if (tick == ring.spinEnd() && ring.spinTicks() > 0) {
                listener.onDialProgress(g.id);
            }
            if (tick >= ring.lockEnd()) {
                StargateSymbol symbol = g.inFlight;
                g.inFlight = null;
                lockChevron(g, symbol);
                if (g.state == WormholeState.DIALING) {
                    startNext(g);
                }
            }
        } else if (g.pending.isEmpty() && tick - g.lastChevronTick >= timings.dialTimeoutTicks()) {
            failDial(g, DialResult.TIMED_OUT);
        }
    }

    /**
     * Starts spinning the ring towards the next queued symbol, if the ring is free. The direction alternates
     * with each chevron.
     *
     * <p><i>FR —</i> Fait tourner l'anneau vers le prochain symbole en file, si l'anneau est libre. Le sens
     * alterne à chaque chevron.
     */
    private void startNext(Gate g) {
        if (g.inFlight != null || g.pending.isEmpty()) {
            return;
        }
        StargateSymbol next = g.pending.poll();
        if (!AddressValidator.canEncode(g.encoded, next)) {
            failDial(g, DialResult.INVALID_SYMBOL);
            return;
        }
        g.inFlight = next;
        boolean forwards = g.encoded.size() % 2 == 0;
        double from = getRing(g.id).position(tick);
        rings.put(g.id, RingMotion.towards(from, next, forwards, tick, timings.ticksPerSymbol(), timings.chevronDelayTicks()));
        listener.onDialProgress(g.id);
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
        if (g.inFlight != null) {
            // The ring stops where it is. / L'anneau s'arrête là où il est.
            rings.put(g.id, RingMotion.rest(getRing(g.id).position(tick)));
            g.inFlight = null;
        }
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
        if (state != WormholeState.DIALING) {
            g.automatic = false;
        }
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
        /** Symbol the ring is bringing up and the chevron locking, or null. / <i>FR —</i> Symbole amené par l'anneau puis verrouillé, ou null. */
        StargateSymbol inFlight;
        /** Dialing from {@link #dial}: no manual symbols. / <i>FR —</i> Numérotation de {@link #dial} : pas de symbole manuel. */
        boolean automatic;
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

        List<StargateSymbol> dialed() {
            List<StargateSymbol> all = new ArrayList<>(encoded);
            if (inFlight != null) {
                all.add(inFlight);
            }
            all.addAll(pending);
            return all;
        }
    }
}

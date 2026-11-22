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
 */
public final class ConnectionManager {
    private final GateDirectory directory;
    private final Timings timings;
    private GateListener listener = GateListener.NONE;

    private final Map<UUID, Gate> gates = new HashMap<>();
    private long tick;

    public ConnectionManager(GateDirectory directory, Timings timings) {
        this.directory = Objects.requireNonNull(directory);
        this.timings = Objects.requireNonNull(timings);
    }

    public void setListener(GateListener listener) {
        this.listener = listener == null ? GateListener.NONE : listener;
    }

    public Timings timings() {
        return timings;
    }

    public long currentTick() {
        return tick;
    }

    // ---------------------------------------------------------------- queries

    public WormholeState getState(UUID gate) {
        Gate g = gates.get(gate);
        return g == null ? WormholeState.IDLE : g.state;
    }

    /** Symbols locked so far (point of origin included once encoded). */
    public List<StargateSymbol> getEncoded(UUID gate) {
        Gate g = gates.get(gate);
        return g == null ? List.of() : List.copyOf(g.encoded);
    }

    public Optional<StargateConnection> getConnection(UUID gate) {
        Gate g = gates.get(gate);
        return g == null ? Optional.empty() : Optional.ofNullable(g.connection);
    }

    /**
     * Where something entering {@code gate}'s event horizon ends up: the far gate, only if {@code gate} is
     * the outgoing end of an {@link WormholeState#OPEN} wormhole.
     */
    public Optional<UUID> getTravelDestination(UUID gate) {
        Gate g = gates.get(gate);
        if (g == null || g.state != WormholeState.OPEN || g.connection == null || !g.connection.isSource(gate)) {
            return Optional.empty();
        }
        return Optional.of(g.connection.target());
    }

    /** Ticks left before the wormhole through {@code gate} closes on its own, if it is open. */
    public Optional<Integer> getRemainingOpenTicks(UUID gate) {
        Gate g = gates.get(gate);
        if (g == null || g.state != WormholeState.OPEN) {
            return Optional.empty();
        }
        return Optional.of((int) Math.max(0, timings.maxOpenTicks() - g.ticksInState()));
    }

    // ---------------------------------------------------------------- commands

    /** Starts an automatic dial of {@code address} from {@code gate}. */
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

    /** Encodes one symbol manually. Not allowed while an automatic dial is running on that gate. */
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

    /** Cancels a dial in progress. Returns {@code false} if the gate wasn't dialing. */
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
     * Returns {@code false} otherwise.
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

    /** Drops all state without firing events (server shutdown). */
    public void clear() {
        gates.clear();
    }

    // ---------------------------------------------------------------- time

    public void tick() {
        tick++;
        for (Gate g : List.copyOf(gates.values())) {
            if (gates.get(g.id) != g) {
                continue; // removed earlier this tick
            }
            switch (g.state) {
                case DIALING -> tickDialing(g);
                case ENGAGING -> {
                    // The dialing end drives the shared timeline so both ends switch on the same tick.
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

    private DialResult lockChevron(Gate g, StargateSymbol symbol) {
        g.encoded.add(symbol);
        g.lastChevronTick = tick;
        listener.onChevronEncoded(g.id, g.encoded.size(), symbol, false);
        if (symbol.isPointOfOrigin()) {
            return engage(g);
        }
        return DialResult.ENCODED;
    }

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
        for (StargateSymbol s : address.get().dialSequence()) {
            target.encoded.add(s);
            listener.onChevronEncoded(target.id, target.encoded.size(), s, true);
        }
        setState(target, WormholeState.ENGAGING);
        return DialResult.LOCKED;
    }

    private void open(StargateConnection c) {
        setState(gates.get(c.source()), WormholeState.OPEN);
        setState(gates.get(c.target()), WormholeState.OPEN);
        listener.onConnectionOpened(c);
    }

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

    private DialResult failDial(Gate g, DialResult reason) {
        g.encoded.clear();
        g.pending.clear();
        setState(g, WormholeState.IDLE);
        listener.onDialFailed(g.id, reason);
        return reason;
    }

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

    private final class Gate {
        final UUID id;
        WormholeState state = WormholeState.IDLE;
        final List<StargateSymbol> encoded = new ArrayList<>();
        final Deque<StargateSymbol> pending = new ArrayDeque<>();
        StargateConnection connection;
        /** Manager tick of the last state change / last locked chevron; timers are derived from these. */
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

package com.stargate.mod.stargate.connection;

import com.stargate.mod.stargate.address.StargateAddress;
import com.stargate.mod.stargate.address.StargateSymbol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.stargate.mod.stargate.connection.WormholeState.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link ConnectionManager} state machine, with short timings and an in-memory directory of
 * three gates.
 *
 * <p><i>FR —</i> Tests de la machine à états de {@link ConnectionManager}, avec des durées courtes et un
 * annuaire en mémoire de trois portes.
 */
class ConnectionManagerTest {
    /**
     * lock 2, engage 5, open 100, close 3, manual timeout 50, instant ring: each symbol takes 2 ticks.
     * <i>FR —</i> mêmes valeurs, en ticks, anneau instantané : chaque symbole prend 2 ticks.
     */
    private static final Timings T = new Timings(2, 5, 100, 3, 50, 0);

    private final Map<StargateAddress, UUID> gates = new HashMap<>();
    private final List<String> events = new ArrayList<>();
    private ConnectionManager cm;

    private final StargateAddress addrA = StargateAddress.of(1, 2, 3, 4, 5, 6);
    private final StargateAddress addrB = StargateAddress.of(7, 8, 9, 10, 11, 12);
    private final StargateAddress addrC = StargateAddress.of(13, 14, 15, 16, 17, 18);
    private UUID a, b, c;

    @BeforeEach
    void setUp() {
        a = add(addrA);
        b = add(addrB);
        c = add(addrC);
        cm = new ConnectionManager(new GateDirectory() {
            @Override
            public Optional<UUID> resolve(StargateAddress address) {
                return Optional.ofNullable(gates.get(address));
            }

            @Override
            public boolean exists(UUID gate) {
                return gates.containsValue(gate);
            }
        }, T);
        cm.setListener(new GateListener() {
            @Override
            public void onDialFailed(UUID gate, DialResult reason) {
                events.add("fail:" + reason);
            }

            @Override
            public void onConnectionOpened(StargateConnection connection) {
                events.add("open");
            }

            @Override
            public void onConnectionClosed(StargateConnection connection, CloseReason reason) {
                events.add("close:" + reason);
            }
        });
    }

    private UUID add(StargateAddress address) {
        UUID id = UUID.randomUUID();
        gates.put(address, id);
        return id;
    }

    private void ticks(int n) {
        for (int i = 0; i < n; i++) {
            cm.tick();
        }
    }

    @Test
    void automaticDialFullLifecycle() {
        assertEquals(DialResult.STARTED, cm.dial(a, addrB));
        assertEquals(DIALING, cm.getState(a));

        ticks(2 * 6);
        assertEquals(6, cm.getEncoded(a).size());
        assertEquals(DIALING, cm.getState(a));
        assertEquals(IDLE, cm.getState(b), "target untouched until lock");

        ticks(2); // point of origin
        assertEquals(ENGAGING, cm.getState(a));
        assertEquals(ENGAGING, cm.getState(b));
        assertEquals(7, cm.getEncoded(b).size());
        assertTrue(cm.getTravelDestination(a).isEmpty(), "vortex is not traversable");

        ticks(5);
        assertEquals(OPEN, cm.getState(a));
        assertEquals(OPEN, cm.getState(b));
        assertEquals(Optional.of(b), cm.getTravelDestination(a));
        assertTrue(cm.getTravelDestination(b).isEmpty(), "one-way travel");

        ticks(100);
        assertEquals(CLOSING, cm.getState(a));
        assertEquals(CLOSING, cm.getState(b));
        ticks(3);
        assertEquals(IDLE, cm.getState(a));
        assertEquals(IDLE, cm.getState(b));
        assertEquals(List.of("open", "close:TIMED_OUT"), events);
    }

    @Test
    void manualDialQueuesAndOpensOnlyOnceAllChevronsLock() {
        for (StargateSymbol s : addrB.symbols()) {
            assertEquals(DialResult.ENCODED, cm.encode(a, s));
        }
        assertEquals(DialResult.ENCODED, cm.encode(a, StargateSymbol.POINT_OF_ORIGIN));
        assertEquals(7, cm.getDialed(a).size(), "all pressed");
        assertEquals(DialResult.INVALID_SYMBOL, cm.encode(a, StargateSymbol.of(20)), "nothing after the point of origin");
        assertEquals(DialResult.INVALID_SYMBOL, cm.encode(a, StargateSymbol.POINT_OF_ORIGIN));
        assertTrue(cm.getEncoded(a).isEmpty(), "none locked yet");
        ticks(13);
        assertEquals(6, cm.getEncoded(a).size());
        assertEquals(DIALING, cm.getState(a), "point of origin not locked yet");
        assertEquals(IDLE, cm.getState(b));
        ticks(1);
        assertEquals(ENGAGING, cm.getState(b));
        assertTrue(cm.getConnection(b).orElseThrow().isSource(a));
    }

    @Test
    void ringSpinsToEachSymbolInAlternatingDirections() {
        ConnectionManager spin = new ConnectionManager(directory(), new Timings(4, 5, 100, 3, 500, 1));
        assertEquals(DialResult.ENCODED, spin.encode(a, StargateSymbol.of(5)));
        RingMotion first = spin.getRing(a);
        assertEquals(5, first.to() - first.from(), 1e-9, "forwards to 5");
        assertEquals(5, first.spinTicks());
        for (int i = 0; i < 5; i++) {
            assertFalse(spin.isLocking(a));
            spin.tick();
        }
        assertTrue(spin.isLocking(a), "top chevron locks once the symbol is up");
        assertTrue(spin.getEncoded(a).isEmpty());
        for (int i = 0; i < 4; i++) {
            spin.tick();
        }
        assertEquals(List.of(StargateSymbol.of(5)), spin.getEncoded(a));
        assertEquals(5, spin.getRing(a).position(spin.currentTick()), 1e-9, "ring stays on the symbol");

        spin.encode(a, StargateSymbol.of(7));
        RingMotion second = spin.getRing(a);
        assertEquals(5, second.from(), 1e-9);
        assertEquals(-37, second.to() - second.from(), 1e-9, "backwards the long way, 5 -> 7");
        assertEquals(7, second.position(second.spinEnd()), 1e-9);
    }

    @Test
    void abortStopsTheRingWhereItIs() {
        ConnectionManager spin = new ConnectionManager(directory(), new Timings(4, 5, 100, 3, 500, 2));
        spin.encode(a, StargateSymbol.of(20));
        for (int i = 0; i < 10; i++) {
            spin.tick();
        }
        double at = spin.getRing(a).position(spin.currentTick());
        assertTrue(at > 0 && at < 20);
        assertTrue(spin.abortDial(a));
        spin.tick();
        assertEquals(at, spin.getRing(a).position(spin.currentTick()), 1e-9);
    }

    @Test
    void manualDialRejectsBadSymbols() {
        assertEquals(DialResult.INVALID_SYMBOL, cm.encode(a, StargateSymbol.POINT_OF_ORIGIN));
        cm.encode(a, StargateSymbol.of(7));
        assertEquals(DialResult.INVALID_SYMBOL, cm.encode(a, StargateSymbol.of(7)), "already queued");
        assertEquals(1, cm.getDialed(a).size());
    }

    @Test
    void unknownAddressFailsAndResets() {
        StargateAddress nowhere = StargateAddress.of(30, 31, 32, 33, 34, 35);
        nowhere.symbols().forEach(s -> cm.encode(a, s));
        assertEquals(DialResult.ENCODED, cm.encode(a, StargateSymbol.POINT_OF_ORIGIN));
        ticks(14);
        assertEquals(List.of("fail:NO_SUCH_ADDRESS"), events);
        assertEquals(IDLE, cm.getState(a));
        assertTrue(cm.getEncoded(a).isEmpty());
    }

    @Test
    void selfDialFails() {
        cm.dial(a, addrA);
        ticks(2 * 7);
        assertEquals(IDLE, cm.getState(a));
        assertEquals(List.of("fail:SELF_DIAL"), events);
    }

    @Test
    void busyTargetFails() {
        manualConnect(a, addrB);
        cm.dial(c, addrB);
        ticks(2 * 7);
        assertEquals(IDLE, cm.getState(c));
        assertTrue(events.contains("fail:TARGET_BUSY"));
    }

    @Test
    void busyGateCannotDialOrEncode() {
        manualConnect(a, addrB);
        assertEquals(DialResult.GATE_BUSY, cm.dial(a, addrC));
        assertEquals(DialResult.GATE_BUSY, cm.dial(b, addrC), "incoming gate is locked");
        assertEquals(DialResult.GATE_BUSY, cm.encode(b, StargateSymbol.of(13)));
    }

    @Test
    void cannotEncodeDuringAutomaticDial() {
        cm.dial(a, addrB);
        assertEquals(DialResult.GATE_BUSY, cm.encode(a, StargateSymbol.of(20)));
    }

    @Test
    void onlyDialingGateCanDisconnect() {
        manualConnect(a, addrB);
        ticks(5);
        assertFalse(cm.disconnect(b));
        assertTrue(cm.disconnect(a));
        assertEquals(CLOSING, cm.getState(a));
        assertEquals(CLOSING, cm.getState(b));
        assertFalse(cm.disconnect(a));
        assertEquals("close:DISCONNECTED", events.getLast());
    }

    @Test
    void cannotRedialWhileClosing() {
        manualConnect(a, addrB);
        cm.disconnect(a);
        assertEquals(DialResult.GATE_BUSY, cm.dial(a, addrB));
        ticks(3);
        assertEquals(DialResult.STARTED, cm.dial(a, addrB));
    }

    @Test
    void manualDialTimesOut() {
        cm.encode(a, StargateSymbol.of(7)); // locks at tick 2 / verrouillé au tick 2
        ticks(51);
        assertEquals(DIALING, cm.getState(a));
        ticks(1);
        assertEquals(IDLE, cm.getState(a));
        assertEquals(List.of("fail:TIMED_OUT"), events);
    }

    @Test
    void abortDial() {
        cm.dial(a, addrB);
        ticks(3);
        assertTrue(cm.abortDial(a));
        assertEquals(IDLE, cm.getState(a));
        ticks(20);
        assertEquals(IDLE, cm.getState(b));
        assertFalse(cm.abortDial(a));
    }

    @Test
    void removingTargetCollapsesWormhole() {
        manualConnect(a, addrB);
        ticks(5);
        gates.remove(addrB);
        cm.onGateRemoved(b);
        assertEquals(CLOSING, cm.getState(a));
        assertEquals(IDLE, cm.getState(b));
        assertEquals("close:GATE_REMOVED", events.getLast());
        ticks(3);
        assertEquals(IDLE, cm.getState(a));
    }

    @Test
    void removingSourceDuringEngageCollapsesWormhole() {
        manualConnect(a, addrB);
        gates.remove(addrA);
        cm.onGateRemoved(a);
        ticks(10);
        assertEquals(IDLE, cm.getState(b));
        assertFalse(events.contains("open"));
    }

    @Test
    void eightChevronAddressResolves() {
        StargateAddress far = StargateAddress.of(20, 21, 22, 23, 24, 25, 26);
        UUID d = add(far);
        far.symbols().forEach(s -> cm.encode(a, s));
        cm.encode(a, StargateSymbol.POINT_OF_ORIGIN);
        ticks(16);
        assertEquals(8, cm.getEncoded(d).size());
    }

    @Test
    void listsConnectionsAndPhaseAge() {
        assertTrue(cm.getConnections().isEmpty());
        manualConnect(a, addrB);
        assertEquals(1, cm.getConnections().size(), "once per wormhole, not per gate");
        assertEquals(a, cm.getConnections().getFirst().source());
        assertEquals(Optional.of(0L), cm.getTicksInState(b));
        ticks(3);
        assertEquals(Optional.of(3L), cm.getTicksInState(a));
        cm.disconnect(a);
        assertTrue(cm.getConnections().isEmpty(), "closing wormholes are not listed");
    }

    private void manualConnect(UUID from, StargateAddress to) {
        to.symbols().forEach(s -> cm.encode(from, s));
        assertEquals(DialResult.ENCODED, cm.encode(from, StargateSymbol.POINT_OF_ORIGIN));
        ticks(2 * (to.length() + 1));
        assertEquals(ENGAGING, cm.getState(from));
    }

    private GateDirectory directory() {
        return new GateDirectory() {
            @Override
            public Optional<UUID> resolve(StargateAddress address) {
                return Optional.ofNullable(gates.get(address));
            }

            @Override
            public boolean exists(UUID gate) {
                return gates.containsValue(gate);
            }
        };
    }
}

package com.stargate.mod.stargate.transport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link GateBasis}: local frame and gate-to-gate transfer.
 *
 * <p><i>FR —</i> Tests de {@link GateBasis} : repère local et transfert d'une porte à l'autre.
 */
class GateBasisTest {
    private static final double EPS = 1e-9;

    @Test
    void localRoundTrip() {
        for (boolean alongX : new boolean[]{true, false}) {
            GateBasis b = GateBasis.of(10, 64, -3, alongX);
            double[] l = b.toLocal(12.3, 66.1, -1.7);
            double[] w = b.toWorld(l[0], l[1], l[2]);
            assertArrayEquals(new double[]{12.3, 66.1, -1.7}, w, EPS);
        }
    }

    @Test
    void frontAndRightMatchTheFrameLayout() {
        GateBasis x = GateBasis.of(0, 0, 0, true);
        assertArrayEquals(new double[]{1, 0, 0}, x.dirToWorld(1, 0, 0), EPS, "X ring: right = +X");
        assertArrayEquals(new double[]{0, 0, 1}, x.dirToWorld(0, 0, 1), EPS, "X ring: front = +Z");
        GateBasis z = GateBasis.of(0, 0, 0, false);
        assertArrayEquals(new double[]{0, 0, 1}, z.dirToWorld(1, 0, 0), EPS, "Z ring: right = +Z");
        assertArrayEquals(new double[]{-1, 0, 0}, z.dirToWorld(0, 0, 1), EPS, "Z ring: front = -X");
        assertEquals(64 + GateBasis.CENTRE_HEIGHT, GateBasis.of(0, 64, 0, true).cy(), EPS);
    }

    @Test
    void walkingInComesOutOfTheTargetFront() {
        GateBasis from = GateBasis.of(0, 64, 0, true);      // front = +Z / avant = +Z
        GateBasis to = GateBasis.of(100, 70, 100, false);   // front = -X / avant = -X
        // Traveller just behind the source plane, 1 block to the gate's right, walking into it (towards -Z, yaw 180).
        double[] pos = from.toWorld(1.0, -1.5, -0.05);
        GateBasis.Exit exit = GateBasis.transfer(from, to, pos, new double[]{0, 0, -0.3}, 180f, 0.6);

        double[] l = to.toLocal(exit.pos()[0], exit.pos()[1], exit.pos()[2]);
        assertEquals(-1.0, l[0], EPS, "keeps the traveller's own right side (gate's left)");
        assertEquals(-1.5, l[1], EPS, "same height in the ring");
        assertEquals(0.65, l[2], EPS, "in front of the target");
        double[] m = to.dirToLocal(exit.motion()[0], exit.motion()[1], exit.motion()[2]);
        assertEquals(0.3, m[2], EPS, "moving out of the target front");
        assertEquals(GateBasis.yawOf(-1, 0), exit.yaw(), 1e-4, "facing out of the target front (-X, west)");
    }

    @Test
    void sameOrientationIsAHalfTurn() {
        GateBasis a = GateBasis.of(0, 64, 0, true);
        GateBasis b = GateBasis.of(50, 64, 0, true);
        GateBasis.Exit exit = GateBasis.transfer(a, b, a.toWorld(0, 0, -0.1), new double[]{0, 0, -1}, 180f, 0.6);
        assertEquals(0f, exit.yaw(), 1e-4, "walked in facing north, comes out facing south");
        assertArrayEquals(new double[]{0, 0, 1}, exit.motion(), EPS);
    }
}

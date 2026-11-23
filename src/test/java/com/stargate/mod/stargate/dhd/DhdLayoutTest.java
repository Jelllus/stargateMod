package com.stargate.mod.stargate.dhd;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DhdLayout}: key layout, facing transforms and click picking.
 *
 * <p><i>FR —</i> Tests de {@link DhdLayout} : disposition des touches, orientations et détection des clics.
 */
class DhdLayoutTest {
    private static final int[][] FACINGS = {{0, 1}, {-1, 0}, {0, -1}, {1, 0}}; // south, west, north, east

    @Test
    void thirtyEightDistinctKeys() {
        Set<Integer> ids = new HashSet<>();
        for (boolean outer : new boolean[]{true, false}) {
            for (int i = 0; i < DhdLayout.KEYS_PER_RING; i++) {
                ids.add(DhdLayout.symbolAt(outer, i));
            }
        }
        assertEquals(38, ids.size());
        assertTrue(ids.stream().allMatch(id -> id >= 1 && id <= 38));
    }

    @Test
    void worldModelRoundTrip() {
        for (int[] f : FACINGS) {
            double[] p = {0.2, 0.7, 0.9};
            assertArrayEquals(p, DhdLayout.toModel(DhdLayout.toWorld(p, f[0], f[1]), f[0], f[1]), 1e-9);
        }
        assertArrayEquals(new double[]{0.5, 0, 1.5}, DhdLayout.toWorld(new double[]{0.5, 0, 1.5}, 0, 1), 1e-9, "south = identity");
        assertArrayEquals(new double[]{-0.5, 0, 0.5}, DhdLayout.toWorld(new double[]{0.5, 0, 1.5}, -1, 0), 1e-9, "front goes west");
    }

    @Test
    void consoleTiltsTowardsTheFront() {
        double[] back = DhdLayout.onConsole(0.4, Math.PI / 2, 0);
        double[] front = DhdLayout.onConsole(0.4, -Math.PI / 2, 0);
        assertTrue(back[1] > front[1], "back edge is higher");
        assertTrue(front[2] > back[2], "front edge is towards +Z");
    }

    @Test
    void lookingAtEachKeyPicksItsSymbol() {
        for (int[] f : FACINGS) {
            for (boolean outer : new boolean[]{true, false}) {
                double r = outer ? (DhdLayout.OUTER_R0 + DhdLayout.OUTER_R1) / 2 : (DhdLayout.INNER_R0 + DhdLayout.INNER_R1) / 2;
                for (int i = 0; i < DhdLayout.KEYS_PER_RING; i++) {
                    double[] a = DhdLayout.keyAngles(i);
                    double[] target = DhdLayout.toWorld(DhdLayout.onConsole(r, (a[0] + a[1]) / 2, DhdLayout.KEY_HEIGHT), f[0], f[1]);
                    // Operator's eye: in front of and above the DHD. / Œil de l'opérateur : devant et au-dessus du DHD.
                    double[] eye = DhdLayout.toWorld(new double[]{0.5, 1.62, 1.6}, f[0], f[1]);
                    double[] dir = {target[0] - eye[0], target[1] - eye[1], target[2] - eye[2]};
                    assertEquals(DhdLayout.symbolAt(outer, i), DhdLayout.pick(eye, dir, f[0], f[1]),
                            "facing " + f[0] + "," + f[1] + " ring " + (outer ? "outer" : "inner") + " key " + i);
                }
            }
        }
    }

    @Test
    void domeAndMisses() {
        double[] eye = {0.5, 1.62, 1.6};
        double[] centre = DhdLayout.onConsole(0, 0, DhdLayout.KEY_HEIGHT);
        assertEquals(DhdLayout.DOME, DhdLayout.pick(eye, new double[]{centre[0] - eye[0], centre[1] - eye[1], centre[2] - eye[2]}, 0, 1));
        assertEquals(DhdLayout.NONE, DhdLayout.pick(eye, new double[]{0, 1, 0}, 0, 1), "looking up");
        assertEquals(DhdLayout.NONE, DhdLayout.pickOnConsole((DhdLayout.INNER_R1 + DhdLayout.OUTER_R0) / 2, 1.0), "between rings");
        assertEquals(DhdLayout.NONE, DhdLayout.pickOnConsole(0.6, 0), "off the console");
    }

    @Test
    void glyphCircleFitsInsideItsKey() {
        double halfAngle = (2 * Math.PI / DhdLayout.KEYS_PER_RING - DhdLayout.KEY_GAP) / 2;
        for (boolean outer : new boolean[]{true, false}) {
            double[] c = DhdLayout.glyphCircle(outer);
            double r0 = outer ? DhdLayout.OUTER_R0 : DhdLayout.INNER_R0, r1 = outer ? DhdLayout.OUTER_R1 : DhdLayout.INNER_R1;
            assertTrue(c[0] + c[1] <= r1 && c[0] - c[1] >= r0, "within the ring");
            assertTrue(c[1] <= c[0] * Math.sin(halfAngle), "between the key's radial edges");
            assertTrue(c[1] > 0.03, "big enough to read");
        }
    }

    @Test
    void keyZeroIsAtTheTopAndIndicesGoClockwise() {
        assertEquals(DhdLayout.symbolAt(true, 0), DhdLayout.pickOnConsole(0.37, Math.PI / 2));
        double step = 2 * Math.PI / DhdLayout.KEYS_PER_RING;
        assertEquals(DhdLayout.symbolAt(true, 1), DhdLayout.pickOnConsole(0.37, Math.PI / 2 - step), "next key to the right");
    }
}

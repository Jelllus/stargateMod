package com.stargate.mod.stargate.connection;

import org.junit.jupiter.api.Test;

import static com.stargate.mod.stargate.connection.EventHorizonShape.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the event horizon timeline: kawoosh, open ripples, closing.
 *
 * <p><i>FR —</i> Tests de la chronologie de l'horizon des événements : kawoosh, ondulations, fermeture.
 */
class EventHorizonShapeTest {
    private static final int ENGAGE = 40, CLOSE = 20;

    @Test
    void kawooshBurstsThenRetracts() {
        assertEquals(0f, kawooshLength(0f), "nothing at the start");
        float peak = 0f;
        float peakAt = 0f;
        for (float p = 0f; p <= 1f; p += 0.01f) {
            float l = kawooshLength(p);
            assertTrue(l >= 0f && l <= KAWOOSH_LENGTH + 1e-4f);
            if (l > peak) {
                peak = l;
                peakAt = p;
            }
        }
        assertEquals(KAWOOSH_LENGTH, peak, 0.05f, "reaches full length");
        assertTrue(peakAt > 0.2f && peakAt < 0.5f, "peaks mid-phase, at " + peakAt);
        assertEquals(0f, kawooshLength(0.75f), "retracted before the end");
        assertEquals(0f, kawooshLength(1f));
    }

    @Test
    void kawooshBulgesForwardAtTheCentre() {
        float age = ENGAGE * 0.35f;
        float centre = offset(WormholeState.ENGAGING, age, ENGAGE, CLOSE, 0f, 0f);
        float rim = offset(WormholeState.ENGAGING, age, ENGAGE, CLOSE, 1f, 0f);
        assertTrue(centre > 3.5f, "centre pushed forward: " + centre);
        assertEquals(0f, rim, 1e-4f, "rim stays in the ring");
    }

    @Test
    void openSurfaceRipplesGently() {
        for (float rn = 0f; rn <= 1f; rn += 0.1f) {
            float o = offset(WormholeState.OPEN, 1000f, ENGAGE, CLOSE, rn, 1f);
            assertTrue(Math.abs(o) < 0.1f, "small ripple at rn=" + rn + ": " + o);
        }
        assertEquals(1f, radiusScale(WormholeState.OPEN, 5f, ENGAGE, CLOSE));
    }

    @Test
    void closingShrinksAndFades() {
        assertEquals(1f, radiusScale(WormholeState.CLOSING, 0f, ENGAGE, CLOSE), 1e-4f);
        assertEquals(0f, radiusScale(WormholeState.CLOSING, CLOSE, ENGAGE, CLOSE), 1e-4f);
        assertTrue(alpha(WormholeState.CLOSING, CLOSE * 0.5f, CLOSE) < alpha(WormholeState.OPEN, 0f, CLOSE));
        assertEquals(0f, alpha(WormholeState.CLOSING, CLOSE, CLOSE), 1e-4f);
    }

    @Test
    void surfaceGrowsInAsTheVortexForms() {
        assertEquals(0f, radiusScale(WormholeState.ENGAGING, 0f, ENGAGE, CLOSE), 1e-4f);
        assertEquals(1f, radiusScale(WormholeState.ENGAGING, ENGAGE * 0.1f, ENGAGE, CLOSE), 1e-4f);
    }

    @Test
    void nothingWhenIdleOrDialing() {
        assertFalse(isVisible(WormholeState.IDLE));
        assertFalse(isVisible(WormholeState.DIALING));
        assertTrue(isVisible(WormholeState.ENGAGING));
        assertEquals(0f, offset(WormholeState.IDLE, 3f, ENGAGE, CLOSE, 0.5f, 0f));
    }
}

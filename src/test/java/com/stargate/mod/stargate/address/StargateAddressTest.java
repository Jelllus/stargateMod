package com.stargate.mod.stargate.address;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link StargateAddress} and {@link AddressValidator}: parsing, validation rules, random draws.
 *
 * <p><i>FR —</i> Tests de {@link StargateAddress} et {@link AddressValidator} : lecture, règles de
 * validation, tirages aléatoires.
 */
class StargateAddressTest {
    @Test
    void parsesAndPrints() {
        StargateAddress a = StargateAddress.parse(" 27-7 - 15-32-12-30 ").orElseThrow();
        assertEquals("27-7-15-32-12-30", a.toString());
        assertEquals(StargateAddress.Type.STANDARD, a.type());
        assertEquals(7, a.chevrons());
        assertEquals(a, StargateAddress.parse(a.toString()).orElseThrow());
    }

    @Test
    void dialSequenceEndsWithPointOfOrigin() {
        List<StargateSymbol> seq = StargateAddress.of(1, 2, 3, 4, 5, 6).dialSequence();
        assertEquals(7, seq.size());
        assertTrue(seq.getLast().isPointOfOrigin());
    }

    @Test
    void rejectsInvalidAddresses() {
        assertTrue(StargateAddress.parse("1-2-3-4-5").isEmpty(), "too short");
        assertTrue(StargateAddress.parse("1-2-3-4-5-6-7-8-9").isEmpty(), "too long");
        assertTrue(StargateAddress.parse("1-2-3-4-5-5").isEmpty(), "duplicate");
        assertTrue(StargateAddress.parse("0-2-3-4-5-6").isEmpty(), "point of origin");
        assertTrue(StargateAddress.parse("1-2-3-4-5-39").isEmpty(), "out of range");
        assertTrue(StargateAddress.parse("a-b-c-d-e-f").isEmpty(), "not numbers");
        assertTrue(StargateAddress.parse("").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> StargateAddress.of(1, 1, 2, 3, 4, 5));
    }

    @Test
    void acceptsEightAndNineChevronAddresses() {
        assertEquals(StargateAddress.Type.EXTENDED, StargateAddress.of(1, 2, 3, 4, 5, 6, 7).type());
        assertEquals(9, StargateAddress.of(1, 2, 3, 4, 5, 6, 7, 8).chevrons());
    }

    @Test
    void randomAddressesAreValid() {
        Random r = new Random(42);
        for (int i = 0; i < 1000; i++) {
            StargateAddress a = StargateAddress.random(r);
            assertEquals(AddressValidator.Result.VALID, AddressValidator.validate(a.symbols()));
        }
    }

    @Test
    void canEncodeRules() {
        List<StargateSymbol> five = StargateAddress.of(1, 2, 3, 4, 5, 6).symbols().subList(0, 5);
        assertFalse(AddressValidator.canEncode(five, StargateSymbol.POINT_OF_ORIGIN), "PoO too early");
        assertFalse(AddressValidator.canEncode(five, StargateSymbol.of(3)), "repeat");
        assertTrue(AddressValidator.canEncode(five, StargateSymbol.of(6)));
        List<StargateSymbol> six = StargateAddress.of(1, 2, 3, 4, 5, 6).symbols();
        assertTrue(AddressValidator.canEncode(six, StargateSymbol.POINT_OF_ORIGIN));
    }
}

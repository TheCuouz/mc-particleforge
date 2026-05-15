package com.cristian.particleforge.primitives;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BurstStepTest {

    private BurstStep make(int durationTicks, Map<String, Object> params) {
        return new BurstStep("burst", durationTicks, params);
    }

    @Test
    void defaultRepeatEmitsOnlyOnTickZero() {
        BurstStep s = make(10, Map.of());
        assertTrue(s.shouldEmitThisTick(0));
        for (int t = 1; t < 10; t++) {
            assertFalse(s.shouldEmitThisTick(t), "tick " + t + " should not emit");
        }
    }

    @Test
    void repeatEveryFiveEmitsOnMultiplesOfFive() {
        BurstStep s = make(20, Map.of("repeat-every-ticks", 5));
        assertTrue(s.shouldEmitThisTick(0));
        assertTrue(s.shouldEmitThisTick(5));
        assertTrue(s.shouldEmitThisTick(10));
        for (int t : new int[]{1, 2, 3, 4, 6, 7, 8, 9}) {
            assertFalse(s.shouldEmitThisTick(t), "tick " + t + " should not emit");
        }
    }

    @Test
    void repeatEveryZeroNeverEmits() {
        BurstStep s = make(10, Map.of("repeat-every-ticks", 0));
        for (int t = 0; t < 20; t++) {
            assertFalse(s.shouldEmitThisTick(t), "tick " + t + " should not emit");
        }
    }

    @Test
    void effectiveCountAtNearFullMultiplier() {
        BurstStep s = make(10, Map.of("count", 32));
        assertEquals(32, s.effectiveCount(0, 1.0));
    }

    @Test
    void effectiveCountFarBucketScalesAndRounds() {
        BurstStep s = make(10, Map.of("count", 32));
        // 32 * 0.2 = 6.4 → round = 6
        assertEquals(6, s.effectiveCount(2, 0.2));
    }

    @Test
    void effectiveCountCullBucketIsZero() {
        BurstStep s = make(10, Map.of("count", 32));
        assertEquals(0, s.effectiveCount(3, 1.0));
        assertEquals(0, s.effectiveCount(3, 0.5));
        assertEquals(0, s.effectiveCount(4, 1.0));
    }

    @Test
    void effectiveCountZeroMultiplierIsZero() {
        BurstStep s = make(10, Map.of("count", 32));
        assertEquals(0, s.effectiveCount(0, 0.0));
    }

    @Test
    void constructorReadsCountAndSpeedFromParams() {
        BurstStep s = new BurstStep("burst", 10, Map.of("count", 50, "speed", 0.7));
        assertEquals(50, s.effectiveCount(0, 1.0));
    }
}

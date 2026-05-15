package com.cristian.particleforge.primitives;

import com.cristian.particleforge.engine.BudgetManager;
import com.cristian.particleforge.model.EffectContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RingStepTest {

    @Test
    void defaultsAreApplied() {
        RingStep s = new RingStep("r", 20, Map.of());
        assertEquals(24, s.effectivePoints(1.0));
        assertEquals(0.0, s.yawAtTick(0), 1e-9);
        assertEquals(0.0, s.yawAtTick(50), 1e-9);
    }

    @Test
    void overridesAreRead() {
        RingStep s = new RingStep("r", 20, Map.of(
            "points", 12,
            "yaw-deg", 30.0,
            "rotation-deg-per-tick", 2.5
        ));
        assertEquals(12, s.effectivePoints(1.0));
        assertEquals(30.0, s.yawAtTick(0), 1e-9);
        assertEquals(30.0 + 2.5 * 4, s.yawAtTick(4), 1e-9);
    }

    @Test
    void effectivePointsRoundsAndClamps() {
        RingStep s = new RingStep("r", 20, Map.of("points", 24));
        // 24 * 0.1 = 2.4 → round = 2
        assertEquals(2, s.effectivePoints(0.1));
        // 24 * 0.01 = 0.24 → round = 0 → clamped to 1
        assertEquals(1, s.effectivePoints(0.01));
        assertEquals(0, s.effectivePoints(0.0));
    }

    @Test
    void cullBucketShortCircuits() {
        RingStep s = new RingStep("r", 20, Map.of());
        EffectContext ctx = mock(EffectContext.class);
        when(ctx.lodBucket()).thenReturn(BudgetManager.BUCKET_CULL);
        when(ctx.lodMultiplier()).thenReturn(1.0);
        // origin() is never reached; if it were and returned null we still wouldn't NPE.
        assertDoesNotThrow(() -> s.tick(ctx, 0));
        verify(ctx, never()).origin();
    }
}

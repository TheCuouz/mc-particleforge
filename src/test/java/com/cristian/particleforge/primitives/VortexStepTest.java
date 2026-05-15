package com.cristian.particleforge.primitives;

import com.cristian.particleforge.engine.BudgetManager;
import com.cristian.particleforge.model.EffectContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VortexStepTest {

    @Test
    void defaultsAreApplied() {
        VortexStep s = new VortexStep("v", 21, Map.of());
        assertEquals(3.0, s.currentRadius(0), 1e-9);
        assertEquals(0.0, s.currentRadius(20), 1e-9);
        assertEquals(8, s.effectivePointsPerTick(1.0));
    }

    @Test
    void lerpsRadiusOverDuration() {
        VortexStep s = new VortexStep("v", 21, Map.of(
            "start-radius", 4.0,
            "end-radius", 0.0
        ));
        // endpoints
        assertEquals(4.0, s.currentRadius(0), 1e-9);
        assertEquals(0.0, s.currentRadius(20), 1e-9);
        // midpoint t=0.5 → r=2.0
        assertEquals(2.0, s.currentRadius(10), 1e-9);
    }

    @Test
    void overridesAreRead() {
        VortexStep s = new VortexStep("v", 21, Map.of(
            "points-per-tick", 16,
            "start-radius", 5.0,
            "end-radius", 1.0
        ));
        assertEquals(16, s.effectivePointsPerTick(1.0));
        assertEquals(5.0, s.currentRadius(0), 1e-9);
        assertEquals(1.0, s.currentRadius(20), 1e-9);
        assertEquals(3.0, s.currentRadius(10), 1e-9);
    }

    @Test
    void cullBucketShortCircuits() {
        VortexStep s = new VortexStep("v", 21, Map.of());
        EffectContext ctx = mock(EffectContext.class);
        when(ctx.lodBucket()).thenReturn(BudgetManager.BUCKET_CULL);
        when(ctx.lodMultiplier()).thenReturn(1.0);
        assertDoesNotThrow(() -> s.tick(ctx, 0));
        verify(ctx, never()).origin();
    }
}

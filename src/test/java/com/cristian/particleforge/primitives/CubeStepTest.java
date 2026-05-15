package com.cristian.particleforge.primitives;

import com.cristian.particleforge.engine.BudgetManager;
import com.cristian.particleforge.model.EffectContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CubeStepTest {

    @Test
    void defaultsAreApplied() {
        CubeStep s = new CubeStep("cu", 20, Map.of());
        assertEquals(4, s.effectivePointsPerEdge(1.0));
        // pulse=false by default => always emit
        assertTrue(s.shouldEmit(0));
        assertTrue(s.shouldEmit(5));
        assertTrue(s.shouldEmit(7));
    }

    @Test
    void overridesAreRead() {
        CubeStep s = new CubeStep("cu", 20, Map.of(
            "points-per-edge", 8,
            "size", 3.0
        ));
        assertEquals(8, s.effectivePointsPerEdge(1.0));
        assertEquals(4, s.effectivePointsPerEdge(0.5));
        assertEquals(1, s.effectivePointsPerEdge(0.01));
    }

    @Test
    void pulseModeEmitsOnlyEveryTenTicks() {
        CubeStep s = new CubeStep("cu", 20, Map.of("pulse", true));
        assertTrue(s.shouldEmit(0));
        assertTrue(s.shouldEmit(10));
        assertTrue(s.shouldEmit(20));
        assertFalse(s.shouldEmit(5));
        assertFalse(s.shouldEmit(7));
        assertFalse(s.shouldEmit(11));
    }

    @Test
    void cullBucketShortCircuits() {
        CubeStep s = new CubeStep("cu", 20, Map.of());
        EffectContext ctx = mock(EffectContext.class);
        when(ctx.lodBucket()).thenReturn(BudgetManager.BUCKET_CULL);
        when(ctx.lodMultiplier()).thenReturn(1.0);
        assertDoesNotThrow(() -> s.tick(ctx, 0));
        verify(ctx, never()).origin();
    }
}

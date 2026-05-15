package com.cristian.particleforge.primitives;

import com.cristian.particleforge.engine.BudgetManager;
import com.cristian.particleforge.model.EffectContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShockwaveStepTest {

    @Test
    void defaultsAreApplied() {
        ShockwaveStep s = new ShockwaveStep("sw", 21, Map.of());
        assertEquals(0.5, s.currentRadius(0), 1e-9);
        assertEquals(6.0, s.currentRadius(20), 1e-9);
        assertEquals(32, s.effectivePointsPerTick(1.0));
    }

    @Test
    void radiusLerpsBetweenStartAndEnd() {
        ShockwaveStep s = new ShockwaveStep("sw", 21, Map.of(
            "start-radius", 1.0,
            "end-radius", 5.0
        ));
        assertEquals(1.0, s.currentRadius(0), 1e-9);
        assertEquals(5.0, s.currentRadius(20), 1e-9);
        // midpoint
        assertEquals(3.0, s.currentRadius(10), 1e-9);
    }

    @Test
    void effectivePointsScalesAndClamps() {
        ShockwaveStep s = new ShockwaveStep("sw", 21, Map.of("points-per-tick", 32));
        assertEquals(32, s.effectivePointsPerTick(1.0));
        assertEquals(16, s.effectivePointsPerTick(0.5));
        assertEquals(1, s.effectivePointsPerTick(0.01));
        assertEquals(0, s.effectivePointsPerTick(0.0));
    }

    @Test
    void cullBucketShortCircuits() {
        ShockwaveStep s = new ShockwaveStep("sw", 21, Map.of());
        EffectContext ctx = mock(EffectContext.class);
        when(ctx.lodBucket()).thenReturn(BudgetManager.BUCKET_CULL);
        when(ctx.lodMultiplier()).thenReturn(1.0);
        assertDoesNotThrow(() -> s.tick(ctx, 0));
        verify(ctx, never()).origin();
    }
}

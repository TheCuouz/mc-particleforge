package com.cristian.particleforge.primitives;

import com.cristian.particleforge.engine.BudgetManager;
import com.cristian.particleforge.model.EffectContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SphereStepTest {

    @Test
    void defaultsAreApplied() {
        SphereStep s = new SphereStep("sp", 20, Map.of());
        assertEquals(1.5, s.currentRadius(0), 1e-9);
        assertEquals(1.5, s.currentRadius(10), 1e-9); // expand=0
        assertEquals(64, s.effectivePoints(1.0));
    }

    @Test
    void radiusExpandsLinearly() {
        SphereStep s = new SphereStep("sp", 20, Map.of(
            "radius", 1.0,
            "expand-per-tick", 0.25
        ));
        assertEquals(1.0, s.currentRadius(0), 1e-9);
        assertEquals(1.25, s.currentRadius(1), 1e-9);
        assertEquals(3.5, s.currentRadius(10), 1e-9);
    }

    @Test
    void effectivePointsScales() {
        SphereStep s = new SphereStep("sp", 20, Map.of("points", 80));
        assertEquals(80, s.effectivePoints(1.0));
        assertEquals(40, s.effectivePoints(0.5));
        assertEquals(1, s.effectivePoints(0.001));
        assertEquals(0, s.effectivePoints(0.0));
    }

    @Test
    void cullBucketShortCircuits() {
        SphereStep s = new SphereStep("sp", 20, Map.of());
        EffectContext ctx = mock(EffectContext.class);
        when(ctx.lodBucket()).thenReturn(BudgetManager.BUCKET_CULL);
        when(ctx.lodMultiplier()).thenReturn(1.0);
        assertDoesNotThrow(() -> s.tick(ctx, 0));
        verify(ctx, never()).origin();
    }
}

package com.cristian.particleforge.primitives;

import com.cristian.particleforge.engine.BudgetManager;
import com.cristian.particleforge.model.EffectContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HelixStepTest {

    @Test
    void defaultsAndTParamEndpoints() {
        HelixStep s = new HelixStep("h", 20, Map.of());
        assertEquals(0.0, s.tParam(0), 1e-9);
        assertEquals(1.0, s.tParam(19), 1e-9);
        assertEquals(2, s.effectiveStrands(1.0));
    }

    @Test
    void strandOffsetMatchesSpec() {
        // radius=1, height=10, turns=2, strand 0 at t=0.5
        HelixStep s = new HelixStep("h", 21, Map.of(
            "radius", 1.0,
            "height", 10.0,
            "turns", 2.0,
            "strands", 2
        ));
        double[] off = s.strandOffset(0.5, 0, 2);
        // y = 10 * 0.5 = 5
        assertEquals(5.0, off[1], 1e-9);
        // x^2 + z^2 = radius^2 = 1
        double rxz = Math.sqrt(off[0] * off[0] + off[2] * off[2]);
        assertEquals(1.0, rxz, 1e-9);
    }

    @Test
    void strandsHavePhaseOffset() {
        HelixStep s = new HelixStep("h", 21, Map.of(
            "radius", 1.0,
            "height", 2.0,
            "turns", 1.0,
            "strands", 2
        ));
        // At t=0, strand 0 angle = 0 → (1,0,0); strand 1 phase = π → (-1,0,0)
        double[] a = s.strandOffset(0.0, 0, 2);
        double[] b = s.strandOffset(0.0, 1, 2);
        assertEquals(1.0, a[0], 1e-9);
        assertEquals(0.0, a[2], 1e-9);
        assertEquals(-1.0, b[0], 1e-9);
        assertEquals(0.0, b[2], 1e-9);
    }

    @Test
    void effectiveStrandsClampsAndCulls() {
        HelixStep s = new HelixStep("h", 10, Map.of("strands", 4));
        assertEquals(4, s.effectiveStrands(1.0));
        assertEquals(2, s.effectiveStrands(0.5));
        assertEquals(1, s.effectiveStrands(0.05));
        assertEquals(0, s.effectiveStrands(0.0));
    }

    @Test
    void cullBucketShortCircuits() {
        HelixStep s = new HelixStep("h", 20, Map.of());
        EffectContext ctx = mock(EffectContext.class);
        when(ctx.lodBucket()).thenReturn(BudgetManager.BUCKET_CULL);
        when(ctx.lodMultiplier()).thenReturn(1.0);
        assertDoesNotThrow(() -> s.tick(ctx, 0));
        verify(ctx, never()).origin();
    }
}

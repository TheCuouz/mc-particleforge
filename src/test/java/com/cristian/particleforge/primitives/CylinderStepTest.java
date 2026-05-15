package com.cristian.particleforge.primitives;

import com.cristian.particleforge.engine.BudgetManager;
import com.cristian.particleforge.model.EffectContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CylinderStepTest {

    @Test
    void defaultsAreApplied() {
        CylinderStep s = new CylinderStep("cy", 20, Map.of());
        assertEquals(5, s.effectiveRings(1.0));
        assertEquals(16, s.effectivePointsPerRing(1.0));
    }

    @Test
    void overridesAreRead() {
        CylinderStep s = new CylinderStep("cy", 20, Map.of(
            "rings", 9,
            "points-per-ring", 36,
            "radius", 2.0,
            "height", 6.0
        ));
        assertEquals(9, s.effectiveRings(1.0));
        assertEquals(36, s.effectivePointsPerRing(1.0));
        // sqrt(0.25) = 0.5 => rings 9*0.5 = 4.5 round 5? round(4.5) = 4 in HALF_EVEN; but Math.round half-up => 5
        // Just assert both scale by sqrt
        int er = s.effectiveRings(0.25);
        int ep = s.effectivePointsPerRing(0.25);
        assertEquals((int) Math.round(9 * 0.5), er);
        assertEquals((int) Math.round(36 * 0.5), ep);
    }

    @Test
    void ringHeightSpacing() {
        CylinderStep s = new CylinderStep("cy", 20, Map.of("height", 4.0, "rings", 5));
        // totalRings=1 => 0
        assertEquals(0.0, s.ringHeight(0, 1), 1e-9);
        // height=4, rings=5 => y at index 2 = 4*2/4 = 2
        assertEquals(2.0, s.ringHeight(2, 5), 1e-9);
        assertEquals(0.0, s.ringHeight(0, 5), 1e-9);
        assertEquals(4.0, s.ringHeight(4, 5), 1e-9);
    }

    @Test
    void cullBucketShortCircuits() {
        CylinderStep s = new CylinderStep("cy", 20, Map.of());
        EffectContext ctx = mock(EffectContext.class);
        when(ctx.lodBucket()).thenReturn(BudgetManager.BUCKET_CULL);
        when(ctx.lodMultiplier()).thenReturn(1.0);
        assertDoesNotThrow(() -> s.tick(ctx, 0));
        verify(ctx, never()).origin();
    }
}

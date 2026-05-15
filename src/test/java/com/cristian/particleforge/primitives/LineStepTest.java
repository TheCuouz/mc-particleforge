package com.cristian.particleforge.primitives;

import com.cristian.particleforge.engine.BudgetManager;
import com.cristian.particleforge.model.EffectContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LineStepTest {

    @Test
    void defaultsAreApplied() {
        LineStep s = new LineStep("ln", 20, Map.of());
        assertEquals(12, s.effectivePoints(1.0));
        assertArrayEquals(new double[]{0.0, 3.0, 0.0}, s.targetVector(), 1e-9);
    }

    @Test
    void overridesAreRead() {
        LineStep s = new LineStep("ln", 20, Map.of(
            "points", 20,
            "target-relative", "2,4,6"
        ));
        assertEquals(20, s.effectivePoints(1.0));
        assertEquals(10, s.effectivePoints(0.5));
        assertArrayEquals(new double[]{2.0, 4.0, 6.0}, s.targetVector(), 1e-9);
    }

    @Test
    void targetVectorParsesWhitespaceAndDecimals() {
        LineStep s = new LineStep("ln", 20, Map.of("target-relative", "1.5, -2, 0.25"));
        assertArrayEquals(new double[]{1.5, -2.0, 0.25}, s.targetVector(), 1e-9);
    }

    @Test
    void targetVectorFallsBackOnGarbage() {
        LineStep s = new LineStep("ln", 20, Map.of("target-relative", "abc"));
        assertArrayEquals(new double[]{0.0, 3.0, 0.0}, s.targetVector(), 1e-9);
    }

    @Test
    void linePointsEdgeCases() {
        // verify Geometry produces the expected endpoints/midpoint behavior we rely on
        assertEquals(1, Geometry.linePoints(0, 3, 0, 1).size());
        assertEquals(2, Geometry.linePoints(0, 3, 0, 2).size());
        // points=1 => midpoint
        double[] mid = Geometry.linePoints(0, 4, 0, 1).get(0);
        assertEquals(2.0, mid[1], 1e-9);
        // points=2 => endpoints
        var two = Geometry.linePoints(0, 4, 0, 2);
        assertEquals(0.0, two.get(0)[1], 1e-9);
        assertEquals(4.0, two.get(1)[1], 1e-9);
    }

    @Test
    void cullBucketShortCircuits() {
        LineStep s = new LineStep("ln", 20, Map.of());
        EffectContext ctx = mock(EffectContext.class);
        when(ctx.lodBucket()).thenReturn(BudgetManager.BUCKET_CULL);
        when(ctx.lodMultiplier()).thenReturn(1.0);
        assertDoesNotThrow(() -> s.tick(ctx, 0));
        verify(ctx, never()).origin();
    }
}

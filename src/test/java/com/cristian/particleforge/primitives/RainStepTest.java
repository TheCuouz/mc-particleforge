package com.cristian.particleforge.primitives;

import com.cristian.particleforge.engine.BudgetManager;
import com.cristian.particleforge.model.EffectContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RainStepTest {

    @Test
    void defaultsAreApplied() {
        RainStep s = new RainStep("r", 20, Map.of());
        assertEquals(4, s.effectiveDensity(1.0));
    }

    @Test
    void overridesAreRead() {
        RainStep s = new RainStep("r", 20, Map.of(
            "density-per-tick", 12,
            "radius", 5.0,
            "height", 6.0
        ));
        assertEquals(12, s.effectiveDensity(1.0));
        assertEquals(6, s.effectiveDensity(0.5));
    }

    @Test
    void zeroDensityReturnsZero() {
        RainStep s = new RainStep("r", 20, Map.of("density-per-tick", 0));
        assertEquals(0, s.effectiveDensity(1.0));
    }

    @Test
    void randomOffsetIsDeterministicAndWithinBounds() {
        RainStep s = new RainStep("r", 20, Map.of("radius", 3.0, "height", 4.0));
        double[] a = s.randomOffsetAt(5, 2);
        double[] b = s.randomOffsetAt(5, 2);
        assertArrayEquals(a, b, 1e-12);
        // bounds: sqrt(x^2 + z^2) <= radius; 0 <= y <= height
        double r = Math.sqrt(a[0] * a[0] + a[2] * a[2]);
        assertTrue(r <= 3.0 + 1e-9);
        assertTrue(a[1] >= 0.0 && a[1] <= 4.0 + 1e-9);
    }

    @Test
    void cullBucketShortCircuits() {
        RainStep s = new RainStep("r", 20, Map.of());
        EffectContext ctx = mock(EffectContext.class);
        when(ctx.lodBucket()).thenReturn(BudgetManager.BUCKET_CULL);
        when(ctx.lodMultiplier()).thenReturn(1.0);
        assertDoesNotThrow(() -> s.tick(ctx, 0));
        verify(ctx, never()).origin();
    }
}

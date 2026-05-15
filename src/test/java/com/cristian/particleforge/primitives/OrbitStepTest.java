package com.cristian.particleforge.primitives;

import com.cristian.particleforge.engine.BudgetManager;
import com.cristian.particleforge.model.EffectContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrbitStepTest {

    @Test
    void defaultsAreApplied() {
        OrbitStep s = new OrbitStep("o", 20, Map.of());
        assertEquals(3, s.effectivePoints(1.0));
    }

    @Test
    void slotZeroAtTickZeroIsOnPositiveX() {
        // radius=1, turns=1, duration=20, slot 0 of 4 → angle=0 → (1,0,0)
        OrbitStep s = new OrbitStep("o", 20, Map.of(
            "radius", 1.0, "turns", 1.0, "points", 4));
        double[] off = s.slotOffset(0, 0, 4);
        assertEquals(1.0, off[0], 1e-9);
        assertEquals(0.0, off[1], 1e-9);
        assertEquals(0.0, off[2], 1e-9);
    }

    @Test
    void halfDurationWithOneTurnRotates180Degrees() {
        // duration=21 so (duration-1)=20; t = 10/20 = 0.5 → turns*t = 0.5 rev.
        // For slot 0 of 4: angle = 2π * (0 + 1 * 0.5) = π → (cos π, 0, sin π) = (-1, 0, 0)
        OrbitStep s = new OrbitStep("o", 21, Map.of(
            "radius", 1.0, "turns", 1.0, "points", 4));
        double[] off = s.slotOffset(10, 0, 4);
        assertEquals(-1.0, off[0], 1e-9);
        assertEquals(0.0, off[1], 1e-9);
        assertEquals(0.0, off[2], 1e-9);
    }

    @Test
    void slotsAreEvenlySpaced() {
        // 4 slots at tick 0 → angles 0, π/2, π, 3π/2
        OrbitStep s = new OrbitStep("o", 20, Map.of(
            "radius", 1.0, "turns", 1.0, "points", 4));
        double[] s0 = s.slotOffset(0, 0, 4);
        double[] s1 = s.slotOffset(0, 1, 4);
        double[] s2 = s.slotOffset(0, 2, 4);
        double[] s3 = s.slotOffset(0, 3, 4);
        assertEquals(1.0, s0[0], 1e-9);
        assertEquals(1.0, s1[2], 1e-9);  // sin(π/2) = 1
        assertEquals(-1.0, s2[0], 1e-9);
        assertEquals(-1.0, s3[2], 1e-9);
    }

    @Test
    void cullBucketShortCircuits() {
        OrbitStep s = new OrbitStep("o", 20, Map.of());
        EffectContext ctx = mock(EffectContext.class);
        when(ctx.lodBucket()).thenReturn(BudgetManager.BUCKET_CULL);
        when(ctx.lodMultiplier()).thenReturn(1.0);
        assertDoesNotThrow(() -> s.tick(ctx, 0));
        verify(ctx, never()).origin();
    }
}

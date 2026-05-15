package com.cristian.particleforge.primitives;

import com.cristian.particleforge.engine.BudgetManager;
import com.cristian.particleforge.model.EffectContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConeStepTest {

    @Test
    void defaultsAreApplied() {
        ConeStep s = new ConeStep("c", 20, Map.of());
        assertEquals(6, s.effectivePointsPerTick(1.0));
        // default direction = "forward" => (0, 0)
        double[] yp = s.axisYawPitch();
        assertEquals(0.0, yp[0], 1e-9);
        assertEquals(0.0, yp[1], 1e-9);
    }

    @Test
    void overridesAreRead() {
        ConeStep s = new ConeStep("c", 20, Map.of(
            "points-per-tick", 10,
            "angle-deg", 30.0,
            "length", 2.0,
            "direction", "up"
        ));
        assertEquals(10, s.effectivePointsPerTick(1.0));
        double[] yp = s.axisYawPitch();
        assertEquals(0.0, yp[0], 1e-9);
        assertEquals(-90.0, yp[1], 1e-9);

        // coneOffsetAt(0, 0) is the tip => zeros
        double[] tip = s.coneOffsetAt(0.0, 0.0);
        assertEquals(0.0, tip[0], 1e-9);
        assertEquals(0.0, tip[1], 1e-9);
        assertEquals(0.0, tip[2], 1e-9);

        // coneOffsetAt(1, 0): r = tan(30°)*2, z = 2
        double[] base = s.coneOffsetAt(1.0, 0.0);
        double expectedR = Math.tan(Math.toRadians(30.0)) * 2.0;
        assertEquals(expectedR, base[0], 1e-9);
        assertEquals(0.0, base[1], 1e-9);
        assertEquals(2.0, base[2], 1e-9);
    }

    @Test
    void axisYawPitchForNamedDirections() {
        assertArrayEquals(new double[]{0.0, 0.0},
            new ConeStep("c", 20, Map.of("direction", "forward")).axisYawPitch(), 1e-9);
        assertArrayEquals(new double[]{0.0, -90.0},
            new ConeStep("c", 20, Map.of("direction", "up")).axisYawPitch(), 1e-9);
        assertArrayEquals(new double[]{0.0, 90.0},
            new ConeStep("c", 20, Map.of("direction", "down")).axisYawPitch(), 1e-9);
        // "1,0,0" => yaw=atan2(1,0)*180/pi=90, pitch=0
        double[] yp = new ConeStep("c", 20, Map.of("direction", "1,0,0")).axisYawPitch();
        assertEquals(90.0, yp[0], 1e-9);
        assertEquals(0.0, yp[1], 1e-9);
    }

    @Test
    void degenerateVectorFallsBackToForward() {
        double[] yp = new ConeStep("c", 20, Map.of("direction", "0,0,0")).axisYawPitch();
        assertEquals(0.0, yp[0], 1e-9);
        assertEquals(0.0, yp[1], 1e-9);
    }

    @Test
    void cullBucketShortCircuits() {
        ConeStep s = new ConeStep("c", 20, Map.of());
        EffectContext ctx = mock(EffectContext.class);
        when(ctx.lodBucket()).thenReturn(BudgetManager.BUCKET_CULL);
        when(ctx.lodMultiplier()).thenReturn(1.0);
        assertDoesNotThrow(() -> s.tick(ctx, 0));
        verify(ctx, never()).origin();
    }
}

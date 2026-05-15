package com.cristian.particleforge.primitives;

import com.cristian.particleforge.engine.BudgetManager;
import com.cristian.particleforge.model.EffectContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShapeStepTest {

    @Test
    void parseVerticesAcceptsStringList() {
        List<double[]> vs = ShapeStep.parseVertices(List.of("0,0", "1,0", "1,1", "0,1"));
        assertEquals(4, vs.size());
        assertArrayEquals(new double[]{0.0, 0.0}, vs.get(0), 1e-9);
        assertArrayEquals(new double[]{1.0, 0.0}, vs.get(1), 1e-9);
        assertArrayEquals(new double[]{1.0, 1.0}, vs.get(2), 1e-9);
        assertArrayEquals(new double[]{0.0, 1.0}, vs.get(3), 1e-9);
    }

    @Test
    void parseVerticesAcceptsListOfLists() {
        List<double[]> vs = ShapeStep.parseVertices(List.of(
            List.of(0.0, 0.0),
            List.of(2.0, 0.0),
            List.of(1.0, 1.5)
        ));
        assertEquals(3, vs.size());
        assertArrayEquals(new double[]{2.0, 0.0}, vs.get(1), 1e-9);
    }

    @Test
    void parseVerticesSkipsGarbage() {
        List<double[]> vs = ShapeStep.parseVertices(List.of("0,0", "abc", "1,1"));
        assertEquals(2, vs.size());
    }

    @Test
    void parseVerticesNullReturnsEmpty() {
        assertTrue(ShapeStep.parseVertices(null).isEmpty());
    }

    @Test
    void emitPointsClosedSquareYieldsFourEdges() {
        ShapeStep s = new ShapeStep("sh", 20, Map.of(
            "vertices", List.of("0,0", "1,0", "1,1", "0,1"),
            "points-per-edge", 5,
            "closed", true
        ));
        // 4 edges * 5 points = 20
        assertEquals(20, s.emitPoints(5).size());
    }

    @Test
    void emitPointsOpenPolygonYieldsNMinus1Edges() {
        ShapeStep s = new ShapeStep("sh", 20, Map.of(
            "vertices", List.of("0,0", "1,0", "1,1", "0,1"),
            "points-per-edge", 5,
            "closed", false
        ));
        // 3 edges * 5 points = 15
        assertEquals(15, s.emitPoints(5).size());
    }

    @Test
    void fewerThanTwoVerticesIsNoOp() {
        ShapeStep s = new ShapeStep("sh", 20, Map.of(
            "vertices", List.of("0,0"),
            "points-per-edge", 5
        ));
        assertTrue(s.emitPoints(5).isEmpty());
    }

    @Test
    void zeroVerticesIsNoOp() {
        ShapeStep s = new ShapeStep("sh", 20, Map.of("points-per-edge", 5));
        assertTrue(s.emitPoints(5).isEmpty());
    }

    @Test
    void effectivePointsPerEdgeScales() {
        ShapeStep s = new ShapeStep("sh", 20, Map.of("points-per-edge", 10));
        assertEquals(10, s.effectivePointsPerEdge(1.0));
        assertEquals(5, s.effectivePointsPerEdge(0.5));
        assertEquals(1, s.effectivePointsPerEdge(0.01)); // clamp
        assertEquals(0, s.effectivePointsPerEdge(0.0));
    }

    @Test
    void cullBucketShortCircuits() {
        ShapeStep s = new ShapeStep("sh", 20, Map.of(
            "vertices", List.of("0,0", "1,0", "1,1")));
        EffectContext ctx = mock(EffectContext.class);
        when(ctx.lodBucket()).thenReturn(BudgetManager.BUCKET_CULL);
        when(ctx.lodMultiplier()).thenReturn(1.0);
        assertDoesNotThrow(() -> s.tick(ctx, 0));
        verify(ctx, never()).origin();
    }
}

package com.cristian.particleforge.primitives;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeometryTest {

    private static final double EPS = 1e-9;

    @Test
    void ringPoints_flat_returns_n_points_on_radius() {
        List<double[]> pts = Geometry.ringPoints(2.0, 8, 0.0, 0.0);
        assertEquals(8, pts.size());
        for (double[] p : pts) {
            assertEquals(0.0, p[1], EPS);
            double dist = Math.sqrt(p[0] * p[0] + p[2] * p[2]);
            assertEquals(2.0, dist, EPS);
        }
    }

    @Test
    void ringPoints_yaw90_rotates_plusX_to_minusZ() {
        List<double[]> pts = Geometry.ringPoints(2.0, 8, 90.0, 0.0);
        // First point originally (2,0,0); yaw=90 -> (0,0,-2)
        assertArrayEquals(new double[]{0.0, 0.0, -2.0}, pts.get(0), EPS);
    }

    @Test
    void spherePoints_100_points_at_radius_1() {
        List<double[]> pts = Geometry.spherePoints(1.0, 100);
        assertEquals(100, pts.size());
        for (double[] p : pts) {
            double dist = Math.sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]);
            assertEquals(1.0, dist, EPS);
        }
    }

    @Test
    void spherePoints_single_point_at_radius_1() {
        List<double[]> pts = Geometry.spherePoints(1.0, 1);
        assertEquals(1, pts.size());
        double[] p = pts.get(0);
        double dist = Math.sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]);
        assertEquals(1.0, dist, EPS);
    }

    @Test
    void helixPoint_midpoint_height_and_radius() {
        List<double[]> pts = Geometry.helixPoint(1.0, 10.0, 2.0, 0.5);
        assertEquals(1, pts.size());
        double[] p = pts.get(0);
        assertEquals(5.0, p[1], EPS);
        double r = Math.sqrt(p[0] * p[0] + p[2] * p[2]);
        assertEquals(1.0, r, EPS);
    }

    @Test
    void helixPoint_start_is_unit_x() {
        List<double[]> pts = Geometry.helixPoint(1.0, 10.0, 2.0, 0.0);
        assertArrayEquals(new double[]{1.0, 0.0, 0.0}, pts.get(0), EPS);
    }

    @Test
    void linePoints_11_evenly_spaced() {
        List<double[]> pts = Geometry.linePoints(1.0, 0.0, 0.0, 11);
        assertEquals(11, pts.size());
        assertArrayEquals(new double[]{0.5, 0.0, 0.0}, pts.get(5), EPS);
        assertArrayEquals(new double[]{0.0, 0.0, 0.0}, pts.get(0), EPS);
        assertArrayEquals(new double[]{1.0, 0.0, 0.0}, pts.get(10), EPS);
    }

    @Test
    void linePoints_zero_vector_all_zero() {
        List<double[]> pts = Geometry.linePoints(0.0, 0.0, 0.0, 5);
        assertEquals(5, pts.size());
        for (double[] p : pts) {
            assertArrayEquals(new double[]{0.0, 0.0, 0.0}, p, EPS);
        }
    }

    @Test
    void linePoints_single_point_is_midpoint() {
        List<double[]> pts = Geometry.linePoints(1.0, 0.0, 0.0, 1);
        assertEquals(1, pts.size());
        assertArrayEquals(new double[]{0.5, 0.0, 0.0}, pts.get(0), EPS);
    }

    @Test
    void rotate_yaw90_plusX_to_minusZ() {
        double[] r = Geometry.rotate(new double[]{1.0, 0.0, 0.0}, 90.0, 0.0);
        assertArrayEquals(new double[]{0.0, 0.0, -1.0}, r, EPS);
    }

    @Test
    void rotate_pitch90_plusY_to_plusZ() {
        double[] r = Geometry.rotate(new double[]{0.0, 1.0, 0.0}, 0.0, 90.0);
        assertArrayEquals(new double[]{0.0, 0.0, 1.0}, r, EPS);
    }

    @Test
    void rotate_identity() {
        double[] r = Geometry.rotate(new double[]{1.0, 0.0, 0.0}, 0.0, 0.0);
        assertArrayEquals(new double[]{1.0, 0.0, 0.0}, r, EPS);
    }

    @Test
    void cubeWireframe_size2_ppe4_has_48_points_on_edges() {
        List<double[]> pts = Geometry.cubeWireframe(2.0, 4);
        assertEquals(48, pts.size());
        for (double[] p : pts) {
            int onFace = 0;
            if (Math.abs(Math.abs(p[0]) - 1.0) < EPS) onFace++;
            if (Math.abs(Math.abs(p[1]) - 1.0) < EPS) onFace++;
            if (Math.abs(Math.abs(p[2]) - 1.0) < EPS) onFace++;
            assertTrue(onFace >= 2,
                "Point should have >= 2 coords at +/-1: ("
                    + p[0] + "," + p[1] + "," + p[2] + ")");
        }
    }

    @Test
    void cubeWireframe_ppe1_returns_12_midpoints() {
        List<double[]> pts = Geometry.cubeWireframe(2.0, 1);
        assertEquals(12, pts.size());
    }

    @Test
    void cylinderRing_at_height_5() {
        List<double[]> pts = Geometry.cylinderRing(1.0, 5.0, 8);
        assertEquals(8, pts.size());
        for (double[] p : pts) {
            assertEquals(5.0, p[1], EPS);
            double r = Math.sqrt(p[0] * p[0] + p[2] * p[2]);
            assertEquals(1.0, r, EPS);
        }
    }

    @Test
    void shockwaveRing_flat_at_radius() {
        List<double[]> pts = Geometry.shockwaveRing(3.0, 12);
        assertEquals(12, pts.size());
        for (double[] p : pts) {
            assertEquals(0.0, p[1], EPS);
            double r = Math.sqrt(p[0] * p[0] + p[2] * p[2]);
            assertEquals(3.0, r, EPS);
        }
    }

    @Test
    void conePoint_mouth_45deg() {
        List<double[]> pts = Geometry.conePoint(45.0, 2.0, 1.0, 0.0);
        assertEquals(1, pts.size());
        assertArrayEquals(new double[]{2.0, 0.0, 2.0}, pts.get(0), EPS);
    }

    @Test
    void conePoint_apex_is_origin() {
        List<double[]> pts = Geometry.conePoint(45.0, 2.0, 0.0, 0.0);
        assertEquals(1, pts.size());
        assertArrayEquals(new double[]{0.0, 0.0, 0.0}, pts.get(0), EPS);
    }
}

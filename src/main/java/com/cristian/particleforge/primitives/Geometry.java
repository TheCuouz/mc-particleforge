package com.cristian.particleforge.primitives;

import java.util.ArrayList;
import java.util.List;

/**
 * Stateless math helpers for particle primitives. Every method returns offsets
 * from the origin (0,0,0); primitives add the origin location at spawn time.
 *
 * Conventions:
 *   - Coordinates use Minecraft's left-handed axis: +X east, +Y up, +Z south.
 *   - Yaw rotates around Y (degrees, 0 = +X). Pitch rotates around the X-axis
 *     (degrees, 0 = horizontal, positive pitches the +Z direction down).
 *   - Returned points are [dx, dy, dz].
 */
public final class Geometry {

    private Geometry() {}

    /**
     * Flat ring in the XZ plane centered at origin, optionally rotated by
     * yaw (around Y) then pitch (around X). Returns {@code points} points
     * evenly spaced around the ring.
     */
    public static List<double[]> ringPoints(double radius, int points, double yawDeg, double pitchDeg) {
        if (points <= 0) return List.of();
        List<double[]> out = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double theta = 2.0 * Math.PI * (i / (double) points);
            double[] p = new double[]{
                Math.cos(theta) * radius,
                0.0,
                Math.sin(theta) * radius
            };
            out.add(rotate(p, yawDeg, pitchDeg));
        }
        return out;
    }

    /**
     * Hollow sphere via Fibonacci lattice -- avoids pole clustering of naive
     * lat/lon sampling.
     */
    public static List<double[]> spherePoints(double radius, int points) {
        if (points <= 0) return List.of();
        List<double[]> out = new ArrayList<>(points);
        double phi = Math.PI * (Math.sqrt(5.0) - 1.0); // golden angle
        double denom = points == 1 ? 1.0 : (double) (points - 1);
        for (int i = 0; i < points; i++) {
            double y = 1.0 - 2.0 * (i / denom);
            double r = Math.sqrt(Math.max(0.0, 1.0 - y * y));
            double theta = phi * i;
            out.add(new double[]{
                Math.cos(theta) * r * radius,
                y * radius,
                Math.sin(theta) * r * radius
            });
        }
        return out;
    }

    /**
     * One point of a vertical helix at parametric position {@code t in [0,1]}.
     * The helix has the given radius and total height; total angular sweep
     * is {@code turns * 2*PI}. Returns a single-element list for primitives
     * that emit one point per tick.
     */
    public static List<double[]> helixPoint(double radius, double height, double turns, double t) {
        double angle = 2.0 * Math.PI * turns * t;
        return List.of(new double[]{
            Math.cos(angle) * radius,
            height * t,
            Math.sin(angle) * radius
        });
    }

    /**
     * Wireframe cube of side {@code size} centered at origin, with
     * {@code pointsPerEdge} points along each of the 12 edges. Total points =
     * 12 * pointsPerEdge.
     */
    public static List<double[]> cubeWireframe(double size, int pointsPerEdge) {
        if (pointsPerEdge <= 0) return List.of();
        double h = size / 2.0;
        double[][] verts = new double[][]{
            {-h, -h, -h}, { h, -h, -h}, { h, -h,  h}, {-h, -h,  h},
            {-h,  h, -h}, { h,  h, -h}, { h,  h,  h}, {-h,  h,  h}
        };
        int[][] edges = new int[][]{
            {0,1},{1,2},{2,3},{3,0},
            {4,5},{5,6},{6,7},{7,4},
            {0,4},{1,5},{2,6},{3,7}
        };
        List<double[]> out = new ArrayList<>(12 * pointsPerEdge);
        for (int[] e : edges) {
            double[] a = verts[e[0]];
            double[] b = verts[e[1]];
            for (int i = 0; i < pointsPerEdge; i++) {
                double t = (pointsPerEdge == 1) ? 0.5 : (i / (double)(pointsPerEdge - 1));
                out.add(new double[]{
                    lerp(a[0], b[0], t),
                    lerp(a[1], b[1], t),
                    lerp(a[2], b[2], t)
                });
            }
        }
        return out;
    }

    /**
     * Single ring at height {@code y} (relative to origin) with {@code points}
     * evenly spaced. Used by CylinderStep to stack rings.
     */
    public static List<double[]> cylinderRing(double radius, double y, int points) {
        if (points <= 0) return List.of();
        List<double[]> out = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double theta = 2.0 * Math.PI * (i / (double) points);
            out.add(new double[]{
                Math.cos(theta) * radius,
                y,
                Math.sin(theta) * radius
            });
        }
        return out;
    }

    /**
     * One point on the surface of a cone. {@code angleDeg} is the half-angle
     * from the axis, {@code length} is axial length, {@code t} in [0,1] is
     * axial position, {@code rotationT} in [0,1) is rotation around the axis.
     * Cone axis points along +Z by convention; callers rotate as needed.
     */
    public static List<double[]> conePoint(double angleDeg, double length, double t, double rotationT) {
        double r = Math.tan(Math.toRadians(angleDeg)) * length * t;
        double theta = 2.0 * Math.PI * rotationT;
        return List.of(new double[]{
            Math.cos(theta) * r,
            Math.sin(theta) * r,
            length * t
        });
    }

    /**
     * Flat ring at y=0 used for SHOCKWAVE primitives.
     */
    public static List<double[]> shockwaveRing(double radius, int points) {
        if (points <= 0) return List.of();
        return ringPoints(radius, points, 0.0, 0.0);
    }

    /**
     * Evenly-spaced points along a line segment from origin to (dx,dy,dz).
     * Endpoints included.
     */
    public static List<double[]> linePoints(double dx, double dy, double dz, int points) {
        if (points <= 0) return List.of();
        if (points == 1) return List.of(new double[]{dx * 0.5, dy * 0.5, dz * 0.5});
        List<double[]> out = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double t = i / (double) (points - 1);
            out.add(new double[]{dx * t, dy * t, dz * t});
        }
        return out;
    }

    /**
     * Rotate a point by yaw (around Y) then pitch (around X). Degrees.
     * Yaw=90 maps +X to -Z.
     */
    public static double[] rotate(double[] xyz, double yawDeg, double pitchDeg) {
        double y = Math.toRadians(yawDeg);
        double p = Math.toRadians(pitchDeg);
        double cosY = Math.cos(y), sinY = Math.sin(y);
        double cosP = Math.cos(p), sinP = Math.sin(p);

        // Yaw around Y: (x,y,z) -> (x*cosY + z*sinY, y, -x*sinY + z*cosY)
        double x1 = xyz[0] * cosY + xyz[2] * sinY;
        double y1 = xyz[1];
        double z1 = -xyz[0] * sinY + xyz[2] * cosY;

        // Pitch around X: (x,y,z) -> (x, y*cosP - z*sinP, y*sinP + z*cosP)
        double x2 = x1;
        double y2 = y1 * cosP - z1 * sinP;
        double z2 = y1 * sinP + z1 * cosP;

        return new double[]{x2, y2, z2};
    }

    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }
}

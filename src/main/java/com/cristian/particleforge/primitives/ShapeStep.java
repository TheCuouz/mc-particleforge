package com.cristian.particleforge.primitives;

import com.cristian.particleforge.engine.BudgetManager;
import com.cristian.particleforge.model.EffectContext;
import com.cristian.particleforge.model.EffectStep;
import org.bukkit.Location;
import org.bukkit.Particle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Arbitrary 2D polygon (in the XZ plane, optionally rotated/translated by
 * yaw/pitch) defined by vertices. Each edge is densified with
 * {@code points-per-edge} points.
 *
 * <h2>Params</h2>
 * <ul>
 *   <li>{@code particle} (string) — default {@code END_ROD}.</li>
 *   <li>{@code count} (int) — per point; default {@code 1}.</li>
 *   <li>{@code vertices} (list of {@code "x,z"} strings, or list of {@code [x,z]} lists) — default empty.</li>
 *   <li>{@code points-per-edge} (int) — default {@code 8}.</li>
 *   <li>{@code y-offset} (double) — Y of the polygon plane relative to origin; default {@code 0}.</li>
 *   <li>{@code closed} (boolean) — connect last vertex back to first; default {@code true}.</li>
 *   <li>{@code yaw-deg} (double) — default {@code 0}.</li>
 *   <li>{@code pitch-deg} (double) — default {@code 0}.</li>
 * </ul>
 *
 * <h2>LOD</h2>
 * Scales {@code points-per-edge}. Bucket {@link BudgetManager#BUCKET_CULL} → no emit.
 */
public final class ShapeStep implements EffectStep {

    private final String id;
    private final int durationTicks;
    private final Particle particle;
    private final int count;
    private final List<double[]> vertices;
    private final int pointsPerEdge;
    private final double yOffset;
    private final boolean closed;
    private final double yawDeg;
    private final double pitchDeg;
    private final double speed;
    private final double spread;

    public ShapeStep(String id, int durationTicks, Map<String, Object> params) {
        this.id = id;
        this.durationTicks = durationTicks;
        this.particle      = ParamUtil.particle(params, "particle", Particle.END_ROD);
        this.count         = ParamUtil.intVal(params, "count", 1);
        this.vertices      = parseVertices(params.get("vertices"));
        this.pointsPerEdge = ParamUtil.intVal(params, "points-per-edge", 8);
        this.yOffset       = ParamUtil.doubleVal(params, "y-offset", 0.0);
        this.closed        = ParamUtil.boolVal(params, "closed", true);
        this.yawDeg        = ParamUtil.doubleVal(params, "yaw-deg", 0.0);
        this.pitchDeg      = ParamUtil.doubleVal(params, "pitch-deg", 0.0);
        this.speed         = ParamUtil.doubleVal(params, "speed", 0.0);
        this.spread        = ParamUtil.doubleVal(params, "spread", 0.0);
    }

    @Override public String id() { return id; }
    @Override public int durationTicks() { return durationTicks; }

    @Override
    public void tick(EffectContext ctx, int tickInStep) {
        if (ctx.lodBucket() >= BudgetManager.BUCKET_CULL || ctx.lodMultiplier() <= 0.0) return;
        if (vertices.size() < 2) return;
        Location origin = ctx.origin();
        if (origin == null) return;
        int effPpe = effectivePointsPerEdge(ctx.lodMultiplier());
        List<double[]> pts = emitPoints(effPpe);
        for (double[] off : pts) {
            Location at = origin.clone().add(off[0], off[1], off[2]);
            SpawnUtil.spawn(at, ctx, particle, count, spread, speed);
        }
    }

    /** Package-private for tests. */
    int effectivePointsPerEdge(double lodMul) {
        if (lodMul <= 0.0) return 0;
        return Math.max(1, (int) Math.round(pointsPerEdge * lodMul));
    }

    /**
     * Package-private for tests. Accepts the YAML form: a list of {@code "x,z"}
     * strings OR a list of lists {@code [[x,z], [x,z], ...]}. Returns vertices
     * as {@code [x, z]} pairs. Garbage entries are skipped.
     */
    static List<double[]> parseVertices(Object raw) {
        List<double[]> out = new ArrayList<>();
        if (!(raw instanceof List<?> list)) return out;
        for (Object item : list) {
            double[] v = parseVertex(item);
            if (v != null) out.add(v);
        }
        return out;
    }

    private static double[] parseVertex(Object item) {
        if (item instanceof String s) {
            String[] parts = s.split(",");
            if (parts.length != 2) return null;
            try {
                return new double[]{
                    Double.parseDouble(parts[0].trim()),
                    Double.parseDouble(parts[1].trim())
                };
            } catch (NumberFormatException ex) { return null; }
        }
        if (item instanceof List<?> l && l.size() == 2) {
            try {
                double x = ((Number) l.get(0)).doubleValue();
                double z = ((Number) l.get(1)).doubleValue();
                return new double[]{x, z};
            } catch (ClassCastException | NullPointerException ex) { return null; }
        }
        return null;
    }

    /** Package-private for tests. Returns all polygon-outline offsets, rotated. */
    List<double[]> emitPoints(int effectivePointsPerEdge) {
        List<double[]> out = new ArrayList<>();
        if (vertices.size() < 2 || effectivePointsPerEdge <= 0) return out;
        int n = vertices.size();
        int edgeCount = closed ? n : n - 1;
        for (int i = 0; i < edgeCount; i++) {
            double[] a = vertices.get(i);
            double[] b = vertices.get((i + 1) % n);
            double dx = b[0] - a[0];
            double dz = b[1] - a[1];
            List<double[]> line = Geometry.linePoints(dx, 0.0, dz, effectivePointsPerEdge);
            for (double[] p : line) {
                double[] world = new double[]{
                    a[0] + p[0],
                    yOffset + p[1],
                    a[1] + p[2]
                };
                out.add(Geometry.rotate(world, yawDeg, pitchDeg));
            }
        }
        return out;
    }
}

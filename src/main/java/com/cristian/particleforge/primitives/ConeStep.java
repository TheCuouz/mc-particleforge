package com.cristian.particleforge.primitives;

import com.cristian.particleforge.engine.BudgetManager;
import com.cristian.particleforge.model.EffectContext;
import com.cristian.particleforge.model.EffectStep;
import org.bukkit.Location;
import org.bukkit.Particle;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Cone of particles emanating from origin along a configurable axis.
 *
 * <h2>Params</h2>
 * <ul>
 *   <li>{@code particle} (string) — default {@code FLAME}.</li>
 *   <li>{@code count} (int) — per emitted point; default {@code 1}.</li>
 *   <li>{@code points-per-tick} (int) — default {@code 6}.</li>
 *   <li>{@code direction} (string) — {@code "forward"|"up"|"down"|"x,y,z"}; default {@code "forward"}.</li>
 *   <li>{@code angle-deg} (double) — half-angle from axis; default {@code 20}.</li>
 *   <li>{@code length} (double) — axial length; default {@code 4.0}.</li>
 *   <li>{@code speed} (double) — default {@code 0}.</li>
 *   <li>{@code spread} (double) — default {@code 0}.</li>
 * </ul>
 *
 * <h2>PRNG</h2>
 * Per-tick stateless PRNG seeded from {@code id.hashCode() ^ tickInStep} —
 * deterministic across runs, no per-step mutable random state.
 */
public final class ConeStep implements EffectStep {

    private final String id;
    private final int durationTicks;
    private final Particle particle;
    private final int count;
    private final int pointsPerTick;
    private final double angleDeg;
    private final double length;
    private final double speed;
    private final double spread;
    private final double axisYaw;
    private final double axisPitch;

    public ConeStep(String id, int durationTicks, Map<String, Object> params) {
        this.id = id;
        this.durationTicks = durationTicks;
        this.particle      = ParamUtil.particle(params, "particle", Particle.FLAME);
        this.count         = ParamUtil.intVal(params, "count", 1);
        this.pointsPerTick = ParamUtil.intVal(params, "points-per-tick", 6);
        this.angleDeg      = ParamUtil.doubleVal(params, "angle-deg", 20.0);
        this.length        = ParamUtil.doubleVal(params, "length", 4.0);
        this.speed         = ParamUtil.doubleVal(params, "speed", 0.0);
        this.spread        = ParamUtil.doubleVal(params, "spread", 0.0);

        String dir = ParamUtil.stringVal(params, "direction", "forward");
        double[] yp = parseDirection(dir);
        this.axisYaw   = yp[0];
        this.axisPitch = yp[1];
    }

    private static double[] parseDirection(String dir) {
        if (dir == null) return new double[]{0.0, 0.0};
        String d = dir.trim().toLowerCase(java.util.Locale.ROOT);
        switch (d) {
            case "forward": return new double[]{0.0, 0.0};
            case "up":      return new double[]{0.0, -90.0};
            case "down":    return new double[]{0.0, 90.0};
            default:
                String[] parts = d.split(",");
                if (parts.length != 3) return new double[]{0.0, 0.0};
                try {
                    double x = Double.parseDouble(parts[0].trim());
                    double y = Double.parseDouble(parts[1].trim());
                    double z = Double.parseDouble(parts[2].trim());
                    double len = Math.sqrt(x * x + y * y + z * z);
                    if (len < 1e-9) return new double[]{0.0, 0.0};
                    double dx = x / len, dy = y / len, dz = z / len;
                    double yawDeg = Math.atan2(dx, dz) * 180.0 / Math.PI;
                    double pitchDeg = -Math.asin(Math.max(-1.0, Math.min(1.0, dy))) * 180.0 / Math.PI;
                    return new double[]{yawDeg, pitchDeg};
                } catch (NumberFormatException ex) {
                    return new double[]{0.0, 0.0};
                }
        }
    }

    @Override public String id() { return id; }
    @Override public int durationTicks() { return durationTicks; }

    @Override
    public void tick(EffectContext ctx, int tickInStep) {
        if (ctx.lodBucket() >= BudgetManager.BUCKET_CULL || ctx.lodMultiplier() <= 0.0) return;
        Location origin = ctx.origin();
        if (origin == null) return;
        int effPts = effectivePointsPerTick(ctx.lodMultiplier());
        if (effPts <= 0) return;
        Random rng = new Random(((long) id.hashCode()) ^ tickInStep);
        for (int i = 0; i < effPts; i++) {
            double t = rng.nextDouble();
            double rotT = rng.nextDouble();
            double[] off = coneOffsetAt(t, rotT);
            double[] rotated = Geometry.rotate(off, axisYaw, axisPitch);
            Location at = origin.clone().add(rotated[0], rotated[1], rotated[2]);
            SpawnUtil.spawn(at, ctx, particle, count, spread, speed);
        }
    }

    /** Package-private for tests. */
    int effectivePointsPerTick(double multiplier) {
        if (multiplier <= 0.0) return 0;
        return Math.max(1, (int) Math.round(pointsPerTick * multiplier));
    }

    /** Package-private for tests. Returns {yaw, pitch} of the cone axis in degrees. */
    double[] axisYawPitch() {
        return new double[]{axisYaw, axisPitch};
    }

    /** Package-private for tests. Returns the unrotated cone offset at (t, rotationT). */
    double[] coneOffsetAt(double t, double rotationT) {
        List<double[]> pts = Geometry.conePoint(angleDeg, length, t, rotationT);
        return pts.get(0);
    }
}

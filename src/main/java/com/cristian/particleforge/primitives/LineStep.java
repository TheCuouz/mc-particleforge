package com.cristian.particleforge.primitives;

import com.cristian.particleforge.engine.BudgetManager;
import com.cristian.particleforge.model.EffectContext;
import com.cristian.particleforge.model.EffectStep;
import org.bukkit.Location;
import org.bukkit.Particle;

import java.util.List;
import java.util.Map;

/**
 * Line segment from origin to a relative target offset.
 *
 * <h2>Params</h2>
 * <ul>
 *   <li>{@code particle} (string) — default {@code END_ROD}.</li>
 *   <li>{@code count} (int) — per point; default {@code 1}.</li>
 *   <li>{@code target-relative} (string) — {@code "dx,dy,dz"}; default {@code "0,3,0"}.</li>
 *   <li>{@code points} (int) — default {@code 12}.</li>
 * </ul>
 */
public final class LineStep implements EffectStep {

    private final String id;
    private final int durationTicks;
    private final Particle particle;
    private final int count;
    private final double dx;
    private final double dy;
    private final double dz;
    private final int points;
    private final double speed;
    private final double spread;

    public LineStep(String id, int durationTicks, Map<String, Object> params) {
        this.id = id;
        this.durationTicks = durationTicks;
        this.particle = ParamUtil.particle(params, "particle", Particle.END_ROD);
        this.count    = ParamUtil.intVal(params, "count", 1);
        this.points   = ParamUtil.intVal(params, "points", 12);
        this.speed    = ParamUtil.doubleVal(params, "speed", 0.0);
        this.spread   = ParamUtil.doubleVal(params, "spread", 0.0);

        String raw = ParamUtil.stringVal(params, "target-relative", "0,3,0");
        double[] t = parseTarget(raw);
        this.dx = t[0];
        this.dy = t[1];
        this.dz = t[2];
    }

    private static double[] parseTarget(String raw) {
        if (raw == null) return new double[]{0.0, 3.0, 0.0};
        String[] parts = raw.split(",");
        if (parts.length != 3) return new double[]{0.0, 3.0, 0.0};
        try {
            return new double[]{
                Double.parseDouble(parts[0].trim()),
                Double.parseDouble(parts[1].trim()),
                Double.parseDouble(parts[2].trim())
            };
        } catch (NumberFormatException ex) {
            return new double[]{0.0, 3.0, 0.0};
        }
    }

    @Override public String id() { return id; }
    @Override public int durationTicks() { return durationTicks; }

    @Override
    public void tick(EffectContext ctx, int tickInStep) {
        if (ctx.lodBucket() >= BudgetManager.BUCKET_CULL || ctx.lodMultiplier() <= 0.0) return;
        Location origin = ctx.origin();
        if (origin == null) return;
        int effPts = effectivePoints(ctx.lodMultiplier());
        List<double[]> pts = Geometry.linePoints(dx, dy, dz, effPts);
        for (double[] off : pts) {
            Location at = origin.clone().add(off[0], off[1], off[2]);
            SpawnUtil.spawn(at, ctx, particle, count, spread, speed);
        }
    }

    /** Package-private for tests. */
    int effectivePoints(double lodMul) {
        if (lodMul <= 0.0) return 0;
        return Math.max(1, (int) Math.round(points * lodMul));
    }

    /** Package-private for tests. Returns the parsed target offset {dx, dy, dz}. */
    double[] targetVector() {
        return new double[]{dx, dy, dz};
    }
}

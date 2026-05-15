package com.cristian.particleforge.primitives;

import com.cristian.particleforge.engine.BudgetManager;
import com.cristian.particleforge.model.EffectContext;
import com.cristian.particleforge.model.EffectStep;
import org.bukkit.Location;
import org.bukkit.Particle;

import java.util.List;
import java.util.Map;

/**
 * Hollow vertical cylinder of stacked rings.
 *
 * <h2>Params</h2>
 * <ul>
 *   <li>{@code particle} (string) — default {@code END_ROD}.</li>
 *   <li>{@code count} (int) — per point; default {@code 1}.</li>
 *   <li>{@code radius} (double) — default {@code 1.5}.</li>
 *   <li>{@code height} (double) — default {@code 3.0}.</li>
 *   <li>{@code points-per-ring} (int) — default {@code 16}.</li>
 *   <li>{@code rings} (int) — default {@code 5}.</li>
 * </ul>
 *
 * <h2>LOD</h2>
 * Both {@code rings} and {@code points-per-ring} scale by {@code sqrt(lodMul)}
 * so the total particle count scales linearly with the multiplier.
 */
public final class CylinderStep implements EffectStep {

    private final String id;
    private final int durationTicks;
    private final Particle particle;
    private final int count;
    private final double radius;
    private final double height;
    private final int pointsPerRing;
    private final int rings;
    private final double speed;
    private final double spread;

    public CylinderStep(String id, int durationTicks, Map<String, Object> params) {
        this.id = id;
        this.durationTicks = durationTicks;
        this.particle      = ParamUtil.particle(params, "particle", Particle.END_ROD);
        this.count         = ParamUtil.intVal(params, "count", 1);
        this.radius        = ParamUtil.doubleVal(params, "radius", 1.5);
        this.height        = ParamUtil.doubleVal(params, "height", 3.0);
        this.pointsPerRing = ParamUtil.intVal(params, "points-per-ring", 16);
        this.rings         = ParamUtil.intVal(params, "rings", 5);
        this.speed         = ParamUtil.doubleVal(params, "speed", 0.0);
        this.spread        = ParamUtil.doubleVal(params, "spread", 0.0);
    }

    @Override public String id() { return id; }
    @Override public int durationTicks() { return durationTicks; }

    @Override
    public void tick(EffectContext ctx, int tickInStep) {
        if (ctx.lodBucket() >= BudgetManager.BUCKET_CULL || ctx.lodMultiplier() <= 0.0) return;
        Location origin = ctx.origin();
        if (origin == null) return;
        int effRings = effectiveRings(ctx.lodMultiplier());
        int effPpr   = effectivePointsPerRing(ctx.lodMultiplier());
        for (int r = 0; r < effRings; r++) {
            double y = ringHeight(r, effRings);
            List<double[]> pts = Geometry.cylinderRing(radius, y, effPpr);
            for (double[] off : pts) {
                Location at = origin.clone().add(off[0], off[1], off[2]);
                SpawnUtil.spawn(at, ctx, particle, count, spread, speed);
            }
        }
    }

    /** Package-private for tests. */
    int effectiveRings(double lodMul) {
        if (lodMul <= 0.0) return 0;
        return Math.max(1, (int) Math.round(rings * Math.sqrt(lodMul)));
    }

    /** Package-private for tests. */
    int effectivePointsPerRing(double lodMul) {
        if (lodMul <= 0.0) return 0;
        return Math.max(1, (int) Math.round(pointsPerRing * Math.sqrt(lodMul)));
    }

    /** Package-private for tests. y-coordinate for the given ring index. */
    double ringHeight(int ringIndex, int totalRings) {
        if (totalRings <= 1) return 0.0;
        return height * ringIndex / (double) (totalRings - 1);
    }
}

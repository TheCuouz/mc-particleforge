package com.cristian.particleforge.primitives;

import com.cristian.particleforge.engine.BudgetManager;
import com.cristian.particleforge.model.EffectContext;
import com.cristian.particleforge.model.EffectStep;
import org.bukkit.Location;
import org.bukkit.Particle;

import java.util.List;
import java.util.Map;

/**
 * Hollow Fibonacci sphere via {@link Geometry#spherePoints}. Optional radius
 * expansion over time via {@code expand-per-tick}.
 *
 * <h2>Params</h2>
 * <ul>
 *   <li>{@code particle} (string) — default {@code END_ROD}.</li>
 *   <li>{@code count} (int) — particles per point; default {@code 1}.</li>
 *   <li>{@code points} (int) — default {@code 64}.</li>
 *   <li>{@code radius} (double) — initial radius; default {@code 1.5}.</li>
 *   <li>{@code expand-per-tick} (double) — added to radius each tick; default {@code 0}.</li>
 * </ul>
 */
public final class SphereStep implements EffectStep {

    private final String id;
    private final int durationTicks;
    private final Particle particle;
    private final int count;
    private final int points;
    private final double radius;
    private final double expandPerTick;
    private final double speed;
    private final double spread;

    public SphereStep(String id, int durationTicks, Map<String, Object> params) {
        this.id = id;
        this.durationTicks  = durationTicks;
        this.particle       = ParamUtil.particle(params, "particle", Particle.END_ROD);
        this.count          = ParamUtil.intVal(params, "count", 1);
        this.points         = ParamUtil.intVal(params, "points", 64);
        this.radius         = ParamUtil.doubleVal(params, "radius", 1.5);
        this.expandPerTick  = ParamUtil.doubleVal(params, "expand-per-tick", 0.0);
        this.speed          = ParamUtil.doubleVal(params, "speed", 0.0);
        this.spread         = ParamUtil.doubleVal(params, "spread", 0.0);
    }

    @Override public String id() { return id; }
    @Override public int durationTicks() { return durationTicks; }

    @Override
    public void tick(EffectContext ctx, int tickInStep) {
        if (ctx.lodBucket() >= BudgetManager.BUCKET_CULL || ctx.lodMultiplier() <= 0.0) return;
        Location origin = ctx.origin();
        if (origin == null) return;
        int effPoints = effectivePoints(ctx.lodMultiplier());
        double r = currentRadius(tickInStep);
        List<double[]> pts = Geometry.spherePoints(r, effPoints);
        for (double[] off : pts) {
            Location at = origin.clone().add(off[0], off[1], off[2]);
            SpawnUtil.spawn(at, ctx, particle, count, spread, speed);
        }
    }

    /** Package-private for tests. */
    double currentRadius(int tickInStep) {
        return radius + expandPerTick * tickInStep;
    }

    /** Package-private for tests. */
    int effectivePoints(double multiplier) {
        if (multiplier <= 0.0) return 0;
        return Math.max(1, (int) Math.round(points * multiplier));
    }
}

package com.cristian.particleforge.primitives;

import com.cristian.particleforge.engine.BudgetManager;
import com.cristian.particleforge.model.EffectContext;
import com.cristian.particleforge.model.EffectStep;
import org.bukkit.Location;
import org.bukkit.Particle;

import java.util.Map;

/**
 * Tornado / inverted helix: contracts from {@code start-radius} to
 * {@code end-radius} over duration.
 *
 * <h2>Params</h2>
 * <ul>
 *   <li>{@code particle} (string) — default {@code PORTAL}.</li>
 *   <li>{@code count} (int) — default {@code 1}.</li>
 *   <li>{@code points-per-tick} (int) — default {@code 8}.</li>
 *   <li>{@code start-radius} (double) — default {@code 3.0}.</li>
 *   <li>{@code end-radius} (double) — default {@code 0.0}.</li>
 *   <li>{@code height} (double) — default {@code 4.0}.</li>
 *   <li>{@code turns} (double) — default {@code 5.0}.</li>
 * </ul>
 */
public final class VortexStep implements EffectStep {

    private final String id;
    private final int durationTicks;
    private final Particle particle;
    private final int count;
    private final int pointsPerTick;
    private final double startRadius;
    private final double endRadius;
    private final double height;
    private final double turns;
    private final double speed;
    private final double spread;

    public VortexStep(String id, int durationTicks, Map<String, Object> params) {
        this.id = id;
        this.durationTicks = durationTicks;
        this.particle      = ParamUtil.particle(params, "particle", Particle.PORTAL);
        this.count         = ParamUtil.intVal(params, "count", 1);
        this.pointsPerTick = ParamUtil.intVal(params, "points-per-tick", 8);
        this.startRadius   = ParamUtil.doubleVal(params, "start-radius", 3.0);
        this.endRadius     = ParamUtil.doubleVal(params, "end-radius", 0.0);
        this.height        = ParamUtil.doubleVal(params, "height", 4.0);
        this.turns         = ParamUtil.doubleVal(params, "turns", 5.0);
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
        int effPts = effectivePointsPerTick(ctx.lodMultiplier());
        double t = tParam(tickInStep);
        double r = currentRadius(tickInStep);
        double y = height * t;
        double rotOffset = 2.0 * Math.PI * turns * t;
        for (int i = 0; i < effPts; i++) {
            double theta = (2.0 * Math.PI * i / (double) effPts) + rotOffset;
            double dx = Math.cos(theta) * r;
            double dz = Math.sin(theta) * r;
            Location at = origin.clone().add(dx, y, dz);
            SpawnUtil.spawn(at, ctx, particle, count, spread, speed);
        }
    }

    /** Package-private for tests. */
    double tParam(int tickInStep) {
        int denom = Math.max(1, durationTicks - 1);
        return (double) tickInStep / (double) denom;
    }

    /** Package-private for tests. */
    double currentRadius(int tickInStep) {
        double t = tParam(tickInStep);
        return startRadius + (endRadius - startRadius) * t;
    }

    /** Package-private for tests. */
    int effectivePointsPerTick(double multiplier) {
        if (multiplier <= 0.0) return 0;
        return Math.max(1, (int) Math.round(pointsPerTick * multiplier));
    }
}

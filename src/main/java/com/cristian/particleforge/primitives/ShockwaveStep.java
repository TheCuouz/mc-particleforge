package com.cristian.particleforge.primitives;

import com.cristian.particleforge.engine.BudgetManager;
import com.cristian.particleforge.model.EffectContext;
import com.cristian.particleforge.model.EffectStep;
import org.bukkit.Location;
import org.bukkit.Particle;

import java.util.List;
import java.util.Map;

/**
 * Expanding flat ring at ground level (origin.y).
 *
 * <h2>Params</h2>
 * <ul>
 *   <li>{@code particle} (string) — default {@code EXPLOSION_NORMAL}.</li>
 *   <li>{@code count} (int) — default {@code 1}.</li>
 *   <li>{@code points-per-tick} (int) — default {@code 32}.</li>
 *   <li>{@code start-radius} (double) — default {@code 0.5}.</li>
 *   <li>{@code end-radius} (double) — default {@code 6.0}.</li>
 * </ul>
 */
public final class ShockwaveStep implements EffectStep {

    private final String id;
    private final int durationTicks;
    private final Particle particle;
    private final int count;
    private final int pointsPerTick;
    private final double startRadius;
    private final double endRadius;
    private final double speed;
    private final double spread;

    public ShockwaveStep(String id, int durationTicks, Map<String, Object> params) {
        this.id = id;
        this.durationTicks = durationTicks;
        // EXPLOSION_NORMAL is the legacy name; Paper 1.21.10 uses EXPLOSION.
        // ParamUtil falls back gracefully — try EXPLOSION_NORMAL first then EXPLOSION.
        this.particle      = ParamUtil.particle(params, "particle", defaultParticle());
        this.count         = ParamUtil.intVal(params, "count", 1);
        this.pointsPerTick = ParamUtil.intVal(params, "points-per-tick", 32);
        this.startRadius   = ParamUtil.doubleVal(params, "start-radius", 0.5);
        this.endRadius     = ParamUtil.doubleVal(params, "end-radius", 6.0);
        this.speed         = ParamUtil.doubleVal(params, "speed", 0.0);
        this.spread        = ParamUtil.doubleVal(params, "spread", 0.0);
    }

    private static Particle defaultParticle() {
        try {
            return Particle.valueOf("EXPLOSION_NORMAL");
        } catch (IllegalArgumentException ex) {
            try {
                return Particle.valueOf("EXPLOSION");
            } catch (IllegalArgumentException ex2) {
                return Particle.FLAME;
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
        double r = currentRadius(tickInStep);
        List<double[]> pts = Geometry.shockwaveRing(r, effPts);
        for (double[] off : pts) {
            Location at = origin.clone().add(off[0], off[1], off[2]);
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

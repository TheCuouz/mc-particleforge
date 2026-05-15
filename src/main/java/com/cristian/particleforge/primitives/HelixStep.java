package com.cristian.particleforge.primitives;

import com.cristian.particleforge.engine.BudgetManager;
import com.cristian.particleforge.model.EffectContext;
import com.cristian.particleforge.model.EffectStep;
import org.bukkit.Location;
import org.bukkit.Particle;

import java.util.Map;

/**
 * DNA-style multi-strand helix climbing upward over {@code durationTicks}.
 *
 * <h2>Params</h2>
 * <ul>
 *   <li>{@code particle} (string) — default {@code FLAME}.</li>
 *   <li>{@code count} (int) — default {@code 1}.</li>
 *   <li>{@code radius} (double) — default {@code 0.6}.</li>
 *   <li>{@code height} (double) — default {@code 3.0}.</li>
 *   <li>{@code turns} (double) — total revolutions over duration; default {@code 3.0}.</li>
 *   <li>{@code strands} (int) — default {@code 2}.</li>
 * </ul>
 */
public final class HelixStep implements EffectStep {

    private final String id;
    private final int durationTicks;
    private final Particle particle;
    private final int count;
    private final double radius;
    private final double height;
    private final double turns;
    private final int strands;
    private final double speed;
    private final double spread;

    public HelixStep(String id, int durationTicks, Map<String, Object> params) {
        this.id = id;
        this.durationTicks = durationTicks;
        this.particle  = ParamUtil.particle(params, "particle", Particle.FLAME);
        this.count     = ParamUtil.intVal(params, "count", 1);
        this.radius    = ParamUtil.doubleVal(params, "radius", 0.6);
        this.height    = ParamUtil.doubleVal(params, "height", 3.0);
        this.turns     = ParamUtil.doubleVal(params, "turns", 3.0);
        this.strands   = ParamUtil.intVal(params, "strands", 2);
        this.speed     = ParamUtil.doubleVal(params, "speed", 0.0);
        this.spread    = ParamUtil.doubleVal(params, "spread", 0.0);
    }

    @Override public String id() { return id; }
    @Override public int durationTicks() { return durationTicks; }

    @Override
    public void tick(EffectContext ctx, int tickInStep) {
        if (ctx.lodBucket() >= BudgetManager.BUCKET_CULL || ctx.lodMultiplier() <= 0.0) return;
        Location origin = ctx.origin();
        if (origin == null) return;
        int effStrands = effectiveStrands(ctx.lodMultiplier());
        double t = tParam(tickInStep);
        for (int s = 0; s < effStrands; s++) {
            double[] off = strandOffset(t, s, effStrands);
            Location at = origin.clone().add(off[0], off[1], off[2]);
            SpawnUtil.spawn(at, ctx, particle, count, spread, speed);
        }
    }

    /** Package-private for tests. Returns t in [0,1]. */
    double tParam(int tickInStep) {
        int denom = Math.max(1, durationTicks - 1);
        return (double) tickInStep / (double) denom;
    }

    /** Package-private for tests. */
    double[] strandOffset(double t, int strandIndex, int strandsTotal) {
        double base = 2.0 * Math.PI * turns * t;
        double phase = 2.0 * Math.PI * strandIndex / (double) Math.max(1, strandsTotal);
        double angle = base + phase;
        return new double[]{
            Math.cos(angle) * radius,
            height * t,
            Math.sin(angle) * radius
        };
    }

    /** Package-private for tests. */
    int effectiveStrands(double multiplier) {
        if (multiplier <= 0.0) return 0;
        return Math.max(1, (int) Math.round(strands * multiplier));
    }
}

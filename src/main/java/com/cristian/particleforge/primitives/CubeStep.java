package com.cristian.particleforge.primitives;

import com.cristian.particleforge.engine.BudgetManager;
import com.cristian.particleforge.model.EffectContext;
import com.cristian.particleforge.model.EffectStep;
import org.bukkit.Location;
import org.bukkit.Particle;

import java.util.List;
import java.util.Map;

/**
 * Wireframe cube outline at origin.
 *
 * <h2>Params</h2>
 * <ul>
 *   <li>{@code particle} (string) — default {@code HAPPY_VILLAGER}.</li>
 *   <li>{@code count} (int) — per point; default {@code 1}.</li>
 *   <li>{@code size} (double) — edge length; default {@code 2.0}.</li>
 *   <li>{@code points-per-edge} (int) — default {@code 4}.</li>
 *   <li>{@code pulse} (boolean) — when true, only emits on ticks where
 *       {@code tickInStep % 10 == 0}; default {@code false}.</li>
 * </ul>
 */
public final class CubeStep implements EffectStep {

    private final String id;
    private final int durationTicks;
    private final Particle particle;
    private final int count;
    private final double size;
    private final int pointsPerEdge;
    private final boolean pulse;
    private final double speed;
    private final double spread;

    public CubeStep(String id, int durationTicks, Map<String, Object> params) {
        this.id = id;
        this.durationTicks = durationTicks;
        this.particle      = ParamUtil.particle(params, "particle", Particle.HAPPY_VILLAGER);
        this.count         = ParamUtil.intVal(params, "count", 1);
        this.size          = ParamUtil.doubleVal(params, "size", 2.0);
        this.pointsPerEdge = ParamUtil.intVal(params, "points-per-edge", 4);
        this.pulse         = ParamUtil.boolVal(params, "pulse", false);
        this.speed         = ParamUtil.doubleVal(params, "speed", 0.0);
        this.spread        = ParamUtil.doubleVal(params, "spread", 0.0);
    }

    @Override public String id() { return id; }
    @Override public int durationTicks() { return durationTicks; }

    @Override
    public void tick(EffectContext ctx, int tickInStep) {
        if (ctx.lodBucket() >= BudgetManager.BUCKET_CULL || ctx.lodMultiplier() <= 0.0) return;
        if (!shouldEmit(tickInStep)) return;
        Location origin = ctx.origin();
        if (origin == null) return;
        int effPpe = effectivePointsPerEdge(ctx.lodMultiplier());
        List<double[]> pts = Geometry.cubeWireframe(size, effPpe);
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

    /** Package-private for tests. */
    boolean shouldEmit(int tickInStep) {
        return !pulse || (tickInStep % 10 == 0);
    }
}

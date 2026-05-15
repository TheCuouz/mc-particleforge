package com.cristian.particleforge.primitives;

import com.cristian.particleforge.engine.BudgetManager;
import com.cristian.particleforge.model.EffectContext;
import com.cristian.particleforge.model.EffectStep;
import org.bukkit.Location;
import org.bukkit.Particle;

import java.util.Map;
import java.util.Random;

/**
 * Particles raining down within a cylinder above the origin.
 *
 * <h2>Params</h2>
 * <ul>
 *   <li>{@code particle} (string) — default {@code DRIPPING_WATER}.</li>
 *   <li>{@code count} (int) — per particle; default {@code 1}.</li>
 *   <li>{@code radius} (double) — XZ radius around origin; default {@code 3.0}.</li>
 *   <li>{@code height} (double) — max spawn height above origin; default {@code 4.0}.</li>
 *   <li>{@code density-per-tick} (int) — default {@code 4}.</li>
 *   <li>{@code speed} (double) — default {@code 0}.</li>
 *   <li>{@code spread} (double) — default {@code 0}.</li>
 * </ul>
 */
public final class RainStep implements EffectStep {

    private final String id;
    private final int durationTicks;
    private final Particle particle;
    private final int count;
    private final double radius;
    private final double height;
    private final int densityPerTick;
    private final double speed;
    private final double spread;

    public RainStep(String id, int durationTicks, Map<String, Object> params) {
        this.id = id;
        this.durationTicks = durationTicks;
        this.particle       = ParamUtil.particle(params, "particle", Particle.DRIPPING_WATER);
        this.count          = ParamUtil.intVal(params, "count", 1);
        this.radius         = ParamUtil.doubleVal(params, "radius", 3.0);
        this.height         = ParamUtil.doubleVal(params, "height", 4.0);
        this.densityPerTick = ParamUtil.intVal(params, "density-per-tick", 4);
        this.speed          = ParamUtil.doubleVal(params, "speed", 0.0);
        this.spread         = ParamUtil.doubleVal(params, "spread", 0.0);
    }

    @Override public String id() { return id; }
    @Override public int durationTicks() { return durationTicks; }

    @Override
    public void tick(EffectContext ctx, int tickInStep) {
        if (ctx.lodBucket() >= BudgetManager.BUCKET_CULL || ctx.lodMultiplier() <= 0.0) return;
        if (densityPerTick <= 0) return;
        Location origin = ctx.origin();
        if (origin == null) return;
        int effDensity = effectiveDensity(ctx.lodMultiplier());
        if (effDensity <= 0) return;
        for (int i = 0; i < effDensity; i++) {
            double[] off = randomOffsetAt(tickInStep, i);
            Location at = origin.clone().add(off[0], off[1], off[2]);
            SpawnUtil.spawn(at, ctx, particle, count, spread, speed);
        }
    }

    /** Package-private for tests. */
    int effectiveDensity(double multiplier) {
        if (multiplier <= 0.0) return 0;
        if (densityPerTick <= 0) return 0;
        return Math.max(1, (int) Math.round(densityPerTick * multiplier));
    }

    /** Package-private for tests. Deterministic per (tickInStep, particleIndex). */
    double[] randomOffsetAt(int tickInStep, int particleIndex) {
        long seed = ((long) id.hashCode()) ^ ((long) tickInStep * 0x9E3779B97F4A7C15L) ^ particleIndex;
        Random rng = new Random(seed);
        double theta = rng.nextDouble() * 2.0 * Math.PI;
        double r = Math.sqrt(rng.nextDouble()) * radius;
        double y = rng.nextDouble() * height;
        return new double[]{Math.cos(theta) * r, y, Math.sin(theta) * r};
    }
}

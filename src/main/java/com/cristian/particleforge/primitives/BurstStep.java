package com.cristian.particleforge.primitives;

import com.cristian.particleforge.engine.BudgetManager;
import com.cristian.particleforge.model.EffectContext;
import com.cristian.particleforge.model.EffectStep;
import org.bukkit.Location;
import org.bukkit.Particle;

import java.util.Map;

/**
 * Radial burst: spawns {@code count} particles centered at {@code ctx.origin()}
 * spread within a bounding box of side {@code 2*spread}. Optionally repeats
 * every {@code repeat-every-ticks} ticks (default: single burst on tick 0).
 *
 * <h2>Params</h2>
 * <ul>
 *   <li>{@code particle}    (string)  — Bukkit Particle enum name; default {@code FLAME}.</li>
 *   <li>{@code count}       (int)     — particles per burst; default {@code 32}.</li>
 *   <li>{@code speed}       (double)  — extra speed parameter; default {@code 0.5}.</li>
 *   <li>{@code spread}      (double)  — bounding box radius for random offset; default {@code 0.5}.</li>
 *   <li>{@code repeat-every-ticks} (int) — emit cadence; default = {@code durationTicks} (one burst).</li>
 * </ul>
 *
 * <h2>LOD</h2>
 * Reads {@code ctx.lodBucket()} / {@code ctx.lodMultiplier()}. Bucket
 * {@link BudgetManager#BUCKET_CULL} → no emit. Otherwise effective count =
 * {@code max(1, (int)(count * multiplier))}.
 */
public final class BurstStep implements EffectStep {

    private final String id;
    private final int durationTicks;
    private final Particle particle;
    private final int count;
    private final double speed;
    private final double spread;
    private final int repeatEvery;

    public BurstStep(String id, int durationTicks, Map<String, Object> params) {
        this.id = id;
        this.durationTicks = durationTicks;
        this.particle = ParamUtil.particle(params, "particle", Particle.FLAME);
        this.count    = ParamUtil.intVal(params, "count", 32);
        this.speed    = ParamUtil.doubleVal(params, "speed", 0.5);
        this.spread   = ParamUtil.doubleVal(params, "spread", 0.5);
        this.repeatEvery = ParamUtil.intVal(params, "repeat-every-ticks", durationTicks);
    }

    @Override public String id() { return id; }
    @Override public int durationTicks() { return durationTicks; }

    @Override
    public void tick(EffectContext ctx, int tickInStep) {
        if (!shouldEmitThisTick(tickInStep)) return;
        int eff = effectiveCount(ctx.lodBucket(), ctx.lodMultiplier());
        if (eff <= 0) return;
        Location loc = ctx.origin();
        if (loc == null) return;
        SpawnUtil.spawn(loc, ctx, particle, eff, spread, speed);
    }

    /** Package-private for tests. */
    boolean shouldEmitThisTick(int tickInStep) {
        return repeatEvery > 0 && (tickInStep % repeatEvery == 0);
    }

    /** Package-private for tests. */
    int effectiveCount(int bucket, double multiplier) {
        if (bucket >= BudgetManager.BUCKET_CULL) return 0;
        if (multiplier <= 0.0) return 0;
        return Math.max(1, (int) Math.round(count * multiplier));
    }
}

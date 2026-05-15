package com.cristian.particleforge.primitives;

import com.cristian.particleforge.engine.BudgetManager;
import com.cristian.particleforge.model.EffectContext;
import com.cristian.particleforge.model.EffectStep;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

import java.util.Map;

/**
 * Emits a particle at the followed entity's current location every tick.
 * Optionally updates {@code ctx.origin} each tick so subsequent steps in a
 * sequence emit from the new location too.
 *
 * <h2>Params</h2>
 * <ul>
 *   <li>{@code particle} (string) — default {@code END_ROD}.</li>
 *   <li>{@code count} (int) — default {@code 1}.</li>
 *   <li>{@code update-origin} (boolean) — default {@code true}.</li>
 *   <li>{@code speed} (double) — default {@code 0}.</li>
 *   <li>{@code spread} (double) — default {@code 0}.</li>
 * </ul>
 *
 * <h2>LOD</h2>
 * Scales {@code count}. Bucket {@link BudgetManager#BUCKET_CULL} → no emit.
 */
public final class TrailStep implements EffectStep {

    private final String id;
    private final int durationTicks;
    private final Particle particle;
    private final int count;
    private final boolean updateOrigin;
    private final double speed;
    private final double spread;

    public TrailStep(String id, int durationTicks, Map<String, Object> params) {
        this.id = id;
        this.durationTicks = durationTicks;
        this.particle     = ParamUtil.particle(params, "particle", Particle.END_ROD);
        this.count        = ParamUtil.intVal(params, "count", 1);
        this.updateOrigin = ParamUtil.boolVal(params, "update-origin", true);
        this.speed        = ParamUtil.doubleVal(params, "speed", 0.0);
        this.spread       = ParamUtil.doubleVal(params, "spread", 0.0);
    }

    @Override public String id() { return id; }
    @Override public int durationTicks() { return durationTicks; }

    @Override
    public void tick(EffectContext ctx, int tickInStep) {
        if (ctx.lodBucket() >= BudgetManager.BUCKET_CULL || ctx.lodMultiplier() <= 0.0) return;
        LivingEntity entity = ctx.follow();
        if (entity == null) return;
        Location at = entity.getLocation();
        if (updateOrigin) ctx.setOrigin(at);
        int eff = effectiveCount(ctx.lodMultiplier());
        SpawnUtil.spawn(at, ctx, particle, eff, spread, speed);
    }

    /** Package-private for tests. */
    int effectiveCount(double lodMul) {
        if (lodMul <= 0.0) return 0;
        return Math.max(1, (int) Math.round(count * lodMul));
    }
}

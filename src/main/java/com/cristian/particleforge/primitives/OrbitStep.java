package com.cristian.particleforge.primitives;

import com.cristian.particleforge.engine.BudgetManager;
import com.cristian.particleforge.model.EffectContext;
import com.cristian.particleforge.model.EffectStep;
import org.bukkit.Location;
import org.bukkit.Particle;

import java.util.Map;

/**
 * N particles orbiting around the origin at {@code radius}, completing
 * {@code turns} full revolutions over {@code durationTicks}.
 *
 * <h2>Params</h2>
 * <ul>
 *   <li>{@code particle} (string) — default {@code FLAME}.</li>
 *   <li>{@code count} (int) — per emitted point; default {@code 1}.</li>
 *   <li>{@code radius} (double) — default {@code 1.0}.</li>
 *   <li>{@code points} (int) — orbiting slots; default {@code 3}.</li>
 *   <li>{@code turns} (double) — full revolutions over duration; default {@code 1.0}.</li>
 *   <li>{@code yaw-deg} (double) — tilt around Y; default {@code 0}.</li>
 *   <li>{@code pitch-deg} (double) — tilt around X; default {@code 0}.</li>
 * </ul>
 *
 * <h2>LOD</h2>
 * Scales {@code points}. Bucket {@link BudgetManager#BUCKET_CULL} → no emit.
 */
public final class OrbitStep implements EffectStep {

    private final String id;
    private final int durationTicks;
    private final Particle particle;
    private final int count;
    private final double radius;
    private final int points;
    private final double turns;
    private final double yawDeg;
    private final double pitchDeg;
    private final double speed;
    private final double spread;

    public OrbitStep(String id, int durationTicks, Map<String, Object> params) {
        this.id = id;
        this.durationTicks = durationTicks;
        this.particle = ParamUtil.particle(params, "particle", Particle.FLAME);
        this.count    = ParamUtil.intVal(params, "count", 1);
        this.radius   = ParamUtil.doubleVal(params, "radius", 1.0);
        this.points   = ParamUtil.intVal(params, "points", 3);
        this.turns    = ParamUtil.doubleVal(params, "turns", 1.0);
        this.yawDeg   = ParamUtil.doubleVal(params, "yaw-deg", 0.0);
        this.pitchDeg = ParamUtil.doubleVal(params, "pitch-deg", 0.0);
        this.speed    = ParamUtil.doubleVal(params, "speed", 0.0);
        this.spread   = ParamUtil.doubleVal(params, "spread", 0.0);
    }

    @Override public String id() { return id; }
    @Override public int durationTicks() { return durationTicks; }

    @Override
    public void tick(EffectContext ctx, int tickInStep) {
        if (ctx.lodBucket() >= BudgetManager.BUCKET_CULL || ctx.lodMultiplier() <= 0.0) return;
        Location origin = ctx.origin();
        if (origin == null) return;
        int effPoints = effectivePoints(ctx.lodMultiplier());
        for (int s = 0; s < effPoints; s++) {
            double[] off = slotOffset(tickInStep, s, effPoints);
            Location at = origin.clone().add(off[0], off[1], off[2]);
            SpawnUtil.spawn(at, ctx, particle, count, spread, speed);
        }
    }

    /** Package-private for tests. */
    int effectivePoints(double lodMul) {
        if (lodMul <= 0.0) return 0;
        return Math.max(1, (int) Math.round(points * lodMul));
    }

    /** Package-private for tests. Returns rotated offset {dx,dy,dz}. */
    double[] slotOffset(int tickInStep, int slot, int slots) {
        double t = tickInStep / (double) Math.max(1, durationTicks - 1);
        double slotFrac = slots <= 0 ? 0.0 : (slot / (double) slots);
        double angle = 2.0 * Math.PI * (slotFrac + turns * t);
        double[] base = new double[]{
            Math.cos(angle) * radius,
            0.0,
            Math.sin(angle) * radius
        };
        return Geometry.rotate(base, yawDeg, pitchDeg);
    }
}

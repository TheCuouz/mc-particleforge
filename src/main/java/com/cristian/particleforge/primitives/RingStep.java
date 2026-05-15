package com.cristian.particleforge.primitives;

import com.cristian.particleforge.engine.BudgetManager;
import com.cristian.particleforge.model.EffectContext;
import com.cristian.particleforge.model.EffectStep;
import org.bukkit.Location;
import org.bukkit.Particle;

import java.util.List;
import java.util.Map;

/**
 * Flat ring at {@code ctx.origin()} via {@link Geometry#ringPoints}. Optional
 * spin via {@code rotation-deg-per-tick}.
 *
 * <h2>Params</h2>
 * <ul>
 *   <li>{@code particle} (string) — default {@code END_ROD}.</li>
 *   <li>{@code count} (int) — particles per ring point per tick; default {@code 1}.</li>
 *   <li>{@code points} (int) — points on the ring; default {@code 24}.</li>
 *   <li>{@code radius} (double) — default {@code 1.0}.</li>
 *   <li>{@code yaw-deg} (double) — default {@code 0}.</li>
 *   <li>{@code pitch-deg} (double) — default {@code 0}.</li>
 *   <li>{@code rotation-deg-per-tick} (double) — default {@code 0}.</li>
 *   <li>{@code speed} (double) — default {@code 0}.</li>
 *   <li>{@code spread} (double) — default {@code 0}.</li>
 * </ul>
 *
 * <h2>LOD</h2>
 * Applies multiplier to {@code points} (drops ring resolution at low LOD).
 */
public final class RingStep implements EffectStep {

    private final String id;
    private final int durationTicks;
    private final Particle particle;
    private final int count;
    private final int points;
    private final double radius;
    private final double yawDeg;
    private final double pitchDeg;
    private final double rotationDegPerTick;
    private final double speed;
    private final double spread;

    public RingStep(String id, int durationTicks, Map<String, Object> params) {
        this.id = id;
        this.durationTicks = durationTicks;
        this.particle           = ParamUtil.particle(params, "particle", Particle.END_ROD);
        this.count              = ParamUtil.intVal(params, "count", 1);
        this.points             = ParamUtil.intVal(params, "points", 24);
        this.radius             = ParamUtil.doubleVal(params, "radius", 1.0);
        this.yawDeg             = ParamUtil.doubleVal(params, "yaw-deg", 0.0);
        this.pitchDeg           = ParamUtil.doubleVal(params, "pitch-deg", 0.0);
        this.rotationDegPerTick = ParamUtil.doubleVal(params, "rotation-deg-per-tick", 0.0);
        this.speed              = ParamUtil.doubleVal(params, "speed", 0.0);
        this.spread             = ParamUtil.doubleVal(params, "spread", 0.0);
    }

    @Override public String id() { return id; }
    @Override public int durationTicks() { return durationTicks; }

    @Override
    public void tick(EffectContext ctx, int tickInStep) {
        if (ctx.lodBucket() >= BudgetManager.BUCKET_CULL || ctx.lodMultiplier() <= 0.0) return;
        Location origin = ctx.origin();
        if (origin == null) return;
        int effPoints = effectivePoints(ctx.lodMultiplier());
        double yaw = yawDeg + rotationDegPerTick * tickInStep;
        List<double[]> pts = Geometry.ringPoints(radius, effPoints, yaw, pitchDeg);
        for (double[] off : pts) {
            Location at = origin.clone().add(off[0], off[1], off[2]);
            SpawnUtil.spawn(at, ctx, particle, count, spread, speed);
        }
    }

    /** Package-private for tests. */
    int effectivePoints(double multiplier) {
        if (multiplier <= 0.0) return 0;
        return Math.max(1, (int) Math.round(points * multiplier));
    }

    /** Package-private for tests. */
    double yawAtTick(int tickInStep) {
        return yawDeg + rotationDegPerTick * tickInStep;
    }
}

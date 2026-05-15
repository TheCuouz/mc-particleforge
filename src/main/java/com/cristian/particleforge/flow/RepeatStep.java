package com.cristian.particleforge.flow;

import com.cristian.particleforge.model.EffectContext;
import com.cristian.particleforge.model.EffectStep;

import java.util.List;

/**
 * Runs the inner step list {@code times} iterations sequentially. The total
 * duration is {@code sum(children.durationTicks()) * times}.
 *
 * <h2>State caveat</h2>
 * <strong>Stateful.</strong> Unlike primitive steps, flow steps carry mutable
 * per-handle state (current iteration, inner-step index, inner tick counter).
 * They MUST NOT be shared across handles — the StepFactory (Task 13)
 * constructs a fresh instance per handle.
 */
public final class RepeatStep implements EffectStep {

    private final String id;
    private final List<EffectStep> children;
    private final int times;

    private int iteration = 0;
    private int innerIndex = 0;
    private int innerTick = -1;     // -1 means begin() pending for innerIndex
    private boolean started = false;
    private final int totalDuration;

    public RepeatStep(String id, List<EffectStep> children, int times) {
        this.id = id;
        this.children = List.copyOf(children);
        this.times = times;
        int innerSum = 0;
        for (EffectStep s : children) innerSum += s.durationTicks();
        this.totalDuration = innerSum * Math.max(0, times);
    }

    @Override public String id() { return id; }
    @Override public int durationTicks() { return totalDuration; }

    @Override
    public void tick(EffectContext ctx, int tickInStep) {
        if (children.isEmpty() || times <= 0 || iteration >= times) return;
        if (!started || innerTick < 0) {
            children.get(innerIndex).begin(ctx);
            innerTick = 0;
            started = true;
        }
        EffectStep current = children.get(innerIndex);
        if (innerTick >= current.durationTicks()) {
            current.end(ctx);
            innerIndex++;
            if (innerIndex >= children.size()) {
                innerIndex = 0;
                iteration++;
                if (iteration >= times) return;
            }
            innerTick = 0;
            children.get(innerIndex).begin(ctx);
        }
        children.get(innerIndex).tick(ctx, innerTick);
        innerTick++;
    }

    @Override
    public void end(EffectContext ctx) {
        // Ensure the currently-running inner step gets its end() invoked.
        if (started && iteration < times && innerIndex < children.size()) {
            try { children.get(innerIndex).end(ctx); } catch (Throwable ignored) {}
        }
        // Mark fully drained so subsequent end() calls are idempotent.
        iteration = times;
    }
}

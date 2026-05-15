package com.cristian.particleforge.flow;

import com.cristian.particleforge.model.EffectContext;
import com.cristian.particleforge.model.EffectStep;

import java.util.List;

/**
 * Ticks all inner steps concurrently. Duration = max of inner durations.
 *
 * <h2>State caveat</h2>
 * <strong>Stateful.</strong> Tracks per-child tick counters and begin/end
 * flags. MUST NOT be shared across handles — the StepFactory (Task 13)
 * constructs a fresh instance per handle.
 */
public final class ParallelStep implements EffectStep {

    private final String id;
    private final List<EffectStep> children;
    private final int[] localTicks;
    private final boolean[] beganFlags;
    private final boolean[] endedFlags;
    private final int totalDuration;

    public ParallelStep(String id, List<EffectStep> children) {
        this.id = id;
        this.children = List.copyOf(children);
        int n = this.children.size();
        this.localTicks = new int[n];
        this.beganFlags = new boolean[n];
        this.endedFlags = new boolean[n];
        int max = 0;
        for (EffectStep s : this.children) max = Math.max(max, s.durationTicks());
        this.totalDuration = max;
    }

    @Override public String id() { return id; }
    @Override public int durationTicks() { return totalDuration; }

    @Override
    public void tick(EffectContext ctx, int tickInStep) {
        for (int i = 0; i < children.size(); i++) {
            EffectStep child = children.get(i);
            if (endedFlags[i]) continue;
            if (!beganFlags[i]) {
                child.begin(ctx);
                beganFlags[i] = true;
            }
            if (localTicks[i] >= child.durationTicks()) {
                child.end(ctx);
                endedFlags[i] = true;
                continue;
            }
            child.tick(ctx, localTicks[i]);
            localTicks[i]++;
        }
    }

    @Override
    public void end(EffectContext ctx) {
        for (int i = 0; i < children.size(); i++) {
            if (beganFlags[i] && !endedFlags[i]) {
                try { children.get(i).end(ctx); } catch (Throwable ignored) {}
                endedFlags[i] = true;
            }
        }
    }
}

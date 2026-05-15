package com.cristian.particleforge.flow;

import com.cristian.particleforge.model.EffectContext;
import com.cristian.particleforge.model.EffectStep;

/**
 * Pure wait. No-op tick. Used by sequences to space out other steps.
 *
 * <p>Stateless — safe to reuse across handles.
 */
public final class DelayStep implements EffectStep {

    private final String id;
    private final int durationTicks;

    public DelayStep(String id, int durationTicks) {
        this.id = id;
        this.durationTicks = durationTicks;
    }

    @Override public String id() { return id; }
    @Override public int durationTicks() { return durationTicks; }
    @Override public void tick(EffectContext ctx, int tickInStep) {}
}

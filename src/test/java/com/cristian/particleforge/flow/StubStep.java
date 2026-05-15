package com.cristian.particleforge.flow;

import com.cristian.particleforge.model.EffectContext;
import com.cristian.particleforge.model.EffectStep;

/** Test stub that counts begin/tick/end calls for flow-step tests. */
final class StubStep implements EffectStep {
    final String id;
    final int duration;
    int beginCount = 0;
    int tickCount = 0;
    int endCount = 0;
    int lastTickInStep = -1;

    StubStep(String id, int duration) {
        this.id = id;
        this.duration = duration;
    }

    @Override public String id() { return id; }
    @Override public int durationTicks() { return duration; }
    @Override public void begin(EffectContext ctx) { beginCount++; }
    @Override public void tick(EffectContext ctx, int tickInStep) { tickCount++; lastTickInStep = tickInStep; }
    @Override public void end(EffectContext ctx) { endCount++; }
}

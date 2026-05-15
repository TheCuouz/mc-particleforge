package com.cristian.particleforge.flow;

import com.cristian.particleforge.model.EffectContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RepeatStepTest {

    @Test
    void totalDurationIsInnerSumTimesIterations() {
        StubStep a = new StubStep("a", 3);
        StubStep b = new StubStep("b", 3);
        RepeatStep r = new RepeatStep("r", List.of(a, b), 2);
        assertEquals(12, r.durationTicks());
    }

    @Test
    void zeroTimesIsNoOp() {
        StubStep a = new StubStep("a", 3);
        RepeatStep r = new RepeatStep("r", List.of(a), 0);
        assertEquals(0, r.durationTicks());
        EffectContext ctx = mock(EffectContext.class);
        for (int t = 0; t < 5; t++) r.tick(ctx, t);
        assertEquals(0, a.beginCount);
        assertEquals(0, a.tickCount);
        assertEquals(0, a.endCount);
    }

    @Test
    void emptyChildrenIsNoOp() {
        RepeatStep r = new RepeatStep("r", List.of(), 5);
        assertEquals(0, r.durationTicks());
        EffectContext ctx = mock(EffectContext.class);
        assertDoesNotThrow(() -> r.tick(ctx, 0));
    }

    @Test
    void runsInnerStepsInSequenceForEachIteration() {
        StubStep a = new StubStep("a", 3);
        StubStep b = new StubStep("b", 3);
        RepeatStep r = new RepeatStep("r", List.of(a, b), 2);
        EffectContext ctx = mock(EffectContext.class);

        // Tick the repeat for its full duration (12 ticks).
        for (int t = 0; t < 12; t++) r.tick(ctx, t);
        // After the last tick, the final inner step has been ticked but not yet
        // ended (end() fires on the boundary check on the NEXT call). Invoke end().
        r.end(ctx);

        // Across 2 iterations, each child should begin & end twice and tick 3 times per iteration.
        assertEquals(2, a.beginCount);
        assertEquals(2, a.endCount);
        assertEquals(6, a.tickCount);
        assertEquals(2, b.beginCount);
        // b's end is called once via the boundary roll-over (after iteration 1)
        // and once via r.end() (end of iteration 2).
        assertEquals(2, b.endCount);
        assertEquals(6, b.tickCount);
    }

    @Test
    void endIsIdempotent() {
        StubStep a = new StubStep("a", 2);
        RepeatStep r = new RepeatStep("r", List.of(a), 1);
        EffectContext ctx = mock(EffectContext.class);
        r.tick(ctx, 0);
        r.end(ctx);
        int firstEnd = a.endCount;
        r.end(ctx);  // second call should not re-end
        assertEquals(firstEnd, a.endCount);
    }
}

package com.cristian.particleforge.flow;

import com.cristian.particleforge.model.EffectContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ParallelStepTest {

    @Test
    void totalDurationIsMaxOfChildren() {
        StubStep a = new StubStep("a", 4);
        StubStep b = new StubStep("b", 8);
        ParallelStep p = new ParallelStep("p", List.of(a, b));
        assertEquals(8, p.durationTicks());
    }

    @Test
    void allChildrenBeginOnFirstTick() {
        StubStep a = new StubStep("a", 4);
        StubStep b = new StubStep("b", 8);
        ParallelStep p = new ParallelStep("p", List.of(a, b));
        EffectContext ctx = mock(EffectContext.class);
        p.tick(ctx, 0);
        assertEquals(1, a.beginCount);
        assertEquals(1, b.beginCount);
    }

    @Test
    void shorterChildEndsBeforeLongerChild() {
        StubStep a = new StubStep("a", 4);
        StubStep b = new StubStep("b", 8);
        ParallelStep p = new ParallelStep("p", List.of(a, b));
        EffectContext ctx = mock(EffectContext.class);

        // Tick 0..3 -> a ticks 4 times (localTick 0..3); on tick 4, a.localTick=4 == duration,
        // so a.end() is called.
        for (int t = 0; t < 5; t++) p.tick(ctx, t);
        assertEquals(4, a.tickCount);
        assertEquals(1, a.endCount);
        // b is still running
        assertEquals(0, b.endCount);
        assertEquals(5, b.tickCount);
    }

    @Test
    void endFinishesRunningChildren() {
        StubStep a = new StubStep("a", 10);
        StubStep b = new StubStep("b", 10);
        ParallelStep p = new ParallelStep("p", List.of(a, b));
        EffectContext ctx = mock(EffectContext.class);

        p.tick(ctx, 0); // begins both
        p.end(ctx);
        assertEquals(1, a.endCount);
        assertEquals(1, b.endCount);
    }

    @Test
    void endIsIdempotent() {
        StubStep a = new StubStep("a", 10);
        ParallelStep p = new ParallelStep("p", List.of(a));
        EffectContext ctx = mock(EffectContext.class);
        p.tick(ctx, 0);
        p.end(ctx);
        p.end(ctx);
        assertEquals(1, a.endCount);
    }

    @Test
    void emptyChildrenIsHarmless() {
        ParallelStep p = new ParallelStep("p", List.of());
        assertEquals(0, p.durationTicks());
        EffectContext ctx = mock(EffectContext.class);
        assertDoesNotThrow(() -> p.tick(ctx, 0));
        assertDoesNotThrow(() -> p.end(ctx));
    }
}

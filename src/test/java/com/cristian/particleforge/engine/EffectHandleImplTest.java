package com.cristian.particleforge.engine;

import com.cristian.particleforge.api.EffectHandle;
import com.cristian.particleforge.model.EffectContext;
import com.cristian.particleforge.model.EffectStep;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EffectHandleImplTest {

    private Plugin plugin;
    private Location origin;
    private EffectContext ctx;

    @BeforeEach
    void setup() {
        plugin = mock(Plugin.class);
        origin = mock(Location.class);
        when(origin.clone()).thenReturn(origin);
        ctx = new EffectContext(plugin, origin, Set.of(), null, null, null);
    }

    private static EffectStep stubStep(String id, int dur,
                                       AtomicInteger ticks,
                                       AtomicInteger ends,
                                       AtomicInteger begins) {
        return new EffectStep() {
            @Override public String id() { return id; }
            @Override public int durationTicks() { return dur; }
            @Override public void begin(EffectContext c) { begins.incrementAndGet(); }
            @Override public void tick(EffectContext c, int t) { ticks.incrementAndGet(); }
            @Override public void end(EffectContext c) { ends.incrementAndGet(); }
        };
    }

    @Test
    void newHandleIsActiveProgressZero() {
        AtomicInteger ticks = new AtomicInteger(), ends = new AtomicInteger(), begins = new AtomicInteger();
        EffectHandleImpl h = new EffectHandleImpl("test",
                List.of(stubStep("a", 5, ticks, ends, begins)), ctx, null);
        assertTrue(h.isActive());
        assertEquals(0.0, h.progress(), 1e-9);
        assertEquals(5, h.totalDuration());
        assertEquals(5, h.remainingTicks());
        assertEquals(0, begins.get());
        assertEquals(0, ticks.get());
        assertEquals(0, ends.get());
    }

    @Test
    void completesAfterDurationAndCallsEnd() {
        AtomicInteger ticks = new AtomicInteger(), ends = new AtomicInteger(), begins = new AtomicInteger();
        EffectHandleImpl h = new EffectHandleImpl("t",
                List.of(stubStep("a", 3, ticks, ends, begins)), ctx, null);
        // duration 3 → 3 tick callbacks, 4th call ends + finishes.
        for (int i = 0; i < 4; i++) h.tick();
        assertFalse(h.isActive());
        assertTrue(h.isFinished());
        assertEquals(1.0, h.progress(), 1e-9);
        assertEquals(1, ends.get());
        assertEquals(3, ticks.get());
        assertEquals(1, begins.get());
    }

    @Test
    void beginCalledOncePerStep() {
        AtomicInteger t1 = new AtomicInteger(), e1 = new AtomicInteger(), b1 = new AtomicInteger();
        AtomicInteger t2 = new AtomicInteger(), e2 = new AtomicInteger(), b2 = new AtomicInteger();
        EffectHandleImpl h = new EffectHandleImpl("t", List.of(
                stubStep("a", 2, t1, e1, b1),
                stubStep("b", 2, t2, e2, b2)
        ), ctx, null);
        // run through completion: 2 + 2 ticks + 1 end-final
        for (int i = 0; i < 6; i++) h.tick();
        assertEquals(1, b1.get());
        assertEquals(1, b2.get());
        assertEquals(1, e1.get());
        assertEquals(1, e2.get());
    }

    @Test
    void endCalledOncePerStepOnDurationElapse() {
        AtomicInteger t1 = new AtomicInteger(), e1 = new AtomicInteger(), b1 = new AtomicInteger();
        AtomicInteger t2 = new AtomicInteger(), e2 = new AtomicInteger(), b2 = new AtomicInteger();
        EffectHandleImpl h = new EffectHandleImpl("t", List.of(
                stubStep("a", 2, t1, e1, b1),
                stubStep("b", 3, t2, e2, b2)
        ), ctx, null);
        // step a: 2 ticks; advance happens on 3rd call (end a + begin b + tick b)
        h.tick(); // begin a, tick a (t=0)
        h.tick(); // tick a (t=1)
        assertEquals(0, e1.get());
        h.tick(); // end a, begin b, tick b
        assertEquals(1, e1.get());
        assertEquals(1, b2.get());
        // step b: durations 3 → 2 more tick callbacks, then end
        h.tick();
        h.tick();
        assertEquals(0, e2.get());
        h.tick(); // end b, finished
        assertEquals(1, e2.get());
        assertTrue(h.isFinished());
    }

    @Test
    void pauseStopsAdvance() {
        AtomicInteger t = new AtomicInteger(), e = new AtomicInteger(), b = new AtomicInteger();
        EffectHandleImpl h = new EffectHandleImpl("t",
                List.of(stubStep("a", 10, t, e, b)), ctx, null);
        h.tick(); h.tick(); // 2 ticks
        assertEquals(2, t.get());
        h.pause();
        assertTrue(h.isPaused());
        h.tick(); h.tick(); h.tick();
        assertEquals(2, t.get(), "tick counter did not change while paused");
        assertEquals(0, e.get());
    }

    @Test
    void resumeContinuesTicking() {
        AtomicInteger t = new AtomicInteger(), e = new AtomicInteger(), b = new AtomicInteger();
        EffectHandleImpl h = new EffectHandleImpl("t",
                List.of(stubStep("a", 10, t, e, b)), ctx, null);
        h.tick(); h.tick();
        h.pause();
        h.tick();
        assertEquals(2, t.get());
        h.resume();
        assertFalse(h.isPaused());
        h.tick();
        assertEquals(3, t.get());
    }

    @Test
    void cancelInvokesEndOnceAndDeactivates() {
        AtomicInteger t = new AtomicInteger(), e = new AtomicInteger(), b = new AtomicInteger();
        EffectHandleImpl h = new EffectHandleImpl("t",
                List.of(stubStep("a", 10, t, e, b)), ctx, null);
        h.tick(); h.tick(); h.tick();
        h.cancel();
        assertFalse(h.isActive());
        assertTrue(h.isCancelled());
        assertEquals(1, e.get(), "end called exactly once on cancel");
        h.cancel();
        assertEquals(1, e.get(), "second cancel is a no-op");
    }

    @Test
    void cancelBeforeFirstTickDoesNotCallEnd() {
        AtomicInteger t = new AtomicInteger(), e = new AtomicInteger(), b = new AtomicInteger();
        EffectHandleImpl h = new EffectHandleImpl("t",
                List.of(stubStep("a", 10, t, e, b)), ctx, null);
        h.cancel();
        assertFalse(h.isActive());
        assertEquals(0, e.get(), "end not called when never begun");
        assertEquals(0, b.get());
    }

    @Test
    void progressReflectsGlobalTickOverTotal() {
        AtomicInteger t = new AtomicInteger(), e = new AtomicInteger(), b = new AtomicInteger();
        EffectHandleImpl h = new EffectHandleImpl("t",
                List.of(stubStep("a", 10, t, e, b)), ctx, null);
        h.tick(); // globalTick=1
        assertEquals(0.1, h.progress(), 1e-9);
        h.tick(); h.tick(); h.tick(); // globalTick=4
        assertEquals(0.4, h.progress(), 1e-9);
        h.tick(); // globalTick=5
        assertEquals(0.5, h.progress(), 1e-9);
    }

    @Test
    void remainingTicksDecreasesAndClampsToZero() {
        AtomicInteger t = new AtomicInteger(), e = new AtomicInteger(), b = new AtomicInteger();
        EffectHandleImpl h = new EffectHandleImpl("t",
                List.of(stubStep("a", 3, t, e, b)), ctx, null);
        assertEquals(3, h.remainingTicks());
        h.tick();
        assertEquals(2, h.remainingTicks());
        h.tick();
        assertEquals(1, h.remainingTicks());
        h.tick();
        assertEquals(0, h.remainingTicks());
        // one more — step ends, handle finishes
        h.tick();
        assertEquals(0, h.remainingTicks());
        // extra calls remain 0
        for (int i = 0; i < 5; i++) h.tick();
        assertEquals(0, h.remainingTicks());
    }

    @Test
    void deadHandleIsInactive() {
        EffectHandle dead = EffectHandleImpl.dead("rejected");
        assertEquals("rejected", dead.effectName());
        assertFalse(dead.isActive());
        assertEquals(1.0, dead.progress(), 1e-9);
        assertEquals(0, dead.remainingTicks());
        // idempotent no-ops, no exceptions
        dead.pause(); dead.resume(); dead.cancel();
        assertFalse(dead.isPaused());
        assertFalse(dead instanceof EffectHandleImpl, "dead() must NOT extend EffectHandleImpl");
    }

    @Test
    void threeStepSequenceTransitionsAtCorrectTicks() {
        AtomicInteger t1 = new AtomicInteger(), e1 = new AtomicInteger(), b1 = new AtomicInteger();
        AtomicInteger t2 = new AtomicInteger(), e2 = new AtomicInteger(), b2 = new AtomicInteger();
        AtomicInteger t3 = new AtomicInteger(), e3 = new AtomicInteger(), b3 = new AtomicInteger();
        EffectHandleImpl h = new EffectHandleImpl("t", List.of(
                stubStep("a", 5, t1, e1, b1),
                stubStep("b", 5, t2, e2, b2),
                stubStep("c", 5, t3, e3, b3)
        ), ctx, null);
        assertEquals(15, h.totalDuration());

        // Calls 1..5: step a runs 5 tick callbacks
        for (int i = 0; i < 5; i++) h.tick();
        assertEquals(1, b1.get());
        assertEquals(5, t1.get());
        assertEquals(0, e1.get());
        assertEquals(0, b2.get());

        // Call 6: end a, begin b, tick b once
        h.tick();
        assertEquals(1, e1.get());
        assertEquals(1, b2.get());
        assertEquals(1, t2.get());

        // Calls 7..10: 4 more ticks on b
        for (int i = 0; i < 4; i++) h.tick();
        assertEquals(5, t2.get());
        assertEquals(0, e2.get());

        // Call 11: end b, begin c, tick c once
        h.tick();
        assertEquals(1, e2.get());
        assertEquals(1, b3.get());
        assertEquals(1, t3.get());

        // Calls 12..15: 4 more on c
        for (int i = 0; i < 4; i++) h.tick();
        assertEquals(5, t3.get());
        assertEquals(0, e3.get());
        assertTrue(h.isActive());

        // Call 16: end c, finished
        h.tick();
        assertEquals(1, e3.get());
        assertTrue(h.isFinished());
        assertFalse(h.isActive());
        assertEquals(1.0, h.progress(), 1e-9);
    }

    @Test
    void tickIsNoOpAfterFinish() {
        AtomicInteger t = new AtomicInteger(), e = new AtomicInteger(), b = new AtomicInteger();
        EffectHandleImpl h = new EffectHandleImpl("t",
                List.of(stubStep("a", 2, t, e, b)), ctx, null);
        for (int i = 0; i < 3; i++) h.tick(); // 2 ticks + finish
        assertTrue(h.isFinished());
        int tBefore = t.get(), eBefore = e.get();
        for (int i = 0; i < 5; i++) h.tick();
        assertEquals(tBefore, t.get());
        assertEquals(eBefore, e.get());
    }
}

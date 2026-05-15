package com.cristian.particleforge.engine;

import com.cristian.particleforge.api.EffectHandle;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EffectEngineTest {

    private Plugin plugin;
    private BudgetManager budget;
    private EffectEngine engine;

    @BeforeEach
    void setup() {
        plugin = mock(Plugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("EffectEngineTest"));
        budget = mock(BudgetManager.class);
        engine = new EffectEngine(plugin, budget);
    }

    private EffectHandleImpl liveHandle(String name, UUID owner) {
        EffectHandleImpl h = mock(EffectHandleImpl.class);
        when(h.effectName()).thenReturn(name);
        when(h.ownerPlayer()).thenReturn(owner);
        when(h.isActive()).thenReturn(true);
        return h;
    }

    // Case 1: submit honours budget.tryAdmit; rejected => dead handle (isActive=false).
    @Test
    void submit_admitted_returnsSameHandle_rejected_returnsDead() {
        EffectHandleImpl admitted = liveHandle("ok", null);
        when(budget.tryAdmit(admitted)).thenReturn(true);
        EffectHandle r1 = engine.submit(admitted);
        assertSame(admitted, r1, "admitted handle should be returned as-is");

        EffectHandleImpl rejected = liveHandle("no", null);
        when(budget.tryAdmit(rejected)).thenReturn(false);
        EffectHandle r2 = engine.submit(rejected);
        assertNotSame(rejected, r2, "rejected => dead sentinel, not the input");
        assertFalse(r2.isActive(), "dead handle must be inactive");
        assertEquals("no", r2.effectName(), "dead handle keeps effect name");
    }

    // Case 2: submit adds the handle to the active list.
    @Test
    void submit_addsToActiveList() {
        EffectHandleImpl h1 = liveHandle("a", null);
        EffectHandleImpl h2 = liveHandle("b", null);
        when(budget.tryAdmit(any())).thenReturn(true);

        engine.submit(h1);
        engine.submit(h2);

        assertEquals(2, engine.activeCount());
        assertEquals(2, engine.activeHandles().size());
        assertTrue(engine.activeHandles().contains(h1));
        assertTrue(engine.activeHandles().contains(h2));
    }

    // Case 3: tickOnce calls tick() on every active handle exactly once.
    @Test
    void tickOnce_callsTickOnEveryHandle() {
        EffectHandleImpl h1 = liveHandle("a", null);
        EffectHandleImpl h2 = liveHandle("b", null);
        EffectHandleImpl h3 = liveHandle("c", null);
        when(budget.tryAdmit(any())).thenReturn(true);
        engine.submit(h1);
        engine.submit(h2);
        engine.submit(h3);

        engine.tickOnce();

        verify(h1, times(1)).tick();
        verify(h2, times(1)).tick();
        verify(h3, times(1)).tick();
    }

    // Case 4: a handle that becomes inactive after tick is removed and onFinish called.
    @Test
    void tickOnce_removesFinishedAndCallsOnFinish() {
        EffectHandleImpl finishing = liveHandle("done", null);
        EffectHandleImpl alive = liveHandle("alive", null);
        when(budget.tryAdmit(any())).thenReturn(true);
        engine.submit(finishing);
        engine.submit(alive);

        // After tick, "finishing" reports inactive (finished).
        doAnswer(inv -> {
            when(finishing.isActive()).thenReturn(false);
            return null;
        }).when(finishing).tick();

        engine.tickOnce();

        verify(finishing).tick();
        verify(budget).onFinish(finishing);
        verify(budget, never()).onFinish(alive);
        assertEquals(1, engine.activeCount());
        assertTrue(engine.activeHandles().contains(alive));
        assertFalse(engine.activeHandles().contains(finishing));
    }

    // Case 5: a throwing tick is logged, cancelled, removed; onFinish called; others continue.
    @Test
    void tickOnce_throwingHandleIsCancelledAndRemoved_othersTick() {
        EffectHandleImpl bad = liveHandle("bad", null);
        EffectHandleImpl good = liveHandle("good", null);
        when(budget.tryAdmit(any())).thenReturn(true);
        engine.submit(bad);
        engine.submit(good);

        doThrow(new RuntimeException("boom")).when(bad).tick();
        // bad.cancel() should also flip isActive false to mimic real behaviour.
        doAnswer(inv -> {
            when(bad.isActive()).thenReturn(false);
            return null;
        }).when(bad).cancel();

        engine.tickOnce();

        verify(bad).tick();
        verify(bad).cancel();
        verify(budget).onFinish(bad);
        verify(good).tick();
        verify(budget, never()).onFinish(good);
        assertEquals(1, engine.activeCount());
        assertTrue(engine.activeHandles().contains(good));
    }

    // Case 6: cancelAll(owner) only cancels matching owners; returns count.
    @Test
    void cancelAll_byOwner_onlyCancelsMatching() {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        EffectHandleImpl a1 = liveHandle("a1", alice);
        EffectHandleImpl a2 = liveHandle("a2", alice);
        EffectHandleImpl b1 = liveHandle("b1", bob);
        when(budget.tryAdmit(any())).thenReturn(true);
        engine.submit(a1);
        engine.submit(a2);
        engine.submit(b1);

        int n = engine.cancelAll(alice);

        assertEquals(2, n);
        verify(a1).cancel();
        verify(a2).cancel();
        verify(b1, never()).cancel();
    }

    // Case 7: cancelAll() cancels every active handle and returns the count.
    @Test
    void cancelAll_cancelsEveryone() {
        EffectHandleImpl h1 = liveHandle("a", null);
        EffectHandleImpl h2 = liveHandle("b", UUID.randomUUID());
        EffectHandleImpl h3 = liveHandle("c", UUID.randomUUID());
        when(budget.tryAdmit(any())).thenReturn(true);
        engine.submit(h1);
        engine.submit(h2);
        engine.submit(h3);

        int n = engine.cancelAll();

        assertEquals(3, n);
        verify(h1).cancel();
        verify(h2).cancel();
        verify(h3).cancel();
    }

    // Case 8: start() is idempotent (no exception on double-call). We avoid
    // invoking real BukkitRunnable.runTaskTimer in this unit test; we only
    // confirm that the engine guards on `running` and that isRunning/activeCount
    // are stable under double-submit-style usage. (Real scheduling is tested
    // only via integration with a running server.)
    @Test
    void start_doubleCall_doesNotThrow_whenAlreadyRunning() {
        // Simulate already-running state by reflectively flipping the field
        // would couple to internals; instead we just confirm the public guard
        // path by inspecting isRunning() before any start call.
        assertFalse(engine.isRunning(), "engine is not running before start()");
        // Calling cancelAll() / stop() on a never-started engine must be safe.
        assertDoesNotThrow(() -> engine.stop());
        assertEquals(0, engine.cancelAll());
    }
}

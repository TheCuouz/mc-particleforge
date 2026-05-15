package com.cristian.particleforge.api;

import com.cristian.particleforge.ParticleForgePlugin;
import com.cristian.particleforge.engine.BudgetManager;
import com.cristian.particleforge.engine.EffectEngine;
import com.cristian.particleforge.engine.EffectHandleImpl;
import com.cristian.particleforge.model.EffectDescriptor;
import com.cristian.particleforge.model.EffectStep;
import com.cristian.particleforge.model.StepDescriptor;
import com.cristian.particleforge.registry.EffectRegistry;
import org.bukkit.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ParticleForgeApiTest {

    private ParticleForgePlugin plugin;
    private EffectEngine engine;
    private EffectRegistry registry;
    private BudgetManager budget;
    private ParticleForgeApi api;
    private Location loc;

    @BeforeEach
    void setup() {
        plugin = mock(ParticleForgePlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("ParticleForgeApiTest"));
        engine = mock(EffectEngine.class);
        registry = mock(EffectRegistry.class);
        budget = mock(BudgetManager.class);
        loc = mock(Location.class);
        when(loc.clone()).thenReturn(loc);
        api = new ParticleForgeApi(plugin, engine, registry, budget);
    }

    @AfterEach
    void teardown() {
        Effects.install(null);
    }

    // Case 1: play(name) with registered name → engine.submit(handle) is called and returns its result.
    @Test
    void play_registered_submitsHandleViaEngine() {
        StepDescriptor sd = new StepDescriptor("s1", "BURST", 10, Map.of(), List.of());
        EffectDescriptor desc = new EffectDescriptor("crate/win", "crate", Map.of(), List.of(sd), null);
        when(registry.find("crate/win")).thenReturn(Optional.of(desc));
        when(engine.submit(any(EffectHandleImpl.class))).thenAnswer(inv -> inv.getArgument(0));

        EffectHandle h = api.play("crate/win", loc);

        ArgumentCaptor<EffectHandleImpl> captor = ArgumentCaptor.forClass(EffectHandleImpl.class);
        verify(engine).submit(captor.capture());
        assertEquals("crate/win", captor.getValue().effectName());
        assertEquals("crate/win", h.effectName());
        assertTrue(h.isActive());
    }

    // Case 2: play(name) with unknown name → dead handle, no engine.submit call.
    @Test
    void play_unknown_returnsDeadHandle_noSubmit() {
        when(registry.find("nope")).thenReturn(Optional.empty());

        EffectHandle h = api.play("nope", loc);

        assertNotNull(h);
        assertFalse(h.isActive());
        assertEquals("nope", h.effectName());
        verify(engine, never()).submit(any());
    }

    // Case 3: playAdhoc(steps, ...) with empty steps → dead handle, no engine.submit call.
    @Test
    void playAdhoc_emptySteps_returnsDeadHandle() {
        EffectHandle h = api.playAdhoc(List.<EffectStep>of(), loc, null, null, null, null, "adhoc/test");

        assertNotNull(h);
        assertFalse(h.isActive());
        assertEquals("adhoc/test", h.effectName());
        verify(engine, never()).submit(any());
    }

    // Case 4: registerEffect(d) delegates to registry.register(d).
    @Test
    void registerEffect_delegatesToRegistry() {
        EffectDescriptor d = new EffectDescriptor("foo/bar", "foo", Map.of(), List.of(), null);
        api.registerEffect(d);
        verify(registry).register(d);
    }
}

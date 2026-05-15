package com.cristian.particleforge.api;

import com.cristian.particleforge.model.EffectStep;
import com.cristian.particleforge.primitives.BurstStep;
import com.cristian.particleforge.primitives.HelixStep;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EffectsTest {

    private ParticleForgeApi api;
    private Location loc;

    @BeforeEach
    void setup() {
        api = mock(ParticleForgeApi.class);
        loc = mock(Location.class);
        when(loc.clone()).thenReturn(loc);
        Effects.install(api);
    }

    @AfterEach
    void teardown() {
        Effects.install(null);
    }

    // Case 1: burst().at(loc).duration(15).count(50).play() → playAdhoc called with one BurstStep, dur 15.
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void burst_buildsBurstStep_withGivenDuration() {
        Effects.burst().at(loc).duration(15).count(50).play();

        ArgumentCaptor<List<EffectStep>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(api).playAdhoc(captor.capture(), eq(loc), any(), any(), any(), any(), any());
        List<EffectStep> steps = captor.getValue();
        assertEquals(1, steps.size());
        assertEquals(15, steps.get(0).durationTicks());
        assertTrue(steps.get(0) instanceof BurstStep,
            "expected BurstStep, got " + steps.get(0).getClass().getSimpleName());
    }

    // Case 2: helix().at(loc).play() → default duration 20.
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void helix_defaultDurationIs20() {
        Effects.helix().at(loc).play();

        ArgumentCaptor<List<EffectStep>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(api).playAdhoc(captor.capture(), eq(loc), any(), any(), any(), any(), any());
        List<EffectStep> steps = captor.getValue();
        assertEquals(1, steps.size());
        assertEquals(20, steps.get(0).durationTicks());
        assertTrue(steps.get(0) instanceof HelixStep);
    }

    // Case 3: named(name).at(loc).param("radius", 5.0).play() → api.play(name, loc, {radius:5.0}, ...).
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void named_callsApiPlayWithOverrides() {
        Effects.named("crate/legendary-win").at(loc).param("radius", 5.0).play();

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass((Class) Map.class);
        verify(api).play(eq("crate/legendary-win"), eq(loc), captor.capture(), any(), any(), any());
        Map<String, Object> overrides = captor.getValue();
        assertEquals(1, overrides.size());
        assertEquals(5.0, overrides.get("radius"));
    }

    // Case 4: builder.play() without at() and without follow() → IllegalStateException.
    @Test
    void play_withoutLocationOrFollow_throws() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> Effects.burst().count(10).play());
        assertTrue(ex.getMessage().contains("burst"));
        verify(api, never()).playAdhoc(any(), any(), any(), any(), any(), any(), any());
    }

    // Case 5: builder.play() with only follow(entity) → uses entity.getLocation().
    @Test
    void play_withOnlyFollow_usesEntityLocation() {
        LivingEntity entity = mock(LivingEntity.class);
        Location entityLoc = mock(Location.class);
        when(entityLoc.clone()).thenReturn(entityLoc);
        when(entity.getLocation()).thenReturn(entityLoc);

        Effects.burst().follow(entity).play();

        verify(api).playAdhoc(any(), eq(entityLoc), any(), any(), eq(entity), any(), any());
    }

    // Case 6: Effects.install(null) makes isAvailable() false and play() throws.
    @Test
    void install_null_disablesFacade() {
        Effects.install(null);
        assertFalse(Effects.isAvailable());
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> Effects.burst().at(loc).play());
        assertTrue(ex.getMessage().toLowerCase().contains("particleforge"));
    }
}

package com.cristian.particleforge.registry;

import com.cristian.particleforge.flow.DelayStep;
import com.cristian.particleforge.flow.ParallelStep;
import com.cristian.particleforge.flow.RepeatStep;
import com.cristian.particleforge.model.EffectStep;
import com.cristian.particleforge.model.StepDescriptor;
import com.cristian.particleforge.primitives.BurstStep;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StepFactoryTest {

    @Test
    void buildsBurst() {
        StepDescriptor d = new StepDescriptor("b1", "BURST", 20, Map.of(), List.of());
        EffectStep s = StepFactory.build(d, Map.of());
        assertTrue(s instanceof BurstStep, "expected BurstStep, got " + s.getClass());
        assertEquals(20, s.durationTicks());
    }

    @Test
    void buildsDelay() {
        StepDescriptor d = new StepDescriptor("d1", "DELAY", 15, Map.of(), List.of());
        EffectStep s = StepFactory.build(d, Map.of());
        assertTrue(s instanceof DelayStep, "expected DelayStep, got " + s.getClass());
        assertEquals(15, s.durationTicks());
    }

    @Test
    void buildsRepeatWithCorrectDuration() {
        int burstDuration = 10;
        StepDescriptor b1 = new StepDescriptor("b1", "BURST", burstDuration, Map.of(), List.of());
        StepDescriptor b2 = new StepDescriptor("b2", "BURST", burstDuration, Map.of(), List.of());
        StepDescriptor rep = new StepDescriptor(
            "rep", "REPEAT", 1, Map.of("times", 3), List.of(b1, b2));
        EffectStep s = StepFactory.build(rep, Map.of());
        assertTrue(s instanceof RepeatStep, "expected RepeatStep, got " + s.getClass());
        assertEquals(2 * burstDuration * 3, s.durationTicks());
    }

    @Test
    void buildsParallelWithMaxDuration() {
        StepDescriptor d1 = new StepDescriptor("d1", "DELAY", 5, Map.of(), List.of());
        StepDescriptor d2 = new StepDescriptor("d2", "DELAY", 12, Map.of(), List.of());
        StepDescriptor par = new StepDescriptor(
            "par", "PARALLEL", 1, Map.of(), List.of(d1, d2));
        EffectStep s = StepFactory.build(par, Map.of());
        assertTrue(s instanceof ParallelStep, "expected ParallelStep, got " + s.getClass());
        assertEquals(12, s.durationTicks());
    }

    @Test
    void unknownTypeThrows() {
        StepDescriptor d = new StepDescriptor("x", "NOPE", 5, Map.of(), List.of());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> StepFactory.build(d, Map.of()));
        assertTrue(ex.getMessage().contains("NOPE"), "msg should mention type: " + ex.getMessage());
    }
}

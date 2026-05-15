package com.cristian.particleforge.flow;

import com.cristian.particleforge.model.EffectContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DelayStepTest {

    @Test
    void idAndDurationAreSet() {
        DelayStep s = new DelayStep("wait", 40);
        assertEquals("wait", s.id());
        assertEquals(40, s.durationTicks());
    }

    @Test
    void tickIsNoOp() {
        DelayStep s = new DelayStep("wait", 40);
        EffectContext ctx = mock(EffectContext.class);
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 40; i++) s.tick(ctx, i);
        });
        verifyNoInteractions(ctx);
    }
}

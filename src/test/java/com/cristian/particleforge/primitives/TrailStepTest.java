package com.cristian.particleforge.primitives;

import com.cristian.particleforge.engine.BudgetManager;
import com.cristian.particleforge.model.EffectContext;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TrailStepTest {

    @Test
    void defaultsAreApplied() {
        TrailStep s = new TrailStep("t", 20, Map.of());
        assertEquals(1, s.effectiveCount(1.0));
        assertEquals(0, s.effectiveCount(0.0));
    }

    @Test
    void effectiveCountScalesAndClamps() {
        TrailStep s = new TrailStep("t", 20, Map.of("count", 10));
        assertEquals(10, s.effectiveCount(1.0));
        assertEquals(5,  s.effectiveCount(0.5));
        // 10 * 0.01 = 0.1 -> rounds to 0 -> clamped to 1
        assertEquals(1, s.effectiveCount(0.01));
    }

    @Test
    void nullFollowIsNoOpAndDoesNotTouchOrigin() {
        TrailStep s = new TrailStep("t", 20, Map.of());
        EffectContext ctx = mock(EffectContext.class);
        when(ctx.lodBucket()).thenReturn(0);
        when(ctx.lodMultiplier()).thenReturn(1.0);
        when(ctx.follow()).thenReturn(null);
        assertDoesNotThrow(() -> s.tick(ctx, 0));
        verify(ctx, never()).setOrigin(any());
    }

    @Test
    void updateOriginTrueCallsSetOrigin() {
        TrailStep s = new TrailStep("t", 20, Map.of("update-origin", true));
        EffectContext ctx = mock(EffectContext.class);
        LivingEntity ent = mock(LivingEntity.class);
        World world = mock(World.class);
        Location loc = new Location(world, 1, 2, 3);
        when(ctx.lodBucket()).thenReturn(0);
        when(ctx.lodMultiplier()).thenReturn(1.0);
        when(ctx.follow()).thenReturn(ent);
        when(ent.getLocation()).thenReturn(loc);
        when(ctx.viewers()).thenReturn(java.util.Set.of());

        s.tick(ctx, 0);
        verify(ctx).setOrigin(loc);
    }

    @Test
    void updateOriginFalseDoesNotCallSetOrigin() {
        TrailStep s = new TrailStep("t", 20, Map.of("update-origin", false));
        EffectContext ctx = mock(EffectContext.class);
        LivingEntity ent = mock(LivingEntity.class);
        World world = mock(World.class);
        Location loc = new Location(world, 1, 2, 3);
        when(ctx.lodBucket()).thenReturn(0);
        when(ctx.lodMultiplier()).thenReturn(1.0);
        when(ctx.follow()).thenReturn(ent);
        when(ent.getLocation()).thenReturn(loc);
        when(ctx.viewers()).thenReturn(java.util.Set.of());

        s.tick(ctx, 0);
        verify(ctx, never()).setOrigin(any());
    }

    @Test
    void cullBucketShortCircuits() {
        TrailStep s = new TrailStep("t", 20, Map.of());
        EffectContext ctx = mock(EffectContext.class);
        when(ctx.lodBucket()).thenReturn(BudgetManager.BUCKET_CULL);
        when(ctx.lodMultiplier()).thenReturn(1.0);
        assertDoesNotThrow(() -> s.tick(ctx, 0));
        verify(ctx, never()).follow();
    }
}

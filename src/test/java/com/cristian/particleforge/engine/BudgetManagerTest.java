package com.cristian.particleforge.engine;

import com.cristian.particleforge.cfg.ConfigManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BudgetManagerTest {

    private ConfigManager config;
    private BudgetManager mgr;

    @BeforeEach
    void setup() {
        config = mock(ConfigManager.class);
        // Sensible defaults; individual tests override what they need.
        when(config.globalMaxActive()).thenReturn(4);
        when(config.perPlayerMaxActive()).thenReturn(2);
        when(config.policy()).thenReturn("FIFO");
        when(config.lodNear()).thenReturn(16);
        when(config.lodMid()).thenReturn(48);
        when(config.lodFar()).thenReturn(96);
        when(config.cullBeyondFar()).thenReturn(true);
        when(config.nearMul()).thenReturn(1.0);
        when(config.midMul()).thenReturn(0.5);
        when(config.farMul()).thenReturn(0.2);
        mgr = new BudgetManager(config);
    }

    private EffectHandleImpl handleFor(UUID owner) {
        EffectHandleImpl h = mock(EffectHandleImpl.class);
        when(h.ownerPlayer()).thenReturn(owner);
        return h;
    }

    // 1
    @Test
    void admitsFirstHandleWhenQueuesEmpty() {
        EffectHandleImpl h = handleFor(UUID.randomUUID());
        assertTrue(mgr.tryAdmit(h));
        assertEquals(1, mgr.activeCount());
    }

    // 2
    @Test
    void fifoRejectsWhenGlobalCapReached() {
        when(config.policy()).thenReturn("FIFO");
        // Use a different owner each time so per-player cap doesn't trigger first.
        for (int i = 0; i < 4; i++) {
            assertTrue(mgr.tryAdmit(handleFor(UUID.randomUUID())));
        }
        assertEquals(4, mgr.activeCount());
        EffectHandleImpl fifth = handleFor(UUID.randomUUID());
        assertFalse(mgr.tryAdmit(fifth));
        assertEquals(4, mgr.activeCount());
    }

    // 3
    @Test
    void fifoRejectsWhenPerPlayerCapReached() {
        when(config.policy()).thenReturn("FIFO");
        UUID owner = UUID.randomUUID();
        assertTrue(mgr.tryAdmit(handleFor(owner)));
        assertTrue(mgr.tryAdmit(handleFor(owner)));
        EffectHandleImpl third = handleFor(owner);
        assertFalse(mgr.tryAdmit(third));
        assertEquals(2, mgr.activeFor(owner));
    }

    // 4
    @Test
    void newestFirstEvictsOldestGlobally() {
        when(config.policy()).thenReturn("NEWEST_FIRST");
        EffectHandleImpl[] hs = new EffectHandleImpl[4];
        for (int i = 0; i < 4; i++) {
            hs[i] = handleFor(UUID.randomUUID());
            assertTrue(mgr.tryAdmit(hs[i]));
        }
        EffectHandleImpl newcomer = handleFor(UUID.randomUUID());
        assertTrue(mgr.tryAdmit(newcomer));
        verify(hs[0], times(1)).cancel();
        // No other victims.
        verify(hs[1], never()).cancel();
        verify(hs[2], never()).cancel();
        verify(hs[3], never()).cancel();
        assertEquals(4, mgr.activeCount());
    }

    // 5
    @Test
    void newestFirstEvictsOldestOfThatPlayer() {
        when(config.policy()).thenReturn("NEWEST_FIRST");
        UUID player = UUID.randomUUID();
        EffectHandleImpl first = handleFor(player);
        EffectHandleImpl second = handleFor(player);
        // A handle from another player admitted before "first" — so globally "other" is oldest.
        EffectHandleImpl other = handleFor(UUID.randomUUID());
        assertTrue(mgr.tryAdmit(other));
        assertTrue(mgr.tryAdmit(first));
        assertTrue(mgr.tryAdmit(second));
        EffectHandleImpl third = handleFor(player);
        assertTrue(mgr.tryAdmit(third));
        // The player's oldest (first) must be evicted, NOT the globally oldest (other).
        verify(first, times(1)).cancel();
        verify(other, never()).cancel();
        assertEquals(2, mgr.activeFor(player));
    }

    // 6
    @Test
    void onFinishRemovesAndFreesSlot() {
        when(config.policy()).thenReturn("FIFO");
        UUID owner = UUID.randomUUID();
        EffectHandleImpl a = handleFor(owner);
        EffectHandleImpl b = handleFor(owner);
        assertTrue(mgr.tryAdmit(a));
        assertTrue(mgr.tryAdmit(b));
        // Cap reached for that player.
        assertFalse(mgr.tryAdmit(handleFor(owner)));
        mgr.onFinish(a);
        assertEquals(1, mgr.activeFor(owner));
        assertEquals(1, mgr.activeCount());
        // Now a new admit should succeed.
        assertTrue(mgr.tryAdmit(handleFor(owner)));
    }

    // 7
    @Test
    void onFinishIsIdempotent() {
        UUID owner = UUID.randomUUID();
        EffectHandleImpl a = handleFor(owner);
        assertTrue(mgr.tryAdmit(a));
        mgr.onFinish(a);
        // Calling again is a no-op.
        assertDoesNotThrow(() -> mgr.onFinish(a));
        // Unknown handle.
        EffectHandleImpl unknown = handleFor(UUID.randomUUID());
        assertDoesNotThrow(() -> mgr.onFinish(unknown));
        assertEquals(0, mgr.activeCount());
    }

    private Location locAtDistance(World world, double d) {
        Location loc = mock(Location.class);
        when(loc.getWorld()).thenReturn(world);
        when(loc.distance(any(Location.class))).thenReturn(d);
        return loc;
    }

    // 8
    @Test
    void lodBucketRanges() {
        World w = mock(World.class);
        Player viewer = mock(Player.class);
        Location vLoc = mock(Location.class);
        when(vLoc.getWorld()).thenReturn(w);
        when(viewer.getWorld()).thenReturn(w);
        when(viewer.getLocation()).thenReturn(vLoc);

        // near (< 16)
        assertEquals(0, mgr.lodBucket(locAtDistance(w, 5.0), viewer));
        // mid (16..48)
        assertEquals(1, mgr.lodBucket(locAtDistance(w, 16.0), viewer));
        assertEquals(1, mgr.lodBucket(locAtDistance(w, 30.0), viewer));
        // far (48..96)
        assertEquals(2, mgr.lodBucket(locAtDistance(w, 48.0), viewer));
        assertEquals(2, mgr.lodBucket(locAtDistance(w, 80.0), viewer));
        // beyond far + cull
        when(config.cullBeyondFar()).thenReturn(true);
        assertEquals(BudgetManager.BUCKET_CULL, mgr.lodBucket(locAtDistance(w, 200.0), viewer));
        // beyond far + no cull → clamp to far (2)
        when(config.cullBeyondFar()).thenReturn(false);
        assertEquals(2, mgr.lodBucket(locAtDistance(w, 200.0), viewer));
    }

    // 9
    @Test
    void lodBucketDifferentWorld() {
        World w1 = mock(World.class);
        World w2 = mock(World.class);
        Player viewer = mock(Player.class);
        Location vLoc = mock(Location.class);
        when(vLoc.getWorld()).thenReturn(w2);
        when(viewer.getLocation()).thenReturn(vLoc);
        Location origin = mock(Location.class);
        when(origin.getWorld()).thenReturn(w1);

        when(config.cullBeyondFar()).thenReturn(true);
        assertEquals(BudgetManager.BUCKET_CULL, mgr.lodBucket(origin, viewer));
        when(config.cullBeyondFar()).thenReturn(false);
        assertEquals(2, mgr.lodBucket(origin, viewer));
    }

    // 10
    @Test
    void lodMultiplierPerBucket() {
        assertEquals(1.0, mgr.lodMultiplier(0));
        assertEquals(0.5, mgr.lodMultiplier(1));
        assertEquals(0.2, mgr.lodMultiplier(2));
        assertEquals(0.0, mgr.lodMultiplier(BudgetManager.BUCKET_CULL));
        // Defensive: any other value → 0.0
        assertEquals(0.0, mgr.lodMultiplier(99));
    }

    @Test
    void resolvePolicyIsCaseInsensitive() {
        when(config.policy()).thenReturn("fifo");
        // Fill the global cap, then admit one more. If "fifo" was parsed correctly, returns false.
        for (int i = 0; i < 4; i++) mgr.tryAdmit(handleFor(UUID.randomUUID()));
        assertFalse(mgr.tryAdmit(handleFor(UUID.randomUUID())));

        // newest_first lowercase → eviction happens.
        BudgetManager mgr2 = new BudgetManager(config);
        when(config.policy()).thenReturn("newest_first");
        EffectHandleImpl first = handleFor(UUID.randomUUID());
        mgr2.tryAdmit(first);
        for (int i = 0; i < 3; i++) mgr2.tryAdmit(handleFor(UUID.randomUUID()));
        EffectHandleImpl evictor = handleFor(UUID.randomUUID());
        mgr2.tryAdmit(evictor);
        verify(first, times(1)).cancel();
    }
}

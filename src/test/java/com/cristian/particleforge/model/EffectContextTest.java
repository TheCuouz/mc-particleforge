package com.cristian.particleforge.model;

import com.cristian.particleforge.api.EffectHandle;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EffectContextTest {
    private Plugin plugin;
    private Location origin;

    @BeforeEach
    void setup() {
        plugin = mock(Plugin.class);
        origin = mock(Location.class);
        when(origin.clone()).thenReturn(origin);
    }

    @Test
    void paramReturnsValueWhenPresent() {
        EffectContext ctx = new EffectContext(plugin, origin, Set.of(),
                Map.of("count", 42), null, null);
        assertEquals(42, (int) ctx.param("count", 0));
    }

    @Test
    void paramReturnsFallbackWhenMissing() {
        EffectContext ctx = new EffectContext(plugin, origin, Set.of(),
                Map.of(), null, null);
        assertEquals(7, (int) ctx.param("missing", 7));
        assertEquals("default", ctx.param("nope", "default"));
    }

    @Test
    void paramWidensNumericTypes() {
        Map<String, Object> params = new HashMap<>();
        params.put("asInt", 5);
        params.put("asLong", 100L);
        params.put("asDouble", 3.5d);
        params.put("asFloat", 1.25f);
        EffectContext ctx = new EffectContext(plugin, origin, Set.of(), params, null, null);

        // int -> double
        assertEquals(5.0d, ctx.param("asInt", 0.0d), 1e-9);
        // long -> int
        assertEquals(100, (int) ctx.param("asLong", 0));
        // double -> float
        assertEquals(3.5f, ctx.param("asDouble", 0.0f), 1e-6f);
        // float -> long
        assertEquals(1L, (long) ctx.param("asFloat", 0L));
        // int -> long
        assertEquals(5L, (long) ctx.param("asInt", 0L));
    }

    @Test
    void paramReturnsFallbackOnIncompatibleType() {
        EffectContext ctx = new EffectContext(plugin, origin, Set.of(),
                Map.of("text", "not-a-number"), null, null);
        // String cannot widen to Double — fallback wins
        assertEquals(2.5d, ctx.param("text", 2.5d), 1e-9);
    }

    @Test
    void viewersAreImmutableCopy() {
        Set<UUID> viewers = new HashSet<>();
        UUID a = UUID.randomUUID();
        viewers.add(a);
        EffectContext ctx = new EffectContext(plugin, origin, viewers, Map.of(), null, null);

        // mutating the source must not affect the context
        viewers.add(UUID.randomUUID());
        assertEquals(1, ctx.viewers().size());
        assertTrue(ctx.viewers().contains(a));

        // returned view itself is immutable
        assertThrows(UnsupportedOperationException.class,
                () -> ctx.viewers().add(UUID.randomUUID()));
    }

    @Test
    void paramsReturnsImmutableView() {
        Map<String, Object> src = new HashMap<>();
        src.put("k", "v");
        EffectContext ctx = new EffectContext(plugin, origin, Set.of(), src, null, null);

        // mutating source after construction must not affect context
        src.put("k2", "v2");
        assertFalse(ctx.params().containsKey("k2"));

        // returned view is immutable
        Map<String, Object> view = ctx.params();
        assertThrows(UnsupportedOperationException.class, () -> view.put("x", "y"));
    }

    @Test
    void setOriginUpdatesAndClones() {
        EffectContext ctx = new EffectContext(plugin, origin, Set.of(), Map.of(), null, null);

        Location next = mock(Location.class);
        Location nextClone = mock(Location.class);
        when(next.clone()).thenReturn(nextClone);

        ctx.setOrigin(next);
        assertSame(nextClone, ctx.origin(),
                "setOrigin must store the defensive clone, not the input");
        verify(next, times(1)).clone();
    }

    @Test
    void lodDefaultsToNearAndFullMultiplier() {
        EffectContext ctx = new EffectContext(plugin, origin, Set.of(), Map.of(), null, null);
        assertEquals(0, ctx.lodBucket());
        assertEquals(1.0, ctx.lodMultiplier(), 1e-9);
    }

    @Test
    void setLodUpdatesBothFields() {
        EffectContext ctx = new EffectContext(plugin, origin, Set.of(), Map.of(), null, null);
        ctx.setLod(2, 0.25);
        assertEquals(2, ctx.lodBucket());
        assertEquals(0.25, ctx.lodMultiplier(), 1e-9);
    }

    @Test
    void attachHandleUpdatesReference() {
        EffectContext ctx = new EffectContext(plugin, origin, Set.of(), Map.of(), null, null);
        assertNull(ctx.handle());

        EffectHandle handle = mock(EffectHandle.class);
        ctx.attachHandle(handle);
        assertSame(handle, ctx.handle());
    }
}

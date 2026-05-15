package com.cristian.particleforge.registry;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParamResolverTest {

    @Test
    void singlePlaceholderReturnsTypedValue() {
        Map<String, Object> raw = Map.of("radius", "${radius}");
        Map<String, Object> defaults = Map.of("radius", 2.0);
        Map<String, Object> out = ParamResolver.resolve(raw, Map.of(), defaults);
        Object v = out.get("radius");
        assertTrue(v instanceof Double, "expected Double, got " + (v == null ? "null" : v.getClass()));
        assertEquals(2.0, (Double) v, 1e-9);
    }

    @Test
    void overrideWinsOverDefaults() {
        Map<String, Object> raw = Map.of("radius", "${radius}");
        Map<String, Object> defaults = Map.of("radius", 2.0);
        Map<String, Object> overrides = Map.of("radius", 5.0);
        Map<String, Object> out = ParamResolver.resolve(raw, overrides, defaults);
        assertEquals(5.0, (Double) out.get("radius"), 1e-9);
    }

    @Test
    void missingKeyThrows() {
        Map<String, Object> raw = Map.of("x", "${nope}");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> ParamResolver.resolve(raw, Map.of(), Map.of()));
        assertTrue(ex.getMessage().contains("nope"), "msg should mention placeholder: " + ex.getMessage());
    }

    @Test
    void cycleDetected() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("a", "${b}");
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("b", "${a}");
        defaults.put("a", "${b}"); // a must exist in ctx so we can recurse into it
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> ParamResolver.resolve(raw, Map.of(), defaults));
        assertTrue(ex.getMessage().toLowerCase().contains("cycle")
                || ex.getMessage().toLowerCase().contains("circular"),
            "msg should mention cycle: " + ex.getMessage());
    }

    @Test
    void plainValuesPassThrough() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("n", 42);
        raw.put("b", true);
        Map<String, Object> out = ParamResolver.resolve(raw, Map.of(), Map.of());
        assertEquals(42, out.get("n"));
        assertEquals(Boolean.TRUE, out.get("b"));
    }

    @Test
    void listRecursion() {
        Map<String, Object> raw = Map.of("xs", List.of("${a}", "${b}", "literal"));
        Map<String, Object> defaults = Map.of("a", 1, "b", 2);
        Map<String, Object> out = ParamResolver.resolve(raw, Map.of(), defaults);
        Object xs = out.get("xs");
        assertTrue(xs instanceof List<?>);
        List<?> list = (List<?>) xs;
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals("literal", list.get(2));
    }

    @Test
    void inlineInterpolationReturnsString() {
        Map<String, Object> raw = Map.of("label", "size-${radius}");
        Map<String, Object> defaults = Map.of("radius", 2.0);
        Map<String, Object> out = ParamResolver.resolve(raw, Map.of(), defaults);
        Object v = out.get("label");
        assertTrue(v instanceof String, "expected String, got " + (v == null ? "null" : v.getClass()));
        assertEquals("size-2.0", v);
    }

    @Test
    void subMapRecursion() {
        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("r", "${radius}");
        Map<String, Object> raw = Map.of("nested", inner);
        Map<String, Object> defaults = Map.of("radius", 3.5);
        Map<String, Object> out = ParamResolver.resolve(raw, Map.of(), defaults);
        Object n = out.get("nested");
        assertTrue(n instanceof Map<?, ?>);
        Map<?, ?> resolved = (Map<?, ?>) n;
        assertEquals(3.5, (Double) resolved.get("r"), 1e-9);
    }
}

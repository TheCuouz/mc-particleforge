package com.cristian.particleforge.primitives;

import org.bukkit.Particle;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParamUtilTest {

    @Test
    void intValReadsNumberStringAndFallsBack() {
        Map<String, Object> p = new HashMap<>();
        p.put("a", 42);
        p.put("b", "17");
        p.put("c", "not-a-number");

        assertEquals(42, ParamUtil.intVal(p, "a", -1));
        assertEquals(17, ParamUtil.intVal(p, "b", -1));
        // non-numeric string -> fallback
        assertEquals(-1, ParamUtil.intVal(p, "c", -1));
        // missing key -> fallback
        assertEquals(99, ParamUtil.intVal(p, "missing", 99));
    }

    @Test
    void particleFallsBackOnUnknownNameWithoutThrowing() {
        Map<String, Object> p = Map.of("particle", "NOT_A_REAL_PARTICLE_XYZ");
        Particle result = ParamUtil.particle(p, "particle", Particle.FLAME);
        assertEquals(Particle.FLAME, result);

        // Valid name resolves
        Map<String, Object> p2 = Map.of("particle", "flame");
        assertEquals(Particle.FLAME, ParamUtil.particle(p2, "particle", Particle.HEART));

        // Missing key -> fallback
        assertEquals(Particle.HEART, ParamUtil.particle(Map.of(), "particle", Particle.HEART));
    }
}

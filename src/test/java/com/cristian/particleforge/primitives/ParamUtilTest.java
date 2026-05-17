package com.cristian.particleforge.primitives;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParamUtilTest {

    @Test
    void colorParsesHexWithAndWithoutHash() {
        Map<String, Object> p = Map.of("a", "#66E1FF", "b", "66E1FF", "c", "0x66e1ff");
        Color fb = Color.fromRGB(0, 0, 0);
        Color expected = Color.fromRGB(0x66, 0xE1, 0xFF);
        assertEquals(expected, ParamUtil.color(p, "a", fb));
        assertEquals(expected, ParamUtil.color(p, "b", fb));
        assertEquals(expected, ParamUtil.color(p, "c", fb));
    }

    @Test
    void colorParsesNamedColors() {
        Map<String, Object> p = Map.of("a", "red", "b", "AQUA", "c", "light_blue");
        Color fb = Color.BLACK;
        assertEquals(Color.RED, ParamUtil.color(p, "a", fb));
        assertEquals(Color.AQUA, ParamUtil.color(p, "b", fb));
        // Bukkit Color doesn't ship LIGHT_BLUE so we resolve to an explicit value
        Color lightBlue = ParamUtil.color(p, "c", fb);
        assertNotEquals(fb, lightBlue);
    }

    @Test
    void colorFallsBackOnGarbageOrMissing() {
        Color fb = Color.fromRGB(0x66, 0xE1, 0xFF);
        assertEquals(fb, ParamUtil.color(Map.of("a", "not-a-color"), "a", fb));
        assertEquals(fb, ParamUtil.color(Map.of(), "a", fb));
        assertEquals(fb, ParamUtil.color(Map.of("a", "#ZZZ"), "a", fb));
    }

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

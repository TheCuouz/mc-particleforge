package com.cristian.particleforge.primitives;

import org.bukkit.Particle;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Helpers for reading typed params out of a {@code Map<String,Object>}.
 * Used by all step primitives.
 */
public final class ParamUtil {

    private static final Logger LOG = Logger.getLogger("ParticleForge");

    private ParamUtil() {}

    public static int intVal(Map<String, Object> p, String key, int fallback) {
        Object v = p.get(key);
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) {
            try { return Integer.parseInt(s.trim()); } catch (NumberFormatException ignored) {}
        }
        return fallback;
    }

    public static double doubleVal(Map<String, Object> p, String key, double fallback) {
        Object v = p.get(key);
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) {
            try { return Double.parseDouble(s.trim()); } catch (NumberFormatException ignored) {}
        }
        return fallback;
    }

    public static String stringVal(Map<String, Object> p, String key, String fallback) {
        Object v = p.get(key);
        return v == null ? fallback : v.toString();
    }

    public static boolean boolVal(Map<String, Object> p, String key, boolean fallback) {
        Object v = p.get(key);
        if (v instanceof Boolean b) return b;
        if (v instanceof String s) return Boolean.parseBoolean(s.trim());
        return fallback;
    }

    public static List<?> listVal(Map<String, Object> p, String key, List<?> fallback) {
        Object v = p.get(key);
        return v instanceof List<?> l ? l : fallback;
    }

    /**
     * Parses a Bukkit {@link Particle} enum by name (case-insensitive).
     * Falls back if invalid; logs a warning once per unknown name.
     */
    public static Particle particle(Map<String, Object> p, String key, Particle fallback) {
        String raw = stringVal(p, key, null);
        if (raw == null) return fallback;
        try {
            return Particle.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            LOG.warning("Unknown particle '" + raw + "' — falling back to " + fallback.name());
            return fallback;
        }
    }
}

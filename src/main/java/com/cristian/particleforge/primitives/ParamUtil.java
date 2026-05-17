package com.cristian.particleforge.primitives;

import org.bukkit.Color;
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
     * Parses a {@link Color} from hex ({@code #RRGGBB}, {@code RRGGBB}, {@code 0xRRGGBB})
     * or a small set of named colors. Falls back on garbage or missing key.
     */
    public static Color color(Map<String, Object> p, String key, Color fallback) {
        String raw = stringVal(p, key, null);
        if (raw == null) return fallback;
        String s = raw.trim();
        if (s.startsWith("#")) s = s.substring(1);
        else if (s.startsWith("0x") || s.startsWith("0X")) s = s.substring(2);
        if (s.length() == 6 && s.chars().allMatch(ParamUtil::isHex)) {
            try {
                int rgb = Integer.parseInt(s, 16);
                return Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
            } catch (NumberFormatException ignored) {}
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "WHITE" -> Color.WHITE;
            case "BLACK" -> Color.BLACK;
            case "RED" -> Color.RED;
            case "GREEN" -> Color.GREEN;
            case "BLUE" -> Color.BLUE;
            case "YELLOW" -> Color.YELLOW;
            case "ORANGE" -> Color.ORANGE;
            case "PURPLE" -> Color.PURPLE;
            case "FUCHSIA", "MAGENTA" -> Color.FUCHSIA;
            case "AQUA", "CYAN" -> Color.AQUA;
            case "LIME" -> Color.LIME;
            case "GRAY", "GREY" -> Color.GRAY;
            case "SILVER", "LIGHT_GRAY", "LIGHT_GREY" -> Color.SILVER;
            case "NAVY" -> Color.NAVY;
            case "TEAL" -> Color.TEAL;
            case "MAROON" -> Color.MAROON;
            case "OLIVE" -> Color.OLIVE;
            case "LIGHT_BLUE" -> Color.fromRGB(0x87, 0xCE, 0xFA);
            case "PINK" -> Color.fromRGB(0xFF, 0xC0, 0xCB);
            default -> fallback;
        };
    }

    private static boolean isHex(int c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
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

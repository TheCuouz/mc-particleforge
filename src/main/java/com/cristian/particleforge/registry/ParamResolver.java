package com.cristian.particleforge.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Substitutes <code>${name}</code> placeholders in a parameter map using an
 * effective context = overrides ∪ defaults (overrides win).
 *
 * Lists and nested maps recurse element-wise. Numbers and other non-string
 * values pass through unchanged. A circular reference (a → b → a) throws
 * {@link IllegalStateException}.
 *
 * The substitution is two-pass: it replaces ${...} tokens repeatedly until the
 * value is fixed or the depth limit is reached (default 8).
 */
public final class ParamResolver {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([a-zA-Z0-9_-]+)\\}");
    private static final int MAX_DEPTH = 8;

    private ParamResolver() {}

    public static Map<String, Object> resolve(Map<String, Object> raw,
                                               Map<String, Object> overrides,
                                               Map<String, Object> defaults) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        if (defaults != null) ctx.putAll(defaults);
        if (overrides != null) ctx.putAll(overrides);

        Map<String, Object> out = new LinkedHashMap<>();
        if (raw != null) {
            for (Map.Entry<String, Object> e : raw.entrySet()) {
                out.put(e.getKey(), resolveValue(e.getValue(), ctx, new ArrayList<>()));
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Object resolveValue(Object value, Map<String, Object> ctx, List<String> visiting) {
        if (value instanceof String s) {
            return resolveString(s, ctx, visiting, 0);
        }
        if (value instanceof List<?> list) {
            List<Object> resolved = new ArrayList<>(list.size());
            for (Object item : list) resolved.add(resolveValue(item, ctx, visiting));
            return resolved;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> resolved = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                resolved.put(e.getKey().toString(), resolveValue(e.getValue(), ctx, visiting));
            }
            return resolved;
        }
        return value; // numbers, booleans, null
    }

    private static Object resolveString(String src, Map<String, Object> ctx,
                                         List<String> visiting, int depth) {
        if (depth > MAX_DEPTH) {
            throw new IllegalStateException("ParamResolver: exceeded max depth (" + MAX_DEPTH +
                ") resolving '" + src + "'. Likely circular reference: " + String.join(" → ", visiting));
        }
        Matcher m = PLACEHOLDER.matcher(src);
        if (!m.find()) return src;

        // Special case: the entire string is a single placeholder → return the typed value.
        if (m.start() == 0 && m.end() == src.length()) {
            String key = m.group(1);
            return resolveKey(key, src, ctx, visiting, depth);
        }

        // Otherwise we're doing string interpolation; result is always a String.
        m.reset();
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String key = m.group(1);
            Object resolved = resolveKey(key, src, ctx, visiting, depth);
            m.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(resolved)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static Object resolveKey(String key, String src, Map<String, Object> ctx,
                                      List<String> visiting, int depth) {
        if (visiting.contains(key)) {
            visiting.add(key);
            throw new IllegalStateException("ParamResolver: cycle detected: " +
                String.join(" → ", visiting));
        }
        if (!ctx.containsKey(key)) {
            throw new IllegalArgumentException(
                "ParamResolver: unresolved placeholder ${" + key + "} in '" + src + "'");
        }
        Object v = ctx.get(key);
        visiting.add(key);
        try {
            if (v instanceof String s) return resolveString(s, ctx, visiting, depth + 1);
            return resolveValue(v, ctx, visiting);
        } finally {
            visiting.remove(visiting.size() - 1);
        }
    }
}

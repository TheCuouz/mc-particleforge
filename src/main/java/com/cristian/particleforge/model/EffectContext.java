package com.cristian.particleforge.model;

// Forward ref — EffectHandle is defined in Task 5 (com.cristian.particleforge.api).
import com.cristian.particleforge.api.EffectHandle;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Mutable per-handle context passed to each step's begin/tick/end.
 * NOT thread-safe; the engine ticks all steps on the main thread.
 */
public final class EffectContext {

    private final Plugin plugin;
    private Location origin;                   // mutable (TrailStep updates each tick)
    private final Set<UUID> viewers;           // empty = world-broadcast
    private final Map<String, Object> params;
    private final LivingEntity follow;         // nullable; used by TrailStep
    private EffectHandle handle;               // back-ref; set by engine after construct

    public EffectContext(Plugin plugin,
                         Location origin,
                         Set<UUID> viewers,
                         Map<String, Object> params,
                         LivingEntity follow,
                         EffectHandle handle) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.origin = Objects.requireNonNull(origin, "origin").clone(); // defensive copy
        this.viewers = viewers == null ? Set.of() : Set.copyOf(viewers);
        this.params = params == null ? new HashMap<>() : new HashMap<>(params);
        this.follow = follow;
        this.handle = handle;
    }

    public Plugin plugin() { return plugin; }
    public Location origin() { return origin; }
    public Set<UUID> viewers() { return viewers; }
    public LivingEntity follow() { return follow; }
    public EffectHandle handle() { return handle; }

    /** Engine calls this after the handle is constructed to close the cycle. */
    public void attachHandle(EffectHandle handle) {
        this.handle = handle;
    }

    /** TrailStep updates origin to follow the entity. */
    public void setOrigin(Location loc) {
        this.origin = Objects.requireNonNull(loc).clone();
    }

    /**
     * Typed param lookup with fallback. Returns {@code fallback} if key absent
     * or value is not assignable to fallback's runtime type.
     */
    @SuppressWarnings("unchecked")
    public <T> T param(String key, T fallback) {
        Object v = params.get(key);
        if (v == null) return fallback;
        if (fallback == null) return (T) v;
        if (fallback.getClass().isInstance(v)) return (T) v;
        // numeric widening: Integer→Double etc.
        if (fallback instanceof Number && v instanceof Number) {
            Number n = (Number) v;
            if (fallback instanceof Double)  return (T) (Double) n.doubleValue();
            if (fallback instanceof Integer) return (T) (Integer) n.intValue();
            if (fallback instanceof Long)    return (T) (Long) n.longValue();
            if (fallback instanceof Float)   return (T) (Float) n.floatValue();
        }
        return fallback;
    }

    /** Raw params view, immutable. */
    public Map<String, Object> params() {
        return Map.copyOf(params);
    }
}

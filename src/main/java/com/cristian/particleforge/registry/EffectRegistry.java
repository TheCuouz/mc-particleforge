package com.cristian.particleforge.registry;

import com.cristian.particleforge.model.EffectDescriptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * In-memory store of {@link EffectDescriptor}s keyed by name (e.g.
 * {@code "crate/legendary-win"}). Thread-safe.
 */
public final class EffectRegistry {

    private static final Logger LOG = Logger.getLogger("ParticleForge");

    private final ConcurrentHashMap<String, EffectDescriptor> byName = new ConcurrentHashMap<>();

    public void register(EffectDescriptor d) {
        if (d == null) return;
        EffectDescriptor prev = byName.put(d.name(), d);
        if (prev != null) {
            LOG.warning("EffectRegistry: overwriting existing effect '" + d.name() + "'");
        }
    }

    public Optional<EffectDescriptor> find(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(byName.get(name));
    }

    public Collection<EffectDescriptor> all() {
        return new ArrayList<>(byName.values());
    }

    public Collection<EffectDescriptor> byCategory(String category) {
        if (category == null) return List.of();
        List<EffectDescriptor> out = new ArrayList<>();
        for (EffectDescriptor d : byName.values()) {
            if (category.equals(d.category())) out.add(d);
        }
        return out;
    }

    public int size() { return byName.size(); }

    public Set<String> categories() {
        Set<String> out = new HashSet<>();
        for (EffectDescriptor d : byName.values()) out.add(d.category());
        return out;
    }

    public void clear() { byName.clear(); }
}

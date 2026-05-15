package com.cristian.particleforge.model;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable representation of an effect parsed from YAML, with inheritance
 * already resolved. {@code extendsName} is preserved for debug/info display
 * only — at this point, {@code defaults} and {@code steps} are flattened.
 */
public record EffectDescriptor(
    String name,
    String category,
    Map<String, Object> defaults,
    List<StepDescriptor> steps,
    String extendsName   // nullable
) {
    public EffectDescriptor {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(category, "category");
        defaults = defaults == null ? Map.of() : Map.copyOf(defaults);
        steps    = steps    == null ? List.of() : List.copyOf(steps);
    }

    /** View of steps by id (preserves insertion order). */
    public Map<String, StepDescriptor> stepsById() {
        LinkedHashMap<String, StepDescriptor> out = new LinkedHashMap<>(steps.size());
        for (StepDescriptor s : steps) out.put(s.id(), s);
        return out;
    }

    /** Total duration in ticks summed across top-level steps (flow steps report their own total). */
    public int totalDurationTicks() {
        int sum = 0;
        for (StepDescriptor s : steps) sum += s.durationTicks();
        return sum;
    }
}

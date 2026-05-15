package com.cristian.particleforge.model;

import java.util.List;
import java.util.Map;

/**
 * Immutable representation of a step parsed from YAML.
 * {@code rawParams} may contain unresolved {@code ${param}} placeholders;
 * {@code children} is populated for flow steps (REPEAT, PARALLEL) and empty
 * for primitives.
 */
public record StepDescriptor(
    String id,
    String type,
    int durationTicks,
    Map<String, Object> rawParams,
    List<StepDescriptor> children
) {
    public StepDescriptor {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("step id required");
        if (type == null || type.isBlank()) throw new IllegalArgumentException("step type required");
        if (durationTicks <= 0) throw new IllegalArgumentException("durationTicks must be > 0 (got " + durationTicks + ")");
        rawParams = rawParams == null ? Map.of() : Map.copyOf(rawParams);
        children  = children  == null ? List.of() : List.copyOf(children);
    }
}

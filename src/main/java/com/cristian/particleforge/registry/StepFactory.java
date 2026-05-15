package com.cristian.particleforge.registry;

import com.cristian.particleforge.flow.DelayStep;
import com.cristian.particleforge.flow.ParallelStep;
import com.cristian.particleforge.flow.RepeatStep;
import com.cristian.particleforge.model.EffectStep;
import com.cristian.particleforge.model.StepDescriptor;
import com.cristian.particleforge.primitives.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds concrete {@link EffectStep} instances from {@link StepDescriptor}s,
 * resolving {@code ${param}} placeholders against {@code defaults} (merged
 * with {@code overrides}) for primitive types.
 *
 * Flow steps (REPEAT/PARALLEL) recurse to build their children.
 *
 * IMPORTANT: flow steps own per-handle mutable state (iteration counters,
 * began/ended flags). Callers MUST build a fresh tree per handle — never share
 * step instances across {@link com.cristian.particleforge.engine.EffectHandleImpl}s.
 */
public final class StepFactory {

    private StepFactory() {}

    /** Build a single step, with overrides applied on top of effect-level defaults. */
    public static EffectStep build(StepDescriptor d,
                                    Map<String, Object> defaults,
                                    Map<String, Object> overrides) {
        Map<String, Object> resolved = ParamResolver.resolve(d.rawParams(), overrides, defaults);
        return switch (d.type().toUpperCase(Locale.ROOT)) {
            case "BURST"     -> new BurstStep(d.id(), d.durationTicks(), resolved);
            case "RING"      -> new RingStep(d.id(), d.durationTicks(), resolved);
            case "SPHERE"    -> new SphereStep(d.id(), d.durationTicks(), resolved);
            case "HELIX"     -> new HelixStep(d.id(), d.durationTicks(), resolved);
            case "VORTEX"    -> new VortexStep(d.id(), d.durationTicks(), resolved);
            case "SHOCKWAVE" -> new ShockwaveStep(d.id(), d.durationTicks(), resolved);
            case "CONE"      -> new ConeStep(d.id(), d.durationTicks(), resolved);
            case "RAIN"      -> new RainStep(d.id(), d.durationTicks(), resolved);
            case "CYLINDER"  -> new CylinderStep(d.id(), d.durationTicks(), resolved);
            case "CUBE"      -> new CubeStep(d.id(), d.durationTicks(), resolved);
            case "LINE"      -> new LineStep(d.id(), d.durationTicks(), resolved);
            case "TRAIL"     -> new TrailStep(d.id(), d.durationTicks(), resolved);
            case "ORBIT"     -> new OrbitStep(d.id(), d.durationTicks(), resolved);
            case "TEXT"      -> new TextStep(d.id(), d.durationTicks(), resolved);
            case "SHAPE"     -> new ShapeStep(d.id(), d.durationTicks(), resolved);
            case "DELAY"     -> new DelayStep(d.id(), d.durationTicks());
            case "REPEAT"    -> buildRepeat(d, defaults, overrides);
            case "PARALLEL"  -> buildParallel(d, defaults, overrides);
            default          -> throw new IllegalArgumentException("Unknown step type: " + d.type());
        };
    }

    /** Convenience for the common case of no per-call overrides. */
    public static EffectStep build(StepDescriptor d, Map<String, Object> defaults) {
        return build(d, defaults, Map.of());
    }

    /** Build an entire step list, recursively. */
    public static List<EffectStep> buildAll(List<StepDescriptor> descriptors,
                                             Map<String, Object> defaults,
                                             Map<String, Object> overrides) {
        List<EffectStep> out = new ArrayList<>(descriptors.size());
        for (StepDescriptor d : descriptors) out.add(build(d, defaults, overrides));
        return out;
    }

    // ---- internals ----

    private static EffectStep buildRepeat(StepDescriptor d,
                                           Map<String, Object> defaults,
                                           Map<String, Object> overrides) {
        Map<String, Object> resolved = ParamResolver.resolve(d.rawParams(), overrides, defaults);
        int times = readIntParam(resolved, "times", 1);
        List<EffectStep> children = buildAll(d.children(), defaults, overrides);
        return new RepeatStep(d.id(), children, times);
    }

    private static EffectStep buildParallel(StepDescriptor d,
                                             Map<String, Object> defaults,
                                             Map<String, Object> overrides) {
        // Parallel has no own params beyond children; ignore d.rawParams().
        List<EffectStep> children = buildAll(d.children(), defaults, overrides);
        return new ParallelStep(d.id(), children);
    }

    private static int readIntParam(Map<String, Object> m, String key, int fallback) {
        Object v = m.get(key);
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) {
            try { return Integer.parseInt(s.trim()); } catch (NumberFormatException ignored) {}
        }
        return fallback;
    }
}

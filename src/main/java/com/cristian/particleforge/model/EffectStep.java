package com.cristian.particleforge.model;

/**
 * A single step of an effect. Either a geometric primitive
 * (BURST, RING, HELIX, …) or a flow controller (DELAY, REPEAT, PARALLEL).
 *
 * Lifecycle, called by the engine:
 *   begin(ctx)            once when the step becomes active
 *   tick(ctx, 0..N-1)     once per server tick while active
 *   end(ctx)              once when the step's duration elapses or it's cancelled
 *
 * Implementations MUST be safe to instantiate once and reuse across multiple
 * handles unless they document otherwise. Per-handle state belongs in the
 * EffectContext or in a wrapping handle field — NOT in step fields.
 */
public interface EffectStep {

    /** Stable id, used for inheritance overrides and debug logging. */
    String id();

    /** Total ticks this step is active. Must be > 0. */
    int durationTicks();

    /** Called once when the step becomes active. Default: no-op. */
    default void begin(EffectContext ctx) {}

    /** Called every tick while active. {@code tickInStep} is 0 on the first tick. */
    void tick(EffectContext ctx, int tickInStep);

    /** Called once when the step's duration elapses or it's cancelled. Default: no-op. */
    default void end(EffectContext ctx) {}
}

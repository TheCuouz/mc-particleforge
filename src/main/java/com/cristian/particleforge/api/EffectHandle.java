package com.cristian.particleforge.api;

/**
 * Public handle returned by {@code Effects.*.play()} and {@code ParticleForgeApi.play()}.
 * Consumers use this to monitor or stop the effect they started.
 *
 * A handle becomes inactive when the effect finishes naturally, is cancelled,
 * or was rejected by the budget at submission time. Handles are NOT reusable
 * across plays.
 */
public interface EffectHandle {

    /** The registered effect name (e.g. "crate/legendary-win") or "&lt;adhoc&gt;" for fluent builds. */
    String effectName();

    /** True if the effect is currently scheduled with the engine (regardless of pause state). */
    boolean isActive();

    /** Pause ticking. Idempotent. Becomes effective on the next engine tick. */
    void pause();

    /** Resume ticking after a pause. Idempotent. */
    void resume();

    /** True if {@link #pause()} was called and {@link #resume()} hasn't yet. */
    boolean isPaused();

    /**
     * Cancel the effect immediately. The current step's {@code end(ctx)} is invoked
     * once (engine guarantees), then the handle becomes inactive.
     */
    void cancel();

    /** Ticks remaining until natural completion, clamped to &ge; 0. */
    int remainingTicks();

    /** Progress as a fraction of total duration, 0.0 at start, 1.0 at finish. */
    double progress();
}

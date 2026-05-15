package com.cristian.particleforge.engine;

import com.cristian.particleforge.api.EffectHandle;
import com.cristian.particleforge.model.EffectContext;
import com.cristian.particleforge.model.EffectStep;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Engine-internal implementation of {@link EffectHandle}. Owns the step list
 * and tick state. Mutations happen on the main thread only (the engine ticks
 * on the main thread).
 *
 * <p>Lifecycle invariants:
 * <ul>
 *   <li>{@link #tick()} returns immediately if paused, cancelled, or finished.</li>
 *   <li>The first call to {@code tick()} after construction calls {@code step.begin(ctx)}
 *       on the first step (the engine is responsible for the initial begin).</li>
 *   <li>When a step's duration elapses, {@code end(ctx)} is invoked exactly once before
 *       advancing.</li>
 *   <li>{@link #cancel()} calls {@code end(ctx)} on the current step once and sets
 *       {@code cancelled=true}.</li>
 * </ul>
 */
public final class EffectHandleImpl implements EffectHandle {

    private final String effectName;
    private final List<EffectStep> steps;
    private final EffectContext context;
    private final UUID ownerPlayer;          // nullable; the player who owns it (per-player budget)
    private final int totalDuration;

    private int currentStepIndex = 0;
    private int tickInCurrentStep = -1;       // -1 means begin() hasn't been called yet
    private int globalTick = 0;
    private boolean paused = false;
    private boolean cancelled = false;
    private boolean finished = false;
    private boolean firstTickPending = true;
    private final long startNanos = System.nanoTime();

    public EffectHandleImpl(String effectName,
                            List<EffectStep> steps,
                            EffectContext context,
                            UUID ownerPlayer) {
        this.effectName = Objects.requireNonNull(effectName);
        this.steps = List.copyOf(Objects.requireNonNull(steps));
        if (this.steps.isEmpty()) {
            throw new IllegalArgumentException("steps must not be empty");
        }
        this.context = Objects.requireNonNull(context);
        this.ownerPlayer = ownerPlayer;
        int sum = 0;
        for (EffectStep s : this.steps) sum += s.durationTicks();
        this.totalDuration = sum;
    }

    /**
     * Factory for a rejected/never-started handle (returned when budget refuses
     * admission). Returns a minimal anonymous {@link EffectHandle} that is
     * permanently inactive — NOT a subclass of {@link EffectHandleImpl}.
     */
    public static EffectHandle dead(String name) {
        return new EffectHandle() {
            @Override public String effectName() { return name; }
            @Override public boolean isActive() { return false; }
            @Override public void pause() {}
            @Override public void resume() {}
            @Override public boolean isPaused() { return false; }
            @Override public void cancel() {}
            @Override public int remainingTicks() { return 0; }
            @Override public double progress() { return 1.0; }
        };
    }

    /**
     * Called by the engine each server tick. Advances tick counters and invokes
     * step lifecycle methods.
     */
    public void tick() {
        if (cancelled || finished || paused) return;
        if (firstTickPending) {
            steps.get(0).begin(context);
            firstTickPending = false;
            tickInCurrentStep = 0;
        } else {
            tickInCurrentStep++;
        }
        EffectStep current = steps.get(currentStepIndex);
        if (tickInCurrentStep >= current.durationTicks()) {
            current.end(context);
            currentStepIndex++;
            if (currentStepIndex >= steps.size()) {
                finished = true;
                return;
            }
            tickInCurrentStep = 0;
            steps.get(currentStepIndex).begin(context);
        }
        // Always tick the (possibly newly begun) current step.
        steps.get(currentStepIndex).tick(context, tickInCurrentStep);
        globalTick++;
    }

    @Override public String effectName() { return effectName; }
    @Override public boolean isActive() { return !cancelled && !finished; }
    @Override public boolean isPaused() { return paused; }
    @Override public void pause() { this.paused = true; }
    @Override public void resume() { this.paused = false; }

    @Override
    public void cancel() {
        if (cancelled || finished) return;
        if (!firstTickPending && currentStepIndex < steps.size()) {
            try { steps.get(currentStepIndex).end(context); } catch (Throwable ignored) {}
        }
        cancelled = true;
    }

    @Override
    public int remainingTicks() {
        if (!isActive()) return 0;
        return Math.max(0, totalDuration - globalTick);
    }

    @Override
    public double progress() {
        if (totalDuration <= 0) return 1.0;
        if (finished) return 1.0;
        double p = (double) globalTick / (double) totalDuration;
        return p < 0.0 ? 0.0 : (p > 1.0 ? 1.0 : p);
    }

    public UUID ownerPlayer() { return ownerPlayer; }
    public EffectContext context() { return context; }
    public boolean isFinished() { return finished; }
    public boolean isCancelled() { return cancelled; }
    public int totalDuration() { return totalDuration; }
    public long startNanos() { return startNanos; }
}

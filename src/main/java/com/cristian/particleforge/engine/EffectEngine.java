package com.cristian.particleforge.engine;

import com.cristian.particleforge.api.EffectHandle;
import com.cristian.particleforge.primitives.UnsupportedParticleDataException;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

/**
 * Central scheduler that ticks all active {@link EffectHandleImpl}s once per
 * server tick. Wraps each handle's tick in a try/catch so one buggy step
 * cannot crash the engine.
 */
public final class EffectEngine {

    private final Plugin plugin;
    private final BudgetManager budget;
    private final CopyOnWriteArrayList<EffectHandleImpl> active = new CopyOnWriteArrayList<>();
    /** Effects that have already logged an unsupported-particle warning (rate-limit to once each). */
    private final Set<String> warnedUnsupported = ConcurrentHashMap.newKeySet();

    private BukkitTask task;
    private boolean running;

    public EffectEngine(Plugin plugin, BudgetManager budget) {
        this.plugin = Objects.requireNonNull(plugin);
        this.budget = Objects.requireNonNull(budget);
    }

    /** Idempotent. Schedules the per-tick run task at 1L period. */
    public void start() {
        if (running) return;
        this.task = new BukkitRunnable() {
            @Override public void run() { tickOnce(); }
        }.runTaskTimer(plugin, 1L, 1L);
        this.running = true;
    }

    /** Cancels the run task and every active handle. */
    public void stop() {
        if (!running) return;
        cancelAll();
        if (task != null) {
            task.cancel();
            task = null;
        }
        running = false;
    }

    /**
     * Submits a handle for ticking. If the budget rejects it, returns the
     * dead-handle sentinel from {@link EffectHandleImpl#dead(String)}.
     */
    public EffectHandle submit(EffectHandleImpl handle) {
        if (!budget.tryAdmit(handle)) {
            return EffectHandleImpl.dead(handle.effectName());
        }
        active.add(handle);
        return handle;
    }

    /** Read-only view of active handles (snapshot). */
    public List<EffectHandle> activeHandles() {
        return Collections.unmodifiableList(new ArrayList<>(active));
    }

    /** Cancel every active handle whose owner matches {@code owner}. */
    public int cancelAll(UUID owner) {
        int n = 0;
        for (EffectHandleImpl h : active) {
            if (Objects.equals(h.ownerPlayer(), owner) && h.isActive()) {
                h.cancel();
                n++;
            }
        }
        return n;
    }

    /** Cancel every active handle. Returns the count cancelled. */
    public int cancelAll() {
        int n = 0;
        for (EffectHandleImpl h : active) {
            if (h.isActive()) {
                h.cancel();
                n++;
            }
        }
        return n;
    }

    /**
     * Package-private: tick all handles once, isolate exceptions, prune finished
     * or cancelled. Called by the BukkitRunnable in {@link #start()}.
     */
    void tickOnce() {
        Iterator<EffectHandleImpl> it = active.iterator();
        List<EffectHandleImpl> toRemove = null;
        while (it.hasNext()) {
            EffectHandleImpl h = it.next();
            if (!h.isActive()) {
                if (toRemove == null) toRemove = new ArrayList<>();
                toRemove.add(h);
                continue;
            }
            try {
                h.tick();
            } catch (UnsupportedParticleDataException ex) {
                // One step has a particle whose data we can't synthesize. Skip
                // this tick but keep the handle alive — the remaining steps in
                // the timeline are independent and probably fine. Log once per
                // effect name so console doesn't drown.
                if (warnedUnsupported.add(h.effectName())) {
                    plugin.getLogger().warning(
                        "EffectEngine: handle '" + h.effectName() + "' uses "
                        + ex.particle().name() + " which requires "
                        + ex.dataType().getSimpleName()
                        + " data — skipping particle, effect continues. "
                        + "Curate the YAML to use a supported particle.");
                }
            } catch (Throwable t) {
                plugin.getLogger().log(Level.WARNING,
                    "EffectEngine: handle '" + h.effectName() + "' threw — cancelling", t);
                try { h.cancel(); } catch (Throwable ignored) {}
                if (toRemove == null) toRemove = new ArrayList<>();
                toRemove.add(h);
                continue;
            }
            if (!h.isActive()) {
                if (toRemove == null) toRemove = new ArrayList<>();
                toRemove.add(h);
            }
        }
        if (toRemove != null) {
            for (EffectHandleImpl h : toRemove) {
                if (active.remove(h)) {
                    budget.onFinish(h);
                }
            }
        }
    }

    public boolean isRunning() { return running; }
    public int activeCount() { return active.size(); }
}

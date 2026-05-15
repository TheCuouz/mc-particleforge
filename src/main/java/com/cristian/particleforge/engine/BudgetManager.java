package com.cristian.particleforge.engine;

import com.cristian.particleforge.cfg.ConfigManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Enforces global and per-player caps on concurrent {@link EffectHandleImpl}s,
 * applying the configured {@link BudgetPolicy} when caps would be exceeded.
 * Also resolves distance-based LOD buckets and per-bucket count multipliers.
 *
 * Not thread-safe. All mutation happens on the main thread via the engine.
 */
public final class BudgetManager {

    /** LOD bucket value meaning "cull this effect entirely". */
    public static final int BUCKET_CULL = 3;

    private final ConfigManager config;
    private final Deque<EffectHandleImpl> globalQueue = new ArrayDeque<>();
    private final Map<UUID, Deque<EffectHandleImpl>> byPlayer = new HashMap<>();

    public BudgetManager(ConfigManager config) {
        this.config = Objects.requireNonNull(config);
    }

    /**
     * Attempts to admit a new handle. Returns true if admitted (the handle was
     * enqueued internally), false if rejected (caller should mark the handle
     * dead-on-arrival).
     */
    public boolean tryAdmit(EffectHandleImpl handle) {
        Objects.requireNonNull(handle);
        BudgetPolicy policy = resolvePolicy();
        UUID owner = handle.ownerPlayer();
        int globalCap = config.globalMaxActive();
        int perPlayerCap = config.perPlayerMaxActive();

        boolean globalFull = globalQueue.size() >= globalCap;
        boolean playerFull = owner != null
            && byPlayer.computeIfAbsent(owner, k -> new ArrayDeque<>()).size() >= perPlayerCap;

        if (!globalFull && !playerFull) {
            enqueue(handle);
            return true;
        }

        if (policy == BudgetPolicy.FIFO) {
            // Reject newcomer.
            return false;
        }

        // NEWEST_FIRST: evict oldest to make room.
        if (playerFull) {
            Deque<EffectHandleImpl> pq = byPlayer.get(owner);
            EffectHandleImpl victim = pq.peekFirst();
            if (victim != null) {
                victim.cancel();
                // Remove from both queues so the new handle's enqueue sees the freed slot.
                pq.pollFirst();
                globalQueue.remove(victim);
            }
        }
        if (globalFull && globalQueue.size() >= globalCap) {
            EffectHandleImpl victim = globalQueue.peekFirst();
            if (victim != null) {
                victim.cancel();
                globalQueue.pollFirst();
                UUID vOwner = victim.ownerPlayer();
                if (vOwner != null) {
                    Deque<EffectHandleImpl> vq = byPlayer.get(vOwner);
                    if (vq != null) {
                        vq.remove(victim);
                        if (vq.isEmpty()) byPlayer.remove(vOwner);
                    }
                }
            }
        }

        enqueue(handle);
        return true;
    }

    /** Called by the engine when a handle finishes or is cancelled. Idempotent. */
    public void onFinish(EffectHandleImpl handle) {
        globalQueue.remove(handle);
        UUID owner = handle.ownerPlayer();
        if (owner != null) {
            Deque<EffectHandleImpl> q = byPlayer.get(owner);
            if (q != null) {
                q.remove(handle);
                if (q.isEmpty()) byPlayer.remove(owner);
            }
        }
    }

    public int activeCount() { return globalQueue.size(); }

    public int activeFor(UUID player) {
        Deque<EffectHandleImpl> q = byPlayer.get(player);
        return q == null ? 0 : q.size();
    }

    /**
     * Returns the LOD bucket for an effect at {@code origin} as seen by {@code viewer}.
     * 0 = near, 1 = mid, 2 = far, {@link #BUCKET_CULL} = cull (beyond far and culling enabled).
     */
    public int lodBucket(Location origin, Player viewer) {
        if (origin == null || viewer == null) return 1; // be conservative
        if (!Objects.equals(origin.getWorld(), viewer.getWorld())) {
            return config.cullBeyondFar() ? BUCKET_CULL : 2;
        }
        double d = origin.distance(viewer.getLocation());
        if (d < config.lodNear()) return 0;
        if (d < config.lodMid())  return 1;
        if (d < config.lodFar())  return 2;
        return config.cullBeyondFar() ? BUCKET_CULL : 2;
    }

    /** Per-bucket multiplier applied to particle counts. Bucket {@link #BUCKET_CULL} → 0.0. */
    public double lodMultiplier(int bucket) {
        return switch (bucket) {
            case 0 -> config.nearMul();
            case 1 -> config.midMul();
            case 2 -> config.farMul();
            default -> 0.0;
        };
    }

    // ---- internals ----

    private void enqueue(EffectHandleImpl handle) {
        globalQueue.addLast(handle);
        UUID owner = handle.ownerPlayer();
        if (owner != null) {
            byPlayer.computeIfAbsent(owner, k -> new ArrayDeque<>()).addLast(handle);
        }
    }

    private BudgetPolicy resolvePolicy() {
        String raw = config.policy();
        if (raw == null) return BudgetPolicy.NEWEST_FIRST;
        try {
            return BudgetPolicy.valueOf(raw.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return BudgetPolicy.NEWEST_FIRST; // safe default
        }
    }
}

package com.cristian.particleforge.primitives;

import com.cristian.particleforge.model.EffectContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.UUID;

/** Shared particle-spawning helper used by every step primitive. */
public final class SpawnUtil {
    private SpawnUtil() {}

    public static void spawn(Location at, EffectContext ctx, Particle particle,
                              int count, double spread, double speed) {
        if (at == null || at.getWorld() == null || count <= 0) return;
        if (ctx.viewers().isEmpty()) {
            at.getWorld().spawnParticle(particle, at, count, spread, spread, spread, speed);
        } else {
            for (UUID uid : ctx.viewers()) {
                Player p = Bukkit.getPlayer(uid);
                if (p != null && p.isOnline()) {
                    p.spawnParticle(particle, at, count, spread, spread, spread, speed);
                }
            }
        }
    }
}

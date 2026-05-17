package com.cristian.particleforge.primitives;

import com.cristian.particleforge.model.EffectContext;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Shared particle-spawning helper used by every step primitive.
 *
 * <p>Paper 1.21.10 promoted several particles ({@code INSTANT_EFFECT} and the
 * EFFECT/ENTITY_EFFECT family) to require {@link Particle.Spell} data. The
 * no-data {@code spawnParticle} overload throws {@link IllegalArgumentException}
 * for those — so we branch on {@link Particle#getDataType()} and pick the right
 * overload. Unsupported data types raise {@link UnsupportedParticleDataException}
 * so the engine can skip the tick cleanly instead of nuking the whole handle.
 */
public final class SpawnUtil {

    /** Curated commercial default — light cyan reads as "mana / arcane" across the suite. */
    public static final Color DEFAULT_SPELL_COLOR = Color.fromRGB(0x66, 0xE1, 0xFF);
    public static final float DEFAULT_SPELL_POWER = 1.0f;

    private SpawnUtil() {}

    public static void spawn(Location at, EffectContext ctx, Particle particle,
                              int count, double spread, double speed) {
        spawn(at, ctx, particle, count, spread, speed, null, DEFAULT_SPELL_POWER);
    }

    /**
     * Spawn variant that supplies {@code color} + {@code power} for data-bearing
     * particles ({@code INSTANT_EFFECT}, etc.). Steps that read these from YAML
     * pass them through; other steps use the no-arg overload above and inherit
     * the curated defaults.
     */
    public static void spawn(Location at, EffectContext ctx, Particle particle,
                              int count, double spread, double speed,
                              Color color, float power) {
        if (at == null || at.getWorld() == null || count <= 0) return;
        Class<?> dataType = particle.getDataType();
        if (dataType == Void.class) {
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
        } else if (dataType == Particle.Spell.class) {
            Particle.Spell data = new Particle.Spell(
                color != null ? color : DEFAULT_SPELL_COLOR, power);
            if (ctx.viewers().isEmpty()) {
                at.getWorld().spawnParticle(particle, at, count, spread, spread, spread, speed, data);
            } else {
                for (UUID uid : ctx.viewers()) {
                    Player p = Bukkit.getPlayer(uid);
                    if (p != null && p.isOnline()) {
                        p.spawnParticle(particle, at, count, spread, spread, spread, speed, data);
                    }
                }
            }
        } else {
            throw new UnsupportedParticleDataException(particle, dataType);
        }
    }
}

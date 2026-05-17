package com.cristian.particleforge.primitives;

import org.bukkit.Particle;

/**
 * Thrown by {@link SpawnUtil} when a {@link Particle} is spawned that requires
 * a data type the engine does not yet know how to synthesize (e.g. {@code Block},
 * {@code Item}, {@code Vibration}). The engine catches this specifically and
 * skips the offending tick instead of cancelling the whole effect handle, so
 * one mis-curated particle in one step never blows up the rest of the timeline.
 *
 * <p>The supported data types today are {@code Void} (no data) and
 * {@link Particle.Spell} (synthesized from {@code color} + {@code power} params).
 */
public class UnsupportedParticleDataException extends RuntimeException {

    private final Particle particle;
    private final Class<?> dataType;

    public UnsupportedParticleDataException(Particle particle, Class<?> dataType) {
        super("Particle " + particle.name() + " requires data type "
            + dataType.getSimpleName() + " which SpawnUtil cannot synthesize. "
            + "Supported: Void, Particle.Spell.");
        this.particle = particle;
        this.dataType = dataType;
    }

    public Particle particle() { return particle; }
    public Class<?> dataType() { return dataType; }
}

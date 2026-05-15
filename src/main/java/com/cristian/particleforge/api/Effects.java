package com.cristian.particleforge.api;

import com.cristian.particleforge.model.EffectStep;
import com.cristian.particleforge.model.StepDescriptor;
import com.cristian.particleforge.registry.StepFactory;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Fluent facade for building and playing particle effects programmatically.
 *
 * <pre>{@code
 * EffectHandle h = Effects.burst()
 *     .at(player.getLocation())
 *     .particle("FLAME")
 *     .count(64)
 *     .speed(0.7)
 *     .duration(10)
 *     .play();
 * }</pre>
 *
 * For pre-registered effects (loaded from YAML or via
 * {@link ParticleForgeApi#registerEffect}), use {@link #named(String)}.
 */
public final class Effects {

    private static volatile ParticleForgeApi api;

    private Effects() {}

    /** Wired by {@link ParticleForgeApi}'s constructor. Tests can reset via reflection if needed. */
    public static void install(ParticleForgeApi api) { Effects.api = api; }

    public static boolean isAvailable() { return api != null; }

    public static Builder burst()     { return new Builder("BURST"); }
    public static Builder ring()      { return new Builder("RING"); }
    public static Builder sphere()    { return new Builder("SPHERE"); }
    public static Builder helix()     { return new Builder("HELIX"); }
    public static Builder vortex()    { return new Builder("VORTEX"); }
    public static Builder shockwave() { return new Builder("SHOCKWAVE"); }
    public static Builder cone()      { return new Builder("CONE"); }
    public static Builder rain()      { return new Builder("RAIN"); }
    public static Builder cylinder()  { return new Builder("CYLINDER"); }
    public static Builder cube()      { return new Builder("CUBE"); }
    public static Builder line()      { return new Builder("LINE"); }
    public static Builder trail()     { return new Builder("TRAIL"); }
    public static Builder orbit()     { return new Builder("ORBIT"); }
    public static Builder text()      { return new Builder("TEXT"); }
    public static Builder shape()     { return new Builder("SHAPE"); }

    /** Look up a registered effect by name; {@link NamedBuilder#play()} submits it. */
    public static NamedBuilder named(String name) { return new NamedBuilder(name); }

    public static final class Builder {
        private final String type;
        private final Map<String, Object> params = new LinkedHashMap<>();
        private int durationTicks = 20;
        private Location at;
        private Set<UUID> viewers = Set.of();
        private UUID owner;
        private LivingEntity follow;

        Builder(String type) { this.type = type; }

        public Builder at(Location loc) { this.at = loc; return this; }
        public Builder duration(int ticks) { this.durationTicks = ticks; return this; }
        public Builder particle(String name) { params.put("particle", name); return this; }
        public Builder count(int n) { params.put("count", n); return this; }
        public Builder speed(double v) { params.put("speed", v); return this; }
        public Builder spread(double v) { params.put("spread", v); return this; }
        public Builder param(String key, Object value) { params.put(key, value); return this; }
        public Builder viewers(Collection<UUID> viewers) {
            this.viewers = viewers == null ? Set.of() : Set.copyOf(viewers);
            return this;
        }
        public Builder owner(UUID owner) { this.owner = owner; return this; }
        public Builder follow(LivingEntity entity) { this.follow = entity; return this; }

        public EffectHandle play() {
            requireApi();
            if (at == null && follow != null) at = follow.getLocation();
            if (at == null) {
                throw new IllegalStateException("Effects." + type.toLowerCase(Locale.ROOT)
                    + "(): missing .at(loc) or .follow(entity)");
            }
            StepDescriptor sd = new StepDescriptor(
                type.toLowerCase(Locale.ROOT), type, durationTicks, params, List.of());
            EffectStep step = StepFactory.build(sd, Map.of(), Map.of());
            return api.playAdhoc(List.of(step), at, viewers, owner, follow, params,
                                  "adhoc/" + type.toLowerCase(Locale.ROOT));
        }
    }

    public static final class NamedBuilder {
        private final String name;
        private final Map<String, Object> overrides = new LinkedHashMap<>();
        private Location at;
        private Set<UUID> viewers = Set.of();
        private UUID owner;
        private LivingEntity follow;

        NamedBuilder(String name) { this.name = name; }

        public NamedBuilder at(Location loc) { this.at = loc; return this; }
        public NamedBuilder param(String key, Object value) { overrides.put(key, value); return this; }
        public NamedBuilder viewers(Collection<UUID> viewers) {
            this.viewers = viewers == null ? Set.of() : Set.copyOf(viewers);
            return this;
        }
        public NamedBuilder owner(UUID owner) { this.owner = owner; return this; }
        public NamedBuilder follow(LivingEntity entity) { this.follow = entity; return this; }

        public EffectHandle play() {
            requireApi();
            if (at == null && follow != null) at = follow.getLocation();
            if (at == null) {
                throw new IllegalStateException("Effects.named('" + name + "').play() requires .at(loc) or .follow(entity)");
            }
            return api.play(name, at, overrides, viewers, owner, follow);
        }
    }

    private static void requireApi() {
        if (api == null) {
            throw new IllegalStateException(
                "ParticleForge is not loaded or not yet initialized. Did you call this before onEnable()?");
        }
    }
}

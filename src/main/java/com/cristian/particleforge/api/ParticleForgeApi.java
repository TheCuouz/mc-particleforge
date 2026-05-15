package com.cristian.particleforge.api;

import com.cristian.particleforge.ParticleForgePlugin;
import com.cristian.particleforge.engine.BudgetManager;
import com.cristian.particleforge.engine.EffectEngine;
import com.cristian.particleforge.engine.EffectHandleImpl;
import com.cristian.particleforge.model.EffectContext;
import com.cristian.particleforge.model.EffectDescriptor;
import com.cristian.particleforge.model.EffectStep;
import com.cristian.particleforge.registry.EffectRegistry;
import com.cristian.particleforge.registry.StepFactory;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Public service object for plugins that soft-depend on ParticleForge.
 * Retrieved via:
 * <pre>
 *   RegisteredServiceProvider&lt;ParticleForgeApi&gt; rsp =
 *       Bukkit.getServicesManager().getRegistration(ParticleForgeApi.class);
 *   ParticleForgeApi pf = rsp.getProvider();
 *   pf.play("crate/legendary-win", loc);
 * </pre>
 */
public final class ParticleForgeApi {

    private final ParticleForgePlugin plugin;
    private final EffectEngine engine;
    private final EffectRegistry registry;
    private final BudgetManager budget;

    public ParticleForgeApi(ParticleForgePlugin plugin,
                             EffectEngine engine,
                             EffectRegistry registry,
                             BudgetManager budget) {
        this.plugin = Objects.requireNonNull(plugin);
        this.engine = Objects.requireNonNull(engine);
        this.registry = Objects.requireNonNull(registry);
        this.budget = Objects.requireNonNull(budget);
        Effects.install(this);
    }

    public EffectHandle play(String effectName, Location at) {
        return play(effectName, at, Map.of(), Set.of(), null, null);
    }

    public EffectHandle play(String effectName,
                              Location at,
                              Map<String, Object> overrides,
                              Set<UUID> viewers,
                              UUID owner,
                              LivingEntity follow) {
        Optional<EffectDescriptor> opt = registry.find(effectName);
        if (opt.isEmpty()) {
            return EffectHandleImpl.dead(effectName);
        }
        EffectDescriptor d = opt.get();

        Map<String, Object> mergedDefaults = new LinkedHashMap<>(d.defaults());
        if (overrides != null) mergedDefaults.putAll(overrides);

        List<EffectStep> steps = StepFactory.buildAll(d.steps(), d.defaults(),
            overrides == null ? Map.of() : overrides);
        if (steps.isEmpty()) return EffectHandleImpl.dead(effectName);

        EffectContext ctx = new EffectContext(plugin, at, viewers, mergedDefaults, follow, null);
        EffectHandleImpl handle = new EffectHandleImpl(effectName, steps, ctx, owner);
        ctx.attachHandle(handle);
        return engine.submit(handle);
    }

    /**
     * Submit an adhoc handle built programmatically (e.g. by the fluent
     * {@link Effects} facade) without a registered effect name.
     */
    public EffectHandle playAdhoc(List<EffectStep> steps,
                                    Location at,
                                    Set<UUID> viewers,
                                    UUID owner,
                                    LivingEntity follow,
                                    Map<String, Object> params,
                                    String label) {
        if (steps == null || steps.isEmpty()) return EffectHandleImpl.dead(label == null ? "<adhoc>" : label);
        EffectContext ctx = new EffectContext(plugin, at, viewers,
            params == null ? Map.of() : params, follow, null);
        EffectHandleImpl handle = new EffectHandleImpl(label == null ? "<adhoc>" : label,
            steps, ctx, owner);
        ctx.attachHandle(handle);
        return engine.submit(handle);
    }

    /** Register an externally-built effect (e.g. constructed in another plugin's startup). */
    public void registerEffect(EffectDescriptor d) { registry.register(d); }

    public Collection<EffectDescriptor> effects() { return registry.all(); }
    public Collection<EffectDescriptor> effects(String category) { return registry.byCategory(category); }
    public EffectRegistry registry() { return registry; }
    public BudgetManager budget() { return budget; }
    public EffectEngine engine() { return engine; }
    public ParticleForgePlugin plugin() { return plugin; }
}

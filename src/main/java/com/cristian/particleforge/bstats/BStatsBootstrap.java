package com.cristian.particleforge.bstats;

import com.cristian.particleforge.engine.EffectEngine;
import com.cristian.particleforge.model.EffectDescriptor;
import com.cristian.particleforge.registry.EffectRegistry;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Wires bStats metrics with a few custom charts.
 */
public final class BStatsBootstrap {

    public static final int BSTATS_PLUGIN_ID = 31357;

    private final JavaPlugin plugin;
    private final EffectRegistry registry;
    private final EffectEngine engine;

    public BStatsBootstrap(JavaPlugin plugin, EffectRegistry registry, EffectEngine engine) {
        this.plugin = Objects.requireNonNull(plugin);
        this.registry = Objects.requireNonNull(registry);
        this.engine = Objects.requireNonNull(engine);
    }

    public void init() {
        Metrics m = new Metrics(plugin, BSTATS_PLUGIN_ID);
        m.addCustomChart(new SimplePie("effects_loaded", () -> bucket(registry.size())));
        m.addCustomChart(new SingleLineChart("active_effects", () -> engine.activeCount()));
        m.addCustomChart(new DrilldownPie("categories", this::categoriesMap));
    }

    /** Package-private for tests. */
    static String bucket(int n) {
        if (n <= 10) return "0-10";
        if (n <= 25) return "11-25";
        if (n <= 50) return "26-50";
        if (n <= 100) return "51-100";
        return "100+";
    }

    /** category → (effect name → 1). Drilldown pie format. */
    private Map<String, Map<String, Integer>> categoriesMap() {
        Map<String, Map<String, Integer>> out = new HashMap<>();
        for (EffectDescriptor d : registry.all()) {
            out.computeIfAbsent(d.category(), k -> new HashMap<>())
               .put(d.name(), 1);
        }
        return out;
    }
}

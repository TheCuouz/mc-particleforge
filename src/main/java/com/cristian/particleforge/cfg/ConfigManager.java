package com.cristian.particleforge.cfg;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Thin typed wrapper around {@link JavaPlugin#getConfig()}.
 *
 * <p>By design this class caches NO mutable state — every getter calls into
 * {@code plugin.getConfig()} on demand. That means {@code /pf reload} only
 * needs to call {@link #reload()} (which delegates to
 * {@link JavaPlugin#reloadConfig()}) and subsequent reads will see the new
 * values automatically.</p>
 *
 * <p>Defaults below MUST match {@code src/main/resources/config.yml}. They
 * are the spec contract: if {@code config.yml} is hand-edited and a key is
 * removed, the plugin keeps working with the documented default.</p>
 *
 * <p><b>About {@link #policy()}:</b> returns the raw string from config so
 * this module does not depend on the {@code BudgetPolicy} enum (built in
 * Task 5). Consumers parse with
 * {@code BudgetPolicy.valueOf(configManager.policy().toUpperCase())}.</p>
 */
public final class ConfigManager {

    private final JavaPlugin plugin;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean debug() {
        return plugin.getConfig().getBoolean("debug", false);
    }

    public int globalMaxActive() {
        return plugin.getConfig().getInt("budget.global-max-active-effects", 64);
    }

    public int perPlayerMaxActive() {
        return plugin.getConfig().getInt("budget.per-player-max-active-effects", 4);
    }

    public String policy() {
        return plugin.getConfig().getString("budget.policy", "NEWEST_FIRST");
    }

    public int lodNear() {
        return plugin.getConfig().getInt("lod.near", 16);
    }

    public int lodMid() {
        return plugin.getConfig().getInt("lod.mid", 48);
    }

    public int lodFar() {
        return plugin.getConfig().getInt("lod.far", 96);
    }

    public boolean cullBeyondFar() {
        return plugin.getConfig().getBoolean("lod.cull-beyond-far", true);
    }

    public double nearMul() {
        return plugin.getConfig().getDouble("lod-multipliers.near", 1.0);
    }

    public double midMul() {
        return plugin.getConfig().getDouble("lod-multipliers.mid", 0.5);
    }

    public double farMul() {
        return plugin.getConfig().getDouble("lod-multipliers.far", 0.2);
    }

    public String lang() {
        return plugin.getConfig().getString("lang", "en");
    }

    public void reload() {
        plugin.reloadConfig();
    }
}

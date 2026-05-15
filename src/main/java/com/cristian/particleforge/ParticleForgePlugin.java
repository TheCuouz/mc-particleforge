package com.cristian.particleforge;

import com.cristian.particleforge.api.ParticleForgeApi;
import com.cristian.particleforge.bstats.BStatsBootstrap;
import com.cristian.particleforge.cfg.ConfigManager;
import com.cristian.particleforge.cfg.MessageManager;
import com.cristian.particleforge.command.ParticleForgeCommand;
import com.cristian.particleforge.engine.BudgetManager;
import com.cristian.particleforge.engine.EffectEngine;
import com.cristian.particleforge.registry.EffectRegistry;
import com.cristian.particleforge.registry.YamlEffectLoader;
import com.ttsstudio.sdk.PluginIdentity;
import com.ttsstudio.sdk.console.ConsoleBanner;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;

public final class ParticleForgePlugin extends JavaPlugin {

    private ConfigManager configManager;
    private MessageManager messageManager;
    private EffectRegistry registry;
    private BudgetManager budgetManager;
    private EffectEngine engine;
    private ParticleForgeApi api;
    private PluginIdentity identity;
    private boolean debug;

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();
        saveDefaultConfig();

        this.identity = PluginIdentity.of(this).withAlias("Particles");

        this.configManager = new ConfigManager(this);
        this.debug = configManager.debug();
        this.messageManager = new MessageManager(this);
        this.budgetManager = new BudgetManager(configManager);
        this.engine = new EffectEngine(this, budgetManager);
        this.engine.start();

        this.registry = new EffectRegistry();
        new YamlEffectLoader(this).loadAll(registry);

        this.api = new ParticleForgeApi(this, engine, registry, budgetManager);
        getServer().getServicesManager().register(
            ParticleForgeApi.class, api, this, ServicePriority.Normal);

        ParticleForgeCommand cmd = new ParticleForgeCommand(this, api, messageManager);
        getCommand("pf").setExecutor(cmd);
        getCommand("pf").setTabCompleter(cmd);

        new BStatsBootstrap(this, registry, engine).init();

        ConsoleBanner.enable(this, identity)
            .status(registry.size() + " effects")
            .status("budget " + configManager.globalMaxActive()
                  + "/" + configManager.perPlayerMaxActive())
            .ready(Duration.ofMillis(System.currentTimeMillis() - start))
            .emit();
    }

    @Override
    public void onDisable() {
        if (engine != null) engine.stop();
        if (identity != null) {
            ConsoleBanner.disable(this, identity).emit();
        }
    }

    public ConfigManager configManager()   { return configManager; }
    public MessageManager messageManager() { return messageManager; }
    public EffectRegistry registry()       { return registry; }
    public BudgetManager budgetManager()   { return budgetManager; }
    public EffectEngine engine()           { return engine; }
    public ParticleForgeApi api()          { return api; }
    public PluginIdentity identity()       { return identity; }
    public boolean debug()                 { return debug; }
    public void setDebug(boolean debug)    { this.debug = debug; }
}

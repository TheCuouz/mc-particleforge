package com.cristian.particleforge.command.sub;

import com.cristian.particleforge.ParticleForgePlugin;
import com.cristian.particleforge.api.ParticleForgeApi;
import com.cristian.particleforge.cfg.MessageManager;
import com.cristian.particleforge.command.Subcommand;
import com.cristian.particleforge.registry.EffectRegistry;
import com.cristian.particleforge.registry.YamlEffectLoader;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * {@code /pf reload} — admin-only. Reloads config.yml, messages, and rebuilds
 * the effect registry from disk. Reports count + elapsed milliseconds.
 */
public final class ReloadSub implements Subcommand {

    private final ParticleForgePlugin plugin;
    private final ParticleForgeApi api;
    private final MessageManager messages;

    public ReloadSub(ParticleForgePlugin plugin, ParticleForgeApi api, MessageManager messages) {
        this.plugin = plugin;
        this.api = api;
        this.messages = messages;
    }

    @Override public String name() { return "reload"; }
    @Override public String permission() { return "particleforge.admin"; }
    @Override public String descriptionKey() { return "help-desc-reload"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        long start = System.currentTimeMillis();
        plugin.reloadConfig();
        plugin.messageManager().reload();
        EffectRegistry reg = api.registry();
        reg.clear();
        new YamlEffectLoader(plugin).loadAll(reg);
        long ms = System.currentTimeMillis() - start;
        messages.send(sender, "reloaded",
            Placeholder.unparsed("count", String.valueOf(reg.size())),
            Placeholder.unparsed("ms", String.valueOf(ms)));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}

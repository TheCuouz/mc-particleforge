package com.cristian.particleforge.command.sub;

import com.cristian.particleforge.ParticleForgePlugin;
import com.cristian.particleforge.cfg.MessageManager;
import com.cristian.particleforge.command.Subcommand;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * {@code /pf debug} — admin-only. Toggles the runtime debug flag on
 * {@link ParticleForgePlugin}.
 */
public final class DebugSub implements Subcommand {

    private final ParticleForgePlugin plugin;
    private final MessageManager messages;

    public DebugSub(ParticleForgePlugin plugin, MessageManager messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override public String name() { return "debug"; }
    @Override public String permission() { return "particleforge.admin"; }
    @Override public String descriptionKey() { return "help-desc-debug"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        boolean next = !plugin.debug();
        plugin.setDebug(next);
        messages.send(sender, next ? "debug-on" : "debug-off");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}

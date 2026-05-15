package com.cristian.particleforge.command.sub;

import com.cristian.particleforge.ParticleForgePlugin;
import com.cristian.particleforge.api.ParticleForgeApi;
import com.cristian.particleforge.cfg.MessageManager;
import com.cristian.particleforge.command.Subcommand;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * {@code /pf stop [player]} — without arg, stops the sender's effects.
 * With arg (admin only), stops the target player's effects.
 */
public final class StopSub implements Subcommand {

    private final ParticleForgePlugin plugin;
    private final ParticleForgeApi api;
    private final MessageManager messages;

    public StopSub(ParticleForgePlugin plugin, ParticleForgeApi api, MessageManager messages) {
        this.plugin = plugin;
        this.api = api;
        this.messages = messages;
    }

    @Override public String name() { return "stop"; }
    /** Base permission is the generic use node; targeting another player additionally requires admin. */
    @Override public String permission() { return "particleforge.use"; }
    @Override public String descriptionKey() { return "help-desc-stop"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("Console must specify a player: /pf stop <player>");
                return;
            }
            int n = api.engine().cancelAll(p.getUniqueId());
            messages.send(sender, "effect-stopped", Placeholder.unparsed("count", String.valueOf(n)));
            return;
        }
        // Targeting another player requires admin permission.
        if (!sender.hasPermission("particleforge.admin")) {
            messages.send(sender, "no-permission");
            return;
        }
        String targetName = args[0];
        UUID targetId;
        Player online = Bukkit.getPlayerExact(targetName);
        if (online != null) {
            targetId = online.getUniqueId();
        } else {
            @SuppressWarnings("deprecation")
            OfflinePlayer off = Bukkit.getOfflinePlayer(targetName);
            targetId = off.getUniqueId();
        }
        int n = api.engine().cancelAll(targetId);
        messages.send(sender, "effect-stopped",
            Placeholder.unparsed("count", String.valueOf(n)),
            Placeholder.unparsed("player", targetName));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                String n = p.getName();
                if (n.toLowerCase(Locale.ROOT).startsWith(prefix)) out.add(n);
            }
            return out;
        }
        return List.of();
    }
}

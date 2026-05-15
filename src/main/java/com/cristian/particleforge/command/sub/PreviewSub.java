package com.cristian.particleforge.command.sub;

import com.cristian.particleforge.api.ParticleForgeApi;
import com.cristian.particleforge.cfg.MessageManager;
import com.cristian.particleforge.command.Subcommand;
import com.cristian.particleforge.model.EffectDescriptor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * {@code /pf preview <effect>} — admin-only. Plays the named effect at the
 * sender's location, restricted to the sender as the only viewer.
 */
public final class PreviewSub implements Subcommand {

    private final ParticleForgeApi api;
    private final MessageManager messages;

    public PreviewSub(ParticleForgeApi api, MessageManager messages) {
        this.api = api;
        this.messages = messages;
    }

    @Override public String name() { return "preview"; }
    @Override public String permission() { return "particleforge.admin"; }
    @Override public String descriptionKey() { return "help-desc-preview"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("This command requires a player.");
            return;
        }
        if (args.length == 0) {
            messages.send(sender, "effect-not-found", Placeholder.unparsed("effect", "(missing)"));
            return;
        }
        String name = args[0];
        if (api.registry().find(name).isEmpty()) {
            messages.send(sender, "effect-not-found", Placeholder.unparsed("effect", name));
            return;
        }
        api.play(name, p.getLocation(), Map.of(),
            Set.of(p.getUniqueId()), p.getUniqueId(), null);
        messages.send(sender, "effect-started", Placeholder.unparsed("effect", name));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return api.registry().all().stream()
                .map(EffectDescriptor::name)
                .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix))
                .sorted()
                .toList();
        }
        return List.of();
    }
}

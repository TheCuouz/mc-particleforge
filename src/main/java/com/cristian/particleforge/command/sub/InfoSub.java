package com.cristian.particleforge.command.sub;

import com.cristian.particleforge.api.ParticleForgeApi;
import com.cristian.particleforge.cfg.MessageManager;
import com.cristian.particleforge.command.Subcommand;
import com.cristian.particleforge.model.EffectDescriptor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * {@code /pf info <effect>} — prints name, category, step count, total
 * duration, and the defaults map (one line per entry, sorted by key).
 */
public final class InfoSub implements Subcommand {

    private final ParticleForgeApi api;
    private final MessageManager messages;

    public InfoSub(ParticleForgeApi api, MessageManager messages) {
        this.api = api;
        this.messages = messages;
    }

    @Override public String name() { return "info"; }
    @Override public String permission() { return "particleforge.use"; }
    @Override public String descriptionKey() { return "help-desc-info"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            messages.send(sender, "effect-not-found", Placeholder.unparsed("effect", "(missing)"));
            return;
        }
        String name = args[0];
        Optional<EffectDescriptor> opt = api.registry().find(name);
        if (opt.isEmpty()) {
            messages.send(sender, "effect-not-found", Placeholder.unparsed("effect", name));
            return;
        }
        EffectDescriptor d = opt.get();
        messages.send(sender, "info-header", Placeholder.unparsed("effect", d.name()));
        messages.send(sender, "info-line-category", Placeholder.unparsed("value", d.category()));
        messages.send(sender, "info-line-steps",
            Placeholder.unparsed("value", String.valueOf(d.steps().size())));
        messages.send(sender, "info-line-duration",
            Placeholder.unparsed("value", String.valueOf(d.totalDurationTicks())));
        d.defaults().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(e -> messages.send(sender, "info-line-default",
                Placeholder.unparsed("key", e.getKey()),
                Placeholder.unparsed("value", String.valueOf(e.getValue()))));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return api.registry().all().stream()
                .map(EffectDescriptor::name)
                .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix))
                .sorted(Comparator.naturalOrder())
                .toList();
        }
        return List.of();
    }
}

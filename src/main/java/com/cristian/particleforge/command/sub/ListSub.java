package com.cristian.particleforge.command.sub;

import com.cristian.particleforge.api.ParticleForgeApi;
import com.cristian.particleforge.cfg.MessageManager;
import com.cristian.particleforge.command.Subcommand;
import com.cristian.particleforge.model.EffectDescriptor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * {@code /pf list [category]} — lists loaded effects, grouped by category when
 * no filter is given, or filtered to one category when supplied.
 */
public final class ListSub implements Subcommand {

    private final ParticleForgeApi api;
    private final MessageManager messages;

    public ListSub(ParticleForgeApi api, MessageManager messages) {
        this.api = api;
        this.messages = messages;
    }

    @Override public String name() { return "list"; }
    @Override public String permission() { return "particleforge.use"; }
    @Override public String descriptionKey() { return "help-desc-list"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Collection<EffectDescriptor> all = api.registry().all();
        if (args.length >= 1) {
            String cat = args[0];
            Collection<EffectDescriptor> filtered = api.registry().byCategory(cat);
            messages.send(sender, "list-header",
                Placeholder.unparsed("count", String.valueOf(filtered.size())));
            messages.send(sender, "list-category", Placeholder.unparsed("category", cat));
            filtered.stream()
                .sorted(Comparator.comparing(EffectDescriptor::name))
                .forEach(d -> messages.send(sender, "list-line",
                    Placeholder.unparsed("effect", d.name()),
                    Placeholder.unparsed("steps", String.valueOf(d.steps().size()))));
            return;
        }

        messages.send(sender, "list-header",
            Placeholder.unparsed("count", String.valueOf(all.size())));

        // Group by category, sorted by category name; within each, sort by effect name.
        Map<String, List<EffectDescriptor>> byCat = new TreeMap<>();
        for (EffectDescriptor d : all) {
            byCat.computeIfAbsent(d.category(), k -> new ArrayList<>()).add(d);
        }
        for (Map.Entry<String, List<EffectDescriptor>> e : byCat.entrySet()) {
            messages.send(sender, "list-category", Placeholder.unparsed("category", e.getKey()));
            e.getValue().stream()
                .sorted(Comparator.comparing(EffectDescriptor::name))
                .forEach(d -> messages.send(sender, "list-line",
                    Placeholder.unparsed("effect", d.name()),
                    Placeholder.unparsed("steps", String.valueOf(d.steps().size()))));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return api.registry().categories().stream()
                .filter(c -> c.toLowerCase(Locale.ROOT).startsWith(prefix))
                .sorted()
                .toList();
        }
        return List.of();
    }
}

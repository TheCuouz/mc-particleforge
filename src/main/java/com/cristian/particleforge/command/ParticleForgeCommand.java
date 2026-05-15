package com.cristian.particleforge.command;

import com.cristian.particleforge.ParticleForgePlugin;
import com.cristian.particleforge.api.ParticleForgeApi;
import com.cristian.particleforge.cfg.MessageManager;
import com.cristian.particleforge.command.sub.DebugSub;
import com.cristian.particleforge.command.sub.InfoSub;
import com.cristian.particleforge.command.sub.ListSub;
import com.cristian.particleforge.command.sub.PreviewSub;
import com.cristian.particleforge.command.sub.ReloadSub;
import com.cristian.particleforge.command.sub.StopSub;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Root dispatcher for {@code /pf}. Routes the first arg to a {@link Subcommand}
 * and exposes a uniform help / tab-completion experience. Permissions are
 * checked here so the subcommands only see authorised invocations.
 */
public final class ParticleForgeCommand implements CommandExecutor, TabCompleter {

    private final ParticleForgePlugin plugin;
    private final ParticleForgeApi api;
    private final MessageManager messages;
    private final Map<String, Subcommand> subs = new LinkedHashMap<>();

    public ParticleForgeCommand(ParticleForgePlugin plugin, ParticleForgeApi api, MessageManager messages) {
        this.plugin = plugin;
        this.api = api;
        this.messages = messages;
        register(new PreviewSub(api, messages));
        register(new ListSub(api, messages));
        register(new InfoSub(api, messages));
        register(new DebugSub(plugin, messages));
        register(new ReloadSub(plugin, api, messages));
        register(new StopSub(plugin, api, messages));
    }

    /**
     * Visible-for-testing constructor: lets tests inject mock {@link Subcommand}
     * instances and verify dispatch behaviour without the real implementations.
     */
    ParticleForgeCommand(ParticleForgePlugin plugin, ParticleForgeApi api, MessageManager messages,
                          List<Subcommand> subcommands) {
        this.plugin = plugin;
        this.api = api;
        this.messages = messages;
        for (Subcommand s : subcommands) register(s);
    }

    private void register(Subcommand sub) { subs.put(sub.name(), sub); }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            messages.send(sender, "help-header");
            for (Subcommand s : subs.values()) {
                if (s.permission() == null || sender.hasPermission(s.permission())) {
                    messages.send(sender, "help-line",
                        Placeholder.unparsed("sub", s.name()),
                        Placeholder.component("desc", messages.component(s.descriptionKey())));
                }
            }
            return true;
        }
        String name = args[0].toLowerCase(Locale.ROOT);
        Subcommand sub = subs.get(name);
        if (sub == null) {
            messages.send(sender, "unknown-subcommand", Placeholder.unparsed("sub", name));
            return true;
        }
        String perm = sub.permission();
        if (perm != null && !sender.hasPermission(perm)) {
            messages.send(sender, "no-permission");
            return true;
        }
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        sub.execute(sender, rest);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (Subcommand s : subs.values()) {
                if ((s.permission() == null || sender.hasPermission(s.permission()))
                    && s.name().startsWith(prefix)) {
                    out.add(s.name());
                }
            }
            return out;
        }
        Subcommand sub = subs.get(args[0].toLowerCase(Locale.ROOT));
        if (sub == null) return List.of();
        if (sub.permission() != null && !sender.hasPermission(sub.permission())) return List.of();
        List<String> result = sub.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
        return result == null ? List.of() : result;
    }
}

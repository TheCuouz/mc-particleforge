package com.cristian.particleforge.cfg;

import com.cristian.particleforge.ParticleForgePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads {@code messages.yml} (the base/English message bank) plus a
 * locale overlay from {@code lang/<configured>.yml}. Overlay keys win.
 *
 * <p>All strings are MiniMessage source. The global {@code prefix} (defined
 * once in {@code messages.yml}) is auto-prepended on send unless the source
 * string starts with {@code "!"}, in which case the bang is stripped and the
 * line is sent prefix-less.</p>
 *
 * <p>Placeholders are passed in as {@link TagResolver} varargs — never by
 * string concatenation. That preserves MiniMessage safety against
 * user-controlled input.</p>
 */
public final class MessageManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final ParticleForgePlugin plugin;
    private final Map<String, String> messages = new HashMap<>();
    private String prefix = "";

    public MessageManager(ParticleForgePlugin plugin) {
        this.plugin = plugin;
        // Ship default copies of every translatable resource to the data folder
        // so server operators can edit them in place.
        plugin.saveResource("messages.yml", false);
        saveLangResource("lang/en.yml");
        saveLangResource("lang/es.yml");
        loadAll();
    }

    /**
     * Re-reads {@code messages.yml} and the lang overlay matching the
     * currently configured language. Called by {@code /pf reload} after the
     * config itself has been reloaded.
     */
    public void reload() {
        messages.clear();
        loadAll();
    }

    private void loadAll() {
        // Base: messages.yml (English, with prefix).
        File baseFile = new File(plugin.getDataFolder(), "messages.yml");
        FileConfiguration base = YamlConfiguration.loadConfiguration(baseFile);
        this.prefix = base.getString("prefix", "");
        for (String key : base.getKeys(false)) {
            if ("prefix".equals(key)) continue;
            String val = base.getString(key);
            if (val != null) messages.put(key, val);
        }

        // Overlay: lang/<configured>.yml. Falls back to "en" if missing.
        String lang = plugin.configManager().lang();
        File overlayFile = new File(plugin.getDataFolder(), "lang/" + lang + ".yml");
        if (!overlayFile.exists()) {
            plugin.getLogger().warning("lang/" + lang + ".yml not found — falling back to en");
            overlayFile = new File(plugin.getDataFolder(), "lang/en.yml");
        }
        if (overlayFile.exists()) {
            FileConfiguration overlay = YamlConfiguration.loadConfiguration(overlayFile);
            for (String key : overlay.getKeys(false)) {
                String val = overlay.getString(key);
                if (val != null) messages.put(key, val); // overlay wins
            }
        }
    }

    /**
     * Resolves the key, prepends the prefix unless the source string starts
     * with {@code "!"}, parses with MiniMessage and sends to {@code to}.
     */
    public void send(CommandSender to, String key, TagResolver... resolvers) {
        to.sendMessage(component(key, resolvers));
    }

    /**
     * Same as {@link #send} but returns the {@link Component} instead of
     * sending it. Useful for help screens that build a multi-line output.
     */
    public Component component(String key, TagResolver... resolvers) {
        String src = messages.get(key);
        if (src == null) {
            return MM.deserialize("<red>[missing message: " + key + "]");
        }
        boolean noPrefix = src.startsWith("!");
        if (noPrefix) src = src.substring(1);
        String composed = noPrefix ? src : (prefix + src);
        return MM.deserialize(composed, resolvers);
    }

    private void saveLangResource(String path) {
        File out = new File(plugin.getDataFolder(), path);
        if (out.exists()) return;
        // saveResource handles directory creation and JAR extraction.
        try {
            plugin.saveResource(path, false);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Bundled resource missing: " + path);
        }
    }
}

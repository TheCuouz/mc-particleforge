package com.cristian.particleforge.cfg;

import com.cristian.particleforge.ParticleForgePlugin;
import com.ttsstudio.sdk.PluginIdentity;
import com.ttsstudio.sdk.chat.ChatPrefix;
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
 * <p>All strings are MiniMessage source. The studio-wide chat prefix from the
 * TTS-SDK ({@link ChatPrefix}) is auto-prepended on send so every plugin in
 * the suite renders with the same visual signature
 * ({@code ◈ Particles › <message>}). A leading {@code "!"} on the source line
 * is stripped and suppresses the prefix — used for fragments that get composed
 * into other messages (e.g. help-line descriptions injected via a
 * {@code <desc>} placeholder), where prefixing the inner fragment would
 * produce a double prefix.</p>
 *
 * <p>Placeholders are passed in as {@link TagResolver} varargs — never by
 * string concatenation. That preserves MiniMessage safety against
 * user-controlled input.</p>
 */
public final class MessageManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final ParticleForgePlugin plugin;
    private final ChatPrefix chatPrefix;
    private final Map<String, String> messages = new HashMap<>();

    public MessageManager(ParticleForgePlugin plugin) {
        this.plugin = plugin;
        // Resolve identity from the SDK registry. ParticleForgePlugin#onEnable
        // also sets withAlias("Particles") on its own identity field, but the
        // registry entry now provides the same alias + a stable suite color,
        // so PluginIdentity.of(plugin) is sufficient here.
        this.chatPrefix = ChatPrefix.of(PluginIdentity.of(plugin));
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
        // Base: messages.yml (English bank).
        File baseFile = new File(plugin.getDataFolder(), "messages.yml");
        FileConfiguration base = YamlConfiguration.loadConfiguration(baseFile);
        for (String key : base.getKeys(false)) {
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
     * Resolves the key, prepends the studio prefix unless the source string
     * starts with {@code "!"}, parses with MiniMessage and sends to {@code to}.
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
        Component body = MM.deserialize(src, resolvers);
        return noPrefix ? body : chatPrefix.then(body);
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

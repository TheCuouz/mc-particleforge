package com.cristian.particleforge.registry;

import com.cristian.particleforge.model.EffectDescriptor;
import com.cristian.particleforge.model.StepDescriptor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Loads effect YAML files from {@code dataFolder/effects/} and registers
 * resolved {@link EffectDescriptor}s with an {@link EffectRegistry}.
 *
 * On first run, extracts every bundled {@code effects/**.yml} resource from
 * the plugin JAR into the data folder (preserving directory structure). Files
 * that already exist on disk are left alone.
 *
 * Inheritance ({@code extends:}) is resolved in topological order with cycle
 * detection. Step overrides match by id (deep-merge of {@code params}).
 *
 * Invalid effects are logged at SEVERE and skipped — they do not throw,
 * because a single bad YAML must not prevent the rest from loading.
 */
public final class YamlEffectLoader {

    private static final Set<String> VALID_TYPES = Set.of(
        "BURST","RING","SPHERE","HELIX","VORTEX","SHOCKWAVE",
        "CONE","RAIN","CYLINDER","CUBE","LINE",
        "TRAIL","ORBIT","TEXT","SHAPE",
        "DELAY","REPEAT","PARALLEL"
    );
    private static final Set<String> CONTAINER_TYPES = Set.of("REPEAT","PARALLEL");

    private final JavaPlugin plugin;
    private final Logger log;

    public YamlEffectLoader(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin);
        this.log = plugin.getLogger();
    }

    public int loadAll(EffectRegistry registry) {
        Objects.requireNonNull(registry);
        File effectsDir = new File(plugin.getDataFolder(), "effects");
        if (!effectsDir.exists()) {
            extractBundledEffectsToDataFolder(effectsDir);
        }
        if (!effectsDir.exists()) {
            log.info("No effects directory found and nothing to extract from JAR; registry empty.");
            return 0;
        }

        // Pass 1: parse all files into raw descriptors (no resolution yet).
        Map<String, Raw> raws = new LinkedHashMap<>();
        try (Stream<Path> stream = Files.walk(effectsDir.toPath())) {
            stream.filter(p -> p.toString().endsWith(".yml") || p.toString().endsWith(".yaml"))
                  .filter(Files::isRegularFile)
                  .sorted()
                  .forEach(p -> {
                      Raw raw = parseRaw(p.toFile());
                      if (raw != null) raws.put(raw.name, raw);
                  });
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Failed to walk effects dir " + effectsDir, ex);
            return 0;
        }

        // Pass 2: resolve inheritance topologically with cycle detection.
        Map<String, EffectDescriptor> resolved = new LinkedHashMap<>();
        Set<String> resolving = new LinkedHashSet<>();
        for (Raw raw : raws.values()) {
            try {
                resolve(raw, raws, resolved, resolving);
            } catch (RuntimeException ex) {
                log.log(Level.SEVERE, "Failed to resolve effect '" + raw.name + "': " + ex.getMessage());
            }
        }

        // Pass 3: validate + register.
        int count = 0;
        for (EffectDescriptor d : resolved.values()) {
            if (validate(d)) {
                registry.register(d);
                count++;
            }
        }
        return count;
    }

    // ---- raw form ----

    private static final class Raw {
        String name;
        String category;
        String extendsName;          // nullable
        Map<String, Object> defaults = new LinkedHashMap<>();
        List<StepDescriptor> steps = new ArrayList<>();
        List<RawOverride> overrides = new ArrayList<>();
        File source;
    }

    private static final class RawOverride {
        String id;
        Integer durationTicks;        // nullable = no override
        Map<String, Object> params;   // nullable = no override
    }

    private Raw parseRaw(File f) {
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(f);
            Raw raw = new Raw();
            raw.source = f;
            raw.name = yaml.getString("name");
            raw.category = yaml.getString("category");
            raw.extendsName = yaml.getString("extends"); // may be null
            ConfigurationSection defs = yaml.getConfigurationSection("defaults");
            if (defs != null) raw.defaults = sectionToMap(defs);
            List<?> stepsList = yaml.getList("steps");
            if (stepsList != null) {
                for (Object item : stepsList) {
                    if (item instanceof Map<?, ?> map) raw.steps.add(parseStep(map));
                }
            }
            List<?> oversList = yaml.getList("overrides");
            if (oversList != null) {
                for (Object item : oversList) {
                    if (item instanceof Map<?, ?> map) raw.overrides.add(parseOverride(map));
                }
            }
            if (raw.name == null || raw.name.isBlank()) {
                log.severe("Effect file '" + f + "' missing required key 'name'.");
                return null;
            }
            // Reject duplicate step ids within a single file. (Merging later
            // would silently dedupe via LinkedHashMap, masking the error.)
            Set<String> seenStepIds = new HashSet<>();
            for (StepDescriptor s : raw.steps) {
                if (!seenStepIds.add(s.id())) {
                    log.severe("Effect '" + raw.name + "' (" + f + "): duplicate step id '" + s.id() + "'");
                    return null;
                }
            }
            if (raw.category == null || raw.category.isBlank()) {
                // Derive category from name if it has the form "category/effect"
                int slash = raw.name.indexOf('/');
                raw.category = slash > 0 ? raw.name.substring(0, slash) : "misc";
            }
            return raw;
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Failed to parse effect file " + f + ": " + ex.getMessage());
            return null;
        }
    }

    private StepDescriptor parseStep(Map<?, ?> map) {
        String id = String.valueOf(map.get("id"));
        String type = String.valueOf(map.get("type"));
        int dur = map.get("duration") instanceof Number n ? n.intValue() : 1;
        Map<String, Object> params = new LinkedHashMap<>();
        Object p = map.get("params");
        if (p instanceof Map<?, ?> pm) {
            for (Map.Entry<?, ?> e : pm.entrySet()) params.put(String.valueOf(e.getKey()), e.getValue());
        }
        List<StepDescriptor> children = new ArrayList<>();
        Object cs = map.get("steps");
        if (cs instanceof List<?> list) {
            for (Object c : list) {
                if (c instanceof Map<?, ?> cm) children.add(parseStep(cm));
            }
        }
        return new StepDescriptor(id, type, dur, params, children);
    }

    private RawOverride parseOverride(Map<?, ?> map) {
        RawOverride o = new RawOverride();
        o.id = String.valueOf(map.get("id"));
        if (map.get("duration") instanceof Number n) o.durationTicks = n.intValue();
        Object p = map.get("params");
        if (p instanceof Map<?, ?> pm) {
            o.params = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : pm.entrySet()) o.params.put(String.valueOf(e.getKey()), e.getValue());
        }
        return o;
    }

    private Map<String, Object> sectionToMap(ConfigurationSection section) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) out.put(key, section.get(key));
        return out;
    }

    // ---- resolution ----

    private EffectDescriptor resolve(Raw raw, Map<String, Raw> all,
                                       Map<String, EffectDescriptor> resolved,
                                       Set<String> resolving) {
        if (resolved.containsKey(raw.name)) return resolved.get(raw.name);
        if (!resolving.add(raw.name)) {
            throw new IllegalStateException("Inheritance cycle: " + String.join(" -> ", resolving) + " -> " + raw.name);
        }
        try {
            EffectDescriptor parent = null;
            if (raw.extendsName != null && !raw.extendsName.isBlank()) {
                Raw parentRaw = all.get(raw.extendsName);
                if (parentRaw == null) {
                    throw new IllegalArgumentException("'" + raw.name + "' extends unknown parent '" + raw.extendsName + "'");
                }
                parent = resolve(parentRaw, all, resolved, resolving);
            }

            // Merge defaults: parent's defaults override-merged with child's defaults.
            Map<String, Object> mergedDefaults = new LinkedHashMap<>();
            if (parent != null) mergedDefaults.putAll(parent.defaults());
            mergedDefaults.putAll(raw.defaults);

            // Merge steps: start with parent's, apply overrides by id, append child's new steps.
            LinkedHashMap<String, StepDescriptor> byId = new LinkedHashMap<>();
            if (parent != null) {
                for (StepDescriptor s : parent.steps()) byId.put(s.id(), s);
            }
            // Apply parent step overrides from child's `overrides:` block.
            for (RawOverride ov : raw.overrides) {
                StepDescriptor base = byId.get(ov.id);
                if (base == null) {
                    log.warning("Effect '" + raw.name + "': override targets unknown step id '" + ov.id + "' - ignored");
                    continue;
                }
                int dur = ov.durationTicks != null ? ov.durationTicks : base.durationTicks();
                Map<String, Object> mergedParams = new LinkedHashMap<>(base.rawParams());
                if (ov.params != null) mergedParams.putAll(ov.params);
                byId.put(ov.id, new StepDescriptor(base.id(), base.type(), dur, mergedParams, base.children()));
            }
            // Append child's own `steps:` (new ids).
            for (StepDescriptor s : raw.steps) {
                if (parent != null && byId.containsKey(s.id())) {
                    log.warning("Effect '" + raw.name + "': step id '" + s.id() + "' shadows inherited step - child wins");
                }
                byId.put(s.id(), s);
            }

            EffectDescriptor d = new EffectDescriptor(
                raw.name,
                raw.category,
                mergedDefaults,
                new ArrayList<>(byId.values()),
                raw.extendsName
            );
            resolved.put(raw.name, d);
            return d;
        } finally {
            resolving.remove(raw.name);
        }
    }

    // ---- validation ----

    private boolean validate(EffectDescriptor d) {
        if (!d.name().matches("[a-z0-9_-]+(/[a-z0-9_-]+)?")) {
            log.severe("Effect '" + d.name() + "': name must match [a-z0-9_-]+/[a-z0-9_-]+");
            return false;
        }
        if (d.steps().isEmpty()) {
            log.severe("Effect '" + d.name() + "': must define at least one step");
            return false;
        }
        Set<String> seenIds = new HashSet<>();
        for (StepDescriptor s : d.steps()) {
            if (!seenIds.add(s.id())) {
                log.severe("Effect '" + d.name() + "': duplicate step id '" + s.id() + "'");
                return false;
            }
            if (!validateStep(s, d.name())) return false;
        }
        // Dry-resolve params against defaults to catch unresolved ${...} placeholders early.
        try {
            for (StepDescriptor s : d.steps()) {
                dryResolveRecursive(s, d.defaults());
            }
        } catch (RuntimeException ex) {
            log.severe("Effect '" + d.name() + "': " + ex.getMessage());
            return false;
        }
        return true;
    }

    private void dryResolveRecursive(StepDescriptor s, Map<String, Object> defaults) {
        ParamResolver.resolve(s.rawParams(), Map.of(), defaults);
        for (StepDescriptor child : s.children()) {
            dryResolveRecursive(child, defaults);
        }
    }

    private boolean validateStep(StepDescriptor s, String effectName) {
        String type = s.type().toUpperCase(Locale.ROOT);
        if (!VALID_TYPES.contains(type)) {
            log.severe("Effect '" + effectName + "' step '" + s.id() + "': unknown type '" + s.type() + "'");
            return false;
        }
        if (s.durationTicks() <= 0 && !CONTAINER_TYPES.contains(type)) {
            log.severe("Effect '" + effectName + "' step '" + s.id() + "': duration must be > 0");
            return false;
        }
        boolean isContainer = CONTAINER_TYPES.contains(type);
        if (isContainer && s.children().isEmpty()) {
            log.severe("Effect '" + effectName + "' step '" + s.id() + "': " + type + " requires nested steps");
            return false;
        }
        if (!isContainer && !s.children().isEmpty()) {
            log.severe("Effect '" + effectName + "' step '" + s.id() + "': " + type + " must not have nested steps");
            return false;
        }
        for (StepDescriptor child : s.children()) {
            if (!validateStep(child, effectName)) return false;
        }
        return true;
    }

    // ---- bundled-resource extraction ----

    private void extractBundledEffectsToDataFolder(File effectsDir) {
        // Locate our own JAR.
        URL jarUrl = getClass().getProtectionDomain().getCodeSource().getLocation();
        if (jarUrl == null) {
            log.info("Plugin code source unknown - skipping bundled effect extraction.");
            return;
        }
        File jarFile = new File(jarUrl.getFile());
        if (!jarFile.exists() || !jarFile.getName().endsWith(".jar")) {
            log.info("Plugin not loaded from a .jar (dev environment?) - skipping bundled effect extraction.");
            return;
        }
        if (!effectsDir.exists() && !effectsDir.mkdirs()) {
            log.warning("Could not create effects dir " + effectsDir);
            return;
        }
        int extracted = 0;
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(jarFile.toPath()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (!name.startsWith("effects/")) continue;
                if (entry.isDirectory()) continue;
                if (!(name.endsWith(".yml") || name.endsWith(".yaml"))) continue;
                File target = new File(plugin.getDataFolder(), name);
                if (target.exists()) continue;
                File parent = target.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    log.warning("Could not create " + parent);
                    continue;
                }
                // Copy bytes from the current zip entry into the target file
                // WITHOUT wrapping `zis` in a try-with-resources (that would close
                // the parent ZipInputStream and abort the iteration).
                try (OutputStream out = Files.newOutputStream(target.toPath())) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = zis.read(buf)) > 0) out.write(buf, 0, n);
                    extracted++;
                } catch (IOException ex) {
                    log.warning("Failed to extract " + name + ": " + ex.getMessage());
                }
            }
        } catch (IOException ex) {
            log.log(Level.WARNING, "Failed to extract bundled effects: " + ex.getMessage(), ex);
        }
        if (extracted > 0) {
            log.info("Extracted " + extracted + " bundled effect(s) to " + effectsDir);
        }
    }
}

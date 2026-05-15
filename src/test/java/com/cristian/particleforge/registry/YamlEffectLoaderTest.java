package com.cristian.particleforge.registry;

import com.cristian.particleforge.model.EffectDescriptor;
import com.cristian.particleforge.model.StepDescriptor;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link YamlEffectLoader}. Uses {@code @TempDir} to give the loader
 * a real on-disk effects/ directory; the {@link JavaPlugin} is mocked.
 *
 * The bundled-JAR-extraction path is not exercised here — it requires the
 * loader's class to be loaded from a real {@code .jar}, which is not the case
 * in the surefire forked-VM classloader.
 */
class YamlEffectLoaderTest {

    private static JavaPlugin mockPlugin(Path tmp) {
        JavaPlugin p = mock(JavaPlugin.class);
        when(p.getDataFolder()).thenReturn(tmp.toFile());
        when(p.getLogger()).thenReturn(Logger.getLogger("test"));
        return p;
    }

    private static Path writeEffect(Path effectsRoot, String relPath, String content) throws IOException {
        Path target = effectsRoot.resolve(relPath);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content);
        return target;
    }

    // ---- 1. single valid effect ----

    @Test
    void loadsValidEffect(@TempDir Path tmp) throws IOException {
        Path effectsRoot = tmp.resolve("effects");
        writeEffect(effectsRoot, "crate/legendary-win.yml", """
                name: crate/legendary-win
                category: crate
                defaults:
                  radius: 2.5
                steps:
                  - id: explode
                    type: BURST
                    duration: 5
                    params:
                      count: 32
                """);

        EffectRegistry reg = new EffectRegistry();
        int n = new YamlEffectLoader(mockPlugin(tmp)).loadAll(reg);

        assertEquals(1, n);
        Optional<EffectDescriptor> opt = reg.find("crate/legendary-win");
        assertTrue(opt.isPresent());
        EffectDescriptor d = opt.get();
        assertEquals("crate", d.category());
        assertEquals(1, d.steps().size());
        assertEquals("explode", d.steps().get(0).id());
        assertEquals("BURST", d.steps().get(0).type());
    }

    // ---- 2. inheritance: merged defaults + step overrides ----

    @Test
    void inheritanceMergesDefaultsAndOverrides(@TempDir Path tmp) throws IOException {
        Path effectsRoot = tmp.resolve("effects");
        writeEffect(effectsRoot, "crate/legendary-win.yml", """
                name: crate/legendary-win
                category: crate
                defaults:
                  radius: 2.5
                  particle: END_ROD
                  count: 24
                steps:
                  - id: build-up
                    type: HELIX
                    duration: 30
                    params:
                      radius: ${radius}
                      height: 4
                      turns: 3
                  - id: explode
                    type: BURST
                    duration: 5
                    params:
                      particle: ${particle}
                      count: ${count}
                      speed: 0.6
                """);
        writeEffect(effectsRoot, "crate/epic-win.yml", """
                name: crate/epic-win
                category: crate
                extends: crate/legendary-win
                defaults:
                  radius: 1.5
                  count: 12
                overrides:
                  - id: build-up
                    duration: 20
                  - id: explode
                    params:
                      speed: 0.4
                """);

        EffectRegistry reg = new EffectRegistry();
        int n = new YamlEffectLoader(mockPlugin(tmp)).loadAll(reg);

        assertEquals(2, n);
        EffectDescriptor epic = reg.find("crate/epic-win").orElseThrow();

        // Defaults: child overrides parent for "radius" and "count", inherits "particle".
        assertEquals(1.5, ((Number) epic.defaults().get("radius")).doubleValue(), 1e-9);
        assertEquals(12,  ((Number) epic.defaults().get("count")).intValue());
        assertEquals("END_ROD", epic.defaults().get("particle"));

        // Steps preserved from parent, with overrides applied.
        Map<String, StepDescriptor> byId = epic.stepsById();
        assertEquals(2, byId.size());
        StepDescriptor build = byId.get("build-up");
        assertNotNull(build);
        assertEquals("HELIX", build.type());
        assertEquals(20, build.durationTicks()); // overridden
        // Inherited param preserved
        assertEquals("${radius}", build.rawParams().get("radius"));
        assertEquals(3, ((Number) build.rawParams().get("turns")).intValue());

        StepDescriptor explode = byId.get("explode");
        assertNotNull(explode);
        assertEquals("BURST", explode.type());
        assertEquals(5, explode.durationTicks()); // not overridden
        // params deep-merged: speed overridden, others inherited
        assertEquals(0.4, ((Number) explode.rawParams().get("speed")).doubleValue(), 1e-9);
        assertEquals("${particle}", explode.rawParams().get("particle"));
        assertEquals("${count}", explode.rawParams().get("count"));
    }

    // ---- 3. inheritance cycle ----

    @Test
    void cycleRejectsBothEffects(@TempDir Path tmp) throws IOException {
        Path effectsRoot = tmp.resolve("effects");
        writeEffect(effectsRoot, "a.yml", """
                name: a
                category: misc
                extends: b
                steps:
                  - id: x
                    type: BURST
                    duration: 5
                """);
        writeEffect(effectsRoot, "b.yml", """
                name: b
                category: misc
                extends: a
                steps:
                  - id: x
                    type: BURST
                    duration: 5
                """);

        EffectRegistry reg = new EffectRegistry();
        int n = new YamlEffectLoader(mockPlugin(tmp)).loadAll(reg);

        assertEquals(0, n);
        assertFalse(reg.find("a").isPresent());
        assertFalse(reg.find("b").isPresent());
    }

    // ---- 4. missing `name` ----

    @Test
    void missingNameSkipsFileButLoadsOthers(@TempDir Path tmp) throws IOException {
        Path effectsRoot = tmp.resolve("effects");
        writeEffect(effectsRoot, "broken.yml", """
                category: misc
                steps:
                  - id: x
                    type: BURST
                    duration: 5
                """);
        writeEffect(effectsRoot, "good.yml", """
                name: good
                category: misc
                steps:
                  - id: x
                    type: BURST
                    duration: 5
                """);

        EffectRegistry reg = new EffectRegistry();
        int n = new YamlEffectLoader(mockPlugin(tmp)).loadAll(reg);

        assertEquals(1, n);
        assertTrue(reg.find("good").isPresent());
    }

    // ---- 5. unresolved ${param} ----

    @Test
    void unresolvedPlaceholderRejectsEffect(@TempDir Path tmp) throws IOException {
        Path effectsRoot = tmp.resolve("effects");
        writeEffect(effectsRoot, "bad.yml", """
                name: bad
                category: misc
                defaults:
                  radius: 1.0
                steps:
                  - id: x
                    type: BURST
                    duration: 5
                    params:
                      count: ${missing}
                """);
        writeEffect(effectsRoot, "ok.yml", """
                name: ok
                category: misc
                steps:
                  - id: x
                    type: BURST
                    duration: 5
                """);

        EffectRegistry reg = new EffectRegistry();
        int n = new YamlEffectLoader(mockPlugin(tmp)).loadAll(reg);

        assertEquals(1, n);
        assertFalse(reg.find("bad").isPresent());
        assertTrue(reg.find("ok").isPresent());
    }

    // ---- 6. unknown step type ----

    @Test
    void unknownStepTypeRejects(@TempDir Path tmp) throws IOException {
        Path effectsRoot = tmp.resolve("effects");
        writeEffect(effectsRoot, "bad.yml", """
                name: bad
                category: misc
                steps:
                  - id: x
                    type: FROBNICATE
                    duration: 5
                """);

        EffectRegistry reg = new EffectRegistry();
        int n = new YamlEffectLoader(mockPlugin(tmp)).loadAll(reg);

        assertEquals(0, n);
        assertFalse(reg.find("bad").isPresent());
    }

    // ---- 7. duplicate step id within one effect ----

    @Test
    void duplicateStepIdRejects(@TempDir Path tmp) throws IOException {
        Path effectsRoot = tmp.resolve("effects");
        writeEffect(effectsRoot, "dup.yml", """
                name: dup
                category: misc
                steps:
                  - id: x
                    type: BURST
                    duration: 5
                  - id: x
                    type: RING
                    duration: 10
                """);

        EffectRegistry reg = new EffectRegistry();
        int n = new YamlEffectLoader(mockPlugin(tmp)).loadAll(reg);

        assertEquals(0, n);
        assertFalse(reg.find("dup").isPresent());
    }
}

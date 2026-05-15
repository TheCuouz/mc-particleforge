package com.cristian.particleforge.registry;

import com.cristian.particleforge.model.EffectDescriptor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EffectRegistryTest {

    private static EffectDescriptor make(String name, String category) {
        return new EffectDescriptor(name, category, Map.of(), List.of(), null);
    }

    @Test
    void registerFindRoundTrip() {
        EffectRegistry reg = new EffectRegistry();
        EffectDescriptor d = make("crate/legendary-win", "crate");
        reg.register(d);
        Optional<EffectDescriptor> found = reg.find("crate/legendary-win");
        assertTrue(found.isPresent());
        assertSame(d, found.get());
        assertTrue(reg.find("nope").isEmpty());
    }

    @Test
    void byCategoryFilters() {
        EffectRegistry reg = new EffectRegistry();
        reg.register(make("a", "crate"));
        reg.register(make("b", "crate"));
        reg.register(make("c", "combat"));
        assertEquals(2, reg.byCategory("crate").size());
        assertEquals(1, reg.byCategory("combat").size());
        assertEquals(0, reg.byCategory("nope").size());
        assertEquals(0, reg.byCategory(null).size());
    }

    @Test
    void sizeAndCategoriesReflectContents() {
        EffectRegistry reg = new EffectRegistry();
        reg.register(make("a", "crate"));
        reg.register(make("b", "combat"));
        reg.register(make("c", "crate"));
        assertEquals(3, reg.size());
        assertEquals(2, reg.categories().size());
        assertTrue(reg.categories().contains("crate"));
        assertTrue(reg.categories().contains("combat"));
    }

    @Test
    void reregisterOverwrites() {
        EffectRegistry reg = new EffectRegistry();
        reg.register(make("a", "crate"));
        EffectDescriptor replacement = make("a", "combat");
        assertDoesNotThrow(() -> reg.register(replacement));
        assertEquals(1, reg.size());
        assertEquals("combat", reg.find("a").orElseThrow().category());
    }

    @Test
    void clearEmptiesRegistry() {
        EffectRegistry reg = new EffectRegistry();
        reg.register(make("a", "crate"));
        reg.register(make("b", "combat"));
        reg.clear();
        assertEquals(0, reg.size());
        assertTrue(reg.all().isEmpty());
    }
}

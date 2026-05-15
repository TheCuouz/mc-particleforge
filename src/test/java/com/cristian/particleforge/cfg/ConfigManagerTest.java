package com.cristian.particleforge.cfg;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies each {@link ConfigManager} getter reads the right key with the
 * documented default. The defaults are the spec contract — if you change one
 * here, also change it in {@code config.yml}.
 */
class ConfigManagerTest {

    private JavaPlugin plugin;
    private FileConfiguration cfg;
    private ConfigManager configManager;

    @BeforeEach
    void setUp() {
        plugin = mock(JavaPlugin.class);
        cfg = mock(FileConfiguration.class);
        when(plugin.getConfig()).thenReturn(cfg);
        configManager = new ConfigManager(plugin);
    }

    @Test
    void debugReadsCorrectKey() {
        when(cfg.getBoolean("debug", false)).thenReturn(true);
        assertTrue(configManager.debug());
        verify(cfg).getBoolean("debug", false);
    }

    @Test
    void debugDefaultIsFalse() {
        when(cfg.getBoolean("debug", false)).thenReturn(false);
        assertFalse(configManager.debug());
        verify(cfg).getBoolean("debug", false);
    }

    @Test
    void globalMaxActiveReadsCorrectKey() {
        when(cfg.getInt("budget.global-max-active-effects", 64)).thenReturn(128);
        assertEquals(128, configManager.globalMaxActive());
        verify(cfg).getInt("budget.global-max-active-effects", 64);
    }

    @Test
    void perPlayerMaxActiveReadsCorrectKey() {
        when(cfg.getInt("budget.per-player-max-active-effects", 4)).thenReturn(8);
        assertEquals(8, configManager.perPlayerMaxActive());
        verify(cfg).getInt("budget.per-player-max-active-effects", 4);
    }

    @Test
    void policyReadsCorrectKey() {
        when(cfg.getString("budget.policy", "NEWEST_FIRST")).thenReturn("FIFO");
        assertEquals("FIFO", configManager.policy());
        verify(cfg).getString("budget.policy", "NEWEST_FIRST");
    }

    @Test
    void lodNearReadsCorrectKey() {
        when(cfg.getInt("lod.near", 16)).thenReturn(20);
        assertEquals(20, configManager.lodNear());
        verify(cfg).getInt("lod.near", 16);
    }

    @Test
    void lodMidReadsCorrectKey() {
        when(cfg.getInt("lod.mid", 48)).thenReturn(50);
        assertEquals(50, configManager.lodMid());
        verify(cfg).getInt("lod.mid", 48);
    }

    @Test
    void lodFarReadsCorrectKey() {
        when(cfg.getInt("lod.far", 96)).thenReturn(100);
        assertEquals(100, configManager.lodFar());
        verify(cfg).getInt("lod.far", 96);
    }

    @Test
    void cullBeyondFarReadsCorrectKey() {
        when(cfg.getBoolean("lod.cull-beyond-far", true)).thenReturn(false);
        assertFalse(configManager.cullBeyondFar());
        verify(cfg).getBoolean("lod.cull-beyond-far", true);
    }

    @Test
    void nearMulReadsCorrectKey() {
        when(cfg.getDouble("lod-multipliers.near", 1.0)).thenReturn(0.9);
        assertEquals(0.9, configManager.nearMul());
        verify(cfg).getDouble("lod-multipliers.near", 1.0);
    }

    @Test
    void midMulReadsCorrectKey() {
        when(cfg.getDouble("lod-multipliers.mid", 0.5)).thenReturn(0.4);
        assertEquals(0.4, configManager.midMul());
        verify(cfg).getDouble("lod-multipliers.mid", 0.5);
    }

    @Test
    void farMulReadsCorrectKey() {
        when(cfg.getDouble("lod-multipliers.far", 0.2)).thenReturn(0.1);
        assertEquals(0.1, configManager.farMul());
        verify(cfg).getDouble("lod-multipliers.far", 0.2);
    }

    @Test
    void langReadsCorrectKey() {
        when(cfg.getString("lang", "en")).thenReturn("es");
        assertEquals("es", configManager.lang());
        verify(cfg).getString("lang", "en");
    }

    @Test
    void reloadDelegatesToPlugin() {
        configManager.reload();
        verify(plugin).reloadConfig();
    }
}

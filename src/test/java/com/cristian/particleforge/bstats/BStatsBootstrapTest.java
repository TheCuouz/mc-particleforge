package com.cristian.particleforge.bstats;

import com.cristian.particleforge.engine.EffectEngine;
import com.cristian.particleforge.registry.EffectRegistry;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BStatsBootstrap}.
 *
 * The chart-callback path is not exercised here: instantiating a real
 * {@link org.bstats.bukkit.Metrics} would attempt to talk to bStats.org.
 * That path is covered by smoke at Task 24.
 */
class BStatsBootstrapTest {

    @Test
    void bucket_covers_documented_ranges() throws Exception {
        Method m = BStatsBootstrap.class.getDeclaredMethod("bucket", int.class);
        m.setAccessible(true);
        assertEquals("0-10",   m.invoke(null, 0));
        assertEquals("0-10",   m.invoke(null, 10));
        assertEquals("11-25",  m.invoke(null, 11));
        assertEquals("26-50",  m.invoke(null, 50));
        assertEquals("51-100", m.invoke(null, 51));
        assertEquals("51-100", m.invoke(null, 100));
        assertEquals("100+",   m.invoke(null, 101));
    }

    @Test
    void init_with_zero_id_logs_info_and_returns_without_throwing() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        EffectRegistry registry = mock(EffectRegistry.class);
        EffectEngine engine = mock(EffectEngine.class);
        Logger logger = Logger.getLogger("BStatsBootstrapTest");
        when(plugin.getLogger()).thenReturn(logger);

        BStatsBootstrap boot = new BStatsBootstrap(plugin, registry, engine);

        // Sanity: confirm the precondition the test depends on.
        assertEquals(0, BStatsBootstrap.BSTATS_PLUGIN_ID,
                "test assumes plugin id not yet assigned");

        assertDoesNotThrow(boot::init);
        verify(plugin).getLogger();
    }
}

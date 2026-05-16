package com.cristian.particleforge.bstats;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}

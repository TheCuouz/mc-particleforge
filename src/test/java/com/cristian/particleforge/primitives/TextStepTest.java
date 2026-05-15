package com.cristian.particleforge.primitives;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TextStepTest {

    @Test
    void pixelOffsetsForACountMatchesLitCount() {
        TextStep s = new TextStep("t", 20, Map.of("text", "A"));
        List<double[]> offs = s.pixelOffsets();
        assertEquals(BlockFont.litCount("A"), offs.size());
    }

    @Test
    void emptyStringYieldsEmptyOffsets() {
        TextStep s = new TextStep("t", 20, Map.of("text", ""));
        assertTrue(s.pixelOffsets().isEmpty());
    }

    @Test
    void offsetsForSingleGlyphAreWithinBoundingBox() {
        double scale = 0.2;
        TextStep s = new TextStep("t", 20, Map.of(
            "text", "A",
            "scale", scale,
            "yaw-deg", 0.0,
            "char-spacing", 0.0
        ));
        // For yaw=0, offsets are not rotated: x in [0, (COLS-1)*scale],
        // y in [0, (ROWS-1)*scale], z = 0
        for (double[] off : s.pixelOffsets()) {
            assertTrue(off[0] >= -1e-9 && off[0] <= (BlockFont.COLS - 1) * scale + 1e-9,
                "x in box: " + off[0]);
            assertTrue(off[1] >= -1e-9 && off[1] <= (BlockFont.ROWS - 1) * scale + 1e-9,
                "y in box: " + off[1]);
            assertEquals(0.0, off[2], 1e-9);
        }
    }

    @Test
    void multiCharOffsetsSumToLitCount() {
        TextStep s = new TextStep("t", 20, Map.of("text", "HI"));
        assertEquals(BlockFont.litCount("HI"), s.pixelOffsets().size());
    }

    @Test
    void effectiveCountScales() {
        TextStep s = new TextStep("t", 20, Map.of("count", 4));
        assertEquals(4, s.effectiveCount(1.0));
        assertEquals(2, s.effectiveCount(0.5));
        assertEquals(0, s.effectiveCount(0.0));
    }
}

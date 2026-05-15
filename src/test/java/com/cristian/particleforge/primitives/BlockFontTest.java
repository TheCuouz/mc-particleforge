package com.cristian.particleforge.primitives;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlockFontTest {

    private static int countLit(boolean[][] g) {
        int n = 0;
        for (boolean[] row : g) for (boolean b : row) if (b) n++;
        return n;
    }

    @Test
    void glyphSizesAreFiveByThree() {
        boolean[][] a = BlockFont.glyph('A');
        assertEquals(BlockFont.ROWS, a.length);
        assertEquals(BlockFont.COLS, a[0].length);
    }

    @Test
    void glyphAHasExpectedLitCount() {
        // .#.  -> 1
        // #.#  -> 2
        // ###  -> 3
        // #.#  -> 2
        // #.#  -> 2
        // total = 10
        assertEquals(10, countLit(BlockFont.glyph('A')));
    }

    @Test
    void caseInsensitiveLookup() {
        assertArrayEquals(BlockFont.glyph('A'), BlockFont.glyph('a'));
    }

    @Test
    void unsupportedCharReturnsBlank() {
        boolean[][] g = BlockFont.glyph('@');
        assertEquals(0, countLit(g));
    }

    @Test
    void spaceIsBlank() {
        assertEquals(0, countLit(BlockFont.glyph(' ')));
    }

    @Test
    void litCountSumsAcrossText() {
        int single = BlockFont.litCount("A");
        int doubled = BlockFont.litCount("AA");
        assertEquals(single * 2, doubled);
    }

    @Test
    void litCountOfEmptyStringIsZero() {
        assertEquals(0, BlockFont.litCount(""));
    }
}

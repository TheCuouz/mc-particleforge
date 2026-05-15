package com.cristian.particleforge.primitives;

import java.util.HashMap;
import java.util.Map;

/** Minimal 5x3 bitmap font for A-Z, 0-9, and space. Used by TextStep. */
public final class BlockFont {

    public static final int ROWS = 5;
    public static final int COLS = 3;

    private static final Map<Character, boolean[][]> GLYPHS = new HashMap<>();

    static {
        // Each glyph defined as 5 strings of 3 chars each ('#' = lit, '.' = empty).
        put(' ', "...", "...", "...", "...", "...");
        put('A', ".#.", "#.#", "###", "#.#", "#.#");
        put('B', "##.", "#.#", "##.", "#.#", "##.");
        put('C', ".##", "#..", "#..", "#..", ".##");
        put('D', "##.", "#.#", "#.#", "#.#", "##.");
        put('E', "###", "#..", "##.", "#..", "###");
        put('F', "###", "#..", "##.", "#..", "#..");
        put('G', ".##", "#..", "#.#", "#.#", ".##");
        put('H', "#.#", "#.#", "###", "#.#", "#.#");
        put('I', "###", ".#.", ".#.", ".#.", "###");
        put('J', "..#", "..#", "..#", "#.#", ".#.");
        put('K', "#.#", "##.", "#..", "##.", "#.#");
        put('L', "#..", "#..", "#..", "#..", "###");
        put('M', "#.#", "###", "###", "#.#", "#.#");
        put('N', "#.#", "###", "###", "###", "#.#");
        put('O', ".#.", "#.#", "#.#", "#.#", ".#.");
        put('P', "##.", "#.#", "##.", "#..", "#..");
        put('Q', ".#.", "#.#", "#.#", "##.", ".##");
        put('R', "##.", "#.#", "##.", "#.#", "#.#");
        put('S', ".##", "#..", ".#.", "..#", "##.");
        put('T', "###", ".#.", ".#.", ".#.", ".#.");
        put('U', "#.#", "#.#", "#.#", "#.#", ".#.");
        put('V', "#.#", "#.#", "#.#", "#.#", ".#.");
        put('W', "#.#", "#.#", "###", "###", "#.#");
        put('X', "#.#", "#.#", ".#.", "#.#", "#.#");
        put('Y', "#.#", "#.#", ".#.", ".#.", ".#.");
        put('Z', "###", "..#", ".#.", "#..", "###");
        put('0', ".#.", "#.#", "#.#", "#.#", ".#.");
        put('1', ".#.", "##.", ".#.", ".#.", "###");
        put('2', "##.", "..#", ".#.", "#..", "###");
        put('3', "##.", "..#", ".#.", "..#", "##.");
        put('4', "#.#", "#.#", "###", "..#", "..#");
        put('5', "###", "#..", "##.", "..#", "##.");
        put('6', ".#.", "#..", "##.", "#.#", ".#.");
        put('7', "###", "..#", ".#.", "#..", "#..");
        put('8', ".#.", "#.#", ".#.", "#.#", ".#.");
        put('9', ".#.", "#.#", ".##", "..#", ".#.");
    }

    private static void put(char ch, String r0, String r1, String r2, String r3, String r4) {
        String[] rows = {r0, r1, r2, r3, r4};
        boolean[][] glyph = new boolean[ROWS][COLS];
        for (int r = 0; r < ROWS; r++) {
            String row = rows[r];
            for (int c = 0; c < COLS; c++) {
                glyph[r][c] = row.charAt(c) == '#';
            }
        }
        GLYPHS.put(ch, glyph);
    }

    /** Returns the glyph, or a blank if char unsupported. */
    public static boolean[][] glyph(char ch) {
        boolean[][] g = GLYPHS.get(Character.toUpperCase(ch));
        return g != null ? g : GLYPHS.get(' ');
    }

    /** Count of lit pixels for a string (sum across glyphs). */
    public static int litCount(String text) {
        int n = 0;
        for (int i = 0; i < text.length(); i++) {
            for (boolean[] row : glyph(text.charAt(i))) {
                for (boolean b : row) if (b) n++;
            }
        }
        return n;
    }

    private BlockFont() {}
}

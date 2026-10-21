package chronorunner.gfx;

import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.util.HashMap;
import java.util.Map;

/**
 * Hand-authored 8-bit bitmap font.
 *
 * <p>Every glyph lives on one fixed cell so text is pure monospace and integer
 * scaling stays razor sharp: nothing is ever antialiased, each source pixel
 * becomes a solid {@code scale x scale} block. The cell is 6 columns wide
 * (5 of ink plus one gutter) and 14 rows tall, laid out as:
 *
 * <pre>
 *   rows  0..1   tone mark   (acute / grave / hook / tilde)
 *   rows  2..3   shape mark  (circumflex / breve) or tone when unstacked
 *   row   4      gap
 *   rows  5..13  base glyph  (7 rows above the baseline, 2 descender rows)
 * </pre>
 *
 * <p>Vietnamese glyphs are not authored one by one: a base letter is composed
 * with a shape mark and a tone mark at load time. Because the marks sit above
 * the base inside the same cell, plain Latin and accented text share a baseline
 * and line up column for column.
 */
public final class PixelFont {

    /** Ink columns per glyph. */
    public static final int COLS = 5;
    /** Cell width including the one-column gutter between glyphs. */
    public static final int CELL_W = 6;
    /** Total cell height. */
    public static final int CELL_H = 14;
    /** Row index of the top of a capital letter. */
    public static final int CAP_TOP = 5;
    /** Row index of the top of an x-height (lowercase) letter. */
    public static final int X_TOP = 7;
    /** Row index of the baseline. */
    public static final int BASELINE_ROW = 11;

    private static final Map<Character, int[]> BASE = new HashMap<>();
    private static final Map<Character, int[]> GLYPHS = new HashMap<>();

    private PixelFont() {
    }

    private enum Shape { NONE, CIRC, BREVE, HORN }

    private enum Tone { NONE, ACUTE, GRAVE, HOOK, TILDE, DOTBELOW }

    // ===== public metrics ===================================================

    /** Pixels from the top of a cell down to the baseline. */
    public static int ascent(int scale) {
        return BASELINE_ROW * scale;
    }

    /** Height of a full cell, marks and descenders included. */
    public static int lineHeight(int scale) {
        return CELL_H * scale;
    }

    /** Width of {@code text} drawn at {@code scale}, spacing included. */
    public static int width(String text, int scale, int tracking) {
        int n = text.length();
        if (n == 0) return 0;
        return n * CELL_W * scale + (n - 1) * tracking;
    }

    public static boolean has(char c) {
        return GLYPHS.containsKey(c);
    }

    // ===== public drawing ===================================================

    /** Draws {@code text} with the cell top-left at {@code (x, top)}. */
    public static void draw(Graphics2D g, String text, int x, int top,
                            int scale, Paint paint, int tracking) {
        drawBaseline(g, text, x, top + ascent(scale), scale, paint, tracking);
    }

    /** Draws {@code text} sitting on {@code baselineY}. */
    public static void drawBaseline(Graphics2D g, String text, int x, int baselineY,
                                    int scale, Paint paint, int tracking) {
        Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Object taa = g.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        Object interp = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        Paint old = g.getPaint();
        if (paint != null) {
            g.setPaint(paint);
        }
        int top = baselineY - ascent(scale);
        int px = x;
        for (int i = 0; i < text.length(); i++) {
            int[] mask = GLYPHS.get(text.charAt(i));
            if (mask != null) {
                blit(g, mask, px, top, scale);
            }
            px += CELL_W * scale;
            if (i < text.length() - 1) {
                px += tracking;
            }
        }
        g.setPaint(old);
        restore(g, RenderingHints.KEY_ANTIALIASING, aa);
        restore(g, RenderingHints.KEY_TEXT_ANTIALIASING, taa);
        restore(g, RenderingHints.KEY_INTERPOLATION, interp);
    }

    private static void restore(Graphics2D g, RenderingHints.Key key, Object value) {
        if (value != null) {
            g.setRenderingHint(key, value);
        }
    }

    private static void blit(Graphics2D g, int[] mask, int x, int top, int scale) {
        for (int row = 0; row < CELL_H; row++) {
            int bits = mask[row];
            if (bits == 0) continue;
            int y = top + row * scale;
            int run = 0;
            for (int col = 0; col <= COLS; col++) {
                // Merge adjacent ink columns into one fill to halve the draw calls.
                boolean ink = col < COLS && (bits & (1 << col)) != 0;
                if (ink) {
                    run++;
                } else if (run > 0) {
                    g.fillRect(x + (col - run) * scale, y, run * scale, scale);
                    run = 0;
                }
            }
        }
    }

    // ===== glyph table construction =========================================

    private static void define(char c, String... rows) {
        int[] mask = new int[CELL_H];
        for (int r = 0; r < rows.length; r++) {
            String row = rows[r];
            int bits = 0;
            for (int col = 0; col < row.length() && col < COLS; col++) {
                if (row.charAt(col) == '#') {
                    bits |= 1 << col;
                }
            }
            mask[CAP_TOP + r] = bits;
        }
        BASE.put(c, mask);
    }

    private static void orRow(int[] mask, int row, String rowBits) {
        int bits = 0;
        for (int col = 0; col < rowBits.length() && col < COLS; col++) {
            if (rowBits.charAt(col) == '#') {
                bits |= 1 << col;
            }
        }
        mask[row] |= bits;
    }

    private static void put(char c, int[] mask) {
        GLYPHS.put(c, mask);
    }

    private static void putCopy(char c, int[] source) {
        put(c, source.clone());
    }

    private static Shape shapeOf(char c) {
        switch (c) {
            case '\u00E2': case '\u00C2':  // â Â
            case '\u00EA': case '\u00CA':  // ê Ê
            case '\u00F4': case '\u00D4':  // ô Ô
                return Shape.CIRC;
            case '\u0103': case '\u0102':  // ă Ă
                return Shape.BREVE;
            case '\u01A1': case '\u01A0':  // ơ Ơ
            case '\u01B0': case '\u01AF':  // ư Ư
                return Shape.HORN;
            default:
                return Shape.NONE;
        }
    }

    /**
     * Builds one accented glyph from a base letter plus the two marks that can
     * stack over it. {@code row} marks where the letter's body starts, so a
     * mark always hugs the letter it belongs to instead of floating away from
     * short lowercase vowels.
     */
    private static int[] compose(char baseChar, Shape shape, Tone tone) {
        int[] mask = BASE.get(baseChar).clone();
        int markRow = Character.isUpperCase(baseChar) ? CAP_TOP : X_TOP;

        switch (shape) {
            case HORN:
                orRow(mask, markRow - 2, "....#");
                orRow(mask, markRow - 1, "....#");
                break;
            case CIRC:
                orRow(mask, markRow - 2, "..#..");
                orRow(mask, markRow - 1, ".#.#.");
                break;
            case BREVE:
                orRow(mask, markRow - 2, ".#.#.");
                orRow(mask, markRow - 1, "..#..");
                break;
            default:
                break;
        }

        if (tone == Tone.NONE) {
            return mask;
        }
        if (tone == Tone.DOTBELOW) {
            orRow(mask, 12, "..##.");
            orRow(mask, 13, "..##.");
            return mask;
        }
        int top = shape == Shape.NONE ? markRow - 2 : markRow - 4;
        switch (tone) {
            case ACUTE:
                orRow(mask, top, "...#.");
                orRow(mask, top + 1, "..#..");
                break;
            case GRAVE:
                orRow(mask, top, "..#..");
                orRow(mask, top + 1, "...#.");
                break;
            case HOOK:
                orRow(mask, top, ".##..");
                orRow(mask, top + 1, "...#.");
                break;
            case TILDE:
                orRow(mask, top, ".##..");
                orRow(mask, top + 1, "..##.");
                break;
            default:
                break;
        }
        return mask;
    }

    /**
     * Registers one vowel family: six glyphs sharing a base and a shape, in
     * tone order none / sắc / huyền / hỏi / ngã / nặng.
     */
    private static void vowel(char baseChar, String family) {
        Shape shape = shapeOf(family.charAt(0));
        for (int t = 0; t < family.length(); t++) {
            char c = family.charAt(t);
            if (shape == Shape.NONE && c == baseChar) {
                continue;
            }
            put(c, compose(baseChar, shape, Tone.values()[t]));
        }
    }

    static {
        defineBase();
        defineLatinExtras();
        GLYPHS.putAll(BASE);

        // Plain lowercase i keeps its dot; every toned or dotted variant drops
        // it, which is the Vietnamese convention.
        putCopy('i', BASE.get('i'));
        orRow(GLYPHS.get('i'), CAP_TOP, "..#..");

        // đ / Đ are a d with a bar through the ascender.
        put('đ', BASE.get('d').clone());
        orRow(GLYPHS.get('đ'), CAP_TOP + 3, "#####");
        put('Đ', BASE.get('D').clone());
        orRow(GLYPHS.get('Đ'), CAP_TOP + 3, "#####");

        defineVietnamese();
    }

    private static void defineVietnamese() {
        vowel('a', "aáàảãạ");
        vowel('a', "âấầẩẫậ");
        vowel('a', "ăắằẳẵặ");
        vowel('e', "eéèẻẽẹ");
        vowel('e', "êếềểễệ");
        vowel('i', "iíìỉĩị");
        vowel('o', "oóòỏõọ");
        vowel('o', "ôốồổỗộ");
        vowel('o', "ơớờởỡợ");
        vowel('u', "uúùủũụ");
        vowel('u', "ưứừửữự");
        vowel('y', "yýỳỷỹỵ");

        vowel('A', "AÁÀẢÃẠ");
        vowel('A', "ÂẤẦẨẪẬ");
        vowel('A', "ĂẮẰẲẴẶ");
        vowel('E', "EÉÈẺẼẸ");
        vowel('E', "ÊẾỀỂỄỆ");
        vowel('I', "IÍÌỈĨỊ");
        vowel('O', "OÓÒỎÕỌ");
        vowel('O', "ÔỐỒỔỖỘ");
        vowel('O', "ƠỚỜỞỠỢ");
        vowel('U', "UÚÙỦŨỤ");
        vowel('U', "ƯỨỪỬỮỰ");
        vowel('Y', "YÝỲỶỸỴ");
    }

    private static void defineLatinExtras() {
        // The ASCII digits and the punctuation the UI actually shows; anything
        // else at runtime is skipped rather than drawn as tofu.
        define(' ', ".....", ".....", ".....", ".....", ".....", ".....", ".....");
        define('\u2022', ".....", ".....", ".....", ".##..", ".##..", ".....", ".....");
        define('\u2014', ".....", ".....", ".....", ".....", "#####", ".....", ".....");
        define('\u2190', ".....", ".....", "..#..", ".##..", "#####", ".##..", "..#..");
        define('\u2192', ".....", ".....", "..#..", "..##.", "#####", "..##.", "..#..");
        define('\u2191', "..#..", ".###.", "#.#.#", "..#..", "..#..", "..#..", "..#..");
        define('\u2193', "..#..", "..#..", "..#..", "..#..", "#.#.#", ".###.", "..#..");
        define('\u25B6', ".....", "#....", "###..", "#####", "###..", "#....", ".....");
    }

    private static void defineBase() {
        define('A', ".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#");
        define('B', "####.", "#...#", "#...#", "####.", "#...#", "#...#", "####.");
        define('C', ".###.", "#...#", "#....", "#....", "#....", "#...#", ".###.");
        define('D', "####.", "#...#", "#...#", "#...#", "#...#", "#...#", "####.");
        define('E', "#####", "#....", "#....", "####.", "#....", "#....", "#####");
        define('F', "#####", "#....", "#....", "####.", "#....", "#....", "#....");
        define('G', ".###.", "#...#", "#....", "#.###", "#...#", "#...#", ".###.");
        define('H', "#...#", "#...#", "#...#", "#####", "#...#", "#...#", "#...#");
        define('I', "#####", "..#..", "..#..", "..#..", "..#..", "..#..", "#####");
        define('J', "..###", "...#.", "...#.", "...#.", "...#.", "#..#.", ".##..");
        define('K', "#...#", "#..#.", "#.#..", "##...", "#.#..", "#..#.", "#...#");
        define('L', "#....", "#....", "#....", "#....", "#....", "#....", "#####");
        define('M', "#...#", "##.##", "#.#.#", "#...#", "#...#", "#...#", "#...#");
        define('N', "#...#", "##..#", "#.#.#", "#..##", "#...#", "#...#", "#...#");
        define('O', ".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###.");
        define('P', "####.", "#...#", "#...#", "####.", "#....", "#....", "#....");
        define('Q', ".###.", "#...#", "#...#", "#...#", "#.#.#", "#..#.", ".##.#");
        define('R', "####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#");
        define('S', ".####", "#....", "#....", ".###.", "....#", "....#", "####.");
        define('T', "#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#..");
        define('U', "#...#", "#...#", "#...#", "#...#", "#...#", "#...#", ".###.");
        define('V', "#...#", "#...#", "#...#", "#...#", "#...#", ".#.#.", "..#..");
        define('W', "#...#", "#...#", "#...#", "#...#", "#.#.#", "##.##", "#...#");
        define('X', "#...#", "#...#", ".#.#.", "..#..", ".#.#.", "#...#", "#...#");
        define('Y', "#...#", "#...#", ".#.#.", "..#..", "..#..", "..#..", "..#..");
        define('Z', "#####", "....#", "...#.", "..#..", ".#...", "#....", "#####");

        define('0', ".###.", "#...#", "#..##", "#.#.#", "##..#", "#...#", ".###.");
        define('1', "..#..", ".##..", "..#..", "..#..", "..#..", "..#..", ".###.");
        define('2', ".###.", "#...#", "....#", "...#.", "..#..", ".#...", "#####");
        define('3', "####.", "....#", "....#", ".###.", "....#", "....#", "####.");
        define('4', "#..#.", "#..#.", "#..#.", "#####", "...#.", "...#.", "...#.");
        define('5', "#####", "#....", "####.", "....#", "....#", "#...#", ".###.");
        define('6', ".####", "#....", "#....", "####.", "#...#", "#...#", ".###.");
        define('7', "#####", "....#", "...#.", "..#..", ".#...", ".#...", ".#...");
        define('8', ".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###.");
        define('9', ".###.", "#...#", "#...#", ".####", "....#", "....#", "####.");

        define('a', ".....", ".....", ".###.", "....#", ".####", "#...#", ".####");
        define('b', "#....", "#....", "####.", "#...#", "#...#", "#...#", "####.");
        define('c', ".....", ".....", ".####", "#....", "#....", "#....", ".####");
        define('d', "....#", "....#", ".####", "#...#", "#...#", "#...#", ".####");
        define('e', ".....", ".....", ".###.", "#...#", "#####", "#....", ".###.");
        define('f', "..##.", ".#...", "####.", ".#...", ".#...", ".#...", ".#...");
        define('g', ".....", ".....", ".####", "#...#", "#...#", "#...#", ".####", "....#", ".###.");
        define('h', "#....", "#....", "####.", "#...#", "#...#", "#...#", "#...#");
        define('i', ".....", ".....", "..#..", "..#..", "..#..", "..#..", "..#..");
        define('j', "...#.", ".....", "...#.", "...#.", "...#.", "...#.", "...#.", "#..#.", ".##..");
        define('k', "#....", "#....", "#..#.", "#.#..", "##...", "#.#..", "#..#.");
        define('l', ".##..", "..#..", "..#..", "..#..", "..#..", "..#..", ".###.");
        define('m', ".....", ".....", "##.#.", "#.#.#", "#.#.#", "#.#.#", "#.#.#");
        define('n', ".....", ".....", "####.", "#...#", "#...#", "#...#", "#...#");
        define('o', ".....", ".....", ".###.", "#...#", "#...#", "#...#", ".###.");
        define('p', ".....", ".....", "####.", "#...#", "#...#", "#...#", "####.", "#....", "#....");
        define('q', ".....", ".....", ".####", "#...#", "#...#", "#...#", ".####", "....#", "....#");
        define('r', ".....", ".....", "#.##.", "##...", "#....", "#....", "#....");
        define('s', ".....", ".....", ".####", "#....", ".###.", "....#", "####.");
        define('t', ".#...", ".#...", "####.", ".#...", ".#...", ".#...", "..##.");
        define('u', ".....", ".....", "#...#", "#...#", "#...#", "#..##", ".##.#");
        define('v', ".....", ".....", "#...#", "#...#", "#...#", ".#.#.", "..#..");
        define('w', ".....", ".....", "#...#", "#...#", "#.#.#", "#.#.#", ".#.#.");
        define('x', ".....", ".....", "#...#", ".#.#.", "..#..", ".#.#.", "#...#");
        define('y', ".....", ".....", "#...#", "#...#", "#...#", "#...#", ".####", "....#", ".###.");
        define('z', ".....", ".....", "#####", "...#.", "..#..", ".#...", "#####");

        define('!', "..#..", "..#..", "..#..", "..#..", "..#..", ".....", "..#..");
        define('"', ".#.#.", ".#.#.", ".....", ".....", ".....", ".....", ".....");
        define('\'', "..#..", "..#..", ".....", ".....", ".....", ".....", ".....");
        define('(', "...#.", "..#..", ".#...", ".#...", ".#...", "..#..", "...#.");
        define(')', ".#...", "..#..", "...#.", "...#.", "...#.", "..#..", ".#...");
        define('[', "..###", "..#..", "..#..", "..#..", "..#..", "..#..", "..###");
        define(']', "###..", "..#..", "..#..", "..#..", "..#..", "..#..", "###..");
        define('+', ".....", "..#..", "..#..", "#####", "..#..", "..#..", ".....");
        define(',', ".....", ".....", ".....", ".....", ".....", "..##.", "..#..", ".#...");
        define('-', ".....", ".....", ".....", ".....", "#####", ".....", ".....");
        define('.', ".....", ".....", ".....", ".....", ".....", ".....", "..#..");
        define('/', "....#", "....#", "...#.", "..#..", ".#...", "#....", "#....");
        define(':', ".....", ".....", ".....", "..#..", ".....", "..#..", ".....");
        define(';', ".....", ".....", ".....", "..#..", ".....", "..##.", "..#..", ".#...");
        define('<', ".....", "...#.", "..#..", ".#...", "..#..", "...#.", ".....");
        define('=', ".....", ".....", ".....", "#####", ".....", "#####", ".....");
        define('>', ".....", ".#...", "..#..", "...#.", "..#..", ".#...", ".....");
        define('?', ".###.", "#...#", "....#", "...#.", "..#..", ".....", "..#..");
        define('\\', "#....", "#....", ".#...", "..#..", "...#.", "....#", "....#");
        define('^', "..#..", ".#.#.", "#...#", ".....", ".....", ".....", ".....");
        define('_', ".....", ".....", ".....", ".....", ".....", ".....", ".....", "#####");
        define('#', ".#.#.", ".#.#.", "#####", ".#.#.", "#####", ".#.#.", ".#.#.");
        define('{', ".###.", ".#...", ".#...", "..#..", ".#...", ".#...", ".###.");
        define('}', ".###.", "...#.", "...#.", "..#..", "...#.", "...#.", ".###.");
    }
}

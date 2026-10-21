package chronorunner.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.util.ArrayList;
import java.util.List;

import chronorunner.core.Game;
import chronorunner.gfx.Art;
import chronorunner.gfx.PixelArt;
import chronorunner.gfx.PixelFont;
import chronorunner.gfx.PixelSprite;

/** Shared drawing helpers and the game's colour/type palette. */
public final class UiKit {

    public static final Color BG_TOP = new Color(8, 10, 22);
    public static final Color BG_BOTTOM = new Color(22, 14, 44);
    public static final Color ACCENT = new Color(90, 200, 255);
    public static final Color ACCENT_WARM = new Color(255, 150, 90);
    public static final Color TEXT = new Color(232, 238, 250);
    public static final Color MUTED = new Color(146, 156, 184);
    public static final Color WARN = new Color(255, 90, 120);
    public static final Color OK = new Color(90, 220, 140);

    public static final Font TITLE = new Font("Dialog", Font.BOLD, 64);
    public static final Font H1 = new Font("Dialog", Font.BOLD, 34);
    public static final Font H2 = new Font("Dialog", Font.BOLD, 22);
    public static final Font BODY = new Font("Dialog", Font.PLAIN, 18);
    public static final Font SMALL = new Font("Dialog", Font.PLAIN, 14);

    private UiKit() {
    }

    /** Flat bands the sky gradient is quantised into, so it reads as pixel art. */
    private static final int SKY_BANDS = 36;

    public static void background(Graphics2D g, Color top, Color bottom) {
        PixelArt.vGradient(g, 0, 0, Game.WIDTH, Game.HEIGHT, top, bottom, SKY_BANDS);
    }

    /**
     * Legacy entry point kept for the older screens. It draws the bitmap font
     * so no screen can fall back to a blurry TrueType face; the font's point
     * size only selects an integer pixel scale.
     */
    public static void centered(Graphics2D g, String text, Font font, Color color, int centerX, int y) {
        pixelCentered(g, text, scaleOf(font), color, centerX, y, 0);
    }

    /** Maps a legacy {@link Font} onto the nearest bitmap-font integer scale. */
    public static int scaleOf(Font font) {
        int size = font.getSize();
        if (size >= 56) return 9;
        if (size >= 30) return 5;
        if (size >= 20) return 4;
        if (size >= 16) return 3;
        return 2;
    }

    public static void panel(Graphics2D g, int x, int y, int w, int h, Color fill, Color border) {
        g.setColor(fill);
        g.fillRoundRect(x, y, w, h, 18, 18);
        g.setColor(border);
        g.drawRoundRect(x, y, w, h, 18, 18);
    }

    public static void bar(Graphics2D g, int x, int y, int w, int h, double ratio, Color fill, Color back) {
        g.setColor(back);
        g.fillRoundRect(x, y, w, h, h, h);
        int filled = (int) (w * Math.max(0, Math.min(1, ratio)));
        if (filled > 0) {
            g.setColor(fill);
            g.fillRoundRect(x, y, filled, h, h, h);
        }
        g.setColor(new Color(255, 255, 255, 60));
        g.drawRoundRect(x, y, w, h, h, h);
    }

    /** Word wraps text to {@code maxWidth}, honouring explicit newlines. */
    public static List<String> wrap(Graphics2D g, String text, int maxWidth, Font font) {
        int scale = scaleOf(font);
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\n", -1)) {
            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }
            StringBuilder current = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                String candidate = current.length() == 0 ? word : current + " " + word;
                if (PixelFont.width(candidate, scale, 0) > maxWidth && current.length() > 0) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    current = new StringBuilder(candidate);
                }
            }
            if (current.length() > 0) {
                lines.add(current.toString());
            }
        }
        return lines;
    }

    // ===== Arcade frame =====================================================
    //
    // The front end shares one look, taken off the reference start-screen
    // mockup: a flat violet field ruled by a thin grid, gold gradient type for
    // anything that shouts, cream for anything that speaks, and a warm
    // triangle marking the current row. Everything below is that vocabulary,
    // drawn with the built-in bitmap font so it stays crisp at any scale.

    public static final Color ARC_BG = new Color(0x21, 0x1C, 0x3A);
    public static final Color ARC_GRID = new Color(0x3D, 0x38, 0x60);
    public static final Color ARC_PANEL = new Color(0x1A, 0x16, 0x30);
    public static final Color ARC_PANEL_SEL = new Color(0x2C, 0x25, 0x4E);
    public static final Color ARC_EDGE = new Color(0x3D, 0x38, 0x60);
    public static final Color ARC_DIM = new Color(0x8A, 0x82, 0xA8);
    public static final Color GOLD_TOP = new Color(0xFA, 0xA4, 0x1A);
    public static final Color GOLD_BOTTOM = new Color(0xF8, 0x7A, 0x15);
    public static final Color GOLD_PALE = new Color(0xEA, 0xBA, 0x6E);
    public static final Color CREAM = new Color(0xFD, 0xF6, 0xD8);

    /** Pixel scales for the shared type ramp: seven-pixel caps times these. */
    public static final int ARC_TITLE_SCALE = 11;
    public static final int ARC_H1_SCALE = 8;
    public static final int ARC_H2_SCALE = 5;
    public static final int ARC_BODY_SCALE = 3;
    public static final int ARC_SMALL_SCALE = 2;

    private static final BasicStroke EDGE_STROKE = new BasicStroke(1f);
    private static final BasicStroke SELECTED_STROKE = new BasicStroke(3f);

    /** Extra pixels between glyphs on the big title. */
    public static final int TITLE_TRACKING = 10;
    /** Extra pixels between glyphs on menu rows. */
    public static final int ITEM_TRACKING = 4;

    /** First-grid-row baselines, shared by every arcade screen. */
    public static final int HEADER_CAPTION_Y = 58;
    public static final int HEADER_VALUE_Y = 100;

    private static final int GRID_COLS = 8;
    private static final int GRID_ROWS = 6;
    private static final int GRID_MARGIN_X = 66;
    private static final int GRID_MARGIN_Y = 50;

    /** Flat violet field ruled by the mockup's thin grid. */
    public static void arcadeBackground(Graphics2D g) {
        PixelArt.rect(g, 0, 0, Game.WIDTH, Game.HEIGHT, ARC_BG);
        int pitchX = (Game.WIDTH - 2 * GRID_MARGIN_X) / (GRID_COLS - 1);
        for (int i = 0; i < GRID_COLS; i++) {
            PixelArt.rect(g, GRID_MARGIN_X + i * pitchX, 0, 1, Game.HEIGHT, ARC_GRID);
        }
        int pitchY = (Game.HEIGHT - 2 * GRID_MARGIN_Y) / (GRID_ROWS - 1);
        for (int j = 0; j < GRID_ROWS; j++) {
            PixelArt.rect(g, 0, GRID_MARGIN_Y + j * pitchY, Game.WIDTH, 1, ARC_GRID);
        }
    }

    /** Top-to-bottom gold gradient, the one paint the whole front end reuses. */
    private static Paint goldPaint(int top, int bottom) {
        return new GradientPaint(0, top, GOLD_TOP, 0, Math.max(top + 1, bottom), GOLD_BOTTOM);
    }

    private static Paint goldPaintFor(int scale, int baselineY) {
        return goldPaint(baselineY - PixelFont.ascent(scale),
                baselineY + (PixelFont.CELL_H - PixelFont.BASELINE_ROW) * scale);
    }

    // ===== bitmap text ======================================================

    /** Width in pixels of {@code text} at {@code scale}. */
    public static int pixelWidth(String text, int scale, int tracking) {
        return PixelFont.width(text, scale, tracking);
    }

    /** Centred bitmap text in one flat colour. */
    public static void pixelCentered(Graphics2D g, String text, int scale, Color color,
                                     int centerX, int baselineY, int tracking) {
        PixelFont.drawBaseline(g, text, centerX - PixelFont.width(text, scale, tracking) / 2,
                baselineY, scale, color, tracking);
    }

    /** Left-aligned bitmap text in one flat colour. */
    public static void pixelLeft(Graphics2D g, String text, int scale, Color color,
                                 int left, int baselineY, int tracking) {
        PixelFont.drawBaseline(g, text, left, baselineY, scale, color, tracking);
    }

    /** Right-aligned bitmap text in one flat colour. */
    public static void pixelRight(Graphics2D g, String text, int scale, Color color,
                                  int right, int baselineY, int tracking) {
        PixelFont.drawBaseline(g, text, right - PixelFont.width(text, scale, tracking),
                baselineY, scale, color, tracking);
    }

    /** Centred bitmap text filled with the gold gradient. */
    public static void pixelGold(Graphics2D g, String text, int scale,
                                 int centerX, int baselineY, int tracking) {
        PixelFont.drawBaseline(g, text, centerX - PixelFont.width(text, scale, tracking) / 2,
                baselineY, scale, goldPaintFor(scale, baselineY), tracking);
    }

    /** Left-aligned bitmap text filled with the gold gradient. */
    public static void pixelGoldLeft(Graphics2D g, String text, int scale,
                                     int left, int baselineY, int tracking) {
        PixelFont.drawBaseline(g, text, left, baselineY, scale,
                goldPaintFor(scale, baselineY), tracking);
    }

    /** Centred arcade text in one flat colour. */
    public static void arcade(Graphics2D g, String text, int scale, Color color,
                              int centerX, int baselineY, int tracking) {
        pixelCentered(g, text, scale, color, centerX, baselineY, tracking);
    }

    /** Centred arcade text filled with the gold gradient. */
    public static void arcadeGold(Graphics2D g, String text, int scale,
                                  int centerX, int baselineY, int tracking) {
        pixelGold(g, text, scale, centerX, baselineY, tracking);
    }

    /** Left-aligned arcade text in one flat colour. */
    public static void arcadeLeft(Graphics2D g, String text, int scale, Color color,
                                  int left, int baselineY, int tracking) {
        pixelLeft(g, text, scale, color, left, baselineY, tracking);
    }

    /** Left-aligned arcade text filled with the gold gradient. */
    public static void arcadeGoldLeft(Graphics2D g, String text, int scale,
                                      int left, int baselineY, int tracking) {
        pixelGoldLeft(g, text, scale, left, baselineY, tracking);
    }

    /** Right-aligned arcade text in one flat colour. */
    public static void arcadeRight(Graphics2D g, String text, int scale, Color color,
                                   int right, int baselineY, int tracking) {
        pixelRight(g, text, scale, color, right, baselineY, tracking);
    }

    /** Width of the cursor triangle at {@code scale}. */
    public static int arrowWidth(int scale) {
        return PixelFont.COLS * scale;
    }

    /**
     * Right-pointing triangle cursor, drawn from the bitmap font so its edges
     * stay stair-stepped rather than antialiased. Vertically centred on
     * {@code centerY}.
     */
    public static void arcadeArrow(Graphics2D g, int left, int centerY, int scale, Color color) {
        PixelFont.drawBaseline(g, "\u25B6", left, centerY + 3 * scale, scale, color, 0);
    }

    /** Caption over value in the top-left, exactly the mockup's HI-SCORE block. */
    public static void arcadeHeader(Graphics2D g, String caption, String value) {
        arcadeLeft(g, caption, ARC_BODY_SCALE, CREAM, 70, HEADER_CAPTION_Y, 2);
        arcadeGoldLeft(g, value, ARC_H2_SCALE, 70, HEADER_VALUE_Y, 2);
    }

    /**
     * The mockup's top-right icon strip, filled with the playable roster.
     * Sprites stand on the first grid row, right-aligned into the margin.
     */
    public static void arcadeRoster(Graphics2D g, String[] spriteIds) {
        int right = Game.WIDTH - 80;
        int pitch = 66;
        int feet = 104;
        for (int i = 0; i < spriteIds.length; i++) {
            int cx = right - (spriteIds.length - 1 - i) * pitch;
            PixelArt.shadow(g, cx, feet + 2, 40, new Color(0, 0, 0, 120));
            Art.player(spriteIds[i]).frame(0)
                    .drawAnchored(g, cx, feet, 1, false, PixelSprite.Mode.NORMAL, 1f);
        }
    }

    /**
     * One centred menu row: cream when current with a warm triangle beside it,
     * dim otherwise.
     */
    public static void arcadeItem(Graphics2D g, String label, int baselineY, boolean selected) {
        arcadeMenuItem(g, label, ARC_H2_SCALE, baselineY, selected, GOLD_TOP);
    }

    /** Menu row with an explicit cursor colour, used by the in-screen menus. */
    public static void arcadeMenuItem(Graphics2D g, String label, int scale, int baselineY,
                                      boolean selected, Color cursor) {
        int left = (Game.WIDTH - PixelFont.width(label, scale, ITEM_TRACKING)) / 2;
        if (selected) {
            arcadeArrow(g, left - arrowWidth(scale) - 24, baselineY - 2 * scale, scale, cursor);
        }
        PixelFont.drawBaseline(g, label, left, baselineY, scale,
                selected ? CREAM : ARC_DIM, ITEM_TRACKING);
    }

    /**
     * One of the title screen's large options: always pale gold, marked by a
     * small white triangle when it is the current one.
     */
    public static void arcadeOption(Graphics2D g, String label, int scale, int baselineY,
                                    boolean selected) {
        int left = (Game.WIDTH - PixelFont.width(label, scale, ITEM_TRACKING)) / 2;
        if (selected) {
            arcadeArrow(g, left - arrowWidth(scale) - 28, baselineY - 2 * scale, scale, Color.WHITE);
        }
        PixelFont.drawBaseline(g, label, left, baselineY, scale, GOLD_PALE, ITEM_TRACKING);
    }

    /**
     * One full-width list row, used by the chapter list. Locked rows are
     * washed over so they read as unavailable without hiding their shape.
     */
    public static void arcadeRow(Graphics2D g, int x, int y, int w, int h,
                                 boolean selected, boolean locked) {
        int radius = 12;
        g.setColor(selected ? ARC_PANEL_SEL : ARC_PANEL);
        g.fillRoundRect(x, y, w, h, radius, radius);
        if (locked) {
            g.setColor(new Color(ARC_BG.getRed(), ARC_BG.getGreen(), ARC_BG.getBlue(), 150));
            g.fillRoundRect(x, y, w, h, radius, radius);
        }
        g.setColor(selected ? GOLD_TOP : ARC_EDGE);
        g.setStroke(selected ? SELECTED_STROKE : EDGE_STROKE);
        g.drawRoundRect(x, y, w, h, radius, radius);
        g.setStroke(EDGE_STROKE);
        if (selected) {
            arcadeArrow(g, x - arrowWidth(ARC_SMALL_SCALE) - 14, y + h / 2, ARC_SMALL_SCALE, GOLD_TOP);
        }
    }

    /** Rounded card: the selected one lifts and takes a gold ring. */
    public static void arcadeCard(Graphics2D g, int x, int y, int w, int h,
                                  boolean selected, Color ring) {
        int radius = 16;
        g.setColor(selected ? ARC_PANEL_SEL : ARC_PANEL);
        g.fillRoundRect(x, y, w, h, radius, radius);
        g.setColor(selected && ring != null ? ring : ARC_EDGE);
        g.setStroke(selected ? SELECTED_STROKE : EDGE_STROKE);
        g.drawRoundRect(x, y, w, h, radius, radius);
        g.setStroke(EDGE_STROKE);
    }

    /**
     * Ring-only overlay for a card whose contents are already painted. Unlike
     * {@link #arcadeCard} this fills nothing, so it can mark a card that is
     * shared by both players without erasing what is drawn inside it.
     */
    public static void arcadeRing(Graphics2D g, int x, int y, int w, int h, Color ring) {
        int radius = 16;
        g.setColor(ring);
        g.setStroke(SELECTED_STROKE);
        g.drawRoundRect(x, y, w, h, radius, radius);
        g.setStroke(EDGE_STROKE);
    }
}

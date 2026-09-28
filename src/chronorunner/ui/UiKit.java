package chronorunner.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import chronorunner.core.Game;
import chronorunner.gfx.PixelArt;

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

    public static void centered(Graphics2D g, String text, Font font, Color color, int centerX, int y) {
        g.setFont(font);
        g.setColor(color);
        int w = g.getFontMetrics().stringWidth(text);
        g.drawString(text, centerX - w / 2, y);
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
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\n", -1)) {
            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }
            StringBuilder current = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                String candidate = current.length() == 0 ? word : current + " " + word;
                if (fm.stringWidth(candidate) > maxWidth && current.length() > 0) {
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
}

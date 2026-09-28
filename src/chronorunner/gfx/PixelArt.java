package chronorunner.gfx;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Primitive shapes drawn without antialiasing so effects, shadows and glows
 * match the hard-edged look of the sprites. The window turns antialiasing on
 * globally, so every helper saves and restores the hint.
 */
public final class PixelArt {

    private PixelArt() {
    }

    /** Filled integer disc. Slight stair-stepping is the point. */
    public static void disc(Graphics2D g, int cx, int cy, int radius, Color color) {
        if (radius <= 0 || color == null || color.getAlpha() == 0) return;
        boolean restore = withNoAA(g);
        g.setColor(color);
        for (int dy = -radius; dy <= radius; dy++) {
            int dx = (int) Math.sqrt((double) (radius * radius - dy * dy));
            g.fillRect(cx - dx, cy + dy, dx * 2 + 1, 1);
        }
        restoreAA(g, restore);
    }

    /** Filled ellipse used for the contact shadow under a character. */
    public static void shadow(Graphics2D g, int cx, int bottomY, int width, Color color) {
        int rx = Math.max(2, width / 2);
        int ry = Math.max(1, width / 8);
        if (color == null || color.getAlpha() == 0) return;
        boolean restore = withNoAA(g);
        g.setColor(color);
        for (int dy = -ry; dy <= ry; dy++) {
            double t = 1.0 - (double) (dy * dy) / (double) (ry * ry);
            int dx = (int) (rx * Math.sqrt(Math.max(0, t)));
            g.fillRect(cx - dx, bottomY + dy - ry / 2, dx * 2 + 1, 1);
        }
        restoreAA(g, restore);
    }

    /** Filled rectangle with no antialiased edges. */
    public static void rect(Graphics2D g, int x, int y, int w, int h, Color color) {
        if (w <= 0 || h <= 0 || color == null) return;
        boolean restore = withNoAA(g);
        g.setColor(color);
        g.fillRect(x, y, w, h);
        restoreAA(g, restore);
    }

    /**
     * Vertical gradient quantised into flat horizontal bands. A real
     * {@code GradientPaint} dithers smoothly, which fights the hard-edged
     * sprites; banding the sky keeps the whole frame on the same pixel grid.
     */
    public static void vGradient(Graphics2D g, int x, int y, int w, int h,
                                 Color top, Color bottom, int bands) {
        if (w <= 0 || h <= 0 || bands <= 0) return;
        boolean restore = withNoAA(g);
        int bandH = Math.max(1, h / bands);
        for (int i = 0; i < bands; i++) {
            int by = y + i * bandH;
            int bh = i == bands - 1 ? y + h - by : bandH;
            if (bh <= 0) break;
            g.setColor(mix(top, bottom, bands == 1 ? 0 : (double) i / (bands - 1)));
            g.fillRect(x, by, w, bh);
        }
        restoreAA(g, restore);
    }

    /** Linear blend between two colours; {@code t} of 0 returns {@code a}. */
    public static Color mix(Color a, Color b, double t) {
        double k = Math.max(0, Math.min(1, t));
        return new Color(
                (int) Math.round(a.getRed() * (1 - k) + b.getRed() * k),
                (int) Math.round(a.getGreen() * (1 - k) + b.getGreen() * k),
                (int) Math.round(a.getBlue() * (1 - k) + b.getBlue() * k));
    }

    private static boolean withNoAA(Graphics2D g) {
        Object hint = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        boolean changed = hint != RenderingHints.VALUE_ANTIALIAS_OFF;
        if (changed) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        }
        return changed;
    }

    private static void restoreAA(Graphics2D g, boolean changed) {
        if (changed) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
    }
}

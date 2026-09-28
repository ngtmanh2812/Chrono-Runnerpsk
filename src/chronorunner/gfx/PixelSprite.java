package chronorunner.gfx;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * A small hand-authored bitmap drawn with nearest-neighbour scaling so it stays
 * crisp at any integer multiple. Pixels are stored as ARGB ints, 0 meaning
 * transparent.
 *
 * <p>Every sprite is expected to be a rectangle: {@link #of} rejects a grid
 * whose rows differ in length, because a ragged row silently shifts art and is
 * near impossible to spot by eye at 2-4x. The tinted variants (white hit flash,
 * slowed blue) are baked once on first use, so a recoloured frame costs no more
 * than a cached {@code drawImage}.
 */
public final class PixelSprite {

    /** How the sprite is recoloured for a frame. */
    public enum Mode { NORMAL, FLASH, SLOW }

    private static final Color FLASH_COLOR = Color.WHITE;
    private static final Color SLOW_COLOR = new Color(150, 225, 255);
    private static final double SLOW_AMOUNT = 0.45;

    private final int w;
    private final int h;
    private final int[] pixels;

    private BufferedImage base;
    private BufferedImage flash;
    private BufferedImage slow;

    private PixelSprite(int w, int h, int[] pixels) {
        this.w = w;
        this.h = h;
        this.pixels = pixels;
    }

    /**
     * Builds a sprite from rows of palette keys. Accepts a text block verbatim;
     * blank trailing rows are allowed but every row that carries art must be the
     * same length as the first.
     */
    public static PixelSprite of(Palette palette, String... rows) {
        if (rows.length == 0) {
            throw new IllegalArgumentException("sprite has no rows");
        }
        int w = rows[0].length();
        for (int y = 0; y < rows.length; y++) {
            if (rows[y].length() != w) {
                throw new IllegalArgumentException("ragged sprite row " + y + ": expected "
                        + w + " columns but got " + rows[y].length() + " (\"" + rows[y] + "\")");
            }
        }
        int h = rows.length;
        int[] px = new int[w * h];
        for (int y = 0; y < h; y++) {
            String row = rows[y];
            for (int x = 0; x < w; x++) {
                Color c = palette.colorOf(row.charAt(x));
                if (c != null) {
                    px[y * w + x] = c.getRGB();
                }
            }
        }
        return new PixelSprite(w, h, px);
    }

    public int width() {
        return w;
    }

    public int height() {
        return h;
    }

    public void draw(Graphics2D g, int left, int top, int scale, boolean flip) {
        draw(g, left, top, scale, flip, Mode.NORMAL, 1f);
    }

    /** Draws with the top-left corner at ({@code left}, {@code top}). */
    public void draw(Graphics2D g, int left, int top, int scale, boolean flip,
                     Mode mode, float alpha) {
        if (scale <= 0 || alpha <= 0f) return;
        BufferedImage img = image(mode);
        int dw = w * scale;
        int dh = h * scale;

        Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Object interp = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        Composite old = null;
        if (alpha < 1f) {
            old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        }

        if (flip) {
            g.drawImage(img, left + dw, top, left, top + dh, 0, 0, w, h, null);
        } else {
            g.drawImage(img, left, top, left + dw, top + dh, 0, 0, w, h, null);
        }

        if (old != null) g.setComposite(old);
        if (aa != null) g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
        if (interp != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interp);
    }

    /**
     * Draws with the sprite centred horizontally on {@code cx} and its feet on
     * {@code feetY}, which is how entities anchor to their collision box.
     */
    public void drawAnchored(Graphics2D g, int cx, int feetY, int scale, boolean flip,
                             Mode mode, float alpha) {
        int left = cx - (w * scale) / 2;
        int top = feetY - h * scale;
        draw(g, left, top, scale, flip, mode, alpha);
    }

    public void drawAnchored(Graphics2D g, int cx, int feetY, int scale, boolean flip) {
        drawAnchored(g, cx, feetY, scale, flip, Mode.NORMAL, 1f);
    }

    private BufferedImage image(Mode mode) {
        switch (mode) {
            case FLASH:
                if (flash == null) flash = build(FLASH_COLOR, null, 0);
                return flash;
            case SLOW:
                if (slow == null) slow = build(null, SLOW_COLOR, SLOW_AMOUNT);
                return slow;
            default:
                if (base == null) base = build(null, null, 0);
                return base;
        }
    }

    private BufferedImage build(Color solid, Color blend, double amount) {
        int[] out = new int[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            if (p == 0) continue;
            if (solid != null) {
                out[i] = (p & 0xFF000000) | (solid.getRGB() & 0x00FFFFFF);
            } else if (blend != null) {
                int r = (p >> 16) & 0xFF;
                int gg = (p >> 8) & 0xFF;
                int b = p & 0xFF;
                int nr = (int) (r * (1 - amount) + blend.getRed() * amount);
                int ng = (int) (gg * (1 - amount) + blend.getGreen() * amount);
                int nb = (int) (b * (1 - amount) + blend.getBlue() * amount);
                out[i] = (p & 0xFF000000) | (nr << 16) | (ng << 8) | nb;
            } else {
                out[i] = p;
            }
        }
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, w, h, out, 0, w);
        return img;
    }
}

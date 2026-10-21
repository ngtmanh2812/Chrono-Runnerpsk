package chronorunner.obstacle;

import java.awt.Color;
import java.awt.Graphics2D;

import chronorunner.gfx.PixelArt;

/** Non moving platform, drawn as stacked pixel tiles. */
public class StaticPlatform extends Platform {

    /** Tile pitch for the brick seams, in pixels. */
    private static final int TILE = 24;
    /** Height of the lit cap along the top edge. */
    private static final int CAP = 6;

    private final Color lit;
    private final Color body;
    private final Color dark;
    private final boolean visible;

    public StaticPlatform(double x, double y, double w, double h, Color color) {
        this(x, y, w, h, color, true);
    }

    /** Image arenas already contain their stone surfaces; only collide there. */
    public StaticPlatform(double x, double y, double w, double h, Color color, boolean visible) {
        super(x, y, w, h, color);
        this.visible = visible;
        this.lit = color;
        this.body = color.darker();
        this.dark = color.darker().darker();
    }

    @Override
    public void render(Graphics2D g) {
        if (!visible) return;
        int ix = (int) x;
        int iy = (int) y;
        int iw = (int) width;
        int ih = (int) height;
        if (iw <= 0 || ih <= 0) return;

        PixelArt.rect(g, ix, iy, iw, ih, body);

        int cap = Math.min(CAP, ih);
        PixelArt.rect(g, ix, iy, iw, cap, lit);
        PixelArt.rect(g, ix, iy, iw, 1, PixelArt.mix(lit, Color.WHITE, 0.35));

        // Offset every other course so the seams read as brickwork rather than
        // a grid. Staggering stops here: the surface is only a few tiles tall.
        int course = 0;
        for (int cy = iy + cap; cy < iy + ih; cy += TILE, course++) {
            int offset = course % 2 == 0 ? 0 : TILE / 2;
            PixelArt.rect(g, ix, cy, iw, 1, dark);
            for (int cx = ix + offset; cx < ix + iw; cx += TILE) {
                PixelArt.rect(g, cx, cy, 1, Math.min(TILE, iy + ih - cy), dark);
            }
        }

        PixelArt.rect(g, ix, iy + ih - 1, iw, 1, dark);
    }
}

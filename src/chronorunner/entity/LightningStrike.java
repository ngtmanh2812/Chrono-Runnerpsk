package chronorunner.entity;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import chronorunner.world.CombatWorld;

/** A visual bolt fixed to the point where the call struck. */
public final class LightningStrike extends Particle {
    private static final double DURATION = .24;
    private static final int[] RISE = {470, 272, 226, 174, 129, 84, 47, 0};
    private static final int[][] SHAPES = {
            {-49, 18, -37, 21, -25, 30, -12, 0},
            {38, -21, 27, -33, 12, -18, 20, 0},
            {-18, 32, -20, 37, -38, 13, -16, 0}
    };
    private double elapsed;

    public LightningStrike(double x, double y) {
        super(x, y, 0, 0, 1, Color.WHITE, DURATION, 0);
    }

    @Override public void update(double dt, CombatWorld world) {
        elapsed += dt;
        if (elapsed >= DURATION) alive = false;
    }

    @Override public void render(Graphics2D g) {
        if (!alive) return;
        int[] shape = SHAPES[(int) (elapsed / .055) % SHAPES.length];
        int[] xs = new int[RISE.length], ys = new int[RISE.length];
        for (int i = 0; i < RISE.length; i++) {
            xs[i] = (int) Math.round(x) + shape[i];
            ys[i] = (int) Math.round(y) - RISE[i];
        }
        double fade = Math.min(1, (DURATION - elapsed) / .08);
        Graphics2D bolt = (Graphics2D) g.create();
        bolt.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        stroke(bolt, new Color(255, 198, 48, (int) (60 * fade)), 18, xs, ys);
        stroke(bolt, new Color(255, 204, 66, (int) (230 * fade)), 8, xs, ys);
        Color core = new Color(255, 247, 200, (int) (255 * fade));
        stroke(bolt, core, 3, xs, ys);
        bolt.drawPolyline(new int[]{xs[3], xs[3] + 31, xs[3] + 54},
                new int[]{ys[3], ys[3] + 12, ys[3] + 37}, 3);
        bolt.drawPolyline(new int[]{xs[5], xs[5] - 37, xs[5] - 51},
                new int[]{ys[5], ys[5] + 20, ys[5] + 46}, 3);
        bolt.dispose();
    }

    private static void stroke(Graphics2D g, Color color, int width, int[] xs, int[] ys) {
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
        g.setColor(color);
        g.drawPolyline(xs, ys, xs.length);
    }
}

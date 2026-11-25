package chronorunner.gfx;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Ground prop and animated hose for Do Mixi's six smoking poses. */
public final class ShishaProp {
    public static final int MOUTH_X = 11;
    public static final int MOUTH_Y = -39;
    private static final int[] TIPS_X = {-4, 7, 8, 3, -2, -5};
    private static final int[] TIPS_Y = {-18, -32, -36, -22, -18, -17};
    private static final PixelSprite ART = load();

    private ShishaProp() {}

    private static PixelSprite load() {
        Path path = Path.of("assets/players/do_mixi/v5/shisha.png");
        try {
            var image = ImageIO.read(path.toFile());
            if (image == null || image.getWidth() != 96 || image.getHeight() != 96
                    || !image.getColorModel().hasAlpha()) {
                throw new IOException("Expected a transparent 96x96 PNG");
            }
            return PixelSprite.fromImage(image);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load Do Mixi's shisha sprite: " + path, e);
        }
    }

    public static void draw(Graphics2D g, double centerX, double feetY, int facing, int pose) {
        int index = Math.min(pose, TIPS_X.length - 1);
        int stand = (int) Math.round(centerX) - 41 * facing;
        int floor = (int) Math.round(feetY);
        ART.draw(g, stand - 48, floor - 86, 1, facing < 0);
        int outletX = stand + 4 * facing;
        int outletY = floor - 37;
        int tipX = (int) Math.round(centerX) + TIPS_X[index] * facing;
        int tipY = floor + TIPS_Y[index];
        Path2D path = new Path2D.Double();
        path.moveTo(outletX, outletY);
        path.curveTo(outletX + 15 * facing, outletY + 37, tipX + 7 * facing, tipY + 27, tipX, tipY);
        Graphics2D hose = (Graphics2D) g.create();
        hose.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        hose.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        hose.setColor(new Color(18, 21, 29));
        hose.draw(path);
        hose.setStroke(new BasicStroke(1));
        hose.setColor(new Color(54, 64, 80));
        hose.draw(path);
        hose.dispose();
    }
}

package chronorunner.world;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/** An image and the single uniform transform shared by its map geometry. */
public final class ArenaBackdrop {
    private final BufferedImage image;
    private final double scale;

    public ArenaBackdrop(String path, int worldHeight) {
        try {
            image = ImageIO.read(new File(path));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load arena background: " + path, e);
        }
        if (image == null) throw new IllegalArgumentException("Invalid arena PNG: " + path);
        scale = worldHeight / (double) image.getHeight();
    }

    public int sourceWidth() { return image.getWidth(); }
    public int sourceHeight() { return image.getHeight(); }
    public double toWorld(double sourcePixels) { return sourcePixels * scale; }
    public double width() { return toWorld(sourceWidth()); }
    public double height() { return toWorld(sourceHeight()); }

    /** Called inside the same camera transform as the platforms and actors. */
    public void render(Graphics2D target) {
        Graphics2D g = (Graphics2D) target.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(image, AffineTransform.getScaleInstance(scale, scale), null);
        g.dispose();
    }
}

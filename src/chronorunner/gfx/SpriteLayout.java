package chronorunner.gfx;

import java.awt.Graphics2D;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

/** Authored frame geometry. Padding and VFX never change an entity's hitbox. */
public final class SpriteLayout {
    public final int width, height, pivotX, pivotY, scale;
    public final int offsetY;
    public final boolean centerAnchor;
    private final Properties properties;

    private SpriteLayout(Properties p) {
        properties = p;
        width = integer(p, "frameWidth"); height = integer(p, "frameHeight");
        pivotX = integer(p, "pivotX"); pivotY = integer(p, "pivotY");
        scale = integer(p, "scale");
        offsetY = Integer.parseInt(p.getProperty("offsetY", "0"));
        String anchor = p.getProperty("anchor");
        if (width < 1 || height < 1 || scale < 1 || scale > 8
                || pivotX < 0 || pivotX > width || pivotY < 0 || pivotY > height
                || !("feet".equals(anchor) || "center".equals(anchor))) {
            throw new IllegalArgumentException("Invalid sprite geometry");
        }
        centerAnchor = "center".equals(anchor);
    }

    private static int integer(Properties p, String name) {
        return Integer.parseInt(p.getProperty(name));
    }

    public static SpriteLayout load(File directory) {
        File file = new File(directory, "animation.properties");
        if (!file.isFile()) return null; // Older optional sprite packs still work.
        Properties p = new Properties();
        try (var input = Files.newInputStream(file.toPath())) {
            p.load(input);
            return new SpriteLayout(p);
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalStateException("Cannot read sprite layout " + file, e);
        }
    }

    public double seconds(String state, double fallback) {
        double value = Double.parseDouble(properties.getProperty(state + ".secondsPerFrame", Double.toString(fallback)));
        if (!Double.isFinite(value) || value < .01) throw new IllegalStateException("Invalid clip timing: " + state);
        return value;
    }

    public boolean loops(String state, boolean fallback) {
        return Boolean.parseBoolean(properties.getProperty(state + ".loop", Boolean.toString(fallback)));
    }

    public void draw(PixelSprite sprite, Graphics2D g, int cx, int feetY, int centerY,
                     boolean flip, PixelSprite.Mode mode, float alpha) {
        int rootY = (centerAnchor ? centerY : feetY) + offsetY;
        // Mirroring moves the authored pivot with the image, including odd widths.
        int x = cx - (flip ? width - pivotX : pivotX) * scale;
        sprite.draw(g, x, rootY - pivotY * scale, scale, flip, mode, alpha);
    }
}

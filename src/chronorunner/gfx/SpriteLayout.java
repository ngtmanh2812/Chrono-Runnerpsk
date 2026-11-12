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

    /** Optional visual frame range, for example take-off through apex in V4.
     * This maps simulation progress to poses without changing jump physics.
     * Older packs retain the caller's original sampling behavior.
     */
    public double frameProgress(String state, double progress, int frames, double fallback) {
        String first = properties.getProperty(state + ".startFrame");
        String last = properties.getProperty(state + ".endFrame");
        if (first == null && last == null) return fallback;
        int start = Integer.parseInt(first == null ? "0" : first);
        int end = Integer.parseInt(last == null ? Integer.toString(frames - 1) : last);
        if (start < 0 || end < start || end >= frames || !Double.isFinite(progress)) {
            throw new IllegalStateException("Invalid visual frame range: " + state);
        }
        double pose = Math.min(end, start + (end - start + 1) * Math.max(0, Math.min(1, progress)));
        return pose / frames;
    }

    /** Map the three gameplay phases onto an authored pose breakdown.
     * A six-frame V4 strike has three anticipation poses, two contact poses and
     * one recovery pose. Older banks keep the original equal-third mapping.
     */
    public double actionProgress(String state, double progress, int frames) {
        String contact = properties.getProperty(state + ".activeFrame",
                properties.getProperty("combat.activeFrame"));
        String recovery = properties.getProperty(state + ".recoveryFrame",
                properties.getProperty("combat.recoveryFrame"));
        if (contact == null && recovery == null) return progress;
        int activeStart = Integer.parseInt(contact == null ? Integer.toString(frames / 3) : contact);
        int recoveryStart = Integer.parseInt(recovery == null ? Integer.toString(frames * 2 / 3) : recovery);
        if (activeStart < 1 || recoveryStart <= activeStart || recoveryStart >= frames
                || !Double.isFinite(progress)) {
            throw new IllegalStateException("Invalid combat pose breakdown: " + state);
        }
        double staged = Math.max(0, Math.min(1, progress)) * 3;
        int phase = Math.min(2, (int)staged);
        int start = phase == 0 ? 0 : phase == 1 ? activeStart : recoveryStart;
        int end = phase == 0 ? activeStart : phase == 1 ? recoveryStart : frames;
        return Math.min(end - 1, start + (staged - phase) * (end - start)) / frames;
    }

    public void draw(PixelSprite sprite, Graphics2D g, int cx, int feetY, int centerY,
                     boolean flip, PixelSprite.Mode mode, float alpha) {
        int rootY = (centerAnchor ? centerY : feetY) + offsetY;
        // Mirroring moves the authored pivot with the image, including odd widths.
        int x = cx - (flip ? width - pivotX : pivotX) * scale;
        sprite.draw(g, x, rootY - pivotY * scale, scale, flip, mode, alpha);
    }
}

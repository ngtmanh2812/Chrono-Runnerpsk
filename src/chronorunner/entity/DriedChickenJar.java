package chronorunner.entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import chronorunner.combat.DamageInfo;
import chronorunner.gfx.PixelArt;
import chronorunner.world.CombatWorld;

/** The oversized jar used by Do Mixi's U skill, from the lift to the throw. */
public final class DriedChickenJar extends Projectile {
    private static final Color TRAIL = new Color(255, 188, 83, 175);
    private static final double GRAVITY = 630;
    private static final BufferedImage SPRITE = load();
    public static final int RELEASE_ANGLE = 8;
    private final int direction;
    private double elapsed;

    public DriedChickenJar(double cx, double cy, int direction, DamageInfo damage) {
        super(cx, cy, 660 * direction, -135, 27, damage, true, TRAIL, 1.7);
        this.direction = direction;
        // A stable box inside the jar's rotating art keeps hits predictable.
        height = 82;
        y = cy - height / 2;
    }

    private static BufferedImage load() {
        Path path = Path.of("assets/players/do_mixi/v5/kho-ga-jar.png");
        try {
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null || image.getWidth() != 128 || image.getHeight() != 128
                    || !image.getColorModel().hasAlpha()) {
                throw new IOException("Expected a transparent 128x128 PNG");
            }
            return image;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load Do Mixi's jar sprite: " + path, e);
        }
    }

    @Override public void update(double dt, CombatWorld world) {
        elapsed += dt;
        vy += GRAVITY * dt;
        super.update(dt, world);
    }

    @Override public void render(Graphics2D g) {
        for (int i = 0; i < 4; i++) {
            int x = (int) centerX() - direction * (39 + i * 8);
            int y = (int) centerY() - 12 + i * 8;
            PixelArt.rect(g, x - 5, y, 10 + i * 3, 2, TRAIL);
        }
        draw(g, centerX(), centerY(), direction * (RELEASE_ANGLE + 650 * elapsed));
    }

    public static void draw(Graphics2D g, double cx, double cy, double angleDegrees) {
        Graphics2D draw = (Graphics2D) g.create();
        draw.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        draw.translate(Math.round(cx), Math.round(cy));
        draw.rotate(Math.toRadians(angleDegrees));
        draw.drawImage(SPRITE, -64, -64, null);
        draw.dispose();
    }
}

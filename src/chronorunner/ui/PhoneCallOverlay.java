package chronorunner.ui;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import chronorunner.core.Game;

/** The borderless call photo lives in screen coordinates. */
final class PhoneCallOverlay {
    private static final BufferedImage PHOTO = load();

    private PhoneCallOverlay() {}

    static void draw(Graphics2D g) {
        int width = 330;
        int height = width * PHOTO.getHeight() / PHOTO.getWidth();
        Graphics2D overlay = (Graphics2D) g.create();
        overlay.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        overlay.drawImage(PHOTO, Game.WIDTH / 4, 146, width, height, null);
        overlay.dispose();
    }

    private static BufferedImage load() {
        Path path = Path.of("assets/players/do_mixi/v5/phone-call.png");
        try {
            BufferedImage photo = ImageIO.read(path.toFile());
            if (photo == null) throw new IOException("Expected a PNG photograph");
            return photo;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load Do Mixi's call photo: " + path, e);
        }
    }
}

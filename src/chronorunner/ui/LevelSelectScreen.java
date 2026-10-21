package chronorunner.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import chronorunner.core.Game;
import chronorunner.core.GameState;
import chronorunner.core.InputHandler;
import chronorunner.core.Screen;

/** Two-chapter selection screen matching the supplied chapter-select artwork. */
public class LevelSelectScreen extends Screen {

    private static final String ART_PATH = "Chrono Runner_ Chapter Selection.png";
    private static final int ART_W = 1585;
    private static final int ART_H = 992;
    private static final int[][] CARD_BOUNDS = {{179, 294, 545, 500}, {760, 294, 562, 500}};
    private static final Color CYAN = new Color(21, 222, 255);
    private static final Color DIM_CYAN = new Color(30, 126, 172);

    private final BufferedImage artwork;
    private int selected;

    public LevelSelectScreen(Game game) {
        super(game);
        artwork = loadArtwork();
    }

    private BufferedImage loadArtwork() {
        File file = new File(ART_PATH);
        if (!file.isFile()) return null;
        try {
            return ImageIO.read(file);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read chapter selection artwork: " + file, e);
        }
    }

    @Override
    public void onEnter() {
        selected = Math.max(0, Math.min(1, game.getSession().chapter - 1));
    }

    @Override
    public void update(double dt) {
        InputHandler in = game.getInput();
        if (in.wasPressed(KeyEvent.VK_LEFT) || in.wasPressed(KeyEvent.VK_RIGHT)
                || in.wasPressed(KeyEvent.VK_UP) || in.wasPressed(KeyEvent.VK_DOWN)
                || in.wasPressed(KeyEvent.VK_A) || in.wasPressed(KeyEvent.VK_D)
                || in.wasPressed(KeyEvent.VK_W) || in.wasPressed(KeyEvent.VK_S)) {
            selected = 1 - selected;
        }
        if (in.wasPressed(KeyEvent.VK_ESCAPE)) {
            game.setState(GameState.MAIN_MENU);
            return;
        }
        if ((in.wasPressed(KeyEvent.VK_ENTER) || in.wasPressed(KeyEvent.VK_SPACE))
                && game.getProgress().isUnlocked(selected + 1)) {
            game.getSession().chapter = selected + 1;
            game.setState(GameState.PLAYING);
        }
    }

    @Override
    public void render(Graphics2D g) {
        int imageW = artwork == null ? ART_W : artwork.getWidth();
        int imageH = artwork == null ? ART_H : artwork.getHeight();
        double scale = Math.min(Game.WIDTH / (double) imageW, Game.HEIGHT / (double) imageH);
        int drawW = (int) Math.round(imageW * scale);
        int drawH = (int) Math.round(imageH * scale);
        int offsetX = (Game.WIDTH - drawW) / 2;
        int offsetY = (Game.HEIGHT - drawH) / 2;

        if (artwork == null) {
            UiKit.arcadeBackground(g);
        } else {
            Object old = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(artwork, offsetX, offsetY, drawW, drawH, null);
            if (old != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, old);
        }

        Graphics2D overlay = (Graphics2D) g.create();
        overlay.translate(offsetX, offsetY);
        overlay.scale(scale, scale);
        drawCardFocus(overlay, 0, selected == 0);
        drawCardFocus(overlay, 1, selected == 1);
        drawLiveStatus(overlay);
        overlay.dispose();

        if (selected == 1 && !game.getProgress().isUnlocked(2)) {
            UiKit.arcade(g, "CHƯƠNG TƯƠNG LAI SẼ MỞ SAU KHI HOÀN THÀNH QUÁ KHỨ",
                    UiKit.ARC_SMALL_SCALE, UiKit.ARC_DIM, Game.WIDTH / 2, Game.HEIGHT - 18, 2);
        }
    }

    private void drawCardFocus(Graphics2D g, int card, boolean focused) {
        int[] b = CARD_BOUNDS[card];
        int x = b[0], y = b[1], w = b[2], h = b[3];
        Path2D frame = new Path2D.Double();
        frame.moveTo(x + 28, y);
        frame.lineTo(x + w - 22, y);
        frame.lineTo(x + w, y + 28);
        frame.lineTo(x + w, y + h - 26);
        frame.lineTo(x + w - 26, y + h);
        frame.lineTo(x + 25, y + h);
        frame.lineTo(x, y + h - 25);
        frame.lineTo(x, y + 25);
        frame.closePath();

        // The reference artwork has the Past card pre-highlighted; repaint that
        // edge when focus moves so only the active card carries the bright glow.
        if (!focused && card == 0) {
            g.setColor(new Color(1, 14, 38, 235));
            g.setStroke(new BasicStroke(13f));
            g.draw(frame);
        }
        g.setColor(focused ? CYAN : DIM_CYAN);
        g.setStroke(new BasicStroke(focused ? 7f : 3f));
        g.draw(frame);
        g.setStroke(new BasicStroke(1f));
    }

    private void drawLiveStatus(Graphics2D g) {
        // Replace the baked-in progress/lock labels with values from this save.
        drawStatusPill(g, 575, 700, 120, 56,
                game.getProgress().isUnlocked(2) ? "ĐÃ XONG" : "SẴN SÀNG");
        if (game.getProgress().isUnlocked(2)) {
            drawStatusPill(g, 1107, 700, 184, 56, "MỞ KHÓA");
        }
    }

    private void drawStatusPill(Graphics2D g, int x, int y, int w, int h, String text) {
        g.setColor(new Color(1, 15, 39, 245));
        g.fillRoundRect(x, y, w, h, 15, 15);
        g.setColor(DIM_CYAN);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(x, y, w, h, 15, 15);
        UiKit.arcade(g, text, UiKit.ARC_SMALL_SCALE, CYAN, x + w / 2, y + h / 2 + 5, 2);
        g.setStroke(new BasicStroke(1f));
    }
}

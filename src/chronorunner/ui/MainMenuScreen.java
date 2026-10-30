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
import chronorunner.core.Screen;
import chronorunner.util.Progress;

/** Main menu styled from the supplied Chrono Runner title-screen reference. */
public class MainMenuScreen extends Screen {

    private static final String REFERENCE_PATH = "assets/ui/main_menu.png";
    private static final String[] OPTIONS = {
            "CHƠI", "TIẾP TỤC", "CÀI ĐẶT", "HƯỚNG DẪN CHƠI", "THOÁT"
    };

    // Menu layout in the reference image's 1585x992 coordinate space.
    private static final int REF_W = 1585;
    private static final int REF_H = 992;
    private static final int BUTTON_X = 138;
    private static final int BUTTON_Y = 394;
    private static final int BUTTON_W = 452;
    private static final int BUTTON_H = 75;
    private static final int BUTTON_PITCH = 94;

    private static final Color NEON = new Color(56, 218, 255);
    private static final Color NEON_DIM = new Color(28, 139, 196);
    private static final Color BUTTON_FILL = new Color(2, 17, 39);

    private final BufferedImage background;
    private int index;
    private int playerCount = 1;

    public MainMenuScreen(Game game) {
        super(game);
        background = loadBackground();
    }

    private BufferedImage loadBackground() {
        File file = new File(REFERENCE_PATH);
        if (!file.isFile()) return null;
        try {
            return ImageIO.read(file);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read main menu artwork: " + file, e);
        }
    }

    @Override
    public void onEnter() {
        playerCount = game.getSession().playerCount == 2 ? 2 : 1;
    }

    @Override
    public void update(double dt) {
        if (game.getInput().wasPressed(KeyEvent.VK_UP)
                || game.getInput().wasPressed(KeyEvent.VK_W)) {
            index = (index - 1 + OPTIONS.length) % OPTIONS.length;
        }
        if (game.getInput().wasPressed(KeyEvent.VK_DOWN)
                || game.getInput().wasPressed(KeyEvent.VK_S)) {
            index = (index + 1) % OPTIONS.length;
        }
        if (index == 0 && (game.getInput().wasPressed(KeyEvent.VK_LEFT)
                || game.getInput().wasPressed(KeyEvent.VK_RIGHT))) {
            playerCount = playerCount == 1 ? 2 : 1;
        }
        if (!game.getInput().wasPressed(KeyEvent.VK_ENTER)
                && !game.getInput().wasPressed(KeyEvent.VK_SPACE)) {
            if (game.getInput().wasPressed(KeyEvent.VK_ESCAPE)) game.quit();
            return;
        }

        switch (index) {
            case 0:
                startFreshRun();
                game.setState(GameState.CHARACTER_SELECT);
                break;
            case 1:
                game.getSession().playerCount = playerCount;
                game.getSession().chapter = game.getProgress().getUnlockedChapters();
                game.setState(GameState.LEVEL_SELECT);
                break;
            case 2:
                game.setState(GameState.SETTINGS);
                break;
            case 3:
                game.setState(GameState.HOW_TO_PLAY);
                break;
            default:
                game.quit();
                break;
        }
    }

    private void startFreshRun() {
        game.getSession().resetForNewRun();
        game.getSession().playerCount = playerCount;
    }

    @Override
    public void render(Graphics2D g) {
        int imageWidth = background == null ? REF_W : background.getWidth();
        int imageHeight = background == null ? REF_H : background.getHeight();
        double scale = Math.min(Game.WIDTH / (double) imageWidth,
                Game.HEIGHT / (double) imageHeight);
        int drawWidth = (int) Math.round(imageWidth * scale);
        int drawHeight = (int) Math.round(imageHeight * scale);
        int offsetX = (Game.WIDTH - drawWidth) / 2;
        int offsetY = (Game.HEIGHT - drawHeight) / 2;

        if (background == null) {
            UiKit.arcadeBackground(g);
        } else {
            Object oldInterpolation = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(background, offsetX, offsetY, drawWidth, drawHeight, null);
            if (oldInterpolation != null) {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterpolation);
            }
        }

        Graphics2D menu = (Graphics2D) g.create();
        menu.translate(offsetX, offsetY);
        menu.scale(scale, scale);
        // Hide all six labels baked into the reference before drawing five live rows.
        menu.setColor(BUTTON_FILL);
        menu.fillRect(BUTTON_X - 8, BUTTON_Y - 8, BUTTON_W + 16, 478);
        for (int i = 0; i < OPTIONS.length; i++) {
            drawButton(menu, i, i == index);
        }
        menu.dispose();
    }

    /** Covers the mockup's baked-in labels and paints live, keyboard-selectable rows. */
    private void drawButton(Graphics2D g, int option, boolean selected) {
        int x = BUTTON_X;
        int y = BUTTON_Y + option * BUTTON_PITCH;
        int w = BUTTON_W;
        int h = BUTTON_H;

        Path2D shape = new Path2D.Double();
        shape.moveTo(x + 18, y);
        shape.lineTo(x + w - 20, y);
        shape.lineTo(x + w, y + 18);
        shape.lineTo(x + w, y + h - 18);
        shape.lineTo(x + w - 20, y + h);
        shape.lineTo(x + 18, y + h);
        shape.lineTo(x, y + h - 18);
        shape.lineTo(x, y + 18);
        shape.closePath();

        g.setColor(selected ? new Color(2, 35, 66) : BUTTON_FILL);
        g.fill(shape);
        g.setColor(selected ? NEON : NEON_DIM);
        g.setStroke(new BasicStroke(selected ? 4f : 2f));
        g.draw(shape);
        g.setStroke(new BasicStroke(1f));

        drawIcon(g, option, x + 58, y + h / 2, selected ? NEON : NEON_DIM);
        int labelScale = option == 3 ? 2 : 3;
        UiKit.arcadeLeft(g, OPTIONS[option], labelScale, selected ? UiKit.CREAM : NEON_DIM,
                x + 116, y + 48, 2);
        if (option == 0) {
            UiKit.arcadeRight(g, "< " + playerCount + "P >", 2, NEON,
                    x + w - 76, y + 48, 2);
        }
        g.setColor(selected ? NEON : NEON_DIM);
        g.setStroke(new BasicStroke(selected ? 4f : 2f));
        g.drawLine(x + w - 48, y + h / 2 - 9, x + w - 38, y + h / 2);
        g.drawLine(x + w - 38, y + h / 2, x + w - 48, y + h / 2 + 9);
        g.setStroke(new BasicStroke(1f));
    }

    private void drawIcon(Graphics2D g, int option, int cx, int cy, Color color) {
        g.setColor(color);
        g.setStroke(new BasicStroke(4f));
        switch (option) {
            case 0: {
                Path2D triangle = new Path2D.Double();
                triangle.moveTo(cx - 10, cy - 16);
                triangle.lineTo(cx + 15, cy);
                triangle.lineTo(cx - 10, cy + 16);
                triangle.closePath();
                g.fill(triangle);
                break;
            }
            case 1:
                g.drawOval(cx - 15, cy - 15, 30, 30);
                g.drawLine(cx, cy, cx, cy - 9);
                g.drawLine(cx, cy, cx + 8, cy + 5);
                break;
            case 2:
                g.drawOval(cx - 11, cy - 11, 22, 22);
                g.fillOval(cx - 4, cy - 4, 8, 8);
                for (int a = 0; a < 8; a++) {
                    double angle = Math.PI * a / 4.0;
                    int x1 = cx + (int) (13 * Math.cos(angle));
                    int y1 = cy + (int) (13 * Math.sin(angle));
                    int x2 = cx + (int) (19 * Math.cos(angle));
                    int y2 = cy + (int) (19 * Math.sin(angle));
                    g.drawLine(x1, y1, x2, y2);
                }
                break;
            case 3:
                g.drawRect(cx - 19, cy - 16, 38, 32);
                g.drawLine(cx, cy - 16, cx, cy + 16);
                g.drawLine(cx - 13, cy - 7, cx - 5, cy - 7);
                g.drawLine(cx + 5, cy - 7, cx + 13, cy - 7);
                g.drawLine(cx - 13, cy + 3, cx - 5, cy + 3);
                g.drawLine(cx + 5, cy + 3, cx + 13, cy + 3);
                break;
            default:
                g.drawRect(cx - 14, cy - 20, 20, 40);
                g.drawLine(cx - 25, cy, cx + 8, cy);
                g.drawLine(cx, cy - 8, cx + 8, cy);
                g.drawLine(cx, cy + 8, cx + 8, cy);
                break;
        }
        g.setStroke(new BasicStroke(1f));
    }
}

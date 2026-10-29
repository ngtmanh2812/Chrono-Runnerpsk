package chronorunner.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

import chronorunner.core.Game;
import chronorunner.core.GameState;
import chronorunner.core.InputHandler;
import chronorunner.core.Screen;
import chronorunner.gfx.PixelFont;

/** Shared pixel menus over an immutable snapshot of the actual arena. */
abstract class GameplayOverlayScreen extends Screen {
    protected enum Theme {
        PAUSE("PAUSED", 0x92dff4, 0x173a4a, 0x92dff4, 0x072b38, 0xeffaff, 0xc6d4dd),
        DEFEAT("GAME OVER", 0xee6673, 0x54131e, 0xae2d41, 0xfff4f6, 0xfff0f3, 0xd4bcc2),
        VICTORY("VICTORY", 0x7ce49e, 0x164328, 0x7ce49e, 0x062a14, 0xecfff2, 0xc4dbcd);

        final String title;
        final Color accent, shadow, fill, ink, text, muted;

        Theme(String title, int accent, int shadow, int fill, int ink, int text, int muted) {
            this.title = title;
            this.accent = new Color(accent);
            this.shadow = new Color(shadow);
            this.fill = new Color(fill);
            this.ink = new Color(ink);
            this.text = new Color(text);
            this.muted = new Color(muted);
        }
    }

    private static final int BUTTON_WIDTH = 470;
    private static final int BUTTON_HEIGHT = 70;
    private static final int BUTTON_GAP = 15;
    private final Theme theme;
    protected Menu menu;
    private BufferedImage gameplayFrame;
    private boolean mouseConfirmed;

    protected GameplayOverlayScreen(Game game, Theme theme, String... choices) {
        super(game);
        this.theme = theme;
        menu = new Menu(choices);
    }

    @Override
    public void onEnter() {
        menu.setIndex(0);
        mouseConfirmed = false;
        Screen play = game.screen(GameState.PLAYING);
        gameplayFrame = play instanceof PlayScreen ? ((PlayScreen) play).captureGameplayFrame() : null;
    }

    @Override
    public void onExit() { gameplayFrame = null; }

    /** Hover only follows actual motion, so a stationary mouse cannot undo a key selection. */
    protected final void updateMenu() {
        InputHandler input = game.getInput();
        if (input.wasPointerMoved()) {
            int row = rowAt(input.pointerX(), input.pointerY());
            if (row >= 0) menu.setIndex(row);
        }
        menu.update(input);
        mouseConfirmed = false;
        if (input.wasPrimaryClicked()) {
            int row = rowAt(input.clickX(), input.clickY());
            if (row >= 0) {
                menu.setIndex(row);
                mouseConfirmed = true;
            }
        }
    }

    protected final boolean menuConfirmed() { return menu.confirmed() || mouseConfirmed; }

    protected abstract String description();
    protected String subtitle() { return ""; }
    protected abstract String footnote();

    private int menuTop() {
        return theme == Theme.PAUSE ? 300 : menu.size() == 1 ? 410 : 376;
    }

    private Rectangle buttonBounds(int row) {
        return new Rectangle((Game.WIDTH - BUTTON_WIDTH) / 2,
                menuTop() + row * (BUTTON_HEIGHT + BUTTON_GAP), BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    private int rowAt(int x, int y) {
        for (int row = 0; row < menu.size(); row++) {
            if (buttonBounds(row).contains(x, y)) return row;
        }
        return -1;
    }

    @Override
    public final void render(Graphics2D target) {
        Graphics2D g = (Graphics2D) target.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setColor(new Color(8, 9, 11));
        g.fillRect(0, 0, Game.WIDTH, Game.HEIGHT);
        if (gameplayFrame != null) g.drawImage(gameplayFrame, 0, 0, null);
        // Neutral dimming only. Status colours belong to the text and buttons.
        g.setColor(new Color(0, 0, 0, 153));
        g.fillRect(0, 0, Game.WIDTH, Game.HEIGHT);

        int cx = Game.WIDTH / 2;
        boolean pause = theme == Theme.PAUSE;
        if (!pause) {
            String eyebrow = theme == Theme.DEFEAT
                    ? "DÒNG THỜI GIAN ĐÃ ĐỨT GÃY" : "DÒNG THỜI GIAN ĐÃ ỔN ĐỊNH";
            centered(g, eyebrow, 2, theme.accent, 122, 0);
        }
        int scale = theme == Theme.DEFEAT ? 11 : 12;
        int tracking = theme == Theme.DEFEAT ? 9 : 11;
        int baseline = pause ? 170 : 214;
        int left = cx - PixelFont.width(theme.title, scale, tracking) / 2;
        PixelFont.drawBaseline(g, theme.title, left + 6, baseline + 6, scale, theme.shadow, tracking);
        PixelFont.drawBaseline(g, theme.title, left, baseline, scale, theme.accent, tracking);

        int ruleY = pause ? 214 : 254;
        g.setColor(theme.shadow);
        g.fillRect(cx - 88, ruleY, 76, 2);
        g.fillRect(cx + 12, ruleY, 76, 2);
        g.setColor(theme.accent);
        g.fillRect(cx - 2, ruleY - 1, 4, 4);
        centered(g, description(), 3, theme.text, pause ? 264 : 302, 0);
        if (!subtitle().isEmpty()) centered(g, subtitle(), 3, theme.muted, 344, 0);
        for (int row = 0; row < menu.size(); row++) drawButton(g, row);
        centered(g, footnote(), 2, theme == Theme.VICTORY ? theme.accent : theme.muted, 592, 0);
        drawFooter(g);
        g.dispose();
    }

    private void centered(Graphics2D g, String text, int scale, Color color, int baseline, int tracking) {
        UiKit.pixelCentered(g, text, scale, color, Game.WIDTH / 2, baseline, tracking);
    }

    private void drawButton(Graphics2D target, int row) {
        Rectangle b = buttonBounds(row);
        boolean primary = row == 0;
        boolean selected = row == menu.index();
        Path2D shape = new Path2D.Double();
        shape.moveTo(b.x + 8, b.y);
        shape.lineTo(b.x + b.width - 8, b.y);
        shape.lineTo(b.x + b.width - 8, b.y + 4);
        shape.lineTo(b.x + b.width, b.y + 4);
        shape.lineTo(b.x + b.width, b.y + b.height - 4);
        shape.lineTo(b.x + b.width - 8, b.y + b.height - 4);
        shape.lineTo(b.x + b.width - 8, b.y + b.height);
        shape.lineTo(b.x + 8, b.y + b.height);
        shape.lineTo(b.x + 8, b.y + b.height - 4);
        shape.lineTo(b.x, b.y + b.height - 4);
        shape.lineTo(b.x, b.y + 4);
        shape.lineTo(b.x + 8, b.y + 4);
        shape.closePath();
        Graphics2D g = (Graphics2D) target.create();
        g.clip(shape);
        g.setColor(primary ? theme.fill : new Color(8, 9, 11, 217));
        g.fillRect(b.x, b.y, b.width, b.height);
        g.setColor(primary ? new Color(0, 0, 0, 64)
                : new Color(theme.accent.getRed(), theme.accent.getGreen(), theme.accent.getBlue(), selected ? 180 : 80));
        g.fillRect(b.x, b.y + b.height - 4, b.width, primary ? 4 : 2);
        if (!primary) g.fillRect(b.x, b.y, b.width, 2);

        Color ink = primary ? theme.ink : selected ? theme.accent : theme.text;
        int cy = b.y + b.height / 2;
        int scale = primary ? 4 : 3;
        String label = menu.item(row);
        while (PixelFont.width(label, scale, 0) > b.width - 160 && scale > 2) scale--;
        UiKit.pixelCentered(g, label, scale, ink, Game.WIDTH / 2, cy + 4 * scale, 0);
        if (selected) {
            UiKit.arcadeArrow(g, b.x + 22, cy, 3, ink);
            if (PixelFont.width(label, scale, 0) <= b.width - 190) {
                UiKit.pixelRight(g, "ENTER", 2, ink, b.x + b.width - 18, cy + 5, 0);
            }
        }
        g.dispose();
    }

    private void drawFooter(Graphics2D g) {
        g.setColor(new Color(theme.accent.getRed(), theme.accent.getGreen(), theme.accent.getBlue(), 70));
        g.fillRect(60, 644, Game.WIDTH - 120, 1);
        String[] labels = {"W / S  CHỌN", "ENTER  XÁC NHẬN", theme == Theme.PAUSE ? "ESC  TIẾP TỤC" : "ESC  MENU"};
        int total = 96;
        for (String label : labels) total += PixelFont.width(label, 2, 0);
        int x = (Game.WIDTH - total) / 2;
        for (String label : labels) {
            UiKit.pixelLeft(g, label, 2, theme.muted, x, 680, 0);
            x += PixelFont.width(label, 2, 0) + 48;
        }
    }
}

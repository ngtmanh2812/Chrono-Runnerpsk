package chronorunner.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

import chronorunner.core.Game;
import chronorunner.core.GameSession;
import chronorunner.core.GameState;
import chronorunner.core.InputHandler;
import chronorunner.core.Screen;
import chronorunner.entity.character.CharacterFactory;
import chronorunner.gfx.Art;
import chronorunner.gfx.PixelArt;
import chronorunner.gfx.PixelSprite;

/**
 * Per-player character picker. P1 uses A/D + J, P2 uses arrow keys + Numpad 1.
 * Press the dash key to un-confirm and change.
 */
public class CharacterSelectScreen extends Screen {

    private static final int CARD_W = 320;
    private static final int CARD_H = 340;
    private static final int CARD_Y = 200;
    private static final int GAP = 40;

    private final int[] cursor = {0, 0};
    private final boolean[] locked = {false, false};

    public CharacterSelectScreen(Game game) {
        super(game);
    }

    @Override
    public void onEnter() {
        GameSession session = game.getSession();
        cursor[0] = session.characterIndex[0];
        cursor[1] = session.characterIndex[1];
        locked[0] = false;
        locked[1] = false;
    }

    private void cycle(int player, int delta) {
        cursor[player] = (cursor[player] + delta + CharacterFactory.NAMES.length)
                % CharacterFactory.NAMES.length;
    }

    @Override
    public void update(double dt) {
        InputHandler in = game.getInput();
        GameSession session = game.getSession();

        if (in.wasPressed(KeyEvent.VK_ESCAPE)) {
            game.setState(GameState.STORY);
            return;
        }

        if (!locked[0]) {
            if (in.wasPressed(KeyEvent.VK_A)) cycle(0, -1);
            if (in.wasPressed(KeyEvent.VK_D)) cycle(0, 1);
            if (in.wasPressed(KeyEvent.VK_J)) locked[0] = true;
        } else if (in.wasPressed(KeyEvent.VK_K)) {
            locked[0] = false;
        }

        if (session.playerCount == 2) {
            if (!locked[1]) {
                if (in.wasPressed(KeyEvent.VK_LEFT)) cycle(1, -1);
                if (in.wasPressed(KeyEvent.VK_RIGHT)) cycle(1, 1);
                if (in.wasPressed(KeyEvent.VK_NUMPAD1)) locked[1] = true;
            } else if (in.wasPressed(KeyEvent.VK_NUMPAD2)) {
                locked[1] = false;
            }
        } else {
            locked[1] = false;
        }

        boolean ready = locked[0] && (session.playerCount == 1 || locked[1]);
        if (ready && in.wasPressed(KeyEvent.VK_ENTER)) {
            session.characterIndex[0] = cursor[0];
            session.characterIndex[1] = cursor[1];
            game.setState(GameState.PLAYING);
        }
    }

    private int cardX(int index) {
        int total = CharacterFactory.NAMES.length * CARD_W + (CharacterFactory.NAMES.length - 1) * GAP;
        int start = (Game.WIDTH - total) / 2;
        return start + index * (CARD_W + GAP);
    }

    @Override
    public void render(Graphics2D g) {
        UiKit.background(g, UiKit.BG_TOP, UiKit.BG_BOTTOM);
        UiKit.centered(g, "CHỌN NHÂN VẬT", UiKit.H1, UiKit.ACCENT, Game.WIDTH / 2, 100);

        GameSession session = game.getSession();
        for (int i = 0; i < CharacterFactory.NAMES.length; i++) {
            drawCard(g, i);
        }

        for (int p = 0; p < session.playerCount; p++) {
            int x = cardX(cursor[p]);
            Color c = p == 0 ? UiKit.ACCENT : UiKit.ACCENT_WARM;
            g.setColor(c);
            g.setStroke(new BasicStroke(4));
            int inset = p == 0 ? 0 : 8;
            g.drawRoundRect(x - inset, CARD_Y - inset, CARD_W + inset * 2, CARD_H + inset * 2, 20, 20);
            g.setStroke(new BasicStroke(1));

            String tag = "P" + (p + 1) + (locked[p] ? " ✓" : "");
            g.setFont(UiKit.H2);
            g.setColor(c);
            g.drawString(tag, x + CARD_W - 74, CARD_Y - 14);
        }

        boolean ready = locked[0] && (session.playerCount == 1 || locked[1]);
        if (ready) {
            UiKit.centered(g, "NHẤN ENTER ĐỂ BẮT ĐẦU", UiKit.H2, UiKit.OK, Game.WIDTH / 2, 596);
        } else {
            UiKit.centered(g, "Mỗi người chơi chọn một nhân vật rồi xác nhận.",
                    UiKit.BODY, UiKit.MUTED, Game.WIDTH / 2, 596);
        }

        UiKit.centered(g, "P1: A / D chọn  •  J xác nhận  •  K chọn lại",
                UiKit.SMALL, UiKit.MUTED, Game.WIDTH / 2, 646);
        if (session.playerCount == 2) {
            UiKit.centered(g, "P2: ← / → chọn  •  Numpad 1 xác nhận  •  Numpad 2 chọn lại",
                    UiKit.SMALL, UiKit.MUTED, Game.WIDTH / 2, 670);
        }
        UiKit.centered(g, "ESC quay lại", UiKit.SMALL, UiKit.MUTED, Game.WIDTH / 2, 700);
    }

    private void drawCard(Graphics2D g, int index) {
        int x = cardX(index);
        Color body = CharacterFactory.colorOf(index);
        UiKit.panel(g, x, CARD_Y, CARD_W, CARD_H, new Color(14, 18, 36, 215), new Color(255, 255, 255, 40));

        g.setFont(UiKit.H2);
        g.setColor(UiKit.TEXT);
        g.drawString(CharacterFactory.nameOf(index), x + 18, CARD_Y + 38);

        g.setFont(UiKit.SMALL);
        g.setColor(UiKit.MUTED);
        g.drawString(CharacterFactory.skinOf(index), x + 18, CARD_Y + 62);

        int figureW = 78;
        int figureH = 104;
        int fx = x + (CARD_W - figureW) / 2;
        int fy = CARD_Y + 80;
        int figureCx = fx + figureW / 2;
        int figureFeet = fy + figureH - 8;
        PixelArt.shadow(g, figureCx, figureFeet, figureW - 12, new Color(0, 0, 0, 110));
        PixelArt.rect(g, fx + 6, figureFeet + 2, figureW - 12, 7, body.darker());
        PixelArt.rect(g, fx + 6, figureFeet + 2, figureW - 12, 2, body);
        Art.player(CharacterFactory.idOf(index)).frame(0)
                .drawAnchored(g, figureCx, figureFeet, 3, false, PixelSprite.Mode.NORMAL, 1f);

        int[] stats = CharacterFactory.statsOf(index);
        statRow(g, x + 18, CARD_Y + 218, "HP", stats[0], new Color(90, 220, 130));
        statRow(g, x + 18, CARD_Y + 244, "Damage", stats[1], new Color(240, 130, 90));
        statRow(g, x + 18, CARD_Y + 270, "Tốc độ", stats[2], new Color(100, 190, 255));

        g.setFont(UiKit.SMALL);
        g.setColor(UiKit.ACCENT_WARM);
        g.drawString("Skill: " + CharacterFactory.skillOf(index), x + 18, CARD_Y + 312);
    }

    private void statRow(Graphics2D g, int x, int y, String label, int value, Color color) {
        g.setFont(UiKit.SMALL);
        g.setColor(UiKit.MUTED);
        g.drawString(label, x, y);
        UiKit.bar(g, x + 80, y - 11, 180, 12, value / 10.0, color, new Color(0, 0, 0, 150));
    }
}

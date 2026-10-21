package chronorunner.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.List;

import chronorunner.core.Game;
import chronorunner.core.GameSession;
import chronorunner.core.GameState;
import chronorunner.core.InputHandler;
import chronorunner.core.Screen;
import chronorunner.entity.character.CharacterFactory;
import chronorunner.gfx.Art;
import chronorunner.gfx.PixelArt;
import chronorunner.gfx.PixelSprite;
import chronorunner.util.Progress;

/**
 * Per-player character picker. P1 uses A/D + J, P2 uses arrow keys + Numpad 1.
 * Press the dash key to un-confirm and change.
 */
public class CharacterSelectScreen extends Screen {

    private static final int CARD_W = 320;
    private static final int CARD_H = 352;
    private static final int CARD_Y = 222;
    private static final int GAP = 40;

    /** Inner gutter of a card, and the player sprite drawn at twice its size. */
    private static final int CARD_PAD = 18;
    private static final int FIGURE_SCALE = 2;
    private static final int FIGURE_W = 48 * FIGURE_SCALE;
    private static final int FIGURE_H = 48 * FIGURE_SCALE;

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
            game.setState(GameState.MAIN_MENU);
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
            game.setState(GameState.LEVEL_SELECT);
        }
    }

    private int cardX(int index) {
        int total = CharacterFactory.NAMES.length * CARD_W + (CharacterFactory.NAMES.length - 1) * GAP;
        int start = (Game.WIDTH - total) / 2;
        return start + index * (CARD_W + GAP);
    }

    @Override
    public void render(Graphics2D g) {
        UiKit.arcadeBackground(g);
        UiKit.arcadeHeader(g, "CHAPTER ĐÃ MỞ KHOÁ",
                game.getProgress().getUnlockedChapters() + " / " + Progress.TOTAL_CHAPTERS);
        UiKit.arcadeRoster(g, ROSTER);

        UiKit.arcadeGold(g, "CHỌN NHÂN VẬT", UiKit.ARC_H1_SCALE,
                Game.WIDTH / 2, 194, UiKit.TITLE_TRACKING);

        GameSession session = game.getSession();
        for (int i = 0; i < CharacterFactory.NAMES.length; i++) {
            boolean selected = cursor[0] == i
                    || (session.playerCount == 2 && cursor[1] == i);
            drawCard(g, i, selected);
        }

        for (int p = 0; p < session.playerCount; p++) {
            int x = cardX(cursor[p]);
            int inset = p == 0 ? 0 : 8;
            Color ring = p == 0 ? UiKit.GOLD_TOP : UiKit.CREAM;
            UiKit.arcadeRing(g, x - inset, CARD_Y - inset,
                    CARD_W + inset * 2, CARD_H + inset * 2, ring);
            UiKit.arcadeRight(g, "P" + (p + 1) + (locked[p] ? " OK" : ""),
                    UiKit.ARC_SMALL_SCALE, ring, x + CARD_W - 18, CARD_Y + 34, 2);
        }

        boolean ready = locked[0] && (session.playerCount == 1 || locked[1]);
        if (ready) {
            UiKit.arcadeGold(g, "NHẤN ENTER ĐỂ CHỌN CHƯƠNG", UiKit.ARC_H2_SCALE,
                    Game.WIDTH / 2, 620, 4);
        } else {
            UiKit.arcade(g, "Mỗi người chơi chọn một nhân vật rồi xác nhận.",
                    UiKit.ARC_BODY_SCALE, UiKit.ARC_DIM, Game.WIDTH / 2, 620, 2);
        }

        UiKit.arcade(g, "P1: A / D chọn  •  J xác nhận  •  K chọn lại",
                UiKit.ARC_SMALL_SCALE, UiKit.ARC_DIM, Game.WIDTH / 2, 656, 2);
        if (session.playerCount == 2) {
            UiKit.arcade(g, "P2: ← / → chọn  •  Numpad 1 xác nhận  •  Numpad 2 chọn lại",
                    UiKit.ARC_SMALL_SCALE, UiKit.ARC_DIM, Game.WIDTH / 2, 682, 2);
        }
        UiKit.arcade(g, "ESC quay lại", UiKit.ARC_SMALL_SCALE, UiKit.ARC_DIM,
                Game.WIDTH / 2, session.playerCount == 2 ? 708 : 682, 2);
    }

    private void drawCard(Graphics2D g, int index, boolean selected) {
        int x = cardX(index);
        Color body = CharacterFactory.colorOf(index);
        UiKit.arcadeCard(g, x, CARD_Y, CARD_W, CARD_H, selected, null);

        int textLeft = x + CARD_PAD;
        int textW = CARD_W - CARD_PAD * 2;

        UiKit.arcadeLeft(g, CharacterFactory.nameOf(index), UiKit.ARC_BODY_SCALE,
                UiKit.CREAM, textLeft, CARD_Y + 34, 2);

        // The blurb and the skill name are the long lines, so they wrap to at
        // most two rows each and never spill past the card edge.
        List<String> skin = UiKit.wrap(g, CharacterFactory.skinOf(index), textW, UiKit.SMALL);
        for (int i = 0; i < skin.size() && i < 2; i++) {
            UiKit.arcadeLeft(g, skin.get(i), UiKit.ARC_SMALL_SCALE,
                    UiKit.ARC_DIM, textLeft, CARD_Y + 58 + i * 20, 2);
        }

        int fx = x + (CARD_W - FIGURE_W) / 2;
        int figureCx = fx + FIGURE_W / 2;
        int figureFeet = CARD_Y + 214;
        PixelArt.shadow(g, figureCx, figureFeet, FIGURE_W - 12, new Color(0, 0, 0, 110));
        PixelArt.rect(g, fx + 6, figureFeet + 2, FIGURE_W - 12, 7, body.darker());
        PixelArt.rect(g, fx + 6, figureFeet + 2, FIGURE_W - 12, 2, body);
        Art.player(CharacterFactory.idOf(index)).frame(0)
                .drawAnchored(g, figureCx, figureFeet, FIGURE_SCALE, false,
                        PixelSprite.Mode.NORMAL, 1f);

        int[] stats = CharacterFactory.statsOf(index);
        statRow(g, textLeft, CARD_Y + 238, "HP", stats[0]);
        statRow(g, textLeft, CARD_Y + 258, "Damage", stats[1]);
        statRow(g, textLeft, CARD_Y + 278, "Tốc độ", stats[2]);

        List<String> skill = UiKit.wrap(g, "Skill: " + CharacterFactory.skillOf(index),
                textW, UiKit.SMALL);
        for (int i = 0; i < skill.size() && i < 2; i++) {
            UiKit.arcadeLeft(g, skill.get(i), UiKit.ARC_SMALL_SCALE,
                    UiKit.GOLD_PALE, textLeft, CARD_Y + 306 + i * 20, 2);
        }
    }

    private void statRow(Graphics2D g, int x, int y, String label, int value) {
        UiKit.arcadeLeft(g, label, UiKit.ARC_SMALL_SCALE, UiKit.CREAM, x, y, 2);
        UiKit.bar(g, x + 92, y - 11, 180, 12, value / 10.0,
                UiKit.GOLD_TOP, new Color(0, 0, 0, 150));
    }

    /** Sprite ids of the playable roster, shown in the top-right strip. */
    private static final String[] ROSTER = {
            CharacterFactory.idOf(0), CharacterFactory.idOf(1), CharacterFactory.idOf(2),
    };
}

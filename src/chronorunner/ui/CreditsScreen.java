package chronorunner.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

import chronorunner.core.Game;
import chronorunner.core.GameState;
import chronorunner.core.Screen;
import chronorunner.entity.character.CharacterFactory;
import chronorunner.gfx.Art;
import chronorunner.gfx.PixelArt;
import chronorunner.gfx.PixelSprite;

/** Team page listing the playable characters shown in the title-screen reference. */
public class CreditsScreen extends Screen {

    private static final int CARD_W = 300;
    private static final int CARD_H = 330;
    private static final int CARD_Y = 250;
    private static final int GAP = 34;

    public CreditsScreen(Game game) {
        super(game);
    }

    @Override
    public void update(double dt) {
        if (game.getInput().wasPressed(KeyEvent.VK_ESCAPE)
                || game.getInput().wasPressed(KeyEvent.VK_ENTER)) {
            game.setState(GameState.MAIN_MENU);
        }
    }

    @Override
    public void render(Graphics2D g) {
        UiKit.arcadeBackground(g);
        UiKit.arcadeGold(g, "ĐỘI NGŨ", UiKit.ARC_H1_SCALE,
                Game.WIDTH / 2, 190, UiKit.TITLE_TRACKING);
        UiKit.arcade(g, "ĐỘI HÌNH CHIẾN ĐẤU CHRONO RUNNER",
                UiKit.ARC_BODY_SCALE, UiKit.ARC_DIM, Game.WIDTH / 2, 225, 2);

        int totalW = CharacterFactory.NAMES.length * CARD_W
                + (CharacterFactory.NAMES.length - 1) * GAP;
        int startX = (Game.WIDTH - totalW) / 2;
        for (int i = 0; i < CharacterFactory.NAMES.length; i++) {
            drawCharacter(g, i, startX + i * (CARD_W + GAP));
        }

        UiKit.arcade(g, "ESC / ENTER quay lại",
                UiKit.ARC_SMALL_SCALE, UiKit.ARC_DIM, Game.WIDTH / 2, 680, 2);
    }

    private void drawCharacter(Graphics2D g, int index, int x) {
        UiKit.arcadeCard(g, x, CARD_Y, CARD_W, CARD_H, false, null);
        int cx = x + CARD_W / 2;
        int feet = CARD_Y + 170;
        PixelArt.shadow(g, cx, feet + 2, 72, new Color(0, 0, 0, 130));
        Art.player(CharacterFactory.idOf(index)).frame(0)
                .drawAnchored(g, cx, feet, 2, false, PixelSprite.Mode.NORMAL, 1f);
        UiKit.arcade(g, CharacterFactory.nameOf(index), UiKit.ARC_BODY_SCALE,
                UiKit.CREAM, cx, CARD_Y + 220, 2);
        UiKit.arcade(g, CharacterFactory.skillOf(index), UiKit.ARC_SMALL_SCALE,
                UiKit.GOLD_PALE, cx, CARD_Y + 266, 2);
        int[] stats = CharacterFactory.statsOf(index);
        UiKit.arcade(g, "HP " + stats[0] + "   DMG " + stats[1]
                        + "   TỐC ĐỘ " + stats[2],
                UiKit.ARC_SMALL_SCALE, UiKit.ARC_DIM, cx, CARD_Y + 302, 2);
    }
}

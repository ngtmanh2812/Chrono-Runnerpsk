package chronorunner.ui;

import java.awt.Color;
import java.awt.Graphics2D;

import chronorunner.core.Game;
import chronorunner.core.GameState;
import chronorunner.core.Screen;
import chronorunner.world.TileType;

/** Shown after the boss is defeated. Offers the next chapter. */
public class VictoryScreen extends Screen {

    private Menu menu = new Menu("VỀ MENU CHÍNH");
    private int chapter = 1;

    public VictoryScreen(Game game) {
        super(game);
    }

    @Override
    public void onEnter() {
        chapter = game.getSession().chapter;
        if (chapter >= 5) {
            menu = new Menu("VỀ MENU CHÍNH");
        } else {
            menu = new Menu("CHAPTER TIẾP THEO", "VỀ MENU CHÍNH");
        }
    }

    @Override
    public void update(double dt) {
        menu.update(game.getInput());
        if (menu.cancelled()) {
            game.setState(GameState.MAIN_MENU);
            return;
        }
        if (!menu.confirmed()) return;
        if (chapter < 5 && menu.index() == 0) {
            game.getSession().chapter = chapter + 1;
            game.setState(GameState.STORY);
        } else {
            game.setState(GameState.MAIN_MENU);
        }
    }

    @Override
    public void render(Graphics2D g) {
        TileType tile = TileType.of(chapter);
        UiKit.background(g, tile.bgTop, tile.bgBottom);

        UiKit.centered(g, "VICTORY", UiKit.TITLE, UiKit.OK, Game.WIDTH / 2, 190);
        UiKit.centered(g, "Chapter " + chapter + " — " + tile.placeName + " đã được giải phóng",
                UiKit.H2, UiKit.TEXT, Game.WIDTH / 2, 240);
        UiKit.centered(g, tile.bossName + " đã bị đánh bại.", UiKit.BODY, UiKit.MUTED,
                Game.WIDTH / 2, 280);

        if (chapter >= 5) {
            UiKit.centered(g, "Chrono Core đã được khôi phục. Phan Sĩ Kiên trở về dòng thời gian của mình.",
                    UiKit.H2, UiKit.ACCENT_WARM, Game.WIDTH / 2, 360);
            UiKit.centered(g, "Cảm ơn bạn đã chơi Chrono Runner!", UiKit.BODY, UiKit.TEXT,
                    Game.WIDTH / 2, 400);
        } else {
            UiKit.centered(g, "Chapter mới đã được mở khoá.", UiKit.BODY, UiKit.MUTED,
                    Game.WIDTH / 2, 350);
        }

        menu.render(g, Game.WIDTH / 2, chapter >= 5 ? 470 : 450, 66);
    }
}

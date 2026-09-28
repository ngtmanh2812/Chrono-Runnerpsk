package chronorunner.ui;

import java.awt.Graphics2D;

import chronorunner.core.Game;
import chronorunner.core.GameState;
import chronorunner.core.Screen;
import chronorunner.world.TileType;

/** Shown when every player is defeated. */
public class GameOverScreen extends Screen {

    private final Menu menu = new Menu("CHƠI LẠI", "VỀ MENU CHÍNH");

    public GameOverScreen(Game game) {
        super(game);
    }

    @Override
    public void update(double dt) {
        menu.update(game.getInput());
        if (menu.cancelled()) {
            game.setState(GameState.MAIN_MENU);
            return;
        }
        if (!menu.confirmed()) return;
        if (menu.index() == 0) {
            game.setState(GameState.PLAYING);
        } else {
            game.setState(GameState.MAIN_MENU);
        }
    }

    @Override
    public void render(Graphics2D g) {
        TileType tile = TileType.of(game.getSession().chapter);
        UiKit.background(g, tile.bgTop.darker(), new java.awt.Color(20, 6, 12));

        UiKit.centered(g, "GAME OVER", UiKit.TITLE, UiKit.WARN, Game.WIDTH / 2, 230);
        UiKit.centered(g, "Chapter " + tile.chapter + " — " + tile.placeName,
                UiKit.H2, UiKit.TEXT, Game.WIDTH / 2, 290);
        UiKit.centered(g, "Tất cả người chơi đã bị hạ. " + tile.bossName + " vẫn đang chờ.",
                UiKit.BODY, UiKit.MUTED, Game.WIDTH / 2, 330);

        menu.render(g, Game.WIDTH / 2, 430, 66);
    }
}

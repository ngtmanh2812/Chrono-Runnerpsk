package chronorunner.ui;

import chronorunner.core.Game;
import chronorunner.core.GameState;
import chronorunner.world.TileType;

/** Shown when every player is defeated. */
public class GameOverScreen extends GameplayOverlayScreen {

    public GameOverScreen(Game game) {
        super(game, Theme.DEFEAT, "CHƠI LẠI", "VỀ MENU CHÍNH");
    }

    @Override
    public void update(double dt) {
        updateMenu();
        if (menu.cancelled()) {
            game.setState(GameState.MAIN_MENU);
            return;
        }
        if (!menuConfirmed()) return;
        if (menu.index() == 0) {
            game.setState(GameState.PLAYING);
        } else {
            game.setState(GameState.MAIN_MENU);
        }
    }

    @Override
    protected String description() {
        TileType tile = TileType.of(game.getSession().chapter);
        return tile.bossName + " vẫn đang chờ.";
    }

    @Override
    protected String subtitle() { return "Đứng dậy. Viết lại dòng thời gian."; }

    @Override
    protected String footnote() { return "BẮT ĐẦU LẠI TỪ ĐẦU CHƯƠNG"; }
}

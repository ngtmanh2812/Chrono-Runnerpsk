package chronorunner.ui;

import chronorunner.core.Game;
import chronorunner.core.GameState;
import chronorunner.world.TileType;

/** Pause overlay drawn on top of the frozen arena. */
public class PauseScreen extends GameplayOverlayScreen {

    public PauseScreen(Game game) {
        super(game, Theme.PAUSE, "TIẾP TỤC", "CHƠI LẠI", "VỀ MENU CHÍNH");
    }

    @Override
    public void update(double dt) {
        updateMenu();
        if (menu.cancelled()) {
            game.resumeState(GameState.PLAYING);
            return;
        }
        if (!menuConfirmed()) return;
        switch (menu.index()) {
            case 0:
                game.resumeState(GameState.PLAYING);
                break;
            case 1:
                game.setState(GameState.PLAYING);
                break;
            default:
                game.setState(GameState.MAIN_MENU);
                break;
        }
    }

    @Override
    protected String description() { return "Trận đấu đang tạm dừng."; }

    @Override
    protected String footnote() {
        TileType tile = TileType.of(game.getSession().chapter);
        return "CHAPTER " + tile.chapter + " / " + tile.placeName.toUpperCase(java.util.Locale.ROOT);
    }
}

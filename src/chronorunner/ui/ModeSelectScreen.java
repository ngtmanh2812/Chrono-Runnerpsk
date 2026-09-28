package chronorunner.ui;

import java.awt.Graphics2D;

import chronorunner.core.Game;
import chronorunner.core.GameState;
import chronorunner.core.Screen;

/** Pick 1 Player or 2 Players. */
public class ModeSelectScreen extends Screen {

    private final Menu menu = new Menu("1 NGƯỜI CHƠI", "2 NGƯỜI CHƠI", "QUAY LẠI");

    public ModeSelectScreen(Game game) {
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
        switch (menu.index()) {
            case 0:
                game.getSession().playerCount = 1;
                game.setState(GameState.LEVEL_SELECT);
                break;
            case 1:
                game.getSession().playerCount = 2;
                game.setState(GameState.LEVEL_SELECT);
                break;
            default:
                game.setState(GameState.MAIN_MENU);
                break;
        }
    }

    @Override
    public void render(Graphics2D g) {
        UiKit.background(g, UiKit.BG_TOP, UiKit.BG_BOTTOM);
        UiKit.centered(g, "CHỌN CHẾ ĐỘ CHƠI", UiKit.H1, UiKit.ACCENT, Game.WIDTH / 2, 170);
        menu.render(g, Game.WIDTH / 2, 300, 68);
        UiKit.centered(g, "Chế độ 2 người chơi: hai người cùng chiến đấu với một Boss.",
                UiKit.BODY, UiKit.MUTED, Game.WIDTH / 2, 560);
        UiKit.centered(g, "Nếu một người bị hạ, người còn lại vẫn tiếp tục trận đấu.",
                UiKit.BODY, UiKit.MUTED, Game.WIDTH / 2, 590);
    }
}

package chronorunner.ui;

import java.awt.Color;
import java.awt.Graphics2D;

import chronorunner.core.Game;
import chronorunner.core.GameState;
import chronorunner.core.Screen;

/** Title screen with the main menu. */
public class MainMenuScreen extends Screen {

    private final Menu menu = new Menu("CHƠI NGAY", "CÀI ĐẶT", "THOÁT");

    public MainMenuScreen(Game game) {
        super(game);
    }

    @Override
    public void update(double dt) {
        menu.update(game.getInput());
        if (!menu.confirmed()) return;
        switch (menu.index()) {
            case 0:
                game.getSession().resetForNewRun();
                game.setState(GameState.MODE_SELECT);
                break;
            case 1:
                game.setState(GameState.SETTINGS);
                break;
            default:
                game.quit();
                break;
        }
    }

    @Override
    public void render(Graphics2D g) {
        UiKit.background(g, UiKit.BG_TOP, UiKit.BG_BOTTOM);

        g.setColor(new Color(90, 200, 255, 28));
        g.drawOval(Game.WIDTH / 2 - 340, 40, 680, 680);
        g.drawOval(Game.WIDTH / 2 - 240, 140, 480, 480);
        g.setColor(new Color(255, 150, 90, 22));
        g.drawOval(Game.WIDTH / 2 - 160, 220, 320, 320);

        UiKit.centered(g, "CHRONO RUNNER", UiKit.TITLE, UiKit.ACCENT, Game.WIDTH / 2, 150);
        UiKit.centered(g, "2D Action Platformer  •  Boss Battle  •  1-2 Players",
                UiKit.BODY, UiKit.MUTED, Game.WIDTH / 2, 188);

        menu.render(g, Game.WIDTH / 2, 300, 64);

        UiKit.centered(g, "Chapter đã mở khoá: " + game.getProgress().getUnlockedChapters() + " / 5",
                UiKit.H2, UiKit.TEXT, Game.WIDTH / 2, 520);
        UiKit.centered(g, "P1:  A / D di chuyển  •  W nhảy  •  J đánh  •  K dash  •  L skill",
                UiKit.SMALL, UiKit.MUTED, Game.WIDTH / 2, 636);
        UiKit.centered(g, "P2:  ← / → di chuyển  •  ↑ nhảy  •  Numpad 1 / 2 / 3 đánh / dash / skill",
                UiKit.SMALL, UiKit.MUTED, Game.WIDTH / 2, 660);
        UiKit.centered(g, "Enter để chọn  •  Mũi tên lên/xuống để di chuyển",
                UiKit.SMALL, UiKit.MUTED, Game.WIDTH / 2, 692);
    }
}

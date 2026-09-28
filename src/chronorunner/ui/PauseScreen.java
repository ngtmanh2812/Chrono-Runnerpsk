package chronorunner.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;

import chronorunner.core.Game;
import chronorunner.core.GameState;
import chronorunner.core.Screen;

/** Pause overlay drawn on top of the frozen arena. */
public class PauseScreen extends Screen {

    private final Menu menu = new Menu("TIẾP TỤC", "CHƠI LẠI", "VỀ MENU CHÍNH");

    public PauseScreen(Game game) {
        super(game);
    }

    @Override
    public void update(double dt) {
        menu.update(game.getInput());
        if (menu.cancelled()) {
            game.resumeState(GameState.PLAYING);
            return;
        }
        if (!menu.confirmed()) return;
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
    public void render(Graphics2D g) {
        Screen play = game.screen(GameState.PLAYING);
        if (play != null) {
            play.render(g);
        }
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.68f));
        g.setColor(new Color(4, 6, 14));
        g.fillRect(0, 0, Game.WIDTH, Game.HEIGHT);
        g.setComposite(old);

        UiKit.centered(g, "TẠM DỪNG", UiKit.TITLE, UiKit.ACCENT, Game.WIDTH / 2, 220);
        menu.render(g, Game.WIDTH / 2, 340, 68);
    }
}

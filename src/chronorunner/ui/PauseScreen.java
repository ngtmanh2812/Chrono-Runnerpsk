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
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.72f));
        g.setColor(new Color(0x14, 0x11, 0x26));
        g.fillRect(0, 0, Game.WIDTH, Game.HEIGHT);
        g.setComposite(old);

        // The overlay keeps the arcade frame's panel and gold display type so
        // pausing reads as part of the same front end, not a system dialog.
        UiKit.panel(g, 350, 150, 580, 400, UiKit.ARC_PANEL, UiKit.ARC_EDGE);
        UiKit.arcadeGold(g, "TẠM DỪNG", UiKit.ARC_H1_SCALE,
                Game.WIDTH / 2, 250, UiKit.TITLE_TRACKING);
        menu.render(g, Game.WIDTH / 2, 340, 68);
        UiKit.arcade(g, "ESC để tiếp tục", UiKit.ARC_SMALL_SCALE,
                UiKit.ARC_DIM, Game.WIDTH / 2, 516, 2);
    }
}

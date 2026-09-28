package chronorunner.ui;

import java.awt.Color;
import java.awt.Graphics2D;

import chronorunner.core.Game;
import chronorunner.core.GameState;
import chronorunner.core.Screen;

/** Minimal options screen. */
public class SettingsScreen extends Screen {

    private final Menu menu = new Menu("RUNG MÀN HÌNH", "QUAY LẠI");

    public SettingsScreen(Game game) {
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
            game.getProgress().setScreenShake(!game.getProgress().isScreenShake());
            game.saveProgress();
        } else {
            game.setState(GameState.MAIN_MENU);
        }
    }

    @Override
    public void render(Graphics2D g) {
        UiKit.background(g, UiKit.BG_TOP, UiKit.BG_BOTTOM);
        UiKit.centered(g, "CÀI ĐẶT", UiKit.H1, UiKit.ACCENT, Game.WIDTH / 2, 180);
        menu.render(g, Game.WIDTH / 2, 320, 64);

        boolean on = game.getProgress().isScreenShake();
        g.setFont(UiKit.H2);
        g.setColor(on ? UiKit.OK : UiKit.WARN);
        String value = on ? "BẬT" : "TẮT";
        g.drawString(value, Game.WIDTH / 2 + 150, 320);

        UiKit.centered(g, "Rung màn hình giúp combat có phản hồi rõ hơn khi trúng đòn.",
                UiKit.SMALL, UiKit.MUTED, Game.WIDTH / 2, 470);
    }
}

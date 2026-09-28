package chronorunner.ui;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.List;

import chronorunner.core.Game;
import chronorunner.core.GameState;
import chronorunner.core.Screen;
import chronorunner.world.TileType;

/** Chapter intro text. */
public class StoryScreen extends Screen {

    public StoryScreen(Game game) {
        super(game);
    }

    @Override
    public void update(double dt) {
        if (game.getInput().wasPressed(KeyEvent.VK_ESCAPE)) {
            game.setState(GameState.LEVEL_SELECT);
        } else if (game.getInput().wasPressed(KeyEvent.VK_ENTER)
                || game.getInput().wasPressed(KeyEvent.VK_SPACE)) {
            game.setState(GameState.CHARACTER_SELECT);
        }
    }

    @Override
    public void render(Graphics2D g) {
        TileType tile = TileType.of(game.getSession().chapter);
        UiKit.background(g, tile.bgTop, tile.bgBottom);

        UiKit.centered(g, "CHAPTER " + tile.chapter, UiKit.H2, UiKit.ACCENT_WARM, Game.WIDTH / 2, 120);
        UiKit.centered(g, tile.placeName.toUpperCase(), UiKit.TITLE, UiKit.TEXT, Game.WIDTH / 2, 200);

        List<String> lines = UiKit.wrap(g, tile.story, 880, UiKit.BODY);
        int y = 300;
        for (String line : lines) {
            UiKit.centered(g, line, UiKit.BODY, UiKit.TEXT, Game.WIDTH / 2, y);
            y += 34;
        }

        int targetY = y + 40;
        UiKit.centered(g, "Mục tiêu: Đánh bại " + tile.bossName,
                UiKit.H2, tile.accent, Game.WIDTH / 2, targetY);

        UiKit.centered(g, "Nhấn ENTER để chọn nhân vật  •  ESC quay lại",
                UiKit.SMALL, UiKit.MUTED, Game.WIDTH / 2, Game.HEIGHT - 46);
    }
}

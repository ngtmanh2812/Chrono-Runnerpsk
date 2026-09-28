package chronorunner.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

import chronorunner.core.Game;
import chronorunner.core.GameState;
import chronorunner.core.InputHandler;
import chronorunner.core.Screen;
import chronorunner.world.TileType;

/** Chapter select. Locked chapters cannot be entered. */
public class LevelSelectScreen extends Screen {

    private int index;

    public LevelSelectScreen(Game game) {
        super(game);
    }

    @Override
    public void onEnter() {
        index = Math.max(0, Math.min(TileType.values().length - 1, game.getSession().chapter - 1));
    }

    @Override
    public void update(double dt) {
        InputHandler in = game.getInput();
        int count = TileType.values().length;
        if (in.wasPressed(KeyEvent.VK_UP) || in.wasPressed(KeyEvent.VK_W)) {
            index = (index - 1 + count) % count;
        }
        if (in.wasPressed(KeyEvent.VK_DOWN) || in.wasPressed(KeyEvent.VK_S)) {
            index = (index + 1) % count;
        }
        if (in.wasPressed(KeyEvent.VK_ESCAPE)) {
            game.setState(GameState.MODE_SELECT);
            return;
        }
        if (in.wasPressed(KeyEvent.VK_ENTER) && game.getProgress().isUnlocked(index + 1)) {
            game.getSession().chapter = index + 1;
            game.setState(GameState.STORY);
        }
    }

    @Override
    public void render(Graphics2D g) {
        UiKit.background(g, UiKit.BG_TOP, UiKit.BG_BOTTOM);
        UiKit.centered(g, "CHỌN CHAPTER", UiKit.H1, UiKit.ACCENT, Game.WIDTH / 2, 100);

        int top = 160;
        int rowH = 88;
        int width = 760;
        int x = (Game.WIDTH - width) / 2;

        TileType[] tiles = TileType.values();
        for (int i = 0; i < tiles.length; i++) {
            TileType tile = tiles[i];
            boolean unlocked = game.getProgress().isUnlocked(tile.chapter);
            boolean selected = i == index;
            int y = top + i * rowH;

            Color border = selected ? UiKit.ACCENT : new Color(255, 255, 255, 40);
            UiKit.panel(g, x, y, width, rowH - 12,
                    selected ? new Color(40, 70, 120, 190) : new Color(14, 18, 34, 190), border);

            g.setFont(UiKit.H2);
            g.setColor(unlocked ? UiKit.TEXT : UiKit.MUTED);
            g.drawString("Chapter " + tile.chapter + "  —  " + tile.placeName, x + 24, y + 34);

            g.setFont(UiKit.SMALL);
            g.setColor(unlocked ? UiKit.ACCENT_WARM : UiKit.MUTED);
            g.drawString(unlocked ? "Boss: " + tile.bossName : "Boss: ??? (LOCKED)", x + 24, y + 60);

            if (unlocked) {
                g.setColor(new Color(tile.accent.getRed(), tile.accent.getGreen(), tile.accent.getBlue(), 220));
                g.fillRoundRect(x + width - 34, y + 18, 14, rowH - 50, 8, 8);
            } else {
                UiKit.centered(g, "LOCKED", UiKit.SMALL, UiKit.WARN, x + width - 60, y + 40);
            }
        }

        UiKit.centered(g, "Enter để bắt đầu  •  ESC quay lại", UiKit.BODY, UiKit.MUTED,
                Game.WIDTH / 2, 660);
    }
}

package chronorunner.core;

import java.awt.Graphics2D;
import java.util.EnumMap;
import java.util.Map;

import chronorunner.ui.CharacterSelectScreen;
import chronorunner.ui.CreditsScreen;
import chronorunner.ui.GameOverScreen;
import chronorunner.ui.GamePanel;
import chronorunner.ui.GameWindow;
import chronorunner.ui.LevelSelectScreen;
import chronorunner.ui.MainMenuScreen;
import chronorunner.ui.PauseScreen;
import chronorunner.ui.PlayScreen;
import chronorunner.ui.SettingsScreen;
import chronorunner.ui.VictoryScreen;
import chronorunner.util.Progress;
import chronorunner.util.SaveManager;

/** Owns the window, the game loop's update/render targets and the screen map. */
public class Game {

    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;

    private final InputHandler input = new InputHandler();
    private final GameSession session = new GameSession();
    private final Progress progress;
    private final Map<GameState, Screen> screens = new EnumMap<>(GameState.class);

    private GameState state;
    private GamePanel panel;
    private GameWindow window;

    public Game() {
        this.progress = SaveManager.load();
    }

    public void start() {
        panel = new GamePanel(this, input);
        window = new GameWindow(panel);
        registerScreens();
        setState(GameState.MAIN_MENU);
        panel.start();
    }

    private void registerScreens() {
        screens.put(GameState.MAIN_MENU, new MainMenuScreen(this));
        screens.put(GameState.CREDITS, new CreditsScreen(this));
        screens.put(GameState.LEVEL_SELECT, new LevelSelectScreen(this));
        screens.put(GameState.SETTINGS, new SettingsScreen(this));
        screens.put(GameState.CHARACTER_SELECT, new CharacterSelectScreen(this));
        screens.put(GameState.PLAYING, new PlayScreen(this));
        screens.put(GameState.PAUSED, new PauseScreen(this));
        screens.put(GameState.VICTORY, new VictoryScreen(this));
        screens.put(GameState.GAME_OVER, new GameOverScreen(this));
    }

    /** Switch screens and run the new screen's onEnter (rebuilds levels, etc). */
    public void setState(GameState next) {
        switchTo(next, true);
    }

    /** Switch back to a screen without running onEnter (used to unpause). */
    public void resumeState(GameState next) {
        switchTo(next, false);
    }

    private void switchTo(GameState next, boolean enter) {
        if (state == next) return;
        Screen old = screens.get(state);
        if (old != null) old.onExit();
        state = next;
        Screen target = screens.get(next);
        if (target != null && enter) target.onEnter();
        input.clearPressed();
    }

    public void update(double dt) {
        Screen screen = screens.get(state);
        if (screen != null) screen.update(dt);
    }

    public void render(Graphics2D g) {
        Screen screen = screens.get(state);
        if (screen != null) screen.render(g);
    }

    public Screen screen(GameState gameState) {
        return screens.get(gameState);
    }

    public GameState getState() {
        return state;
    }

    public InputHandler getInput() {
        return input;
    }

    public GameSession getSession() {
        return session;
    }

    public Progress getProgress() {
        return progress;
    }

    public void saveProgress() {
        SaveManager.save(progress);
    }

    public void quit() {
        if (panel != null) panel.stop();
        if (window != null) window.dispose();
        System.exit(0);
    }
}

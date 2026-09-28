package chronorunner.core;

import java.awt.Graphics2D;

/** Base class for every screen in the game (State Pattern). */
public abstract class Screen {

    protected final Game game;

    protected Screen(Game game) {
        this.game = game;
    }

    /** Called every time the game switches into this state. */
    public void onEnter() {
    }

    /** Called when the game switches away from this state. */
    public void onExit() {
    }

    public abstract void update(double dt);

    public abstract void render(Graphics2D g);
}

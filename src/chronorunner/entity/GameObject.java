package chronorunner.entity;

import java.awt.Graphics2D;

import chronorunner.util.Rect;
import chronorunner.world.CombatWorld;

/** Root of every object that lives in the arena. */
public abstract class GameObject {

    public double x, y;
    public double width, height;
    public double vx, vy;
    public boolean alive = true;
    public boolean grounded = false;

    protected GameObject(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public abstract void update(double dt, CombatWorld world);

    public abstract void render(Graphics2D g);

    public Rect bounds() {
        return new Rect(x, y, width, height);
    }

    public double centerX() {
        return x + width / 2.0;
    }

    public double centerY() {
        return y + height / 2.0;
    }

    public double right() {
        return x + width;
    }

    public double bottom() {
        return y + height;
    }
}

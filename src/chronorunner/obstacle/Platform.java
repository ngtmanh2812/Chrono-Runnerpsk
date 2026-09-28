package chronorunner.obstacle;

import java.awt.Color;

/** A surface characters can stand on. */
public abstract class Platform extends Obstacle {

    protected final Color color;

    protected Platform(double x, double y, double w, double h, Color color) {
        super(x, y, w, h);
        this.color = color;
    }

    public Color getColor() {
        return color;
    }
}

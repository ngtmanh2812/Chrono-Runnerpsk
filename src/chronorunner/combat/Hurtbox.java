package chronorunner.combat;

import chronorunner.util.Rect;

/** The region of a character that can receive damage. */
public class Hurtbox {

    private final Rect rect = new Rect(0, 0, 0, 0);

    public void set(double x, double y, double w, double h) {
        rect.set(x, y, w, h);
    }

    public Rect rect() {
        return rect;
    }
}

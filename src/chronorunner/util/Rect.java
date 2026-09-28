package chronorunner.util;

/** Axis-aligned rectangle in world space (top-left origin). */
public class Rect {
    public double x, y, w, h;

    public Rect(double x, double y, double w, double h) {
        set(x, y, w, h);
    }

    public void set(double x, double y, double w, double h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public double right() { return x + w; }
    public double bottom() { return y + h; }
    public double centerX() { return x + w / 2.0; }
    public double centerY() { return y + h / 2.0; }

    public boolean intersects(Rect o) {
        return x < o.x + o.w && x + w > o.x && y < o.y + o.h && y + h > o.y;
    }

    public boolean contains(double px, double py) {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }

    public Rect copy() {
        return new Rect(x, y, w, h);
    }
}

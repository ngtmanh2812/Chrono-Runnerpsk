package chronorunner.util;

/** Shared geometry helpers for physics and combat. */
public final class Collision {

    private Collision() {
    }

    public static boolean intersects(Rect a, Rect b) {
        return a.intersects(b);
    }

    public static boolean contains(Rect r, double px, double py) {
        return r.contains(px, py);
    }

    public static double overlapX(Rect a, Rect b) {
        return Math.min(a.right(), b.right()) - Math.max(a.x, b.x);
    }

    public static double overlapY(Rect a, Rect b) {
        return Math.min(a.bottom(), b.bottom()) - Math.max(a.y, b.y);
    }

    public static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public static int clampInt(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}

package chronorunner.core;

import chronorunner.entity.GameObject;
import chronorunner.obstacle.Platform;
import chronorunner.world.CombatWorld;

/** Gravity, terminal velocity and shared AABB movement resolution. */
public final class Physics {

    public static final double GRAVITY = 2100;
    public static final double MAX_FALL = 1400;

    private Physics() {
    }

    public static double applyGravity(double vy, double dt) {
        return Math.min(vy + GRAVITY * dt, MAX_FALL);
    }

    /**
     * Integrates velocity into position, keeps the body inside the arena and
     * lands it on top of one-way platforms. Returns true when grounded.
     */
    public static boolean moveAndCollide(GameObject body, CombatWorld world, double dt) {
        body.x += body.vx * dt;
        if (body.x < 0) {
            body.x = 0;
            if (body.vx < 0) body.vx = 0;
        }
        double maxX = world.worldWidth() - body.width;
        if (body.x > maxX) {
            body.x = maxX;
            if (body.vx > 0) body.vx = 0;
        }

        double previousBottom = body.y + body.height;
        body.y += body.vy * dt;

        boolean grounded = false;
        double landingY = Double.POSITIVE_INFINITY;
        for (Platform p : world.platforms()) {
            boolean overlapsX = body.x + body.width > p.x && body.x < p.x + p.width;
            if (!overlapsX) continue;
            boolean movingDown = body.vy >= 0;
            boolean wasAbove = previousBottom <= p.y + 1e-7;
            boolean nowBelow = body.y + body.height >= p.y;
            // Select the first surface crossed, regardless of list order or
            // fall speed. Upward and side crossings never block a one-way cap.
            if (movingDown && wasAbove && nowBelow) landingY = Math.min(landingY, p.y);
        }
        if (Double.isFinite(landingY)) {
            body.y = landingY - body.height;
            body.vy = 0;
            grounded = true;
        }
        body.grounded = grounded;
        return grounded;
    }
}

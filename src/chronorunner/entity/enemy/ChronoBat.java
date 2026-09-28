package chronorunner.entity.enemy;

import chronorunner.entity.Player;
import chronorunner.util.Collision;
import chronorunner.world.CombatWorld;

/** Flying minion that homes in on the nearest player. */
public class ChronoBat extends Enemy {

    public ChronoBat(double x, double y) {
        super("Chrono Bat", x, y, 40, 28, 30, 7, 165);
    }

    @Override
    protected void updateBehavior(double dt, CombatWorld world) {
        Player target = nearestPlayer(world);
        if (target != null) {
            double dx = target.centerX() - centerX();
            double dy = (target.centerY() - 40) - centerY();
            facing = dx < 0 ? -1 : 1;
            double len = Math.max(1, Math.hypot(dx, dy));
            vx = dx / len * moveSpeed * speedMultiplier();
            vy = dy / len * moveSpeed * 0.7 * speedMultiplier();
        } else {
            vx *= 0.95;
            vy *= 0.95;
        }

        x = Collision.clamp(x + vx * dt, 0, world.worldWidth() - width);
        y = Collision.clamp(y + vy * dt, 40, world.worldHeight() - height - 40);
        grounded = false;
        updateAnimationState();
    }
}

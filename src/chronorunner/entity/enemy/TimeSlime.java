package chronorunner.entity.enemy;

/** Slow ground minion that hops at the player. */
public class TimeSlime extends Enemy {

    private double hopTimer = 0.6;

    public TimeSlime(double x, double y) {
        super("Time Slime", x, y, 40, 34, 40, 8, 95);
    }

    @Override
    protected void updateBehavior(double dt, chronorunner.world.CombatWorld world) {
        hopTimer -= dt;
        super.updateBehavior(dt, world);
        if (grounded && hopTimer <= 0) {
            vy = -520;
            grounded = false;
            hopTimer = 1.1 + Math.random() * 0.6;
        }
    }
}

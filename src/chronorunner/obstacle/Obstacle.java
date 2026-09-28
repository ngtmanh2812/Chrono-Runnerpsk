package chronorunner.obstacle;

import chronorunner.entity.GameObject;
import chronorunner.world.CombatWorld;

/** Anything static in the arena that bodies can interact with. */
public abstract class Obstacle extends GameObject {

    protected Obstacle(double x, double y, double w, double h) {
        super(x, y, w, h);
    }

    @Override
    public void update(double dt, CombatWorld world) {
        // Static by default.
    }
}

package chronorunner.entity.boss;

import java.awt.Color;

import chronorunner.entity.Player;
import chronorunner.util.Collision;
import chronorunner.world.CombatWorld;

/** Chapter 3: sword combos, sword beams and teleport strikes. */
public class TemporalKnight extends Boss {

    private int combo;

    public TemporalKnight(double x, double y) {
        super("Temporal Knight", x, y, 88, 122, 1100, 13, 170,
                new Color(255, 220, 140));
    }

    @Override
    protected void updateAI(double dt, CombatWorld world) {
        Player target = nearestPlayer(world);
        if (target == null) {
            vx = 0;
            return;
        }
        faceTarget(target);
        double distance = Math.abs(target.centerX() - centerX());

        if (specialTimer <= 0) {
            specialAttack(world);
            specialTimer = 4.0 * cooldownScale();
            return;
        }

        if (attackTimer <= 0) {
            if (distance < 140) {
                combo++;
                melee(world, 104, 100, scaleDamage(11), 280);
                attackTimer = (combo % 3 == 0 ? 0.8 : 0.28) * cooldownScale();
            } else {
                shootAngled(world, target, 0, scaleDamage(9), 520, 7);
                attackTimer = 1.4 * cooldownScale();
            }
        }
        walkToward(target, speed(), distance < 130 ? 0 : 110);
    }

    @Override
    public void specialAttack(CombatWorld world) {
        Player target = nearestPlayer(world);
        if (target == null) return;
        // Teleport behind the target, then slash.
        double behind = target.getFacing() >= 0
                ? target.x - 110
                : target.x + target.width + 30;
        x = Collision.clamp(behind, 20, world.worldWidth() - width - 20);
        y = target.y;
        // A teleport is not a fall: without this the boss keeps whatever
        // downward speed it had and plumbs out of the new position.
        vy = 0;
        facing = target.getFacing() >= 0 ? 1 : -1;
        melee(world, 130, 110, scaleDamage(17), 400);
        world.shake(0.25);
    }
}

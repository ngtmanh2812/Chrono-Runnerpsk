package chronorunner.entity.boss;

import java.awt.Color;

import chronorunner.entity.Player;
import chronorunner.world.CombatWorld;

/** Chapter 1: melee swipes, projectile volley and a lunging dash. */
public class TimeGuardian extends Boss {

    public TimeGuardian(double x, double y) {
        super("The Time Guardian", x, y, 96, 124, 700, 12, 150,
                new Color(255, 170, 52));
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
            specialTimer = 4.2 * cooldownScale();
            return;
        }

        if (attackTimer <= 0) {
            anim.play("attack");
            if (distance < 150) {
                melee(world, 110, 96, scaleDamage(14), 340);
                attackTimer = 1.2 * cooldownScale();
            } else {
                int count = getPhase() >= 2 ? 3 : 1;
                for (int i = 0; i < count; i++) {
                    shootAngled(world, target, (i - (count - 1) / 2.0) * 0.18,
                            scaleDamage(10), 430, 9);
                }
                attackTimer = 1.9 * cooldownScale();
            }
        }
        walkToward(target, speed(), distance < 150 ? 0 : 140);
    }

    @Override
    public void specialAttack(CombatWorld world) {
        anim.play("special");
        Player target = nearestPlayer(world);
        if (target == null) return;
        faceTarget(target);
        dash(facing, speed() * 3.4, 0.35);
        melee(world, 150, 110, scaleDamage(18), 460);
    }
}

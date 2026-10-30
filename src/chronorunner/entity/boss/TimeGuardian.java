package chronorunner.entity.boss;

import java.awt.Color;

import chronorunner.entity.Player;
import chronorunner.world.CombatWorld;

/** Chapter 1: melee swipes, projectile volley and a lunging dash. */
public class TimeGuardian extends Boss {

    // Gameplay windows stay independent of sprite frame counts and asset FPS.
    private static final double ATTACK_WINDUP = .18, ATTACK_RECOVERY = .22;
    private static final double SPECIAL_WINDUP = .28, SPECIAL_RECOVERY = .30;

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

        if (specialTimer <= 0) {
            specialAttack(world);
            specialTimer = 4.2 * cooldownScale();
            return;
        }

        if (attackTimer <= 0) {
            boolean melee = canMelee(target, 110, 96);
            double aimX = target.centerX(), aimY = target.centerY();
            int count = getPhase() >= 2 ? 3 : 1;
            if (beginAction(world, "attack", ATTACK_WINDUP, melee ? .18 : .10,
                    ATTACK_RECOVERY, () -> {
                        if (melee) melee(world, 110, 96, scaleDamage(14), 340);
                        else for (int i = 0; i < count; i++) shootAt(world, aimX, aimY,
                                (i - (count - 1) / 2.0) * .18, scaleDamage(10), 430, 9);
                    })) {
                attackTimer = (melee ? 1.2 : 1.9) * cooldownScale();
            }
            return;
        }
        walkToward(target, speed(), canMelee(target, 110, 96) ? Double.POSITIVE_INFINITY : 125);
    }

    @Override
    public void specialAttack(CombatWorld world) {
        Player target = nearestPlayer(world);
        if (target == null || !canBeginAction(world)) return;
        faceTarget(target);
        // A target on a higher ledge receives an aimed volley rather than a
        // horizontal charge that cannot reach its height.
        boolean sameHeight = target.bottom() > y && target.y < bottom();
        double aimX = target.centerX(), aimY = target.centerY();
        beginAction(world, "special", SPECIAL_WINDUP, .35, SPECIAL_RECOVERY, () -> {
            if (sameHeight) {
                dash(facing, speed() * 3.4, .35);
                melee(world, 150, 110, scaleDamage(18), 460);
            } else {
                shootAt(world, aimX, aimY, 0, scaleDamage(10), 430, 9);
            }
        });
    }
}

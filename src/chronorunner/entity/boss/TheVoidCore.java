package chronorunner.entity.boss;

import java.awt.Color;

import chronorunner.entity.Player;
import chronorunner.entity.enemy.ChronoBat;
import chronorunner.util.Collision;
import chronorunner.world.CombatWorld;

/** Future chapter boss. Keeps the existing temporal AI combat behavior. */
public class TheVoidCore extends Boss {

    private int cycle;

    public TheVoidCore(double x, double y) {
        super("The Void Core", x, y, 116, 140, 1600, 16, 160,
                new Color(105, 112, 255));
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
            specialTimer = 4.4 * cooldownScale();
            return;
        }

        if (attackTimer <= 0) {
            anim.play("attack");
            if (distance < 160) {
                melee(world, 120, 110, scaleDamage(13), 340);
                attackTimer = 1.1 * cooldownScale();
            } else {
                int count = getPhase() >= 3 ? 5 : 3;
                for (int i = 0; i < count; i++) {
                    shootAngled(world, target, (i - (count - 1) / 2.0) * 0.14,
                            scaleDamage(10), 500, 9);
                }
                attackTimer = 1.5 * cooldownScale();
            }
        }
        walkToward(target, speed(), distance < 160 ? 0 : 130);
    }

    @Override
    public void specialAttack(CombatWorld world) {
        anim.play("special");
        cycle++;
        Player target = nearestPlayer(world);
        switch (cycle % 3) {
            case 0:
                // Keep the existing clone-summon pattern without changing its AI.
                summon(world, new ChronoBat(centerX() - 260, centerY() - 120));
                summon(world, new ChronoBat(centerX() + 260, centerY() - 120));
                if (getPhase() >= 3) {
                    summon(world, new ChronoBat(centerX(), centerY() - 200));
                }
                world.shake(0.3);
                break;
            case 1:
                // Radial burst.
                shootRadial(world, getPhase() >= 2 ? 12 : 8, scaleDamage(9), 430, 9);
                world.shake(0.35);
                break;
            default:
                // Teleport above the player and dive.
                if (target != null) {
                    x = Collision.clamp(target.centerX() - width / 2.0, 40, world.worldWidth() - width - 40);
                    y = Math.max(60, target.y - 220);
                    vy = 400;
                }
                world.shake(0.25);
                break;
        }
    }
}

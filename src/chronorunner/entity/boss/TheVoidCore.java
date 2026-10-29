package chronorunner.entity.boss;

import java.awt.Color;

import chronorunner.core.Physics;
import chronorunner.entity.Player;
import chronorunner.obstacle.Platform;
import chronorunner.util.Collision;
import chronorunner.world.CombatWorld;

/** Flying chapter boss: existing volleys, radial bursts and a telegraphed dive. */
public class TheVoidCore extends Boss {

    private static final double ATTACK_WINDUP = .05, ATTACK_RECOVERY = .05;
    private static final double SPECIAL_WINDUP = .06, SPECIAL_RECOVERY = .06;

    private int cycle;
    private boolean diving;
    private double diveSurface;
    private double hoverSurface;

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

        if (specialTimer <= 0) {
            specialAttack(world);
            specialTimer = 4.4 * cooldownScale();
            return;
        }

        if (attackTimer <= 0) {
            boolean melee = canMelee(target, 120, 110);
            double aimX = target.centerX(), aimY = target.centerY();
            int count = getPhase() >= 3 ? 5 : 3;
            if (beginAction(world, "attack", ATTACK_WINDUP, melee ? .18 : .05,
                    ATTACK_RECOVERY, () -> {
                        if (melee) melee(world, 120, 110, scaleDamage(13), 340);
                        else for (int i = 0; i < count; i++) shootAt(world, aimX, aimY,
                                (i - (count - 1) / 2.0) * .14, scaleDamage(10), 500, 9);
                    })) {
                attackTimer = (melee ? 1.1 : 1.5) * cooldownScale();
            }
            return;
        }
        walkToward(target, speed(), canMelee(target, 120, 110) ? Double.POSITIVE_INFINITY : 140);
    }

    @Override
    public void specialAttack(CombatWorld world) {
        Player target = nearestPlayer(world);
        if (target == null || !canBeginAction(world)) return;
        faceTarget(target);
        double landing = surfaceBelow(target, world);
        double aimX = target.centerX(), aimY = target.y;
        cycle++;
        // Keep the two existing attacks; the obsolete minion branch is removed.
        if (cycle % 2 == 1 || landing < height + 60) {
            beginAction(world, "special", SPECIAL_WINDUP, .06, SPECIAL_RECOVERY, () -> {
                        shootRadial(world, getPhase() >= 2 ? 12 : 8, scaleDamage(9), 430, 9);
                        world.shake(.35);
                    });
        } else {
            beginAction(world, "special", SPECIAL_WINDUP, 1.0, SPECIAL_RECOVERY, () -> {
                diveSurface = landing;
                x = Collision.clamp(aimX - width / 2, 40, world.worldWidth() - width - 40);
                y = Math.min(landing - height, Math.max(60, aimY - 220));
                vy = 400; grounded = false; diving = true;
                world.shake(.25);
            });
        }
    }

    private double surfaceBelow(Player target, CombatWorld world) {
        double surface = world.worldHeight();
        for (Platform platform : world.platforms()) {
            if (target.right() > platform.x && target.x < platform.right()
                    && platform.y >= target.bottom() - 1e-6) surface = Math.min(surface, platform.y);
        }
        return surface;
    }

    @Override
    protected void applyVerticalMotion(double dt, CombatWorld world) {
        if (diving || isStaggered()) {
            vy = Physics.applyGravity(vy, dt);
        } else {
            Player target = nearestPlayer(world);
            hoverSurface = target == null ? world.worldHeight() : surfaceBelow(target, world);
            double destination = Math.max(60, hoverSurface - height - 24);
            double limit = baseSpeed * speedMultiplier();
            vy = Collision.clamp((destination - y) * 4, -limit, limit);
        }
    }

    @Override
    protected void moveBody(double dt, CombatWorld world) {
        if (diving) {
            Physics.moveAndCollide(this, world, dt, platform -> platform.y >= diveSurface - 1e-6);
            if (grounded) finishActiveAction();
        } else if (isStaggered()) {
            super.moveBody(dt, world);
        } else {
            Physics.moveAndCollide(this, world, dt, platform -> platform.y >= hoverSurface - 1e-6);
            grounded = false;
        }
    }

    @Override
    protected boolean isHovering() { return !diving && !isStaggered(); }

    @Override
    protected void stopAttackMotion() {
        super.stopAttackMotion();
        diving = false;
    }
}

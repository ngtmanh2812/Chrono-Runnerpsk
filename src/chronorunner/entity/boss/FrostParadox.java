package chronorunner.entity.boss;

import java.awt.Color;

import chronorunner.combat.DamageInfo;
import chronorunner.combat.Hitbox;
import chronorunner.entity.Particle;
import chronorunner.entity.Player;
import chronorunner.util.Collision;
import chronorunner.util.Rect;
import chronorunner.world.CombatWorld;

/** Chapter 4: ice shards, a slowing freeze nova and teleports. */
public class FrostParadox extends Boss {

    public FrostParadox(double x, double y) {
        super("Frost Paradox", x, y, 104, 128, 1300, 13, 135,
                new Color(200, 240, 255));
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
            specialTimer = 4.8 * cooldownScale();
            return;
        }

        if (attackTimer <= 0) {
            if (distance < 150) {
                melee(world, 110, 100, scaleDamage(11), 300);
                attackTimer = 1.3 * cooldownScale();
            } else {
                int count = getPhase() >= 2 ? 5 : 3;
                for (int i = 0; i < count; i++) {
                    shootAngled(world, target, (i - (count - 1) / 2.0) * 0.16,
                            scaleDamage(9), 470, 8);
                }
                attackTimer = 1.7 * cooldownScale();
            }
        }
        // Keep a mid range, teleporting away when the players get too close.
        if (distance < 90 && getPhase() >= 2) {
            specialAttack(world);
            specialTimer = 3.0;
        }
        walkToward(target, speed(), distance < 220 ? 0 : 220);
    }

    @Override
    public void specialAttack(CombatWorld world) {
        Player target = nearestPlayer(world);
        if (target != null && Math.abs(target.centerX() - centerX()) < 260) {
            // Freeze nova around the boss.
            Rect nova = new Rect(centerX() - 250, centerY() - 190, 500, 380);
            world.addHitbox(new Hitbox(nova,
                    new DamageInfo(scaleDamage(10), 0, -260, false, 2.5), 0.3, false, this));
            for (int i = 0; i < 28; i++) {
                double angle = Math.PI * 2 * i / 28.0;
                world.addParticle(new Particle(centerX(), centerY(),
                        Math.cos(angle) * 320, Math.sin(angle) * 320, 9, secondary, 0.5, 0));
            }
            world.shake(0.4);
        } else {
            // Teleport to a new spot, then fire a shard volley.
            x = Collision.clamp(320 + Math.random() * (world.worldWidth() - 700), 40, world.worldWidth() - width - 40);
            y = 200 + Math.random() * 300;
            // Teleporting resets vertical speed; otherwise the boss resumes
            // falling from mid-air the moment it arrives.
            vy = 0;
            if (target != null) {
                for (int i = 0; i < 4; i++) {
                    shootAngled(world, target, (i - 1.5) * 0.22, scaleDamage(10), 500, 8);
                }
            }
            world.shake(0.2);
        }
    }
}

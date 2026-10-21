package chronorunner.entity.enemy;

import java.awt.Color;
import java.awt.Graphics2D;

import chronorunner.animation.Animation;
import chronorunner.core.Physics;
import chronorunner.entity.Character;
import chronorunner.entity.Player;
import chronorunner.gfx.Art;
import chronorunner.gfx.PixelArt;
import chronorunner.gfx.PixelSprite;
import chronorunner.world.CombatWorld;

/** Minor enemy summoned by bosses. Walks toward the nearest player. */
public abstract class Enemy extends Character {

    protected double moveSpeed;

    protected Enemy(String name, double x, double y, double w, double h,
                    int maxHealth, int contactDamage, double moveSpeed) {
        super(name, x, y, w, h, maxHealth, contactDamage);
        this.moveSpeed = moveSpeed;
    }

    @Override
    protected void updateBehavior(double dt, CombatWorld world) {
        Player target = nearestPlayer(world);
        if (target != null) {
            double dx = target.centerX() - centerX();
            facing = dx < 0 ? -1 : 1;
            vx = Math.signum(dx) * moveSpeed * speedMultiplier();
        } else {
            vx *= 0.9;
        }
        vy = Physics.applyGravity(vy, dt);
        Physics.moveAndCollide(this, world, dt);
        updateAnimationState();
    }

    protected void updateAnimationState() {
        if (hurtFlash > 0) {
            anim.play("hurt");
        } else if (!grounded) {
            anim.play("jump");
        } else if (Math.abs(vx) > 10) {
            anim.play("run");
        } else {
            anim.play("idle");
        }
    }

    protected Player nearestPlayer(CombatWorld world) {
        Player best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Player p : world.players()) {
            if (p.isDead()) continue;
            double d = Math.abs(p.centerX() - centerX());
            if (d < bestDistance) {
                bestDistance = d;
                best = p;
            }
        }
        return best;
    }

    @Override
    public void render(Graphics2D g) {
        int cx = (int) centerX();
        int feetY = (int) (y + height);
        Animation a = anim.current();
        int step = a == null ? 0 : a.frame();

        PixelArt.shadow(g, cx, feetY, (int) width, new Color(0, 0, 0, 80));

        PixelSprite.Mode mode = hurtFlash > 0 ? PixelSprite.Mode.FLASH
                : (slow > 0 ? PixelSprite.Mode.SLOW : PixelSprite.Mode.NORMAL);
        PixelSprite art = Art.enemy(name).frame(step);
        art.drawAnchored(g, cx, feetY, art.scaleFor((int) width), facing < 0, mode, 1f);
    }
}

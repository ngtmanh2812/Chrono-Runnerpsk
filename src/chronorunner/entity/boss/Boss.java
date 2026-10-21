package chronorunner.entity.boss;

import java.awt.Color;
import java.awt.Graphics2D;

import chronorunner.animation.Animation;
import chronorunner.combat.DamageInfo;
import chronorunner.combat.Hitbox;
import chronorunner.core.Physics;
import chronorunner.entity.Character;
import chronorunner.entity.Player;
import chronorunner.entity.Projectile;
import chronorunner.entity.enemy.Enemy;
import chronorunner.gfx.Art;
import chronorunner.gfx.PixelArt;
import chronorunner.gfx.PixelSprite;
import chronorunner.util.Rect;
import chronorunner.world.CombatWorld;

/**
 * Base class for every chapter boss. Owns the HP-driven phase system and the
 * shared building blocks (melee, projectiles, dashing) that subclasses use to
 * script their attack patterns.
 */
public abstract class Boss extends Character {

    private int phase = 1;
    protected double attackTimer = 1.4;
    protected double specialTimer = 3.2;
    protected final double baseSpeed;
    protected Color secondary;

    private double dashTimer;
    private double dashVX;

    protected Boss(String name, double x, double y, double w, double h,
                   int maxHealth, int contactDamage, double baseSpeed,
                   Color secondary) {
        super(name, x, y, w, h, maxHealth, contactDamage);
        this.baseSpeed = baseSpeed;
        this.secondary = secondary;
    }

    @Override
    protected void setupAnimations() {
        anim.add("idle", new Animation(Art.bossAnimation(name, "idle").count(), 0.16, true));
        anim.add("run", new Animation(Art.bossAnimation(name, "run").count(), 0.085, true));
        anim.add("jump", new Animation(Art.bossAnimation(name, "jump").count(), 0.11, true));
        anim.add("attack", new Animation(Art.bossAnimation(name, "attack").count(), 0.075, false));
        anim.add("hurt", new Animation(Art.bossAnimation(name, "hurt").count(), 0.07, false));
        anim.add("death", new Animation(Art.bossAnimation(name, "death").count(), 0.13, false));
        anim.add("walk", new Animation(Art.bossAnimation(name, "walk").count(), 0.13, true));
        anim.add("special", new Animation(Art.bossAnimation(name, "special").count(), 0.09, false));
    }

    @Override
    public void takeDamage(DamageInfo info) {
        boolean aliveBeforeHit = !isDead();
        super.takeDamage(info);
        if (aliveBeforeHit && isDead()) anim.play("death");
    }

    @Override
    protected void updateBehavior(double dt, CombatWorld world) {
        updatePhase();
        if (attackTimer > 0) attackTimer -= dt;
        if (specialTimer > 0) specialTimer -= dt;

        if (dashTimer > 0) {
            dashTimer -= dt;
            vx = dashVX;
            vy = 0;
        } else {
            vy = Physics.applyGravity(vy, dt);
            updateAI(dt, world);
        }
        Physics.moveAndCollide(this, world, dt);
        updateAnimationState();
    }

    protected abstract void updateAI(double dt, CombatWorld world);

    public abstract void specialAttack(CombatWorld world);

    private void updatePhase() {
        double ratio = healthRatio();
        int next = ratio > 0.7 ? 1 : (ratio > 0.4 ? 2 : 3);
        if (next != phase) {
            phase = next;
            onPhaseChange();
        }
    }

    protected void onPhaseChange() {
    }

    public int getPhase() {
        return phase;
    }

    protected double speed() {
        return baseSpeed * (phase == 1 ? 1.0 : phase == 2 ? 1.25 : 1.5);
    }

    protected double cooldownScale() {
        return phase == 1 ? 1.0 : phase == 2 ? 0.8 : 0.65;
    }

    protected int scaleDamage(int base) {
        return (int) Math.round(base * (1 + 0.18 * (phase - 1)));
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

    protected void faceTarget(Player target) {
        if (target != null) {
            facing = target.centerX() < centerX() ? -1 : 1;
        }
    }

    protected void walkToward(Player target, double speed, double stopDistance) {
        if (target == null) {
            vx = 0;
            return;
        }
        double dx = target.centerX() - centerX();
        vx = Math.abs(dx) > stopDistance ? Math.signum(dx) * speed * speedMultiplier() : 0;
    }

    protected void hop(double power) {
        if (grounded) {
            vy = -power;
            grounded = false;
        }
    }

    protected void dash(int direction, double speed, double duration) {
        dashVX = direction * speed;
        dashTimer = duration;
    }

    protected Hitbox melee(CombatWorld world, double reach, double height, int damage, double knockback) {
        double hx = facing >= 0 ? right() : this.x - reach;
        Rect rect = new Rect(hx, y + (this.height - height) / 2.0, reach, height);
        Hitbox hitbox = new Hitbox(rect, new DamageInfo(damage, knockback * facing, -260, false), 0.18, false, this);
        world.addHitbox(hitbox);
        return hitbox;
    }

    protected void shootAngled(CombatWorld world, Player target, double angleOffset,
                               int damage, double speed, double radius) {
        double dx = target.centerX() - centerX();
        double dy = target.centerY() - centerY();
        double angle = Math.atan2(dy, dx) + angleOffset;
        double px = Math.cos(angle);
        double py = Math.sin(angle);
        world.addProjectile(new Projectile(centerX(), centerY(), px * speed, py * speed, radius,
                new DamageInfo(damage, px * 150, -140, false), false, secondary, 3.0));
    }

    protected void shootRadial(CombatWorld world, int count, int damage, double speed, double radius) {
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2 * i / count;
            world.addProjectile(new Projectile(centerX(), centerY(),
                    Math.cos(angle) * speed, Math.sin(angle) * speed, radius,
                    new DamageInfo(damage, Math.cos(angle) * 150, -120, false), false, secondary, 3.0));
        }
    }

    protected void summon(CombatWorld world, Enemy enemy) {
        world.addEnemy(enemy);
    }

    protected void updateAnimationState() {
        if (isDead()) {
            anim.play("death");
            return;
        }
        if (hurtFlash > 0) {
            anim.play("hurt");
            return;
        }

        String current = anim.currentName();
        if ("attack".equals(current) || "special".equals(current) || "hurt".equals(current)) {
            Animation active = anim.current();
            if (active != null && !active.finished()) return;
        }

        if (!grounded) anim.play("jump");
        else if (Math.abs(vx) >= baseSpeed * 1.12) anim.play("run");
        else if (Math.abs(vx) > 12) anim.play("walk");
        else anim.play("idle");
    }

    @Override
    public void render(Graphics2D g) {
        int cx = (int) centerX();
        int cy = (int) centerY();
        int feetY = (int) (y + height);
        PixelArt.shadow(g, cx, feetY, (int) width, new Color(0, 0, 0, 100));

        PixelSprite.Mode mode = hurtFlash > 0 ? PixelSprite.Mode.FLASH
                : (slow > 0 ? PixelSprite.Mode.SLOW : PixelSprite.Mode.NORMAL);
        Animation current = anim.current();
        String animation = anim.currentName();
        PixelSprite art = Art.bossAnimation(name, animation)
                .frame(current == null ? 0 : current.frame());
        // Give bosses more screen presence while leaving their combat hitboxes
        // and world coordinates untouched.
        int scale = art.scaleFor((int) width) + 1;
        if ("The Void Core".equals(name)) {
            art.draw(g, cx - art.width() * scale / 2, cy - art.height() * scale / 2,
                    scale, facing < 0, mode, 1f);
        } else {
            art.drawAnchored(g, cx, feetY, scale, facing < 0, mode, 1f);
        }

    }
}

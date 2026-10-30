package chronorunner.entity.boss;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Map;
import java.util.WeakHashMap;

import chronorunner.animation.Animation;
import chronorunner.combat.Attack;
import chronorunner.combat.AttackData;
import chronorunner.combat.DamageInfo;
import chronorunner.combat.Hitbox;
import chronorunner.core.Physics;
import chronorunner.entity.Character;
import chronorunner.entity.Player;
import chronorunner.entity.Projectile;
import chronorunner.entity.enemy.Enemy;
import chronorunner.gfx.Art;
import chronorunner.gfx.PixelArt;
import chronorunner.gfx.PixelFont;
import chronorunner.gfx.PixelSprite;
import chronorunner.gfx.SpriteSet;
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
    private double landingAnimTimer;
    private Attack action;
    private String actionAnimation;
    private Hitbox actionHitbox;
    private final Map<DamageInfo.MeleeChain, Integer> acceptedMeleeSteps = new WeakHashMap<>();
    private static final double MAX_CONTINUOUS_STAGGER = .90;
    private static final double STAGGER_RESISTANCE_SECONDS = .45;
    private double staggerTimer;
    private double continuousStagger;
    private double staggerResistance;

    protected Boss(String name, double x, double y, double w, double h,
                   int maxHealth, int contactDamage, double baseSpeed,
                   Color secondary) {
        super(name, x, y, w, h, maxHealth, contactDamage);
        this.baseSpeed = baseSpeed;
        this.secondary = secondary;
    }

    @Override
    protected void setupAnimations() {
        addAnimation("idle", .16, true);
        addAnimation("run", .085, true);
        addAnimation("jump", .11, false);
        addAnimation("fall", .10, true);
        addAnimation("land", .08, false);
        addAnimation("dash", .025, false);
        addAnimation("attack", .075, false);
        addAnimation("hurt", .07, false);
        addAnimation("death", .13, false);
        addAnimation("walk", .13, true);
        addAnimation("special", .09, false);
        // A visual bank for future use; no boss guarding behavior is introduced.
        addAnimation("block", .08, false);
    }

    private void addAnimation(String state, double seconds, boolean loop) {
        anim.add(state, Art.bossAnimation(name, state).animation(state, seconds, loop));
    }

    @Override
    protected Rect damageBounds() {
        return BossHurtGeometry.bounds(name, x, y, width, height, facing);
    }

    @Override
    protected void updateTimers(double dt) {
        double lockedTime = Math.min(dt, Math.max(stun, staggerTimer));
        super.updateTimers(dt);
        staggerTimer = Math.max(0, staggerTimer - dt);
        staggerResistance = Math.max(0, staggerResistance - dt);
        if (lockedTime > 0) {
            continuousStagger += lockedTime;
            if (continuousStagger >= MAX_CONTINUOUS_STAGGER - 1e-9) {
                // Return control after one bounded opening, including co-op hits.
                stun = 0; staggerTimer = 0; continuousStagger = 0;
                staggerResistance = STAGGER_RESISTANCE_SECONDS;
            }
        } else if (!isStaggered() && staggerResistance <= 0) continuousStagger = 0;
    }

    @Override
    public boolean isInvulnerableTo(DamageInfo info) {
        if (isTaggedMelee(info)) {
            // Distinct committed J moves can connect, including another player
            // or the next chain; replaying an accepted step never deals damage.
            return acceptedMeleeSteps.getOrDefault(info.meleeChain, 0) >= info.meleeStep;
        }
        return super.isInvulnerableTo(info);
    }

    private boolean isTaggedMelee(DamageInfo info) {
        return info != null && info.fromPlayer && info.meleeChain != null
                && info.meleeStep >= 1 && info.meleeStep <= 3;
    }

    @Override
    public void takeDamage(DamageInfo info) {
        if (info == null || info.amount <= 0) return;
        int before = health;
        double previousVX = vx, previousVY = vy;
        boolean wasGrounded = grounded;
        super.takeDamage(info);
        if (health < before) {
            if (isTaggedMelee(info)) acceptedMeleeSteps.put(info.meleeChain, info.meleeStep);
            if (!isDead() && isStaggerResistant()) {
                // Damage still lands during resistance; the next attack cannot
                // refresh the boss's control lock or cancel a committed response.
                vx = previousVX; vy = previousVY; grounded = wasGrounded;
                stun = 0;
                updateAnimationState();
                return;
            }
            double duration = info.meleeStep == 3 ? .42 : info.meleeStep == 2 ? .36 : .30;
            staggerTimer = Math.max(staggerTimer,
                    Math.min(duration, Math.max(0, MAX_CONTINUOUS_STAGGER - continuousStagger)));
            double hitVX = vx, hitVY = vy;
            cancelAction();
            vx = hitVX; vy = hitVY;
            if (isDead()) anim.play("death");
        }
    }

    @Override
    protected void updateBehavior(double dt, CombatWorld world) {
        if (landingAnimTimer > 0) landingAnimTimer -= dt;
        updatePhase();
        if (attackTimer > 0) attackTimer -= dt;
        if (specialTimer > 0) specialTimer -= dt;

        if (!world.isCombatActive()) {
            endCombat();
            return;
        }
        if (isStaggered()) {
            vx *= Math.pow(.86, dt * 60);
            applyVerticalMotion(dt, world);
        } else if (action != null) {
            action.update(dt);
            if (dashTimer > 0) {
                dashTimer = Math.max(0, dashTimer - dt);
                vx = dashVX; vy = 0;
            } else applyVerticalMotion(dt, world);
            if (action.isReady()) {
                action = null;
                actionAnimation = null;
            }
        } else if (dashTimer > 0) {
            dashTimer = Math.max(0, dashTimer - dt);
            vx = dashVX; vy = 0;
        } else {
            applyVerticalMotion(dt, world);
            updateAI(dt, world);
        }
        boolean wasAirborne = !grounded;
        moveBody(dt, world);
        if (wasAirborne && grounded) landingAnimTimer = .16;
        updateAnimationState();
    }

    protected void applyVerticalMotion(double dt, CombatWorld world) {
        vy = Physics.applyGravity(vy, dt);
    }

    protected void moveBody(double dt, CombatWorld world) {
        Physics.moveAndCollide(this, world, dt);
    }

    protected boolean isHovering() { return false; }

    protected final boolean isStaggered() {
        return stun > 0 || staggerTimer > 0;
    }

    public final boolean isStaggerResistant() { return staggerResistance > 0; }

    protected final boolean canBeginAction(CombatWorld world) {
        return !isDead() && !isStaggered() && (action == null || action.isReady()) && world.isCombatActive();
    }

    /** Reuse Attack's windows for melee, projectile volleys and scripted motion. */
    protected final boolean beginAction(CombatWorld world, String clip, double startup,
                                         double active, double recovery, Runnable effect) {
        if (!canBeginAction(world)) return false;
        AttackData data = new AttackData(clip, 0, 0, 0,
                Math.max(1, (int)Math.round(startup * 60)),
                Math.max(1, (int)Math.round(active * 60)),
                Math.max(1, (int)Math.round(recovery * 60)), 0);
        action = new Attack(data, effect, this::stopAttackMotion);
        actionAnimation = clip;
        vx = 0;
        action.start(this, facing, false, world);
        anim.restart(clip);
        return true;
    }

    protected final void finishActiveAction() {
        if (action != null) action.completeActive();
    }

    protected void stopAttackMotion() {
        if (actionHitbox != null) actionHitbox.cancel();
        actionHitbox = null;
        dashTimer = 0; dashVX = 0; vx = 0;
    }

    private void cancelAction() {
        if (action != null) action.reset();
        action = null; actionAnimation = null;
        stopAttackMotion();
    }

    public final void endCombat() {
        cancelAction();
        staggerTimer = 0; continuousStagger = 0; staggerResistance = 0; stun = 0;
        if (!isDead()) anim.play("idle");
    }

    public final boolean isContactDamageEnabled() {
        return !isDead() && !isStaggered() && action != null && action.isActive() && isBodyContactAction();
    }

    /** Most attacks own a dedicated melee hitbox instead of damaging with the body. */
    protected boolean isBodyContactAction() { return false; }

    /** Stable only for one dangerous contact action; PlayScreen deduplicates per player. */
    public final Object getContactActionId() { return isContactDamageEnabled() ? action : null; }

    @Override
    protected void updateDeath(double dt, CombatWorld world) {
        vx = 0;
        if (!grounded) {
            vy = Physics.applyGravity(vy, dt);
            Physics.moveAndCollide(this, world, dt);
        }
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
            double d = Math.hypot(p.centerX() - centerX(), p.centerY() - centerY());
            if (d < bestDistance) {
                bestDistance = d;
                best = p;
            }
        }
        return best;
    }

    protected void faceTarget(Player target) {
        if (target != null && (action == null || action.isReady())) {
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
        vx = dashVX;
        vy = 0;
    }

    protected final boolean canMelee(Player target, double reach, double attackHeight) {
        return target != null && meleeRegion(reach, attackHeight).intersects(target.bounds());
    }

    private Rect meleeRegion(double reach, double attackHeight) {
        return new Rect(facing >= 0 ? right() : x - reach,
                y + (height - attackHeight) / 2, reach, attackHeight);
    }

    protected Hitbox melee(CombatWorld world, double reach, double height, int damage, double knockback) {
        Rect rect = meleeRegion(reach, height);
        double life = action == null ? .18 : action.getData().activeSeconds();
        Hitbox hitbox = new Hitbox(rect, new DamageInfo(damage, knockback * facing, -260, false), life, false, this, action != null);
        if (action != null) actionHitbox = hitbox;
        world.addHitbox(hitbox);
        return hitbox;
    }

    protected void shootAngled(CombatWorld world, Player target, double angleOffset,
                               int damage, double speed, double radius) {
        shootAt(world, target.centerX(), target.centerY(), angleOffset, damage, speed, radius);
    }

    protected void shootAt(CombatWorld world, double aimX, double aimY, double angleOffset,
                             int damage, double speed, double radius) {
        double dx = aimX - centerX();
        double dy = aimY - centerY();
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
        if (isStaggered()) {
            anim.play("hurt");
            return;
        }

        if (action != null) {
            anim.play(actionAnimation);
            anim.current().setProgress(action.animationProgress());
            return;
        }

        if (dashTimer > 0) anim.play("dash");
        else if (!grounded && !isHovering()) {
            anim.play(vy < 0 ? "jump" : "fall");
            Animation airborne = anim.current();
            if (airborne != null) airborne.setProgress(vy < 0 ? .33 : Math.min(1, vy / Physics.MAX_FALL));
        }
        else if (landingAnimTimer > 0) {
            anim.play("land");
            anim.current().setProgress(1 - landingAnimTimer / .16);
        }
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
        SpriteSet sprites = Art.bossAnimation(name, animation);
        PixelSprite art = sprites.frame(current == null ? 0 : current.frame());
        // Visual size is independent of the terrain body. damageBounds() aligns
        // the vulnerable torso separately without inflating world collision.
        int scale = art.scaleFor((int) width) + 1;
        if (sprites.layout() != null) {
            sprites.layout().draw(art, g, cx, feetY, cy, facing < 0, mode, 1f);
        } else if ("The Void Core".equals(name)) {
            art.draw(g, cx - art.width() * scale / 2, cy - art.height() * scale / 2,
                    scale, facing < 0, mode, 1f);
        } else {
            art.drawAnchored(g, cx, feetY, scale, facing < 0, mode, 1f);
        }
        if (!isDead() && isStaggerResistant()) {
            String cue = "KHÁNG CHOÁNG";
            int cueScale = 2;
            int cueX = cx - PixelFont.width(cue, cueScale, 0) / 2;
            // Keep the short cue above the enlarged sprite/halo, not over its core.
            int cueY = Math.max(32, (int)Math.min(damageBounds().y - 16, y - 80));
            PixelFont.drawBaseline(g, cue, cueX + 1, cueY + 1, cueScale,
                    new Color(16, 10, 2, 220), 0);
            PixelFont.drawBaseline(g, cue, cueX, cueY, cueScale,
                    new Color(255, 196, 83), 0);
        }
    }
}

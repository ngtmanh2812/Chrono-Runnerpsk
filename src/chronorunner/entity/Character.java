package chronorunner.entity;

import java.awt.Graphics2D;

import chronorunner.animation.Animation;
import chronorunner.animation.AnimationController;
import chronorunner.combat.DamageInfo;
import chronorunner.combat.Hurtbox;
import chronorunner.world.CombatWorld;

/** Shared behaviour for players, enemies and bosses: health + damage timers. */
public abstract class Character extends GameObject {

    protected final String name;
    protected int maxHealth;
    protected int health;
    protected int facing = 1;
    protected int contactDamage;
    protected double invuln;
    protected double hurtFlash;
    protected double stun;
    protected double slow;

    protected final Hurtbox hurtbox = new Hurtbox();
    protected final AnimationController anim = new AnimationController();

    protected Character(String name, double x, double y, double w, double h,
                        int maxHealth, int contactDamage) {
        super(x, y, w, h);
        this.name = name;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.contactDamage = contactDamage;
        setupAnimations();
        hurtbox.set(x, y, w, h);
    }

    protected void setupAnimations() {
        anim.add("idle", new Animation(2, 0.45, true));
        anim.add("run", new Animation(6, 0.09, true));
        anim.add("jump", new Animation(1, 0.3, true));
        anim.add("dash", new Animation(3, 0.06, false));
        anim.add("attack", new Animation(3, 0.05, false));
        anim.add("hurt", new Animation(1, 0.12, false));
    }

    @Override
    public final void update(double dt, CombatWorld world) {
        updateTimers(dt);
        if (!isDead()) {
            updateBehavior(dt, world);
        }
        hurtbox.set(x, y, width, height);
    }

    protected abstract void updateBehavior(double dt, CombatWorld world);

    protected void updateTimers(double dt) {
        if (invuln > 0) invuln -= dt;
        if (hurtFlash > 0) hurtFlash -= dt;
        if (stun > 0) stun -= dt;
        if (slow > 0) slow -= dt;
        anim.update(dt);
    }

    public void takeDamage(DamageInfo info) {
        if (info == null || invuln > 0 || isDead()) return;
        health -= info.amount;
        if (health < 0) health = 0;
        invuln = 0.55;
        hurtFlash = 0.22;
        stun = Math.max(stun, 0.14);
        vx += info.knockbackX;
        if (info.knockbackY < 0) {
            vy = Math.min(vy, info.knockbackY);
            grounded = false;
        }
        anim.play("hurt");
    }

    public boolean isDead() {
        return health <= 0;
    }

    public boolean isInvulnerable() {
        return invuln > 0;
    }

    public void applySlow(double seconds) {
        slow = Math.max(slow, seconds);
    }

    public double speedMultiplier() {
        return slow > 0 ? 0.45 : 1.0;
    }

    public Hurtbox hurtbox() {
        return hurtbox;
    }

    public String getName() {
        return name;
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public int getFacing() {
        return facing;
    }

    public int getContactDamage() {
        return contactDamage;
    }

    public double healthRatio() {
        return maxHealth <= 0 ? 0 : (double) health / maxHealth;
    }

    protected void restoreHealth(int amount) {
        health = Math.min(maxHealth, health + amount);
    }

    @Override
    public abstract void render(Graphics2D g);
}

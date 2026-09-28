package chronorunner.entity;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

import chronorunner.animation.Animation;
import chronorunner.combat.Attack;
import chronorunner.core.InputHandler;
import chronorunner.core.Physics;
import chronorunner.gfx.Art;
import chronorunner.gfx.PixelArt;
import chronorunner.gfx.PixelSprite;
import chronorunner.util.Rect;
import chronorunner.world.CombatWorld;

/**
 * Base class for the three playable characters. Handles movement, double jump,
 * dash, attack/skill dispatch and rendering. Subclasses only supply stats and
 * the actual attack/skill payloads.
 */
public abstract class Player extends Character {

    public static final int[] P1_KEYS = {
            KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_W,
            KeyEvent.VK_J, KeyEvent.VK_K, KeyEvent.VK_L
    };
    public static final int[] P2_KEYS = {
            KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP,
            KeyEvent.VK_NUMPAD1, KeyEvent.VK_NUMPAD2, KeyEvent.VK_NUMPAD3
    };

    public static int[] keysFor(int playerIndex) {
        return playerIndex == 0 ? P1_KEYS : P2_KEYS;
    }

    public final int playerIndex;

    private final InputHandler input;
    private final int kLeft, kRight, kJump, kAttack, kDash, kSkill;

    protected double moveSpeed;
    protected double jumpSpeed;
    protected double dashSpeed;
    protected double dashDuration;
    protected double dashCooldown;
    protected double skillCooldown;
    protected int maxJumps;
    protected int attackPower;

    protected Attack basicAttack;
    protected Color accentColor = Color.WHITE;
    protected String skillName = "Skill";

    private int jumpsLeft;
    private double dashTimer;
    private double dashCooldownTimer;
    private double skillTimer;
    private double skillFlash;

    protected Player(int playerIndex, InputHandler input, String name,
                     double x, double y, int[] keys) {
        super(name, x, y, 44, 62, 100, 0);
        this.playerIndex = playerIndex;
        this.input = input;
        this.kLeft = keys[0];
        this.kRight = keys[1];
        this.kJump = keys[2];
        this.kAttack = keys[3];
        this.kDash = keys[4];
        this.kSkill = keys[5];
    }

    @Override
    protected void updateTimers(double dt) {
        super.updateTimers(dt);
        // Death skips updateBehavior, so a swing in flight would otherwise stay
        // frozen mid-window and keep its attacker/world references alive.
        if (isDead() && basicAttack.isBusy()) {
            basicAttack.reset();
        }
    }

    @Override
    protected void updateBehavior(double dt, CombatWorld world) {
        // Advance the attack state machine before reading input so an attack
        // started this frame keeps its full startup window.
        basicAttack.update(dt);

        boolean controllable = stun <= 0;

        if (dashTimer > 0) {
            dashTimer -= dt;
            vx = dashSpeed * facing;
            vy = 0;
            spawnDashTrail(world);
        } else {
            if (controllable) {
                int dir = 0;
                if (input.isDown(kLeft)) dir--;
                if (input.isDown(kRight)) dir++;
                vx = dir * moveSpeed * speedMultiplier();
                // The direction is latched when the swing starts, so turning
                // mid-move would make the hitbox fire opposite the sprite.
                if (dir != 0 && !basicAttack.isBusy()) facing = dir;
            } else {
                vx *= 0.86;
            }
            vy = Physics.applyGravity(vy, dt);
        }

        if (controllable) {
            if (input.wasPressed(kJump)) tryJump();
            // A committed attack owns the character: no dash, no skill and no
            // second swing until its recovery window has finished. Each check
            // reads isReady() live rather than a snapshot taken before the
            // checks run, so a skill or dash pressed on the same frame as the
            // attack cannot slip through behind the attack that just committed.
            if (basicAttack.isReady() && input.wasPressed(kDash) && dashCooldownTimer <= 0) startDash();
            if (basicAttack.isReady() && input.wasPressed(kAttack)) performAttack(world);
            if (basicAttack.isReady() && input.wasPressed(kSkill) && skillTimer <= 0) {
                performSkill(world);
                skillTimer = skillCooldown;
                skillFlash = 0.25;
            }
        }

        if (dashCooldownTimer > 0) dashCooldownTimer -= dt;
        if (skillTimer > 0) skillTimer -= dt;
        if (skillFlash > 0) skillFlash -= dt;

        Physics.moveAndCollide(this, world, dt);
        if (grounded) jumpsLeft = maxJumps;

        updateAnimationState();
    }

    private void tryJump() {
        if (grounded) {
            vy = -jumpSpeed;
            grounded = false;
            jumpsLeft = maxJumps - 1;
        } else if (jumpsLeft > 0) {
            vy = -jumpSpeed * 0.9;
            jumpsLeft--;
        }
    }

    private void startDash() {
        // Clear first so startSkillDash's extend-never-shorten rule cannot turn
        // a normal dash into a longer one if a character ever sets
        // dashCooldown below dashDuration.
        dashTimer = 0;
        startSkillDash(dashDuration);
        dashCooldownTimer = dashCooldown;
    }

    /** Gives a subclass a brief scripted dash as part of its skill (no cooldown cost). */
    protected void startSkillDash(double duration) {
        dashTimer = Math.max(dashTimer, duration);
        invuln = Math.max(invuln, duration + 0.05);
        vy = 0;
        anim.play("dash");
    }

    private void spawnDashTrail(CombatWorld world) {
        world.addParticle(new Particle(centerX() - width / 2, y + Math.random() * height,
                -facing * 70, (Math.random() - 0.5) * 50, 6, accentColor, 0.28, 0));
    }

    private void updateAnimationState() {
        if (hurtFlash > 0) {
            anim.play("hurt");
        } else if (basicAttack.isBusy()) {
            anim.play("attack");
        } else if (dashTimer > 0) {
            anim.play("dash");
        } else if (!grounded) {
            anim.play("jump");
        } else if (Math.abs(vx) > 12) {
            anim.play("run");
        } else {
            anim.play("idle");
        }
    }

    /** Spawns particles + floating text for a successful skill; used by subclasses. */
    protected void announceSkill(CombatWorld world) {
        world.addFloatingText(new FloatingText(centerX() - 30, y - 24, skillName, accentColor, 22, 1.0));
    }

    protected abstract void performAttack(CombatWorld world);

    protected abstract void performSkill(CombatWorld world);

    public abstract String characterId();

    public String getSkillName() {
        return skillName;
    }

    public double getSkillCooldownRatio() {
        return skillCooldown <= 0 ? 1 : 1 - Math.max(0, skillTimer) / skillCooldown;
    }

    public double getDashCooldownRatio() {
        return dashCooldown <= 0 ? 1 : 1 - Math.max(0, dashCooldownTimer) / dashCooldown;
    }

    /** Player art is 22x31; two of those tiles exactly fill the 44x62 hitbox. */
    private static final int SCALE = 2;

    @Override
    public void render(Graphics2D g) {
        boolean dead = isDead();
        float alpha = dead ? 0.35f : 1f;
        if (!dead && invuln > 0 && ((int) (invuln * 22)) % 2 == 0) {
            alpha = 0.5f;
        }

        int bx = (int) x;
        int by = (int) y;
        int cx = (int) centerX();
        int feetY = (int) (y + height);

        Animation a = anim.current();
        int step = a == null ? 0 : a.frame();
        // The run cycle keeps the old vertical bob on odd steps so the stride
        // reads even though the two sprite frames are not a full walk cycle.
        int bob = "run".equals(anim.currentName()) && step % 2 != 0 ? -3 : 0;

        PixelArt.shadow(g, cx, feetY, (int) width, new Color(0, 0, 0, 80));

        if (skillFlash > 0) {
            int radius = (int) (Math.max(width, height) * 0.6);
            PixelArt.disc(g, cx, (int) (y + height / 2) + bob, radius,
                    new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 90));
        }

        PixelSprite.Mode mode = hurtFlash > 0 ? PixelSprite.Mode.FLASH
                : (slow > 0 ? PixelSprite.Mode.SLOW : PixelSprite.Mode.NORMAL);
        Art.player(characterId()).frame(step)
                .drawAnchored(g, cx, feetY + bob, SCALE, facing < 0, mode, alpha);

        drawSwing(g);

        g.setFont(new Font("Dialog", Font.BOLD, 12));
        g.setColor(new Color(255, 255, 255, 200));
        g.drawString("P" + (playerIndex + 1), bx + (int) width / 2 - 8, by + bob - 6);
    }

    /** Outlines the startup wind-up, then fills the region that deals damage. */
    private void drawSwing(Graphics2D g) {
        Rect region = basicAttack.region();
        if (region == null) return;

        int rx = (int) region.x;
        int ry = (int) region.y;
        int rw = (int) region.w;
        int rh = (int) region.h;
        Color accent = accentColor;

        if (basicAttack.isStartup()) {
            g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 45));
            g.fillRect(rx, ry, rw, rh);
            g.setColor(new Color(255, 255, 255, 120));
            g.drawRect(rx, ry, rw, rh);
        } else if (basicAttack.isActive()) {
            g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 170));
            g.fillRect(rx, ry, rw, rh);
            g.setColor(Color.WHITE);
            g.drawRect(rx, ry, rw, rh);
        }
    }
}

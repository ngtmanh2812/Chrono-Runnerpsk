package chronorunner.entity;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

import chronorunner.animation.Animation;
import chronorunner.combat.Attack;
import chronorunner.combat.DamageInfo;
import chronorunner.combat.PlayerDefense;
import chronorunner.core.InputHandler;
import chronorunner.core.Physics;
import chronorunner.gfx.Art;
import chronorunner.gfx.PixelArt;
import chronorunner.gfx.PixelSprite;
import chronorunner.world.CombatWorld;

/**
 * Base class for the three playable characters. Handles movement, double jump,
 * dash, attack/skill dispatch and rendering. Subclasses supply stats, attack
 * configuration and signature skills; PlayerDefense owns the guard timers.
 */
public abstract class Player extends Character {

    public static final int PLAYER_BASE_ATTACK_DAMAGE = 30;

    public static final int[] P1_KEYS = {
            KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_W,
            KeyEvent.VK_J, KeyEvent.VK_K, KeyEvent.VK_L, KeyEvent.VK_S
    };
    public static final int[] P2_KEYS = {
            KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP,
            KeyEvent.VK_NUMPAD1, KeyEvent.VK_NUMPAD2, KeyEvent.VK_NUMPAD3, KeyEvent.VK_DOWN
    };

    public static int[] keysFor(int playerIndex) {
        return playerIndex == 0 ? P1_KEYS : P2_KEYS;
    }

    public final int playerIndex;

    private final InputHandler input;
    private final int kLeft, kRight, kJump, kAttack, kDash, kSkill, kBlock;
    private final PlayerDefense defense = new PlayerDefense();

    protected double moveSpeed;
    protected double jumpSpeed;
    protected double dashSpeed;
    protected double dashDuration;
    protected double dashCooldown;
    protected double skillCooldown;
    protected int maxJumps;

    protected Attack basicAttack;
    protected Color accentColor = Color.WHITE;
    /** Shared temporal-energy hue sampled from the player sprite effects. */
    protected Color skillEffectColor = new Color(68, 205, 255);
    protected String skillName = "Skill";

    /** How long the one-shot skill animation stays on screen. */
    private static final double SPECIAL_ANIM_SECONDS = 0.54;
    /** Minimum time the hurt animation is held after a successful hit. */
    private static final double HURT_ANIM_SECONDS = 0.30;
    /** Two-pixel rows forming a small shield, placed beside the original sprite. */
    private static final int[] GUARD_HALF_WIDTHS = {6, 10, 12, 12, 12, 12, 12, 12, 10, 10, 8, 8, 6, 4, 2, 2};

    private int jumpsLeft;
    private double dashTimer;
    private double dashCooldownTimer;
    private double skillTimer;
    private double skillFlash;
    private double specialAnimTimer;
    private double hurtAnimTimer;

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
        this.kBlock = keys[6];
    }

    /**
     * Player frame counts match the sprite sets sliced from the supplied sheet.
     * The frame index is all that matters to rendering; the durations only drive
     * the looping states, because attack and jump are pinned by gameplay.
     */
    @Override
    protected void setupAnimations() {
        anim.add("idle", new Animation(10, 0.12, true));
        anim.add("walk", new Animation(10, 0.08, true));
        anim.add("run", new Animation(10, 0.055, true));
        anim.add("jump", new Animation(10, 0.5, false));
        anim.add("dash", new Animation(10, 0.02, false));
        anim.add("attack", new Animation(10, 0.3, false));
        anim.add("special", new Animation(9, 0.06, false));
        anim.add("hurt", new Animation(10, 0.03, false));
        anim.add("death", new Animation(10, 0.09, false));
    }

    @Override
    protected void updateTimers(double dt) {
        super.updateTimers(dt);
        if (hurtAnimTimer > 0) hurtAnimTimer -= dt;
        // A hit interrupts the one-shot skill clip; do not resume an old cast
        // after Hurt has taken priority.
        if (specialAnimTimer > 0) specialAnimTimer -= dt;
        // Death skips updateBehavior, so a swing in flight would otherwise stay
        // frozen mid-window and keep its attacker/world references alive. The
        // death pose is latched here too, so it cannot be restarted per frame.
        if (isDead()) {
            defense.cancel();
            specialAnimTimer = 0;
            if (basicAttack.isBusy()) {
                basicAttack.reset();
            }
            anim.play("death");
        }
    }

    /**
     * Adds the hurt/death animation windows on top of the shared damage logic.
     * All combat hits enter here. A guard consumes one valid hit before the
     * shared health, knockback, stun, invulnerability and hurt logic runs.
     */
    @Override
    public void takeDamage(DamageInfo info) {
        if (info == null || info.amount <= 0 || invuln > 0 || isDead()) return;
        if (defense.absorb(info)) return;
        super.takeDamage(info);
        if (isDead()) {
            defense.cancel();
            hurtAnimTimer = 0;
            specialAnimTimer = 0;
            // Cancel the in-flight swing immediately: a dead attacker should
            // not keep a committed attack (or its pending hitbox) alive.
            if (basicAttack.isBusy()) basicAttack.reset();
            anim.play("death");
        } else {
            hurtAnimTimer = HURT_ANIM_SECONDS;
        }
    }

    @Override
    protected void updateBehavior(double dt, CombatWorld world) {
        // Pause does not update the player; end-of-round gameplay explicitly
        // cancels guard. Tick before input so a new stance gets its full window.
        boolean combatActive = world.isCombatActive();
        if (combatActive) defense.update(dt);
        else defense.cancel();
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
            // Keep committed swings intact. On a simultaneous S + J press,
            // guard wins and prevents a new normal attack for this stance.
            if (combatActive && basicAttack.isReady() && input.wasPressed(kBlock)) defense.activate();
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
                specialAnimTimer = SPECIAL_ANIM_SECONDS;
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
                -facing * 70, (Math.random() - 0.5) * 50, 6, skillEffectColor, 0.28, 0));
    }

    /**
     * One state machine, in priority order:
     * death &gt; hurt &gt; attack/special &gt; dash &gt; jump/fall &gt; run/walk &gt; idle.
     * Physics is never read back from the animation: only the frame index is
     * chosen here, so animation changes cannot alter movement.
     */
    private void updateAnimationState() {
        if (isDead()) {
            anim.play("death");
            return;
        }
        if (hurtAnimTimer > 0) {
            anim.play("hurt");
            return;
        }
        if (basicAttack.isBusy()) {
            anim.play("attack");
            Animation attack = anim.current();
            if (attack != null) {
                // Pin the strike frame to the hitbox window instead of letting
                // elapsed animation time drift away from the damage event.
                attack.setProgress(basicAttack.progress());
            }
            return;
        }
        if (specialAnimTimer > 0) {
            anim.play("special");
            return;
        }
        if (dashTimer > 0) {
            anim.play("dash");
            return;
        }
        if (!grounded) {
            anim.play("jump");
            syncJumpFrame();
            return;
        }
        if (Math.abs(vx) > 12) {
            // The game has one movement speed, so "walk" is what a slowed
            // character plays; normal movement is the run cycle.
            boolean running = Math.abs(vx) >= moveSpeed * 0.7;
            anim.play(running ? "run" : "walk");
        } else {
            anim.play("idle");
        }
    }

    /** Rising uses the first half of the jump row, falling the second half. */
    private void syncJumpFrame() {
        Animation jump = anim.current();
        if (jump == null || !"jump".equals(anim.currentName())) return;
        double progress;
        if (vy < 0) {
            double rising = Math.min(1.0, -vy / Math.max(1.0, jumpSpeed));
            progress = 0.5 * (1.0 - rising);
        } else {
            progress = 0.5 + 0.5 * Math.min(1.0, vy / Physics.MAX_FALL);
        }
        jump.setProgress(progress);
    }

    /** Spawns particles + floating text for a successful skill; used by subclasses. */
    protected void announceSkill(CombatWorld world) {
        world.addFloatingText(new FloatingText(centerX() - 30, y - 24, skillName, skillEffectColor, 22, 1.0));
    }

    /** Shared normal-attack dispatch for every playable character. */
    protected final void performAttack(CombatWorld world) {
        if (!defense.isBlocking()) basicAttack.start(this, facing, true, world);
    }

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

    public boolean isBlocking() { return defense.isBlocking(); }
    public double getBlockCooldownRemaining() { return defense.cooldownRemaining(); }
    public double getBlockCooldownRatio() { return defense.cooldownRatio(); }
    public String getBlockKeyLabel() { return KeyEvent.getKeyText(kBlock); }
    public void cancelBlock() { defense.cancel(); }

    @Override
    public void render(Graphics2D g) {
        boolean dead = isDead();
        String animName = anim.currentName() == null ? "idle" : anim.currentName();
        Animation current = anim.current();
        // The supplied death sheet fades the body out over its last frames, so
        // the sprite stays fully opaque while the fall plays and only dims once
        // the pose has settled.
        boolean deathSettled = dead && "death".equals(animName)
                && current != null && current.finished();
        float alpha = dead ? (deathSettled ? 0.45f : 1f) : 1f;
        if (!dead && invuln > 0 && ((int) (invuln * 22)) % 2 == 0) {
            alpha = 0.5f;
        }

        int bx = (int) x;
        int by = (int) y;
        int cx = (int) centerX();
        int feetY = (int) (y + height);

        int step = current == null ? 0 : current.frame();

        PixelArt.shadow(g, cx, feetY, (int) width, new Color(0, 0, 0, 80));

        if (skillFlash > 0) {
            int radius = (int) (Math.max(width, height) * 0.6);
            PixelArt.disc(g, cx, (int) (y + height / 2), radius,
                    new Color(skillEffectColor.getRed(), skillEffectColor.getGreen(),
                            skillEffectColor.getBlue(), 90));
        }

        PixelSprite.Mode mode = hurtFlash > 0 ? PixelSprite.Mode.FLASH
                : (slow > 0 ? PixelSprite.Mode.SLOW : PixelSprite.Mode.NORMAL);
        // Each state reads its own sprite set; the frames are already authored
        // at the 48x48 target, so scaleFor keeps them at 1x against the 44px
        // hitbox and the feet-anchored draw keeps the pivot stable.
        PixelSprite art = Art.playerAnim(characterId(), animName).frame(step);
        art.drawAnchored(g, cx, feetY, art.scaleFor((int) width), facing < 0, mode, alpha);

        if (!dead && (defense.isBlocking() || defense.isImpactVisible())) renderGuard(g, cx, by);

        g.setFont(new Font("Dialog", Font.BOLD, 12));
        g.setColor(new Color(255, 255, 255, 200));
        g.drawString("P" + (playerIndex + 1), bx + (int) width / 2 - 8, by - 6);
    }

    /** Temporary pixel shield; it never changes the sprite or hurtbox. */
    private void renderGuard(Graphics2D g, int cx, int top) {
        boolean impact = defense.isImpactVisible();
        Color color = impact ? new Color(235, 255, 255) : new Color(95, 225, 255, 210);
        Color fill = new Color(65, 200, 255, impact ? 100 : 40);
        int sx = cx + facing * ((int) width / 2 + 5);
        int sy = top + (int) height - 40;
        for (int row = 0; row < GUARD_HALF_WIDTHS.length; row++) {
            int half = GUARD_HALF_WIDTHS[row];
            int yy = sy + row * 2;
            if (row == 0 || half <= 2) {
                PixelArt.rect(g, sx - half, yy, half * 2, 2, color);
            } else {
                PixelArt.rect(g, sx - half, yy, 2, 2, color);
                PixelArt.rect(g, sx + half - 2, yy, 2, 2, color);
                PixelArt.rect(g, sx - half + 2, yy, half * 2 - 4, 2, fill);
            }
        }
        PixelArt.rect(g, sx - 1, sy + 8, 2, 14, color);
        PixelArt.rect(g, sx - 5, sy + 12, 10, 2, color);
        if (impact) {
            PixelArt.rect(g, sx - 22, sy + 14, 6, 2, color);
            PixelArt.rect(g, sx + 16, sy + 14, 6, 2, color);
            PixelArt.rect(g, sx - 1, sy - 10, 2, 6, color);
        }
    }

}

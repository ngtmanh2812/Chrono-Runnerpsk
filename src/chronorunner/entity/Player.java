package chronorunner.entity;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import chronorunner.animation.Animation;
import chronorunner.combat.Attack;
import chronorunner.combat.AttackData;
import chronorunner.combat.Hitbox;
import chronorunner.combat.DamageInfo;
import chronorunner.combat.PlayerDefense;
import chronorunner.core.InputHandler;
import chronorunner.core.Physics;
import chronorunner.gfx.Art;
import chronorunner.gfx.PixelArt;
import chronorunner.gfx.PixelSprite;
import chronorunner.gfx.SpriteSet;
import chronorunner.gfx.SpriteLayout;
import chronorunner.world.CombatWorld;

/**
 * Base class for the three playable characters. Handles movement, double jump,
 * dash, attack/skill dispatch and rendering. Subclasses supply stats, attack
 * configuration and signature skills; PlayerDefense owns the guard timers.
 */
public abstract class Player extends Character {

    public static final int PLAYER_BASE_ATTACK_DAMAGE = 30;
    private static final boolean GOD_MODE = Boolean.getBoolean("chronorunner.godMode");

    /** Opt-in for this JVM session only; normal launches keep damage enabled. */
    public static boolean isGodModeEnabled() { return GOD_MODE; }

    // The first seven entries retain the public array layout used by tooling.
    // Slots 7/8 are reserved after retiring separate Heavy/up-modifier bindings.
    public static final int[] P1_KEYS = {
            KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_W,
            KeyEvent.VK_J, KeyEvent.VK_K, KeyEvent.VK_U, KeyEvent.VK_S,
            KeyEvent.VK_UNDEFINED, KeyEvent.VK_UNDEFINED, KeyEvent.VK_L, KeyEvent.VK_I, KeyEvent.VK_O
    };
    public static final int[] P2_KEYS = {
            KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_NUMPAD0,
            KeyEvent.VK_NUMPAD1, KeyEvent.VK_NUMPAD3, KeyEvent.VK_NUMPAD4, KeyEvent.VK_DOWN,
            KeyEvent.VK_UNDEFINED, KeyEvent.VK_UNDEFINED, InputHandler.NUMPAD_ENTER, KeyEvent.VK_NUMPAD5, KeyEvent.VK_NUMPAD6
    };
    public static int[] keysFor(int playerIndex) { return playerIndex == 0 ? P1_KEYS : P2_KEYS; }
    public static final double MAX_ENERGY = 100;
    public static final double CHARGE_PER_SECOND = 18;
    private static final int[] SKILL_COST = {20, 30, 100};
    private static final String[] SKILL_CLIP = {"skill_u", "skill_i", "ultimate"};
    public final int playerIndex;

    private final InputHandler input;
    private final int kLeft, kRight, kJump, kAttack, kDash, kBlock, kCharge;
    private final int[] skillKeys;
    private final PlayerDefense defense = new PlayerDefense();
    private final Attack[] skills = new Attack[3];
    private final String[] skillNames = new String[3];
    private final double[] skillCooldowns = new double[3];
    private final double[] skillTimers = new double[3];
    private final List<Hitbox> skillHitboxes = new ArrayList<>();
    private final Attack heavyAttack = new Attack(new AttackData("Heavy", 40, 112, 76, 14, 8, 18, 120, 0));
    private final Attack launcherAttack = new Attack(new AttackData("Launcher", 35, 104, 108, 12, 7, 18, 180, -620));
    private static final double CHAIN_WINDOW_SECONDS = .60;
    private static final double ATTACK_BUFFER_SECONDS = 12.0 / 60;
    private static final double LINK_RECOVERY_SECONDS = 2.0 / 60;
    private static final int[] MELEE_ENERGY_GAIN = {5, 7, 10};
    private boolean meleeHitConfirmed;
    private int meleeStep;
    private double chainWindow, bufferedAttack;
    private DamageInfo.MeleeChain meleeChain;
    private CombatWorld actionWorld;
    private String actionClip = "light_1";
    private double energy = MAX_ENERGY;
    private boolean charging;
    private double chargeVisualTime;

    protected double moveSpeed;
    protected double jumpSpeed;
    protected double dashSpeed;
    protected double dashDuration;
    protected double dashCooldown;
    protected int maxJumps;
    protected Attack basicAttack;
    protected Color accentColor = Color.WHITE;
    protected Color skillEffectColor = new Color(68, 205, 255);

    /** Minimum time the hurt animation is held after a successful hit. */
    private static final double HURT_ANIM_SECONDS = 0.30;
    /** Two-pixel rows forming a small shield, placed beside the original sprite. */
    private static final int[] GUARD_HALF_WIDTHS = {6, 10, 12, 12, 12, 12, 12, 12, 10, 10, 8, 8, 6, 4, 2, 2};

    private int jumpsLeft;
    private double dashTimer;
    private double dashCooldownTimer;
    private double skillFlash;
    private double hurtAnimTimer;
    private double landingAnimTimer;
    private double dashVisualDuration;
    private static final double LAND_ANIM_SECONDS = .16;

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
        this.kBlock = keys[6];
        this.kCharge = keys[9];
        this.skillKeys = new int[]{keys[5], keys[10], keys[11]};
    }

    /**
     * Frame counts and default timings come from the installed sprite pack.
     * Combat, dash and airborne clips follow gameplay progress; animation never
     * changes the simulation's timing or coordinates.
     */
    @Override
    protected void setupAnimations() {
        addAnimation("idle", .12, true);
        addAnimation("walk", .08, true);
        addAnimation("run", .055, true);
        addAnimation("jump", .12, false);
        addAnimation("fall", .10, true);
        addAnimation("land", .08, false);
        addAnimation("dash", .025, false);
        addAnimation("attack", .075, false);
        addAnimation("special", .09, false);
        addAnimation("block", PlayerDefense.BLOCK_DURATION_SECONDS / 6, false);
        addAnimation("hurt", .05, false);
        addAnimation("death", .14, false);
        for (String state : new String[]{"light_1", "light_2", "light_3", "heavy", "launcher", "air_attack",
                "skill_u", "skill_i", "ultimate"}) addAnimation(state, .075, false);
        addAnimation("charge", .10, true);
    }

    private void addAnimation(String state, double seconds, boolean loop) {
        anim.add(state, Art.playerAnim(characterId(), state).animation(state, seconds, loop));
    }

    @Override
    protected void updateTimers(double dt) {
        super.updateTimers(dt);
        hurtAnimTimer = Math.max(0, hurtAnimTimer - dt);
        landingAnimTimer = Math.max(0, landingAnimTimer - dt);
        if (isDead()) {
            defense.cancel();
            interruptAction();
            anim.play("death");
        }
    }

    @Override
    public void takeDamage(DamageInfo info) {
        if (info == null || info.amount <= 0 || invuln > 0 || isDead()) return;
        if (defense.absorb(info)) return;
        if (GOD_MODE) return;
        super.takeDamage(info);
        interruptAction();
        if (isDead()) {
            defense.cancel(); hurtAnimTimer = 0; anim.play("death");
        } else hurtAnimTimer = HURT_ANIM_SECONDS;
    }

    @Override
    protected void updateBehavior(double dt, CombatWorld world) {
        boolean combatActive = world.isCombatActive();
        if (combatActive) defense.update(dt);
        else endCombat();
        Attack action = currentAction();
        if (action != null) action.update(dt);
        updateMeleeChain(dt, action);
        if (currentAction() == null) actionWorld = null;
        dashCooldownTimer = Math.max(0, dashCooldownTimer - dt);
        for (int slot = 0; slot < 3; slot++) skillTimers[slot] = Math.max(0, skillTimers[slot] - dt);
        skillFlash = Math.max(0, skillFlash - dt);

        boolean controllable = combatActive && stun <= 0 && hurtAnimTimer <= 0;
        int dir = controllable ? (input.isDown(kRight) ? 1 : 0) - (input.isDown(kLeft) ? 1 : 0) : 0;
        if (dashTimer > 0) {
            dashTimer = Math.max(0, dashTimer - dt);
            vx = dashSpeed * facing; vy = 0;
            spawnDashTrail(world);
        } else {
            if (controllable) {
                vx = dir * moveSpeed * speedMultiplier();
                if (dir != 0 && currentAction() == null) facing = dir;
            } else vx *= .86;
            vy = Physics.applyGravity(vy, dt);
        }

        if (controllable) {
            if (input.wasPressed(kJump)) {
                stopCharging(); tryJump();
            }
            if (currentAction() == null && input.wasPressed(kBlock)) {
                stopCharging();
                if (defense.activate()) clearMeleeChain();
            }
            if (canStartAction() && input.wasPressed(kDash) && dashCooldownTimer <= 0) {
                stopCharging(); clearMeleeChain(); startDash();
            }
            if (!defense.isBlocking() && dashTimer <= 0) {
                if (input.wasPressed(kAttack)) {
                    if (canStartAction() || canLinkMelee()) performAttack(world);
                    else if (meleeStep > 0 && meleeStep < 3 && isMeleeAction(currentAction()))
                        bufferedAttack = ATTACK_BUFFER_SECONDS;
                } else if (bufferedAttack > 0 && (canStartAction() || canLinkMelee())) performAttack(world);
            }
            for (int slot = 0; slot < 3; slot++) {
                if (canStartAction() && input.wasPressed(skillKeys[slot])) trySkill(slot, world);
            }
        }

        boolean neutral = controllable && grounded && dir == 0 && currentAction() == null
                && dashTimer <= 0 && !defense.isBlocking() && hurtAnimTimer <= 0;
        if (input.wasPressed(kCharge) && neutral) { clearMeleeChain(); charging = true; }
        if (!neutral || !input.isDown(kCharge)) stopCharging();
        if (charging) {
            energy = Math.min(MAX_ENERGY, energy + CHARGE_PER_SECOND * dt);
            chargeVisualTime += dt;
        }

        boolean wasAirborne = !grounded;
        Physics.moveAndCollide(this, world, dt);
        if (wasAirborne && grounded) landingAnimTimer = LAND_ANIM_SECONDS;
        if (grounded) jumpsLeft = maxJumps;
        updateAnimationState();
    }

    private boolean canStartAction() { return currentAction() == null && dashTimer <= 0; }

    private boolean isMeleeAction(Attack action) {
        return action != null && (action == basicAttack || action == heavyAttack || action == launcherAttack);
    }

    private boolean canLinkMelee() {
        Attack action = currentAction();
        if (!meleeHitConfirmed || meleeStep < 1 || meleeStep >= 3 || !isMeleeAction(action)
                || action.phase() != Attack.Phase.RECOVERY) return false;
        AttackData data = action.getData();
        double done = action.progress() * data.totalSeconds();
        return done + 1e-9 >= data.startupSeconds() + data.activeSeconds() + LINK_RECOVERY_SECONDS;
    }

    /** Called only after this player's tagged melee actually removes target HP. */
    public final void confirmMeleeHit(DamageInfo info) {
        if (isDead() || info.meleeChain == null || info.meleeChain != meleeChain
                || info.meleeStep != meleeStep || meleeHitConfirmed || meleeStep < 1 || meleeStep > 3) return;
        meleeHitConfirmed = true;
        energy = Math.min(MAX_ENERGY, energy + MELEE_ENERGY_GAIN[meleeStep - 1]);
    }

    private void updateMeleeChain(double dt, Attack previous) {
        bufferedAttack = Math.max(0, bufferedAttack - dt);
        if (meleeStep == 0) return;
        if (isMeleeAction(previous) && !previous.isBusy()) {
            if (meleeStep == 3) clearMeleeChain();
            else chainWindow = CHAIN_WINDOW_SECONDS;
        } else if (currentAction() == null) {
            chainWindow = Math.max(0, chainWindow - dt);
            if (chainWindow <= 1e-9) clearMeleeChain();
        }
    }

    private void clearMeleeChain() {
        meleeStep = 0; chainWindow = 0; bufferedAttack = 0; meleeChain = null; meleeHitConfirmed = false;
    }

    /** Discard buffered combat input on a screen transition; committed poses stay frozen. */
    public final void pauseCombatInput() { stopCharging(); clearMeleeChain(); }

    private Attack currentAction() {
        if (basicAttack != null && basicAttack.isBusy()) return basicAttack;
        if (heavyAttack.isBusy()) return heavyAttack;
        if (launcherAttack.isBusy()) return launcherAttack;
        for (Attack skill : skills) if (skill != null && skill.isBusy()) return skill;
        return null;
    }

    /** Configure one authored cast; the callback runs once at the active window. */
    protected final void configureSkill(int slot, String name, double cooldown, int startup, int active, int recovery) {
        skillNames[slot] = name; skillCooldowns[slot] = cooldown;
        skills[slot] = new Attack(new AttackData(name, 0, 0, 0, startup, active, recovery, 0), () -> {
            if (actionWorld != null && actionWorld.isCombatActive() && !isDead()) {
                skillFlash = .25;
                announceSkill(actionWorld, name);
                performSkill(slot, actionWorld);
                emitActionParticles(actionWorld, facing, slot + 2);
            }
        }, this::finishSkillActive);
    }

    private boolean trySkill(int slot, CombatWorld world) {
        if (skills[slot] == null || skillTimers[slot] > 1e-9 || energy + 1e-9 < SKILL_COST[slot]) return false;
        actionWorld = world;
        if (!skills[slot].start(this, facing, true, world)) return false;
        stopCharging(); clearMeleeChain();
        energy = Math.max(0, energy - SKILL_COST[slot]);
        skillTimers[slot] = skillCooldowns[slot];
        actionClip = SKILL_CLIP[slot];
        return true;
    }

    protected final void addSkillHitbox(CombatWorld world, Hitbox hitbox) {
        skillHitboxes.add(hitbox); world.addHitbox(hitbox);
    }

    private void finishSkillActive() {
        for (Hitbox hitbox : skillHitboxes) hitbox.cancel();
        skillHitboxes.clear();
        dashTimer = 0;
    }

    private void interruptAction() {
        stopCharging(); clearMeleeChain();
        if (basicAttack != null) basicAttack.reset();
        heavyAttack.reset(); launcherAttack.reset();
        for (Attack skill : skills) if (skill != null) skill.reset();
        finishSkillActive(); actionWorld = null;
    }

    /** Pause stops a held charge; a fresh press is needed after resuming. */
    public final void stopCharging() { charging = false; chargeVisualTime = 0; }

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
        dashVisualDuration = dashTimer;
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
     * death &gt; hurt &gt; committed action &gt; block &gt; charge &gt; dash &gt; air/land &gt; movement.
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
        Attack action = currentAction();
        if (action != null) {
            anim.play(actionClip);
            Animation attack = anim.current();
            if (attack != null) {
                // Pin the strike frame to the hitbox window instead of letting
                // elapsed animation time drift away from the damage event.
                // The bank maps anticipation/contact/recovery poses separately;
                // damage windows and input timing remain owned by Attack.
                double progress = action.animationProgress();
                SpriteLayout layout = Art.playerAnim(characterId(), actionClip).layout();
                attack.setProgress(layout == null ? progress
                        : layout.actionProgress(actionClip, progress, attack.frameCount()));
            }
            return;
        }
        if (defense.isBlocking() || defense.isImpactVisible()) {
            anim.play("block");
            anim.current().setProgress(defense.isImpactVisible() ? .55 : defense.blockProgress());
            return;
        }
        if (charging) {
            anim.play("charge");
            return;
        }
        if (dashTimer > 0) {
            anim.play("dash");
            anim.current().setProgress(1 - dashTimer / Math.max(.001, dashVisualDuration));
            return;
        }
        if (!grounded) {
            anim.play(vy < 0 ? "jump" : "fall");
            syncJumpFrame();
            return;
        }
        if (landingAnimTimer > 0) {
            anim.play("land");
            anim.current().setProgress(1 - landingAnimTimer / LAND_ANIM_SECONDS);
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

    /** Airborne poses follow velocity; the landing poses only play on contact. */
    private void syncJumpFrame() {
        Animation jump = anim.current();
        if (jump == null) return;
        double progress;
        if (vy < 0) {
            double rising = Math.min(1.0, -vy / Math.max(1.0, jumpSpeed));
            progress = (1 + Math.min(1, (1 - rising) * 2)) / jump.frameCount();
            SpriteLayout layout = Art.playerAnim(characterId(), "jump").layout();
            if (layout != null) {
                progress = layout.frameProgress("jump", 1 - rising, jump.frameCount(), progress);
            }
        } else {
            progress = Math.min(1.0, vy / Physics.MAX_FALL);
        }
        jump.setProgress(progress);
    }

    protected void announceSkill(CombatWorld world, String name) {
        world.addFloatingText(new FloatingText(centerX() - 30, y - 24, name, skillEffectColor, 20, 1.0));
    }

    protected final void performAttack(CombatWorld world) {
        if (defense.isBlocking()) return;
        Attack previous = currentAction();
        if (previous != null) {
            if (!canLinkMelee()) return;
            previous.reset();
        }
        stopCharging();
        if (meleeStep == 0) meleeChain = new DamageInfo.MeleeChain();
        meleeStep++;
        Attack attack = meleeStep == 1 ? basicAttack : meleeStep == 2 ? heavyAttack : launcherAttack;
        actionClip = meleeStep == 1 ? (grounded ? "light_1" : "air_attack")
                : meleeStep == 2 ? "heavy" : "launcher";
        bufferedAttack = 0; chainWindow = 0;
        meleeHitConfirmed = false;
        attack.start(this, facing, true, world, meleeChain, meleeStep);
    }

    protected abstract void performSkill(int slot, CombatWorld world);

    /** Called exactly once when a committed player melee hitbox opens. */
    public final void onMeleeActive(CombatWorld world, int direction, int step) {
        if (isDead() || !world.isCombatActive()) return;
        emitActionParticles(world, direction, Math.max(1, step));
        emitMeleeTrail(world, direction, step);
    }

    /** Optional authored trail; the shared particles never own a damage region. */
    protected void emitMeleeTrail(CombatWorld world, int direction, int step) { }

    private void emitActionParticles(CombatWorld world, int direction, int force) {
        int count=6+Math.min(4,force)*2;
        double startX=centerX()+direction*(width*.5+10), startY=centerY()-5;
        for(int i=0;i<count;i++) {
            double spread=(i-(count-1)/2.0)*6;
            Color identity = characterId().equals("do_mixi") ? accentColor : skillEffectColor;
            Color tint=i%3==0?new Color(220,250,255):identity;
            world.addParticle(new Particle(startX+direction*(i%4)*8,startY+spread,
                    direction*(90+i*11+force*12),spread*2-40,2+i%2,tint,.16+(i%4)*.035,0));
        }
        if(grounded && force>=2) for(int i=0;i<4;i++) {
            world.addParticle(new Particle(centerX()-direction*(6+i*5),bottom()-3,
                    -direction*(35+i*18),-25-i*8,3,new Color(125,132,142),.20,.35));
        }
    }
    public abstract String characterId();
    public String getSkillName() { return getSkillName(0); }
    public String getSkillName(int slot) { return skillNames[slot]; }
    public double getEnergy() { return energy; }
    public double getEnergyRatio() { return energy / MAX_ENERGY; }
    public boolean isCharging() { return charging; }
    public int getSkillCost(int slot) { return SKILL_COST[slot]; }
    public String getSkillKeyLabel(int slot) {
        if (playerIndex == 1) return "N" + (slot + 4);
        return KeyEvent.getKeyText(skillKeys[slot]);
    }
    public double getSkillCooldownRatio() { return getSkillCooldownRatio(0); }
    public double getSkillCooldownRatio(int slot) {
        return skillCooldowns[slot] <= 0 ? 1 : 1 - skillTimers[slot] / skillCooldowns[slot];
    }
    public double getSkillCooldownRemaining(int slot) { return skillTimers[slot]; }
    public String getChargeKeyLabel() { return playerIndex == 0 ? "L" : "NUM ENTER"; }
    public double getDashCooldownRatio() {
        return dashCooldown <= 0 ? 1 : 1 - Math.max(0, dashCooldownTimer) / dashCooldown;
    }
    public boolean isBlocking() { return defense.isBlocking(); }
    public double getBlockCooldownRemaining() { return defense.cooldownRemaining(); }
    public double getBlockCooldownRatio() { return defense.cooldownRatio(); }
    public String getBlockKeyLabel() { return KeyEvent.getKeyText(kBlock); }
    public void cancelBlock() { defense.cancel(); }
    public void endCombat() {
        defense.cancel(); interruptAction();
        landingAnimTimer = 0; vx = 0;
        if (!isDead()) anim.play("idle");
    }

    @Override
    public void render(Graphics2D g) {
        boolean dead = isDead();
        String animName = anim.currentName() == null ? "idle" : anim.currentName();
        Animation current = anim.current();
        // Keep the fall fully opaque, then dim the settled corpse.
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

        if (charging) {
            int pulse = (int)(chargeVisualTime * 12) % 4;
            PixelArt.shadow(g, cx, feetY, 52 + pulse * 4, new Color(68, 205, 255, 150));
            for (int i = 0; i < 6; i++) {
                int yy = feetY - ((int)(chargeVisualTime * 70) + i * 13) % 76;
                PixelArt.rect(g, cx + (i % 2 == 0 ? -31 : 29), yy, 3, 7, skillEffectColor);
            }
        }
        if (skillFlash > 0) {
            int radius = (int) (Math.max(width, height) * 0.6);
            PixelArt.disc(g, cx, (int) (y + height / 2), radius,
                    new Color(skillEffectColor.getRed(), skillEffectColor.getGreen(),
                            skillEffectColor.getBlue(), 90));
        }

        PixelSprite.Mode mode = hurtFlash > 0 ? PixelSprite.Mode.FLASH
                : (slow > 0 ? PixelSprite.Mode.SLOW : PixelSprite.Mode.NORMAL);
        SpriteSet sprites = Art.playerAnim(characterId(), animName);
        PixelSprite art = sprites.frame(step);
        if (sprites.layout() != null) {
            sprites.layout().draw(art, g, cx, feetY, (int)centerY(), facing < 0, mode, alpha);
        } else art.drawAnchored(g, cx, feetY, art.scaleFor((int) width), facing < 0, mode, alpha);

        if (!dead && (defense.isBlocking() || defense.isImpactVisible())
                && (sprites.layout() == null || !"block".equals(animName))) renderGuard(g, cx, by);

        g.setFont(new Font("Dialog", Font.BOLD, 12));
        g.setColor(new Color(255, 255, 255, 200));
        g.drawString("P" + (playerIndex + 1), bx + (int) width / 2 - 8, by - 6);
    }

    /** Fallback for older packs without an authored guard animation. */
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

package chronorunner.combat;

import chronorunner.entity.GameObject;
import chronorunner.util.Rect;
import chronorunner.world.CombatWorld;

/**
 * A melee attack built from {@link AttackData}, played out as a
 * Startup -&gt; Active -&gt; Recovery state machine.
 *
 * <p>{@link #start} only winds the attack up; the hitbox is created later, when
 * the startup window ends. Between those two moments the attacker is committed
 * to the move ({@link #isBusy}), so the damage never lands on the frame the key
 * was pressed.
 */
public class Attack {

    /** Which window of the attack is currently playing. */
    public enum Phase {
        READY, STARTUP, ACTIVE, RECOVERY
    }

    /**
     * Slack allowed when comparing accumulated time to a window length. The loop
     * is variable-dt, so a window of N frames arrives as the sum of N dt's and
     * can land a few ULPs short of N/60 (0.1 / 60 * 6 == 0.09999999999999999).
     * Without this the attack would visibly slip a frame per window.
     */
    private static final double EPS = 1e-9;

    private final AttackData data;
    private final Runnable activeEffect;
    private final Runnable activeEnd;
    private Phase phase = Phase.READY;
    private double elapsed;
    /** Seconds finished by earlier windows of the current swing. */
    private double completed;

    private GameObject attacker;
    private int facing;
    private boolean friendly;
    private CombatWorld world;

    /**
     * The rect handed to the hitbox on the startup -&gt; active transition. Both
     * the damaging hitbox and the on-screen swing read this, so the telegraph
     * cannot drift away from what actually deals damage if the attacker keeps
     * moving mid-swing.
     */
    private Rect activeRect;
    private Hitbox activeHitbox;
    private DamageInfo.MeleeChain meleeChain;
    private int meleeStep;

    public Attack(AttackData data) {
        this(data, null, null);
    }

    /** Optional effects share the same windows as melee (boss volleys/dashes). */
    public Attack(AttackData data, Runnable activeEffect, Runnable activeEnd) {
        this.data = data;
        this.activeEffect = activeEffect;
        this.activeEnd = activeEnd;
    }

    public AttackData getData() {
        return data;
    }

    public Phase phase() {
        return phase;
    }

    /** True when the attacker may begin this attack. */
    public boolean isReady() {
        return phase == Phase.READY;
    }

    /** True while the move is committed, i.e. startup, active or recovery. */
    public boolean isBusy() {
        return phase != Phase.READY;
    }

    /** True only during the window that actually deals damage. */
    public boolean isActive() {
        return phase == Phase.ACTIVE;
    }

    /** True during the wind-up, before the hitbox appears. */
    public boolean isStartup() {
        return phase == Phase.STARTUP;
    }

    /**
     * 0..1 progress through startup + active + recovery. The attack animation
     * is pinned to this value so its strike frame lands on the active window.
     */
    public double progress() {
        double total = data.totalSeconds();
        if (total <= 0) return 0;
        double done = completed + (phase == Phase.READY ? 0 : elapsed);
        return Math.max(0, Math.min(1, done / total));
    }

    /** Equal thirds for the authored wind-up/strike/recovery pose groups. */
    public double animationProgress() {
        if (phase == Phase.READY) return 1;
        double local = Math.max(0, Math.min(1, elapsed / Math.max(EPS, phaseSeconds())));
        return (phase.ordinal() - 1 + local) / 3;
    }

    /** A motion-driven move may recover early when it reaches its landing. */
    public void completeActive() {
        if (phase != Phase.ACTIVE) return;
        completed = data.startupSeconds() + data.activeSeconds();
        elapsed = 0;
        advance();
    }

    /**
     * Winds the attack up in front of {@code attacker}. The hitbox is spawned
     * later, once {@link AttackData#startupFrames} have elapsed.
     *
     * @return false if the previous attack has not finished yet.
     */
    public boolean start(GameObject attacker, int facing, boolean friendly, CombatWorld world) {
        return start(attacker, facing, friendly, world, null, 0);
    }

    public boolean start(GameObject attacker, int facing, boolean friendly, CombatWorld world,
                         DamageInfo.MeleeChain chain, int step) {
        if (!isReady()) return false;
        this.attacker = attacker;
        this.facing = facing;
        this.friendly = friendly;
        this.world = world;
        this.phase = Phase.STARTUP;
        this.elapsed = 0;
        this.completed = 0;
        this.meleeChain = chain;
        this.meleeStep = step;
        return true;
    }

    public void update(double dt) {
        if (phase == Phase.READY) return;
        elapsed += dt;
        // Carry surplus time into the next window so a dropped frame does not
        // stretch the attack.
        while (phase != Phase.READY && elapsed >= phaseSeconds() - EPS) {
            double consumed = phaseSeconds();
            elapsed -= consumed;
            completed += consumed;
            advance();
        }
    }

    /**
     * The damage region of this attack, or null when nothing should be drawn.
     *
     * <p>During startup this tracks the attacker so the wind-up telegraph stays
     * attached to the body. During the active window it returns the frozen rect
     * the hitbox was built from, so the swing graphic and the damage agree
     * exactly. Recovery draws nothing.
     */
    public Rect region() {
        if (phase == Phase.STARTUP) return computeRect();
        if (phase == Phase.ACTIVE) return activeRect;
        return null;
    }

    /** Cancels any attack in progress. */
    public void reset() {
        if (phase == Phase.ACTIVE && activeEnd != null) activeEnd.run();
        if (activeHitbox != null) activeHitbox.cancel();
        activeHitbox = null;
        phase = Phase.READY;
        elapsed = 0;
        completed = 0;
        attacker = null;
        world = null;
        activeRect = null;
        meleeChain = null; meleeStep = 0;
    }

    private double phaseSeconds() {
        switch (phase) {
            case STARTUP: return data.startupSeconds();
            case ACTIVE: return data.activeSeconds();
            case RECOVERY: return data.recoverySeconds();
            default: return Double.MAX_VALUE;
        }
    }

    private void advance() {
        switch (phase) {
            case STARTUP:
                phase = Phase.ACTIVE;
                activeRect = computeRect();
                if (activeEffect == null) spawnHitbox();
                else activeEffect.run();
                break;
            case ACTIVE:
                if (activeEnd != null) activeEnd.run();
                if (activeHitbox != null) activeHitbox.cancel();
                activeHitbox = null;
                phase = Phase.RECOVERY;
                activeRect = null;
                break;
            case RECOVERY:
                phase = Phase.READY;
                attacker = null;
                world = null;
                activeRect = null;
                meleeChain = null; meleeStep = 0;
                break;
            default:
                break;
        }
    }

    /** Damage rect in front of the attacker, in the direction it is facing. */
    private Rect computeRect() {
        double x = facing >= 0 ? attacker.x + attacker.width : attacker.x - data.reach;
        return new Rect(x, attacker.y + (attacker.height - data.height) / 2.0,
                data.reach, data.height);
    }

    private void spawnHitbox() {
        if (world == null || attacker == null) {
            activeRect = null;
            return;
        }
        DamageInfo info = new DamageInfo(data.damage, data.knockback * facing, data.knockbackY, friendly)
                .withMeleeChain(meleeChain, meleeStep);
        activeHitbox = new Hitbox(activeRect, info, data.activeSeconds(), friendly, attacker);
        world.addHitbox(activeHitbox);
    }
}

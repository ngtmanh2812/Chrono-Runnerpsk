package chronorunner.combat;

/** Immutable description of a single hit. */
public class DamageInfo {

    /** Unique identity for one player's three committed melee moves. */
    public static final class MeleeChain { }

    /** Presentation identity travels with a hit; it never changes damage rules. */
    public enum Feedback {
        NONE, PHAN_SWORD_LIGHT, PHAN_SWORD_MEDIUM, PHAN_SWORD_ULTIMATE,
        DO_MIXI_JAR, DO_MIXI_SMOKE, DO_MIXI_CALL
    }

    public final int amount;
    public final double knockbackX;
    public final double knockbackY;
    public final boolean fromPlayer;
    /** Seconds of slow applied on hit (0 = none). Used by Frost Paradox. */
    public final double slowSeconds;
    public final MeleeChain meleeChain;
    public final int meleeStep;
    public final Feedback feedback;
    public final double stunSeconds;

    public DamageInfo(int amount, double knockbackX, double knockbackY, boolean fromPlayer) {
        this(amount, knockbackX, knockbackY, fromPlayer, 0);
    }

    public DamageInfo(int amount, double knockbackX, double knockbackY, boolean fromPlayer, double slowSeconds) {
        this(amount, knockbackX, knockbackY, fromPlayer, slowSeconds, null, 0, Feedback.NONE, 0);
    }

    private DamageInfo(int amount, double knockbackX, double knockbackY, boolean fromPlayer,
                       double slowSeconds, MeleeChain meleeChain, int meleeStep, Feedback feedback, double stunSeconds) {
        this.amount = amount;
        this.knockbackX = knockbackX;
        this.knockbackY = knockbackY;
        this.fromPlayer = fromPlayer;
        this.slowSeconds = slowSeconds;
        this.meleeChain = meleeChain;
        this.meleeStep = meleeStep;
        this.feedback = feedback;
        this.stunSeconds = stunSeconds;
    }

    public DamageInfo withMeleeChain(MeleeChain chain, int step) {
        if (chain == null || !fromPlayer || step < 1 || step > 3) return this;
        return new DamageInfo(amount, knockbackX, knockbackY, fromPlayer, slowSeconds, chain, step, feedback, stunSeconds);
    }

    /** Adds optional hit feedback while preserving ownership, slow and chain ID. */
    public DamageInfo withFeedback(Feedback value) {
        Feedback selected = value == null ? Feedback.NONE : value;
        if (selected == feedback) return this;
        return new DamageInfo(amount, knockbackX, knockbackY, fromPlayer, slowSeconds,
                meleeChain, meleeStep, selected, stunSeconds);
    }

    public DamageInfo withStun(double seconds) {
        return new DamageInfo(amount, knockbackX, knockbackY, fromPlayer, slowSeconds,
                meleeChain, meleeStep, feedback, Math.max(0, Math.min(2, seconds)));
    }
}

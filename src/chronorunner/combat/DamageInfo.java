package chronorunner.combat;

/** Immutable description of a single hit. */
public class DamageInfo {

    /** Unique identity for one player's three committed melee moves. */
    public static final class MeleeChain { }

    public final int amount;
    public final double knockbackX;
    public final double knockbackY;
    public final boolean fromPlayer;
    /** Seconds of slow applied on hit (0 = none). Used by Frost Paradox. */
    public final double slowSeconds;
    public final MeleeChain meleeChain;
    public final int meleeStep;

    public DamageInfo(int amount, double knockbackX, double knockbackY, boolean fromPlayer) {
        this(amount, knockbackX, knockbackY, fromPlayer, 0);
    }

    public DamageInfo(int amount, double knockbackX, double knockbackY, boolean fromPlayer, double slowSeconds) {
        this(amount, knockbackX, knockbackY, fromPlayer, slowSeconds, null, 0);
    }

    private DamageInfo(int amount, double knockbackX, double knockbackY, boolean fromPlayer,
                       double slowSeconds, MeleeChain meleeChain, int meleeStep) {
        this.amount = amount;
        this.knockbackX = knockbackX;
        this.knockbackY = knockbackY;
        this.fromPlayer = fromPlayer;
        this.slowSeconds = slowSeconds;
        this.meleeChain = meleeChain;
        this.meleeStep = meleeStep;
    }

    public DamageInfo withMeleeChain(MeleeChain chain, int step) {
        if (chain == null || !fromPlayer || step < 1 || step > 3) return this;
        return new DamageInfo(amount, knockbackX, knockbackY, fromPlayer, slowSeconds, chain, step);
    }
}

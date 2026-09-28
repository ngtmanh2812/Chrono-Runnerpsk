package chronorunner.combat;

/** Immutable description of a single hit. */
public class DamageInfo {

    public final int amount;
    public final double knockbackX;
    public final double knockbackY;
    public final boolean fromPlayer;
    /** Seconds of slow applied on hit (0 = none). Used by Frost Paradox. */
    public final double slowSeconds;

    public DamageInfo(int amount, double knockbackX, double knockbackY, boolean fromPlayer) {
        this(amount, knockbackX, knockbackY, fromPlayer, 0);
    }

    public DamageInfo(int amount, double knockbackX, double knockbackY, boolean fromPlayer, double slowSeconds) {
        this.amount = amount;
        this.knockbackX = knockbackX;
        this.knockbackY = knockbackY;
        this.fromPlayer = fromPlayer;
        this.slowSeconds = slowSeconds;
    }
}

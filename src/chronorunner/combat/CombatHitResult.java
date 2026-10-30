package chronorunner.combat;

/** Actual damage resolution, separate from geometric overlap. */
public record CombatHitResult(Kind kind, int damage) {
    public enum Kind { HIT, BLOCK, IMMUNE, NONE }
    public static final CombatHitResult BLOCK = new CombatHitResult(Kind.BLOCK, 0);
    public static final CombatHitResult IMMUNE = new CombatHitResult(Kind.IMMUNE, 0);
    public static final CombatHitResult NONE = new CombatHitResult(Kind.NONE, 0);
    public boolean landed() { return kind == Kind.HIT && damage > 0; }
    public boolean blocked() { return kind == Kind.BLOCK; }
    public boolean consumed() { return landed() || blocked(); }
}

package chronorunner.combat;

/** Per-player, one-hit guard. Timers advance only with the gameplay simulation. */
public final class PlayerDefense {
    public static final double BLOCK_DURATION_SECONDS = 0.5;
    public static final double BLOCK_COOLDOWN_SECONDS = 2.0;
    private static final double IMPACT_SECONDS = 0.18;
    private static final double EPS = 1e-9;

    private double remaining;
    private double cooldown;
    private double impact;

    public boolean activate() {
        if (isBlocking() || cooldown > 0) return false;
        remaining = BLOCK_DURATION_SECONDS;
        cooldown = BLOCK_COOLDOWN_SECONDS;
        return true;
    }

    public void update(double dt) {
        remaining = tick(remaining, dt);
        cooldown = tick(cooldown, dt);
        impact = tick(impact, dt);
    }

    private static double tick(double timer, double dt) {
        return timer - dt < EPS ? 0 : timer - dt;
    }

    /** Called only after the player's dead/invulnerable checks. */
    public boolean absorb(DamageInfo hit) {
        if (!isBlocking() || hit == null || hit.amount <= 0 || hit.fromPlayer) return false;
        remaining = 0;
        impact = IMPACT_SECONDS;
        return true;
    }

    /** Ends a stance on death or round end without making its cooldown reusable. */
    public void cancel() {
        remaining = 0;
        impact = 0;
    }

    public boolean isBlocking() { return remaining > 0; }
    public double cooldownRemaining() { return cooldown; }
    public double cooldownRatio() { return 1 - cooldown / BLOCK_COOLDOWN_SECONDS; }
    public boolean isImpactVisible() { return impact > 0; }
}

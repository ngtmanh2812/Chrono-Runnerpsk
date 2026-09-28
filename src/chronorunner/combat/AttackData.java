package chronorunner.combat;

/**
 * Tuning values for one attack.
 *
 * <p>An attack is played out as three consecutive windows measured in frames at
 * {@value #FPS} FPS:
 *
 * <pre>
 * attack
 *  |
 *  +-- startup   wind-up. No hitbox exists yet.
 *  |             Frames 0 .. startupFrames-1
 *  +-- active    The hitbox exists and deals damage.
 *  |             Frames startupFrames .. +activeFrames-1
 *  +-- recovery  Hitbox is gone and the attacker cannot act yet.
 *                Frames +activeFrames .. +recoveryFrames-1
 * </pre>
 *
 * <p>Because the damage only lands once the startup frames have elapsed,
 * pressing attack does not deal damage immediately. The frame numbers are
 * durations, so an attack with {@code startupFrames = 9} is wind-up for frames
 * 0-8 and its hitbox appears on frame 9.
 */
public class AttackData {

    /** Frame rate the windows are expressed in. */
    public static final int FPS = 60;

    public final String name;
    public final int damage;
    public final double reach;
    public final double height;
    public final int startupFrames;
    public final int activeFrames;
    public final int recoveryFrames;
    public final double knockback;

    public AttackData(String name, int damage, double reach, double height,
                      int startupFrames, int activeFrames, int recoveryFrames,
                      double knockback) {
        this.name = name;
        this.damage = damage;
        this.reach = reach;
        this.height = height;
        this.startupFrames = Math.max(0, startupFrames);
        this.activeFrames = Math.max(1, activeFrames);
        this.recoveryFrames = Math.max(0, recoveryFrames);
        this.knockback = knockback;
    }

    public int totalFrames() {
        return startupFrames + activeFrames + recoveryFrames;
    }

    public double startupSeconds() {
        return startupFrames / (double) FPS;
    }

    public double activeSeconds() {
        return activeFrames / (double) FPS;
    }

    public double recoverySeconds() {
        return recoveryFrames / (double) FPS;
    }

    public double totalSeconds() {
        return totalFrames() / (double) FPS;
    }
}

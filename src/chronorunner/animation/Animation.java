package chronorunner.animation;

/**
 * A time driven sprite counter. Non-looping clips hold their final pose;
 * combat and airborne states can pin progress to the simulation.
 */
public class Animation {

    private final int frames;
    private final double frameDuration;
    private final boolean loop;
    private double time;

    public Animation(int frames, double frameDuration, boolean loop) {
        this.frames = Math.max(1, frames);
        this.frameDuration = Math.max(0.01, frameDuration);
        this.loop = loop;
    }

    public void update(double dt) {
        time += dt;
        double total = total();
        if (loop) {
            time %= total;
        } else {
            time = Math.min(time, total);
        }
    }

    public int frame() {
        // A pinned phase boundary can divide to 1.9999999999999998.
        return Math.min(frames - 1, (int) (time / frameDuration + 1e-9));
    }

    /** Number of frames this animation was authored with. */
    public int frameCount() {
        return frames;
    }

    /** 0..1 progress through the whole animation. */
    public double progress() {
        return Math.min(1, time / total());
    }

    /**
     * Pins the animation to a 0..1 progress mark. Used by states whose sprite
     * frame must follow gameplay instead of wall-clock time, such as the attack
     * animation following {@link chronorunner.combat.Attack#progress()}.
     */
    public void setProgress(double progress) {
        double clamped = Math.max(0, Math.min(1, progress));
        time = clamped * total();
    }

    public boolean finished() {
        return time >= total();
    }

    public void reset() {
        time = 0;
    }

    private double total() {
        return frames * frameDuration;
    }
}

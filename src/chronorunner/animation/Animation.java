package chronorunner.animation;

/**
 * A time driven frame counter. The MVP draws characters with procedural shapes,
 * so an animation is used to drive visual steps (walk bob, attack flash, ...)
 * rather than sprite frames.
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
        return Math.min(frames - 1, (int) (time / frameDuration));
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

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

    /** 0..1 progress through the whole animation. */
    public double progress() {
        return Math.min(1, time / total());
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

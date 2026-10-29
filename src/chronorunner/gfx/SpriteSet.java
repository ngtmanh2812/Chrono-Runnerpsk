package chronorunner.gfx;

import chronorunner.animation.Animation;

/** A sprite plus its animation frames. Frame indices wrap in both directions. */
public final class SpriteSet {

    private final PixelSprite[] frames;
    private final SpriteLayout layout;

    public SpriteSet(PixelSprite... frames) {
        this(null, frames);
    }

    public SpriteSet(SpriteLayout layout, PixelSprite... frames) {
        if (frames.length == 0) {
            throw new IllegalArgumentException("sprite set needs at least one frame");
        }
        this.frames = frames;
        this.layout = layout;
        if (layout != null) for (PixelSprite frame : frames) {
            if (frame.width() != layout.width || frame.height() != layout.height) {
                throw new IllegalArgumentException("Sprite frame does not match its layout");
            }
        }
    }

    public SpriteLayout layout() { return layout; }

    /** Count comes from the actual files; authored timing remains visual only. */
    public Animation animation(String state, double seconds, boolean loop) {
        return new Animation(count(), layout == null ? seconds : layout.seconds(state, seconds),
                layout == null ? loop : layout.loops(state, loop));
    }

    public int count() {
        return frames.length;
    }

    public PixelSprite frame(int index) {
        int i = index % frames.length;
        if (i < 0) i += frames.length;
        return frames[i];
    }
}

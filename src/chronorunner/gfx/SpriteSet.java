package chronorunner.gfx;

/** A sprite plus its animation frames. Frame indices wrap in both directions. */
public final class SpriteSet {

    private final PixelSprite[] frames;

    public SpriteSet(PixelSprite... frames) {
        if (frames.length == 0) {
            throw new IllegalArgumentException("sprite set needs at least one frame");
        }
        this.frames = frames;
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

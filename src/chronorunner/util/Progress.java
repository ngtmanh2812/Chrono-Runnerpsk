package chronorunner.util;

/** Persistent player progress and options. */
public class Progress {

    public static final int TOTAL_CHAPTERS = 5;

    private int unlockedChapters = 1;
    private boolean screenShake = true;

    public int getUnlockedChapters() {
        return unlockedChapters;
    }

    public void unlockThrough(int chapter) {
        unlockedChapters = Math.max(unlockedChapters, Collision.clampInt(chapter, 1, TOTAL_CHAPTERS));
    }

    public boolean isUnlocked(int chapter) {
        return chapter <= unlockedChapters;
    }

    public boolean isScreenShake() {
        return screenShake;
    }

    public void setScreenShake(boolean value) {
        this.screenShake = value;
    }
}

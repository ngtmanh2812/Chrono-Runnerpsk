package chronorunner.core;

/** Mutable data describing the run currently being played (not persisted). */
public class GameSession {
    public int playerCount = 1;
    public int chapter = 1;
    /** Selected character index per player slot (0 = Phan Si Kien, 1 = Ronaldo, 2 = Do Mixi). */
    public final int[] characterIndex = {0, 2};

    public void resetForNewRun() {
        chapter = 1;
        characterIndex[0] = 0;
        characterIndex[1] = 2;
    }
}

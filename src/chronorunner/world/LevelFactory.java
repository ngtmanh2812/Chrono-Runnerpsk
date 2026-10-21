package chronorunner.world;

import chronorunner.entity.boss.TimeGuardian;
import chronorunner.entity.boss.TheVoidCore;
import chronorunner.core.Game;

/** Builds one of the two campaign chapter arenas. */
public final class LevelFactory {

    private static final double GROUND_Y = 656;

    private LevelFactory() {
    }

    public static Level create(int chapter) {
        return chapter == 2 ? future() : past();
    }

    private static Level past() {
        ArenaBackdrop art = new ArenaBackdrop("assets/backgrounds/chapter_1.png", Game.HEIGHT);
        // Measured on the unmodified 1920x819 PNG, in source-image pixels.
        // Each rectangle covers the flat stone/grass cap, not the hanging rock.
        int[][] caps = {
                {0, 290, 314}, {438, 348, 177}, {253, 435, 141},
                {582, 466, 174}, {0, 536, 350},
                {1607, 290, 313}, {1307, 348, 177}, {1535, 435, 139},
                {1166, 466, 175}, {1572, 536, 348}
        };
        double ground = art.toWorld(638);
        TimeGuardian boss = new TimeGuardian(art.toWorld(1440), 0);
        boss.y = ground - boss.height;
        boss.grounded = true;
        LevelBuilder builder = new LevelBuilder().chapter(1).backdrop(art)
                .groundY(ground).addGround().boss(boss)
                .spawns(art.toWorld(420), art.toWorld(510));
        for (int[] cap : caps) {
            builder.addPlatform(art.toWorld(cap[0]), art.toWorld(cap[1]),
                    art.toWorld(cap[2]), art.toWorld(12));
        }
        return builder.build();
    }

    private static Level future() {
        ArenaBackdrop art = new ArenaBackdrop("assets/backgrounds/chapter_2.png", Game.HEIGHT);
        if (art.sourceWidth() != 1913 || art.sourceHeight() != 822) {
            throw new IllegalStateException("Chapter 2 artwork changed; remeasure its platform caps");
        }
        // Coordinates measured on Shadowed Ruins Over a Sunlit Canal.png.
        // Only the slab tops collide. Cars, containers, fences and the broken
        // structures below the main highway remain background decorations.
        int[][] caps = {
                {0, 155, 241}, {255, 243, 185}, {0, 319, 278},
                {142, 415, 249}, {611, 397, 242},
                {1533, 243, 77}, {1671, 319, 242}, {1588, 431, 178},
                {1090, 397, 226}
        };
        double ground = art.toWorld(568);
        // Airborne start above the right suspended slab. Keep the core's
        // existing center pivot, gravity, teleport/dive and attack behavior.
        TheVoidCore boss = new TheVoidCore(art.toWorld(1260), art.toWorld(170));
        LevelBuilder builder = new LevelBuilder().chapter(2).backdrop(art)
                .groundY(ground).addGround().boss(boss)
                .spawns(art.toWorld(650), art.toWorld(730));
        for (int[] cap : caps) {
            builder.addPlatform(art.toWorld(cap[0]), art.toWorld(cap[1]),
                    art.toWorld(cap[2]), art.toWorld(12));
        }
        return builder.build();
    }
}

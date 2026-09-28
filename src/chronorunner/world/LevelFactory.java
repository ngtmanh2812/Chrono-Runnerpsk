package chronorunner.world;

import chronorunner.entity.boss.ChronoBeast;
import chronorunner.entity.boss.FrostParadox;
import chronorunner.entity.boss.TemporalKnight;
import chronorunner.entity.boss.TheParadox;
import chronorunner.entity.boss.TimeGuardian;
import chronorunner.entity.enemy.ChronoBat;
import chronorunner.entity.enemy.TimeSlime;

/** Builds the arena for a given chapter. */
public final class LevelFactory {

    private static final double GROUND_Y = 656;

    private LevelFactory() {
    }

    public static Level create(int chapter) {
        switch (chapter) {
            case 2:
                return forgottenForest();
            case 3:
                return ancientRuins();
            case 4:
                return frozenFuture();
            case 5:
                return chronoCore();
            default:
                return brokenCity();
        }
    }

    private static Level brokenCity() {
        return new LevelBuilder().chapter(1).groundY(GROUND_Y).addGround()
                .addPlatform(520, 470, 220, 24)
                .addPlatform(1080, 380, 220, 24)
                .addPlatform(760, 540, 180, 24)
                .addEnemy(new TimeSlime(1180, 560))
                .boss(new TimeGuardian(1480, 500))
                .spawns(360, 520)
                .build();
    }

    private static Level forgottenForest() {
        return new LevelBuilder().chapter(2).groundY(GROUND_Y).addGround()
                .addPlatform(420, 480, 200, 24)
                .addPlatform(900, 420, 200, 24)
                .addPlatform(1320, 500, 220, 24)
                .addEnemy(new TimeSlime(700, 520))
                .addEnemy(new TimeSlime(1000, 520))
                .boss(new ChronoBeast(1480, 500))
                .spawns(360, 520)
                .build();
    }

    private static Level ancientRuins() {
        return new LevelBuilder().chapter(3).groundY(GROUND_Y).addGround()
                .addPlatform(400, 440, 180, 24)
                .addPlatform(760, 340, 180, 24)
                .addPlatform(1140, 440, 180, 24)
                .addPlatform(1500, 520, 200, 24)
                .addEnemy(new TimeSlime(880, 560))
                .addEnemy(new ChronoBat(1240, 280))
                .addEnemy(new TimeSlime(1500, 460))
                .boss(new TemporalKnight(1480, 500))
                .spawns(360, 520)
                .build();
    }

    private static Level frozenFuture() {
        return new LevelBuilder().chapter(4).groundY(GROUND_Y).addGround()
                .addPlatform(500, 460, 220, 24)
                .addPlatform(1000, 360, 220, 24)
                .addPlatform(1400, 480, 200, 24)
                .addEnemy(new ChronoBat(620, 300))
                .addEnemy(new TimeSlime(1080, 560))
                .addEnemy(new ChronoBat(1180, 260))
                .boss(new FrostParadox(1480, 500))
                .spawns(360, 520)
                .build();
    }

    private static Level chronoCore() {
        return new LevelBuilder().chapter(5).groundY(GROUND_Y).addGround()
                .addPlatform(380, 470, 180, 24)
                .addPlatform(700, 370, 160, 24)
                .addPlatform(1020, 300, 160, 24)
                .addPlatform(1360, 400, 180, 24)
                .addPlatform(1600, 500, 200, 24)
                .addEnemy(new ChronoBat(1200, 260))
                .addEnemy(new ChronoBat(1300, 340))
                .boss(new TheParadox(1480, 500))
                .spawns(360, 520)
                .build();
    }
}

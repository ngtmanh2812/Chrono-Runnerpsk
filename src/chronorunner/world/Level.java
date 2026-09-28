package chronorunner.world;

import java.util.List;

import chronorunner.entity.boss.Boss;
import chronorunner.entity.enemy.Enemy;
import chronorunner.obstacle.Platform;

/** One boss arena: platforms, theme, spawn points and combatants. */
public class Level {

    private final int chapter;
    private final TileType tile;
    private final double width;
    private final double height;
    private final double groundY;
    private final List<Platform> platforms;
    private final Boss boss;
    private final List<Enemy> enemies;
    private final double[] playerSpawns;

    Level(int chapter, TileType tile, double width, double height, double groundY,
          List<Platform> platforms, Boss boss, List<Enemy> enemies, double[] playerSpawns) {
        this.chapter = chapter;
        this.tile = tile;
        this.width = width;
        this.height = height;
        this.groundY = groundY;
        this.platforms = platforms;
        this.boss = boss;
        this.enemies = enemies;
        this.playerSpawns = playerSpawns;
    }

    public int chapter() { return chapter; }

    public TileType tile() { return tile; }

    public double width() { return width; }

    public double height() { return height; }

    public double groundY() { return groundY; }

    public List<Platform> platforms() { return platforms; }

    public Boss boss() { return boss; }

    public List<Enemy> enemies() { return enemies; }

    public double playerSpawn(int index) {
        return playerSpawns[Math.min(index, playerSpawns.length - 1)];
    }
}

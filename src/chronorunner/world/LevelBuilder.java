package chronorunner.world;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import chronorunner.entity.boss.Boss;
import chronorunner.entity.enemy.Enemy;
import chronorunner.obstacle.Platform;
import chronorunner.obstacle.StaticPlatform;

/** Builder Pattern: assembles a {@link Level} piece by piece. */
public class LevelBuilder {

    private int chapter = 1;
    private TileType tile;
    private double width = 1920;
    private double height = 720;
    private double groundY = 656;
    private final List<Platform> platforms = new ArrayList<>();
    private Boss boss;
    private final List<Enemy> enemies = new ArrayList<>();
    private double[] playerSpawns = {360, 500};

    public LevelBuilder chapter(int value) {
        this.chapter = value;
        this.tile = TileType.of(value);
        return this;
    }

    public LevelBuilder width(double value) {
        this.width = value;
        return this;
    }

    public LevelBuilder groundY(double value) {
        this.groundY = value;
        return this;
    }

    public LevelBuilder addPlatform(double x, double y, double w, double h) {
        Color color = (tile == null ? TileType.BROKEN_CITY : tile).platformColor;
        platforms.add(new StaticPlatform(x, y, w, h, color));
        return this;
    }

    public LevelBuilder addGround() {
        Color color = (tile == null ? TileType.BROKEN_CITY : tile).platformColor;
        platforms.add(new StaticPlatform(0, groundY, width, height - groundY + 80, color));
        return this;
    }

    public LevelBuilder boss(Boss value) {
        this.boss = value;
        return this;
    }

    public LevelBuilder addEnemy(Enemy enemy) {
        enemies.add(enemy);
        return this;
    }

    public LevelBuilder spawns(double p1, double p2) {
        this.playerSpawns = new double[]{p1, p2};
        return this;
    }

    public Level build() {
        if (tile == null) {
            tile = TileType.of(chapter);
        }
        return new Level(chapter, tile, width, height, groundY,
                new ArrayList<>(platforms), boss, new ArrayList<>(enemies), playerSpawns);
    }
}

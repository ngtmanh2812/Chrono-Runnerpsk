import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import chronorunner.combat.*;
import chronorunner.core.*;
import chronorunner.entity.*;
import chronorunner.entity.boss.Boss;
import chronorunner.entity.enemy.Enemy;
import chronorunner.gfx.*;
import chronorunner.obstacle.Platform;
import chronorunner.ui.PlayScreen;
import chronorunner.world.*;

/** Uses production player, physics, combat and screen rendering without writing saves.
 * Compile with ChapterOneMapCheck.java, then run ChapterTwoMapCheck.
 * Optional --live opens Chapter 2 using the normal game window. */
public final class ChapterTwoMapCheck {
    static final File OUTPUT = new File("out/chapter2-check");
    static class World extends ChapterOneMapCheck.World {
        int projectiles, summons, melee;
        World() { super(2); }
        public void addProjectile(Projectile p) { super.addProjectile(p); projectiles++; }
        public void addEnemy(Enemy e) { summons++; }
        public void addHitbox(Hitbox h) { super.addHitbox(h); melee++; }
    }
    static void check(boolean ok, String message) { ChapterOneMapCheck.check(ok, message); }
    static boolean near(double a, double b) { return ChapterOneMapCheck.near(a, b); }

    static void walk(World world, int character) {
        InputHandler input = new InputHandler();
        Player p = ChapterOneMapCheck.player(input, 0, world.level.groundY(), character);
        ChapterOneMapCheck.key(input, KeyEvent.VK_D, true);
        for (int frame = 0; frame < 1400; frame++) {
            ChapterOneMapCheck.step(p, input, world);
            check(p.grounded && near(p.bottom(), world.level.groundY()), "Floor gap / decoration blocked movement");
        }
        check(near(p.right(), world.worldWidth()), "Could not reach right arena boundary");
        ChapterOneMapCheck.key(input, KeyEvent.VK_D, false);
        ChapterOneMapCheck.key(input, KeyEvent.VK_A, true);
        for (int frame = 0; frame < 1400; frame++) ChapterOneMapCheck.step(p, input, world);
        check(near(p.x, 0), "Could not reach left arena boundary");
        System.out.println("Character " + character + ": full ground traversal and boundaries PASS");
    }

    static void boss(World world) {
        Boss core = world.level.boss();
        check(core.getName().equals("The Void Core"), "Wrong Chapter 2 boss");
        check(!core.grounded && core.bottom() < world.level.groundY(), "Core must spawn airborne");
        PixelSprite sprite = Art.bossAnimation(core.getName(), "idle").frame(0);
        int scale = sprite.scaleFor((int) core.width) + 1;
        check(core.centerX() + sprite.width() * scale / 2.0 < Game.WIDTH,
                "Core artwork starts outside initial viewport");
        check(core.centerY() - sprite.height() * scale / 2.0 > 90, "Core obscured by boss HUD");
        Player target = ChapterOneMapCheck.player(new InputHandler(), world.level.playerSpawn(0), world.level.groundY());
        world.players.add(target);
        for (int frame = 0; frame < 2400; frame++) {
            core.update(ChapterOneMapCheck.DT, world);
            check(Double.isFinite(core.x) && Double.isFinite(core.y), "Non-finite core movement");
            check(core.x >= 0 && core.right() <= world.worldWidth() + .001, "Core escaped arena");
            check(core.bottom() <= world.level.groundY() + .001, "Core fell through road");
        }
        check(world.projectiles > 8 && world.summons >= 2 && world.melee > 0,
                "Existing volley/radial/summon/melee cycle did not execute");
        core.takeDamage(new DamageInfo(600, 0, 0, true));
        core.update(ChapterOneMapCheck.DT, world);
        check(core.getPhase() == 2 && core.getHealth() == 1000, "Phase 2 / health regression");
        for (int i = 0; i < 80; i++) core.update(ChapterOneMapCheck.DT, world);
        core.takeDamage(new DamageInfo(500, 0, 0, true));
        core.update(ChapterOneMapCheck.DT, world);
        check(core.getPhase() == 3, "Phase 3 regression");
        System.out.println("Void Core: airborne spawn, movement, volleys, radial burst, teleport/dive, summons and phases PASS");
    }

    static Game game() throws Exception {
        Game game = new Game(); game.getSession().chapter = 2;
        Method register = Game.class.getDeclaredMethod("registerScreens"); register.setAccessible(true);
        register.invoke(game); game.setState(GameState.PLAYING);
        return game;
    }
    static Level level(PlayScreen screen) throws Exception {
        Field field = PlayScreen.class.getDeclaredField("level"); field.setAccessible(true);
        return (Level) field.get(screen);
    }
    static void capture(Game game, String name) throws Exception {
        BufferedImage image = new BufferedImage(Game.WIDTH, Game.HEIGHT, 2);
        Graphics2D g = image.createGraphics(); game.render(g); g.dispose();
        ImageIO.write(image, "png", new File(OUTPUT, name + ".png"));
    }
    static void escape(Game game) {
        ChapterOneMapCheck.key(game.getInput(), KeyEvent.VK_ESCAPE, true);
        game.getInput().beginFrame(); game.update(1.0 / 60); game.getInput().endFrame();
        ChapterOneMapCheck.key(game.getInput(), KeyEvent.VK_ESCAPE, false);
    }
    static void screens() throws Exception {
        Game game = game(); PlayScreen screen = (PlayScreen) game.screen(GameState.PLAYING);
        check(near(screen.players().get(0).bottom(), level(screen).groundY()), "Player feet at spawn");
        Field f = PlayScreen.class.getDeclaredField("camera"); f.setAccessible(true);
        Camera camera = (Camera) f.get(screen);
        for (boolean debug : new boolean[]{true, false}) {
            System.setProperty("chronorunner.debugCollision", Boolean.toString(debug));
            for (boolean right : new boolean[]{false, true}) {
                camera.snapTo(right ? screen.worldWidth() : 0, 360, screen.worldWidth(), screen.worldHeight());
                check(near(camera.offsetX(), right ? screen.worldWidth() - 1280 : 0), "Camera x bound");
                check(near(camera.offsetY(), 0), "Unexpected vertical scroll");
                capture(game, (right ? "right" : "left") + (debug ? "-debug" : "-normal"));
            }
        }
        escape(game); check(game.getState() == GameState.PAUSED, "Pause failed");
        capture(game, "pause-normal");
        double playerX = screen.players().get(0).x;
        escape(game); check(game.getState() == GameState.PLAYING, "Resume failed");
        check(near(playerX, screen.players().get(0).x), "Resume rebuilt arena");
        // Keep endTimer short of completion, so these checks never write saves.
        level(screen).boss().takeDamage(new DamageInfo(100000, 0, 0, true));
        game.update(1.0 / 60);
        Field victory = PlayScreen.class.getDeclaredField("victory"); victory.setAccessible(true);
        check(victory.getBoolean(screen), "Victory detection failed");
        capture(game, "victory-normal");
        Game defeatGame = game(); PlayScreen defeatScreen = (PlayScreen) defeatGame.screen(GameState.PLAYING);
        defeatScreen.players().get(0).takeDamage(new DamageInfo(100000, 0, 0, false));
        defeatGame.update(1.0 / 60);
        Field defeat = PlayScreen.class.getDeclaredField("defeat"); defeat.setAccessible(true);
        check(defeat.getBoolean(defeatScreen), "Game-over detection failed");
        check(!Boolean.getBoolean("chronorunner.debugCollision"), "Debug left enabled");
        System.out.println("Chapter 2 loading/rendering, camera ends, HUD, pause/resume, victory/game-over PASS");
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("--live")) {
            SwingUtilities.invokeLater(() -> {
                Game game = new Game(); game.getSession().chapter = 2; game.start();
                game.screen(GameState.PLAYING).onEnter(); game.resumeState(GameState.PLAYING);
            });
            return;
        }
        OUTPUT.mkdirs(); System.setProperty("chronorunner.debugCollision", "true");
        World world = new World();
        check(world.platforms.size() == 10, "Expected road plus nine distinct slabs");
        ChapterOneMapCheck.surfaces(world, 1913, 822);
        for (int character = 0; character < 3; character++) {
            ChapterOneMapCheck.reachable(world, character); walk(world, character);
        }
        boss(world); screens();
        System.out.printf("World %.6f x %.6f; floor %.6f; camera maxX %.6f%n",
                world.worldWidth(), world.worldHeight(), world.level.groundY(), world.worldWidth() - 1280);
        System.out.printf("Player x %.6f; P2 x %.6f; core spawn %.6f, %.6f%n",
                world.level.playerSpawn(0), world.level.playerSpawn(1),
                world.level.backdrop().toWorld(1260), world.level.backdrop().toWorld(170));
        System.out.println("PASS: " + ChapterOneMapCheck.checks + " checks; debug OFF at finish");
    }
}

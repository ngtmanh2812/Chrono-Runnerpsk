import java.awt.Canvas;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import chronorunner.combat.Hitbox;
import chronorunner.core.*;
import chronorunner.entity.*;
import chronorunner.entity.character.CharacterFactory;
import chronorunner.entity.enemy.Enemy;
import chronorunner.gfx.CollisionDebug;
import chronorunner.obstacle.Platform;
import chronorunner.ui.PlayScreen;
import chronorunner.world.*;

/** Run after build: javac -cp out -d out tools/ChapterOneMapCheck.java
 *  java -Djava.awt.headless=true -cp out ChapterOneMapCheck
 *  --live opens the real game directly in Chapter 1 for desktop inspection. */
public final class ChapterOneMapCheck {
    static int checks;
    static final double DT = 1.0 / 120;
    static final Canvas KEYS = new Canvas();
    static final File OUTPUT = new File("out/chapter1-check");

    static class World implements CombatWorld {
        final Level level;
        final List<Player> players = new ArrayList<>();
        final List<Platform> platforms;
        World() { this(1); }
        World(int chapter) {
            level = LevelFactory.create(chapter);
            platforms = new ArrayList<>(level.platforms());
        }
        int attacks;
        public List<Player> players() { return players; }
        public List<Platform> platforms() { return platforms; }
        public double worldWidth() { return level.width(); }
        public double worldHeight() { return level.height(); }
        public void addHitbox(Hitbox v) { attacks++; }
        public void addProjectile(Projectile v) { attacks++; }
        public void addParticle(Particle v) { }
        public void addFloatingText(FloatingText v) { }
        public void addEnemy(Enemy v) { }
        public void shake(double v) { }
    }

    static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
    static boolean near(double a, double b) { return Math.abs(a - b) < 0.001; }
    static Player player(InputHandler input, double x, double feet) {
        return player(input, x, feet, 0);
    }
    static Player player(InputHandler input, double x, double feet, int character) {
        Player p = CharacterFactory.create(character, 0, input, x, 0);
        p.y = feet - p.height;
        return p;
    }
    static void key(InputHandler input, int code, boolean down) {
        KeyEvent e = new KeyEvent(KEYS, down ? KeyEvent.KEY_PRESSED : KeyEvent.KEY_RELEASED,
                0, 0, code, KeyEvent.CHAR_UNDEFINED);
        if (down) input.keyPressed(e); else input.keyReleased(e);
    }
    static void step(Player p, InputHandler input, World world) {
        input.beginFrame(); p.update(DT, world); input.endFrame();
    }

    static void renderArena(World world, Player player, String name) throws Exception {
        BufferedImage image = new BufferedImage((int) Math.ceil(world.worldWidth()), 720, 2);
        Graphics2D g = image.createGraphics();
        world.level.backdrop().render(g);
        world.level.boss().render(g); player.render(g);
        CollisionDebug.render(g, world.level, List.of(player));
        File folder = new File("out/chapter" + world.level.chapter() + "-check");
        folder.mkdirs();
        g.dispose(); ImageIO.write(image, "png", new File(folder, name + ".png"));
    }

    static void surfaces(World world) throws Exception {
        check(world.platforms.size() == 11, "Expected floor plus ten stone caps");
        surfaces(world, 1920, 819);
        check(near(world.level.boss().bottom(), world.level.groundY()), "Boss spawn not on floor");
    }
    static void surfaces(World world, int sourceWidth, int sourceHeight) throws Exception {
        check(world.level.backdrop().sourceWidth() == sourceWidth
                && world.level.backdrop().sourceHeight() == sourceHeight, "Source PNG changed");
        for (int i = 1; i < world.platforms.size(); i++) {
            Platform cap = world.platforms.get(i);
            InputHandler input = new InputHandler();
            Player p = player(input, cap.centerX() - 22, cap.y + 80);
            p.grounded = true;
            key(input, KeyEvent.VK_W, true);
            boolean crossedUp = false, landed = false;
            for (int frame = 0; frame < 240; frame++) {
                double before = p.bottom();
                step(p, input, world);
                if (frame == 0) key(input, KeyEvent.VK_W, false);
                if (before > cap.y && p.bottom() < cap.y && p.vy < 0) {
                    crossedUp = true;
                    check(!p.grounded, "Upward crossing blocked: " + i);
                }
                if (p.grounded && frame > 2) {
                    landed = near(p.bottom(), cap.y); break;
                }
            }
            check(crossedUp && landed, "Cannot jump through / land on cap " + i);
            renderArena(world, p, "platform-" + i + "-debug");

            // A fall crossing several caps must hit the nearest, even reversed.
            p = player(input, cap.centerX() - 22, cap.y - 1);
            p.vy = 1400;
            Collections.reverse(world.platforms);
            Physics.moveAndCollide(p, world, .5);
            check(near(p.bottom(), cap.y), "Fast fall skipped cap " + i);
            Collections.reverse(world.platforms);

            // Sides and underside are deliberately nonblocking.
            p = player(input, cap.x + 2, cap.y + 50);
            p.vx = 100; p.vy = 0;
            double startX = p.x;
            Physics.moveAndCollide(p, world, .01);
            check(near(p.x, startX + 1) && !p.grounded, "One-way side snag: " + i);
            System.out.printf("cap %02d: x=%.2f y=%.2f w=%.2f PASS%n", i, cap.x, cap.y, cap.width);
        }
        for (double x = 0; x < world.worldWidth() - 44; x += 16) {
            Player p = player(new InputHandler(), x, world.level.groundY() - 1);
            p.vy = 200; Physics.moveAndCollide(p, world, .1);
            check(near(p.bottom(), world.level.groundY()) && p.grounded, "Gap in floor at " + x);
        }
        Player p = player(new InputHandler(), -100, world.level.groundY());
        Physics.moveAndCollide(p, world, DT); check(near(p.x, 0), "Left arena boundary");
        p.x = world.worldWidth() + 100;
        Physics.moveAndCollide(p, world, DT);
        check(near(p.right(), world.worldWidth()), "Right arena boundary");
    }

    /** Explore real player jump arcs and double jumps from every stone cap. */
    static void reachable(World world) {
        reachable(world, 0);
    }
    static void reachable(World world, int character) {
        Set<Integer> reached = new HashSet<>(Set.of(0));
        ArrayDeque<Integer> queue = new ArrayDeque<>(List.of(0));
        while (!queue.isEmpty()) {
            Platform from = world.platforms.get(queue.remove());
            // The floor needs more takeoff samples because it spans the arena.
            int samples = from.width > 1000 ? 20 : 4;
            for (int sample = 0; sample <= samples; sample++) {
                for (int direction = -1; direction <= 1; direction++) {
                    for (boolean twice : new boolean[]{false, true}) {
                        InputHandler input = new InputHandler();
                        double x = Math.max(0, Math.min(world.worldWidth() - 44,
                                from.x + from.width * sample / samples - 22));
                        Player p = player(input, x, from.y, character); p.grounded = true;
                        if (direction != 0) key(input, direction < 0 ? KeyEvent.VK_A : KeyEvent.VK_D, true);
                        key(input, KeyEvent.VK_W, true);
                        for (int frame = 0; frame < 260; frame++) {
                            if (twice && frame == 40) key(input, KeyEvent.VK_W, true);
                            step(p, input, world);
                            if (frame == 0 || frame == 40) key(input, KeyEvent.VK_W, false);
                            if (p.grounded && frame > 2) {
                                for (int target = 0; target < world.platforms.size(); target++) {
                                    Platform to = world.platforms.get(target);
                                    if (near(p.bottom(), to.y) && p.right() > to.x && p.x < to.right()
                                            && reached.add(target)) queue.add(target);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
        check(reached.size() == world.platforms.size(), "Unreachable platforms with existing movement: " + reached);
        System.out.println("Reachability: character " + character + ", all "
                + (world.platforms.size() - 1) + " caps reachable using existing jumps");
    }

    static void rendering() throws Exception {
        Game game = new Game();
        PlayScreen screen = new PlayScreen(game); screen.onEnter();
        check(near(screen.players().get(0).bottom(), LevelFactory.create(1).groundY()), "Player spawn");
        Field field = PlayScreen.class.getDeclaredField("camera"); field.setAccessible(true);
        Camera camera = (Camera) field.get(screen);
        double width = screen.worldWidth();
        for (boolean debug : new boolean[]{true, false}) {
            System.setProperty("chronorunner.debugCollision", Boolean.toString(debug));
            for (boolean right : new boolean[]{false, true}) {
                camera.snapTo(right ? width : 0, 360, width, 720);
                check(near(camera.offsetX(), right ? width - 1280 : 0), "Camera clamp");
                BufferedImage image = new BufferedImage(1280, 720, 2);
                Graphics2D g = image.createGraphics(); screen.render(g); g.dispose();
                ImageIO.write(image, "png", new File(OUTPUT,
                        (right ? "right" : "left") + (debug ? "-debug" : "-normal") + ".png"));
            }
        }
        check(!Boolean.getBoolean("chronorunner.debugCollision"), "Debug must finish disabled");
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("--live")) {
            SwingUtilities.invokeLater(() -> {
                Game game = new Game(); game.start();
                // Build before publishing PLAYING to the running game loop.
                game.screen(GameState.PLAYING).onEnter();
                game.resumeState(GameState.PLAYING);
            });
            return;
        }
        OUTPUT.mkdirs(); System.setProperty("chronorunner.debugCollision", "true");
        World world = new World(); surfaces(world); reachable(world);
        Player target = player(new InputHandler(), world.level.playerSpawn(0), world.level.groundY());
        world.players.add(target);
        for (int frame = 0; frame < 600; frame++) {
            world.level.boss().update(DT, world);
            check(world.level.boss().bottom() <= world.level.groundY() + .001, "Boss fell through floor");
        }
        check(world.attacks > 0, "Boss combat did not execute");
        rendering();
        System.out.println("PASS: " + checks + " checks; debug on for tests, OFF at finish");
    }
}

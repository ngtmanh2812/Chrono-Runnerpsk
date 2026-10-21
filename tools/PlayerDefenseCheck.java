import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import javax.imageio.ImageIO;

import chronorunner.animation.AnimationController;
import chronorunner.combat.*;
import chronorunner.core.*;
import chronorunner.entity.Player;
import chronorunner.entity.Projectile;
import chronorunner.entity.boss.Boss;
import chronorunner.entity.enemy.TimeSlime;
import chronorunner.ui.PlayScreen;
import chronorunner.util.Rect;
import chronorunner.world.Level;

/** Focused regression checks against real input, player, collision and screen code.
 * No saves are written. javac -encoding UTF-8 -cp out -d out tools/PlayerDefenseCheck.java
 * java -Djava.awt.headless=true -cp out PlayerDefenseCheck */
public final class PlayerDefenseCheck {
    static final double DT = 1.0 / 60;
    static final Canvas KEYS = new Canvas();
    static int checks;

    static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
    static boolean near(double a, double b) { return Math.abs(a - b) < 1e-7; }
    static Field field(Object object, String name) throws Exception {
        Class<?> type = object.getClass();
        while (type != null) {
            try { Field f = type.getDeclaredField(name); f.setAccessible(true); return f; }
            catch (NoSuchFieldException e) { type = type.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }
    static Object get(Object object, String name) throws Exception { return field(object, name).get(object); }
    static void call(Object object, String name) throws Exception {
        Method m = object.getClass().getDeclaredMethod(name); m.setAccessible(true); m.invoke(object);
    }
    static Attack attack(Player p) throws Exception { return (Attack) get(p, "basicAttack"); }
    static String animation(Player p) throws Exception { return ((AnimationController) get(p, "anim")).currentName(); }
    static void key(InputHandler input, int code, boolean down) {
        KeyEvent event = new KeyEvent(KEYS, down ? KeyEvent.KEY_PRESSED : KeyEvent.KEY_RELEASED,
                0, 0, code, KeyEvent.CHAR_UNDEFINED);
        if (down) input.keyPressed(event); else input.keyReleased(event);
    }

    static final class Fixture {
        final Game game = new Game();
        final InputHandler input = game.getInput();
        final PlayScreen screen;
        final Level level;
        Fixture(int chapter, int character, int count) throws Exception {
            game.getSession().chapter = chapter;
            game.getSession().playerCount = count;
            game.getSession().characterIndex[0] = character;
            game.getSession().characterIndex[1] = character;
            call(game, "registerScreens");
            game.setState(GameState.PLAYING);
            screen = (PlayScreen) game.screen(GameState.PLAYING);
            level = (Level) get(screen, "level");
            for (Player p : screen.players()) p.update(0, screen); // synchronize hurtboxes
        }
        Player p() { return screen.players().get(0); }
        void players(double dt) {
            input.beginFrame();
            for (Player p : screen.players()) p.update(dt, screen);
            input.endFrame();
        }
        void press(int... keys) {
            for (int k : keys) key(input, k, true);
            players(DT);
            for (int k : keys) key(input, k, false);
        }
        void tick(int frames) { for (int i = 0; i < frames; i++) players(DT); }
        void realTick() { input.beginFrame(); game.update(DT); input.endFrame(); }
        void realPress(int code) { key(input, code, true); realTick(); key(input, code, false); }
        Hitbox hit(Player p, int amount) {
            return new Hitbox(new Rect(p.x, p.y, p.width, p.height),
                    new DamageInfo(amount, 180, -220, false, 1), 1.2, false, level.boss());
        }
        Projectile projectile(Player p, int amount) {
            return new Projectile(p.centerX(), p.centerY(), 0, 0, 4,
                    new DamageInfo(amount, 180, -220, false, 1), false, Color.MAGENTA, 2);
        }
        void snapshot(String name) throws Exception {
            File folder = new File("out/player-defense-check"); folder.mkdirs();
            BufferedImage image = new BufferedImage(Game.WIDTH, Game.HEIGHT, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics(); game.render(g); g.dispose();
            ImageIO.write(image, "png", new File(folder, name + ".png"));
        }
    }

    static void normalAttacks() throws Exception {
        int[][] expected = {{60,54,9,6,11,260}, {48,46,5,6,7,180}, {72,66,14,10,18,380}};
        for (int chapter = 1; chapter <= 2; chapter++) {
            for (int character = 0; character < 3; character++) {
                for (int index = 0; index < 2; index++) {
                    Fixture f = new Fixture(chapter, character, 2);
                    Player p = f.screen.players().get(index);
                    p.x = 850; p.y = f.level.groundY() - p.height; p.grounded = true;
                    Boss boss = f.level.boss(); boss.x = p.right() + 12; boss.y = f.level.groundY() - boss.height;
                    boss.update(0, f.screen); p.update(0, f.screen);
                    Attack a = attack(p); AttackData d = a.getData(); int[] e = expected[character];
                    check(d.damage == 30, "Normal damage must be 30 for every character/player slot");
                    check(d.reach == e[0] && d.height == e[1] && d.startupFrames == e[2]
                            && d.activeFrames == e[3] && d.recoveryFrames == e[4] && d.knockback == e[5],
                            "Existing attack timing/geometry/knockback changed");
                    int hp = boss.getHealth(); f.press(Player.keysFor(index)[3]);
                    check(a.isStartup() && boss.getHealth() == hp, "Attack must retain startup");
                    for (int n = 0; n < d.startupFrames - 1; n++) {
                        f.players(DT); call(f.screen, "resolveHitboxes");
                        check(boss.getHealth() == hp, "Early normal hit");
                    }
                    f.players(DT); call(f.screen, "resolveHitboxes");
                    check(boss.getHealth() == hp - 30, "Normal hit did not deal exactly 30 to chapter " + chapter);
                    // Clear boss invulnerability: the same hitbox must still never hit twice.
                    field(boss, "invuln").setDouble(boss, 0);
                    for (int n = 0; n < 5; n++) call(f.screen, "resolveHitboxes");
                    check(boss.getHealth() == hp - 30, "One swing dealt repeated damage");

                    Fixture miss = new Fixture(chapter, character, 2);
                    int missHP = miss.level.boss().getHealth(); miss.press(Player.keysFor(index)[3]);
                    for (int n = 0; n < d.totalFrames(); n++) {
                        miss.players(DT); call(miss.screen, "resolveHitboxes");
                    }
                    check(miss.level.boss().getHealth() == missHP, "Miss dealt damage");
                }
            }
        }
        System.out.println("Normal attacks: 3 characters x 2 slots x 2 bosses, hit/miss/dedup and unchanged timing PASS");
    }

    static void guardLifecycle() throws Exception {
        for (int character = 0; character < 3; character++) {
            Fixture f = new Fixture(1, character, 1); Player p = f.p();
            f.press(KeyEvent.VK_S); check(p.isBlocking(), "S did not activate guard");
            check(near(p.getBlockCooldownRemaining(), 2), "Cooldown must begin on activation");
            int hp = p.getHealth(); double vx = p.vx, vy = p.vy; String pose = animation(p);
            p.takeDamage(null); p.takeDamage(new DamageInfo(0, 180, -300, false));
            p.takeDamage(new DamageInfo(-5, 180, -300, false));
            check(p.isBlocking() && p.getHealth() == hp, "Invalid hit consumed guard/changed HP");
            p.takeDamage(new DamageInfo(17, 180, -300, false));
            check(p.getHealth() == hp && !p.isBlocking(), "Guard must absorb exactly one hit");
            check(near(vx, p.vx) && near(vy, p.vy) && pose.equals(animation(p))
                            && !p.isInvulnerable() && near(field(p, "stun").getDouble(p), 0),
                    "Guard applied normal knockback, stun, invulnerability or hurt animation");
            p.takeDamage(new DamageInfo(9, 0, 0, false));
            check(p.getHealth() == hp - 9 && animation(p).equals("hurt"), "Second separate hit must damage normally");

            Fixture time = new Fixture(1, character, 1); Player q = time.p();
            key(time.input, KeyEvent.VK_S, true); time.players(DT);
            time.tick(29); check(q.isBlocking(), "Guard expired before 0.5 seconds");
            time.tick(1); check(!q.isBlocking(), "Guard did not expire at 0.5 seconds");
            key(time.input, KeyEvent.VK_S, false); time.press(KeyEvent.VK_S);
            check(!q.isBlocking(), "Cooldown allowed reactivation");
            time.tick(88); check(q.getBlockCooldownRemaining() > 0, "Cooldown ended before 2 seconds");
            time.tick(1); check(near(q.getBlockCooldownRemaining(), 0), "Cooldown failed at 2 seconds");
            time.press(KeyEvent.VK_S); check(q.isBlocking(), "New press after cooldown did not activate");

            Fixture held = new Fixture(1, character, 1);
            key(held.input, KeyEvent.VK_S, true); held.players(DT); held.tick(140);
            check(!held.p().isBlocking(), "Holding S reactivated guard automatically");
            key(held.input, KeyEvent.VK_S, false);

            Fixture dash = new Fixture(1, character, 1); dash.press(KeyEvent.VK_S, KeyEvent.VK_K);
            check(dash.p().isInvulnerable() && dash.p().isBlocking(), "Existing dash no longer works with guard");
            dash.p().takeDamage(new DamageInfo(20, 0, 0, false));
            check(dash.p().isBlocking(), "Invulnerable hit consumed guard");
            dash.tick(15); dash.p().takeDamage(new DamageInfo(20, 0, 0, false));
            check(!dash.p().isBlocking() && dash.p().getHealth() == dash.p().getMaxHealth(), "Guard after dash failed");
        }
        System.out.println("Guard: activation, invalid hits, one hit, second hit, 0.5s/2s boundaries, held key and dash PASS");
    }

    static void collisionPipeline() throws Exception {
        for (int chapter = 1; chapter <= 2; chapter++) {
            Fixture f = new Fixture(chapter, 0, 1); Player p = f.p(); int hp = p.getHealth();
            f.press(KeyEvent.VK_S);
            Hitbox first = f.hit(p, 17), second = f.hit(p, 11);
            f.screen.addHitbox(first); f.screen.addHitbox(second); call(f.screen, "resolveHitboxes");
            check(first.hasHit(p) && second.hasHit(p) && p.getHealth() == hp - 11,
                    "Hitboxes must consume guard then apply next hit in insertion order");
            field(p, "invuln").setDouble(p, 0); call(f.screen, "resolveHitboxes");
            check(p.getHealth() == hp - 11, "Blocked melee damaged again");

            Fixture ranged = new Fixture(chapter, 0, 1); Player q = ranged.p(); ranged.press(KeyEvent.VK_S);
            Projectile orb = ranged.projectile(q, 14); ranged.screen.addProjectile(orb);
            call(ranged.screen, "resolveProjectiles");
            check(!orb.alive && q.getHealth() == q.getMaxHealth() && !q.isBlocking(), "Blocked projectile not consumed");
            check(near(q.speedMultiplier(), 1) && !q.isInvulnerable(), "Blocked energy attack applied slow/invuln");
            call(ranged.screen, "resolveProjectiles"); check(q.getHealth() == q.getMaxHealth(), "Dead orb damaged again");
            ranged.screen.addProjectile(ranged.projectile(q, 8)); call(ranged.screen, "resolveProjectiles");
            check(q.getHealth() == q.getMaxHealth() - 8 && q.speedMultiplier() < 1, "Unblocked projectile/slow failed");

            Fixture stacked = new Fixture(chapter, 0, 1); stacked.press(KeyEvent.VK_S);
            stacked.screen.addProjectile(stacked.projectile(stacked.p(), 19));
            stacked.screen.addProjectile(stacked.projectile(stacked.p(), 7));
            call(stacked.screen, "resolveProjectiles");
            check(stacked.p().getHealth() == stacked.p().getMaxHealth() - 7, "Stacked projectiles resolved nondeterministically");

            Fixture liveOrder = new Fixture(chapter, 0, 1);
            liveOrder.screen.addProjectile(liveOrder.projectile(liveOrder.p(), 18));
            liveOrder.screen.addHitbox(liveOrder.hit(liveOrder.p(), 6));
            liveOrder.realPress(KeyEvent.VK_S);
            check(liveOrder.p().getHealth() == liveOrder.p().getMaxHealth() - 6,
                    "Real screen must resolve projectiles before melee in its existing stable order");

            Fixture zero = new Fixture(chapter, 0, 1); zero.press(KeyEvent.VK_S);
            zero.screen.addProjectile(zero.projectile(zero.p(), 0));
            zero.screen.addHitbox(zero.hit(zero.p(), -3));
            call(zero.screen, "resolveProjectiles"); call(zero.screen, "resolveHitboxes");
            check(zero.p().isBlocking() && zero.p().getHealth() == zero.p().getMaxHealth(),
                    "Invalid combat collisions consumed guard or healed the player");

            Fixture pair = new Fixture(chapter, 0, 2); Player secondPlayer = pair.screen.players().get(1);
            pair.press(KeyEvent.VK_DOWN); pair.screen.addProjectile(pair.projectile(secondPlayer, 18));
            call(pair.screen, "resolveProjectiles");
            check(!secondPlayer.isBlocking() && secondPlayer.getHealth() == secondPlayer.getMaxHealth()
                    && near(pair.p().getBlockCooldownRemaining(), 0), "P2 damage/guard state leaked into P1");

            Fixture contact = new Fixture(chapter, 0, 1); Player r = contact.p(); Boss boss = contact.level.boss();
            boss.x = r.x; boss.y = r.y; contact.press(KeyEvent.VK_S);
            call(contact.screen, "resolveContact");
            check(!r.isBlocking() && r.getHealth() == r.getMaxHealth(), "Boss contact bypassed guard");
            for (int i = 0; i < 40; i++) call(contact.screen, "resolveContact");
            check(r.getHealth() == r.getMaxHealth(), "Same continuous contact damaged after block");
            boss.x += 300; call(contact.screen, "resolveContact"); boss.x = r.x;
            call(contact.screen, "resolveContact");
            check(r.getHealth() == r.getMaxHealth() - boss.getContactDamage(), "New contact should damage");

            Fixture enemy = new Fixture(chapter, 0, 1); enemy.press(KeyEvent.VK_S);
            enemy.screen.addEnemy(new TimeSlime(enemy.p().x, enemy.p().y));
            call(enemy.screen, "resolveContact");
            check(!enemy.p().isBlocking() && enemy.p().getHealth() == enemy.p().getMaxHealth(), "Enemy contact bypassed guard");

            // Exercise a boss-authored melee hit, not just a synthetic damage event.
            Fixture melee = new Fixture(chapter, 0, 1); Boss b = melee.level.boss(); Player m = melee.p();
            b.x = m.x - b.width - 4; b.y = m.y; field(b, "facing").setInt(b, 1);
            Method strike = Boss.class.getDeclaredMethod("melee", chronorunner.world.CombatWorld.class,
                    double.class, double.class, int.class, double.class);
            strike.setAccessible(true); melee.press(KeyEvent.VK_S);
            strike.invoke(b, melee.screen, 80.0, 100.0, 23, 280.0);
            call(melee.screen, "resolveHitboxes");
            check(!m.isBlocking() && m.getHealth() == m.getMaxHealth(), "Real boss melee bypassed guard");
        }
        System.out.println("Both chapters: boss melee/contact, enemy contact, energy/projectiles, slow and overlap dedup PASS");
    }

    static void controlsAndScreens() throws Exception {
        for (int chapter = 1; chapter <= 2; chapter++) {
            for (int character = 0; character < 3; character++) {
                Fixture f = new Fixture(chapter, character, 2); Player p = f.p(), p2 = f.screen.players().get(1);
                f.press(KeyEvent.VK_S, KeyEvent.VK_J, KeyEvent.VK_D, KeyEvent.VK_W);
                check(p.isBlocking() && attack(p).isReady() && p.vx > 0 && p.vy < 0,
                        "Guard must block new attack and preserve move/jump");
                check(!p2.isBlocking() && near(p2.getBlockCooldownRemaining(), 0), "S affected P2");
                f.press(KeyEvent.VK_DOWN); check(p2.isBlocking(), "P2 Down key failed");
                f.press(KeyEvent.VK_L); check(p.getSkillCooldownRatio() < 1, "Existing ability blocked by guard");
                if (character == 0) {
                    List<?> orbs = (List<?>) get(f.screen, "projectiles");
                    check(orbs.size() == 1 && ((Projectile) orbs.get(0)).damage().amount == 20, "Chrono Shot damage changed");
                } else {
                    List<?> hits = (List<?>) get(f.screen, "hitboxes");
                    check(hits.size() == 1 && ((Hitbox) hits.get(0)).damage.amount == (character == 1 ? 22 : 26),
                            "Signature skill damage changed");
                }

                Fixture swing = new Fixture(chapter, character, 1); swing.press(KeyEvent.VK_J); swing.press(KeyEvent.VK_S);
                check(!swing.p().isBlocking() && attack(swing.p()).isBusy(), "Guard interrupted committed attack");
            }

            Fixture paused = new Fixture(chapter, 0, 2); paused.press(KeyEvent.VK_S, KeyEvent.VK_DOWN);
            double cd1 = paused.p().getBlockCooldownRemaining(), cd2 = paused.screen.players().get(1).getBlockCooldownRemaining();
            paused.realPress(KeyEvent.VK_ESCAPE); check(paused.game.getState() == GameState.PAUSED, "Pause failed");
            for (int n = 0; n < 240; n++) paused.realTick();
            check(paused.p().isBlocking() && near(cd1, paused.p().getBlockCooldownRemaining())
                    && near(cd2, paused.screen.players().get(1).getBlockCooldownRemaining()), "Pause advanced guard timers");
            paused.realPress(KeyEvent.VK_ESCAPE); check(paused.game.getState() == GameState.PLAYING, "Resume failed");
            paused.p().takeDamage(new DamageInfo(10, 0, 0, false));
            check(paused.p().getHealth() == paused.p().getMaxHealth() && !paused.p().isBlocking(), "Guard broken after resume");
            paused.press(KeyEvent.VK_J); check(attack(paused.p()).isBusy(), "Attack broken after resume");
            paused.game.setState(GameState.PAUSED); paused.game.setState(GameState.PLAYING);
            check(!paused.screen.players().get(0).isBlocking() && near(paused.p().getBlockCooldownRemaining(), 0), "Restart kept old guard state");

            Fixture victory = new Fixture(chapter, 0, 1); victory.press(KeyEvent.VK_S);
            victory.level.boss().takeDamage(new DamageInfo(100000, 0, 0, true)); victory.realTick();
            check(!victory.p().isBlocking(), "Victory did not cancel stance");
            victory.press(KeyEvent.VK_S); check(!victory.p().isBlocking(), "Guard activated in victory overlay");
            victory.game.setState(GameState.VICTORY); victory.realPress(KeyEvent.VK_S);
            check(!victory.p().isBlocking(), "Victory screen processed guard");
            victory.game.getSession().chapter = chapter == 1 ? 2 : 1; victory.game.setState(GameState.PLAYING);
            check(near(victory.p().getBlockCooldownRemaining(), 0), "Chapter transition carried cooldown");
            victory.press(KeyEvent.VK_S); check(victory.p().isBlocking(), "Guard unavailable in next chapter");

            Fixture dead = new Fixture(chapter, 0, 1); dead.p().takeDamage(new DamageInfo(100000, 0, 0, false));
            dead.realPress(KeyEvent.VK_S); check(dead.p().isDead() && !dead.p().isBlocking(), "Dead player activated guard");
            for (int n = 0; n < 110; n++) dead.realTick();
            check(dead.game.getState() == GameState.GAME_OVER, "Game over failed");
            dead.game.setState(GameState.PLAYING); dead.press(KeyEvent.VK_S);
            check(dead.p().isBlocking() && !dead.p().isDead(), "Respawn did not reset guard");

            Fixture render = new Fixture(chapter, 0, 2); render.press(KeyEvent.VK_S, KeyEvent.VK_DOWN);
            render.snapshot("chapter" + chapter + "-guard");
            render.p().takeDamage(new DamageInfo(10, 100, -100, false));
            render.snapshot("chapter" + chapter + "-impact");
            render.tick(40); render.snapshot("chapter" + chapter + "-cooldown");
        }
        check(!Boolean.getBoolean("chronorunner.debugCollision"), "Debug overlay enabled");
        System.out.println("Controls, skills, 2-player independence, pause/resume, restart, endings, transitions and HUD render PASS");
    }

    public static void main(String[] args) throws Exception {
        normalAttacks(); guardLifecycle(); collisionPipeline(); controlsAndScreens();
        System.out.println("PASS: " + checks + " player combat checks; saves untouched; debug OFF");
    }
}

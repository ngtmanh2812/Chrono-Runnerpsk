import java.awt.Canvas;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import chronorunner.animation.AnimationController;
import chronorunner.combat.Attack;
import chronorunner.combat.DamageInfo;
import chronorunner.combat.Hitbox;
import chronorunner.combat.PlayerDefense;
import chronorunner.core.Game;
import chronorunner.core.GameState;
import chronorunner.core.InputHandler;
import chronorunner.entity.Player;
import chronorunner.entity.Projectile;
import chronorunner.entity.boss.Boss;
import chronorunner.ui.PlayScreen;
import chronorunner.world.Level;

/**
 * Real Game/PlayScreen updates, including boss AI and movement. Cooldowns are
 * pinned only in explicitly isolated ownership/energy/geometry cases; the live
 * approach test below leaves both boss timers running. Save writes intercepted.
 */
public final class MeleeFairnessCheck {
    static final double DT = 1.0 / 60;
    static final Canvas KEYS = new Canvas();
    static final Path OUTPUT = Path.of("out/melee-fairness-check");
    static int checks;

    static void check(boolean ok, String why) {
        checks++;
        if (!ok) throw new AssertionError(why);
    }
    static boolean near(double a, double b) { return Math.abs(a - b) < 1e-6; }
    static Field field(Object object, String name) throws Exception {
        for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) {
            try { Field f = type.getDeclaredField(name); f.setAccessible(true); return f; }
            catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name);
    }
    static Object get(Object object, String name) throws Exception { return field(object, name).get(object); }
    static void set(Object object, String name, double value) throws Exception { field(object, name).setDouble(object, value); }
    static void call(Object object, String name) throws Exception {
        for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) {
            try { Method method = type.getDeclaredMethod(name); method.setAccessible(true); method.invoke(object); return; }
            catch (NoSuchMethodException ignored) { }
        }
        throw new NoSuchMethodException(name);
    }
    static void key(InputHandler input, int code, boolean down) {
        KeyEvent event = new KeyEvent(KEYS, down ? KeyEvent.KEY_PRESSED : KeyEvent.KEY_RELEASED,
                0, 0, code, KeyEvent.CHAR_UNDEFINED);
        if (down) input.keyPressed(event); else input.keyReleased(event);
    }
    static class NoSave extends Game { int saves; @Override public void saveProgress() { saves++; } }
    static final class Fixture {
        final NoSave game = new NoSave();
        final InputHandler input = game.getInput();
        final PlayScreen screen;
        final Level level;
        int ticks;
        Fixture(int chapter, int hero, int players, boolean isolated) throws Exception {
            game.getSession().chapter = chapter; game.getSession().playerCount = players;
            game.getSession().characterIndex[0] = hero; game.getSession().characterIndex[1] = hero;
            call(game, "registerScreens"); game.setState(GameState.PLAYING);
            screen = (PlayScreen) game.screen(GameState.PLAYING); level = (Level)get(screen, "level");
            if (isolated) { set(boss(), "attackTimer", 100); set(boss(), "specialTimer", 100); }
            for (Player p : screen.players()) {
                p.x = 100; p.y = level.groundY() - p.height; p.grounded = true; p.update(0, screen);
            }
        }
        Player p(int slot) { return screen.players().get(slot); }
        Boss boss() { return level.boss(); }
        void tick() { input.beginFrame(); game.update(DT); input.endFrame(); ticks++; }
        void ticks(int count) { for (int i = 0; i < count; i++) tick(); }
        void press(int code) { key(input, code, true); tick(); key(input, code, false); }
        void target(int slot, double gap) {
            Player p = p(slot); p.x = 850; p.y = level.groundY() - p.height; p.grounded = true;
            boss().x = p.right() + gap;
            boss().y = level.groundY() - boss().height - (level.chapter() == 2 ? 24 : 0);
            boss().grounded = level.chapter() == 1; boss().update(0, screen); p.update(0, screen);
        }
        @SuppressWarnings("unchecked") List<Hitbox> hits() throws Exception { return (List<Hitbox>)get(screen, "hitboxes"); }
        @SuppressWarnings("unchecked") List<Projectile> shots() throws Exception { return (List<Projectile>)get(screen, "projectiles"); }
        void capture(String name) throws Exception {
            var image = new BufferedImage(Game.WIDTH, Game.HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics(); game.render(g); g.dispose();
            ImageIO.write(image, "png", OUTPUT.resolve(name + ".png").toFile());
        }
    }
    static Attack attack(Player p, int step) throws Exception {
        return (Attack)get(p, step == 0 ? "basicAttack" : step == 1 ? "heavyAttack" : "launcherAttack");
    }
    static void waitHit(Fixture f, int beforeHP) {
        for (int n = 0; n < 100 && f.boss().getHealth() == beforeHP; n++) f.tick();
        check(f.boss().getHealth() < beforeHP, "Expected committed melee did not connect");
    }
    static void finish(Fixture f, Attack action) {
        for (int n = 0; n < 150 && action.isBusy(); n++) f.tick();
        check(!action.isBusy(), "Committed action remained busy");
    }
    static void link(Fixture f, Player p, int fromStep, int meleeKey) throws Exception {
        Attack old = attack(p, fromStep), next = attack(p, fromStep + 1);
        for (int n = 0; n < 120 && old.phase() != Attack.Phase.RECOVERY; n++) f.tick();
        check(old.phase() == Attack.Phase.RECOVERY, "Melee never reached recovery");
        // Preserve visible recovery poses before asking for a confirmed cancel.
        f.ticks(2); f.press(meleeKey);
        for (int n = 0; n < 3 && !next.isBusy(); n++) f.tick();
        check(next.isBusy(), "Fresh J did not link after a confirmed hit");
        check(old.isReady(), "Previous hitbox/action remained alive after link");
    }

    static void energyAndLinks() throws Exception {
        for (int chapter = 1; chapter <= 2; chapter++) for (int hero = 0; hero < 3; hero++) for (int slot = 0; slot < 2; slot++) {
            Fixture f = new Fixture(chapter, hero, 2, true); Player p = f.p(slot); f.target(slot, 30);
            for (Player player : f.screen.players()) set(player, "energy", 0);
            int meleeKey = Player.keysFor(slot)[3], hp = f.boss().getHealth();
            f.press(meleeKey); check(near(p.getEnergy(), 0), "Energy awarded before actual damage");
            waitHit(f, hp); hp -= 30;
            check(f.boss().getHealth() == hp && near(p.getEnergy(), 5), "Normal must deal30 and gain5 once");
            int linkStart = f.ticks; link(f, p, 0, meleeKey);
            check(f.ticks - linkStart < attack(p, 0).getData().activeFrames + attack(p, 0).getData().recoveryFrames,
                    "Confirmed normal did not open an earlier link");
            waitHit(f, hp); hp -= 40;
            check(f.boss().getHealth() == hp && near(p.getEnergy(), 12), "Heavy must deal40 and gain7 once");
            link(f, p, 1, meleeKey); waitHit(f, hp); hp -= 35;
            check(f.boss().getHealth() == hp && near(p.getEnergy(), 22), "Launcher must deal35 and gain10 once");
            finish(f, attack(p, 2)); f.ticks(5);
            check(near(p.getEnergy(), 22), "Active frames awarded melee energy repeatedly");
            check(near(f.p(1 - slot).getEnergy(), 0), "Melee energy credited to the other player");
            check(f.game.saves == 0, "Isolated fixture wrote progress");
        }
        System.out.println("Real moving bosses: 3 heroes x 2 slots x 2 chapters; hit-confirm links, damage30/40/35, energy5/7/10 and ownership PASS");
    }

    static void invalidAndSkillEnergy() throws Exception {
        for (int hero = 0; hero < 3; hero++) {
            Fixture miss = new Fixture(1, hero, 1, true); Player p = miss.p(0);
            p.x = 150; miss.boss().x = 1400; p.update(0, miss.screen); miss.boss().update(0, miss.screen); set(p, "energy", 0);
            miss.press(KeyEvent.VK_J);
            Attack basic = attack(p, 0);
            while (basic.phase() != Attack.Phase.RECOVERY) miss.tick();
            miss.press(KeyEvent.VK_J);
            check(!attack(p, 1).isBusy() && basic.isBusy(), "Whiff cancelled recovery into Heavy");
            finish(miss, basic); check(near(p.getEnergy(), 0), "Whiff awarded energy");

            Fixture immune = new Fixture(1, hero, 1, true); immune.target(0, 30); set(immune.p(0), "energy", 0);
            int hp = immune.boss().getHealth(); immune.press(KeyEvent.VK_J); waitHit(immune, hp); hp -= 30;
            Hitbox original = immune.hits().stream().filter(h -> h.owner == immune.p(0) && h.damage.meleeStep == 1).findFirst().orElseThrow();
            immune.screen.addHitbox(new Hitbox(immune.boss().hurtbox().rect().copy(), original.damage, .1, true, immune.p(0)));
            immune.tick();
            check(immune.boss().getHealth() == hp && near(immune.p(0).getEnergy(), 5), "Rejected duplicate overlap awarded damage/energy");

            Fixture held = new Fixture(1, hero, 1, true); held.target(0, 30); set(held.p(0), "energy", 0); hp = held.boss().getHealth();
            key(held.input, KeyEvent.VK_J, true);
            for (int n = 0; n < 150; n++) { key(held.input, KeyEvent.VK_J, true); held.tick(); }
            key(held.input, KeyEvent.VK_J, false);
            check(held.boss().getHealth() == hp - 30 && near(held.p(0).getEnergy(), 5), "Held/OS repeat J farmed damage or energy");

            Fixture cap = new Fixture(1, hero, 1, true); cap.target(0, 30); set(cap.p(0), "energy", 99); hp = cap.boss().getHealth();
            cap.press(KeyEvent.VK_J); waitHit(cap, hp); check(near(cap.p(0).getEnergy(), 100), "Melee energy exceeded cap100");

            Fixture skill = new Fixture(1, hero, 1, true); skill.target(0, 30); set(skill.p(0), "energy", 50); hp = skill.boss().getHealth();
            skill.press(KeyEvent.VK_U); check(near(skill.p(0).getEnergy(), 30), "U cost changed"); waitHit(skill, hp);
            check(near(skill.p(0).getEnergy(), 30), "Energy-spending skill refunded melee energy");

            Fixture interrupted = new Fixture(1, hero, 1, true); interrupted.target(0, 30); set(interrupted.p(0), "energy", 0); hp = interrupted.boss().getHealth();
            interrupted.press(KeyEvent.VK_J); interrupted.p(0).takeDamage(new DamageInfo(1, 0, 0, false)); interrupted.ticks(60);
            check(interrupted.boss().getHealth() == hp && near(interrupted.p(0).getEnergy(), 0), "Cancelled startup emitted damage/energy later");
        }
        System.out.println("Whiff/immune/interruption give no energy; no whiff cancel; held input/dedup/cap protected; skills do not refund PASS");
    }

    static void movingHitboxAndGroundReach() throws Exception {
        for (int hero = 0; hero < 3; hero++) for (int direction : new int[]{-1, 1}) {
            Fixture f = new Fixture(1, hero, 1, true); Player p = f.p(0);
            p.x = direction > 0 ? 150 : 1250; f.boss().x = direction > 0 ? 1450 : 100;
            p.update(0, f.screen); f.boss().update(0, f.screen);
            int move = direction > 0 ? KeyEvent.VK_D : KeyEvent.VK_A;
            key(f.input, move, true); f.tick(); f.press(KeyEvent.VK_J);
            while (!attack(p, 0).isActive()) f.tick();
            Hitbox box = f.hits().stream().filter(h -> h.owner == p && h.friendly && h.damage.meleeStep == 1).findFirst().orElseThrow();
            double offset = box.rect.x - p.x, x = box.rect.x;
            f.tick();
            check((box.rect.x - x) * direction > 0, "Moving melee left its hitbox behind");
            check(near(box.rect.x - p.x, offset), "Melee offset drifted while moving");
            check(near(attack(p, 0).region().x, box.rect.x), "Rendered swing region disagrees with damage box");
            key(f.input, move, false);
        }
        for (int chapter = 1; chapter <= 2; chapter++) for (int hero = 0; hero < 3; hero++) {
            Fixture f = new Fixture(chapter, hero, 1, true); f.target(0, 60);
            int hp = f.boss().getHealth(); f.press(KeyEvent.VK_J); waitHit(f, hp);
            check(f.boss().getHealth() == hp - 30, "Ground melee cannot reach the displayed engagement distance");
            check(f.p(0).grounded, "Core reach required forcing the player airborne");
        }
        System.out.println("Both directions: melee follows movement and telegraph; all heroes reach ground Guardian/hovering Core from a usable gap PASS");
    }

    static void passiveContactAndLiveAI() throws Exception {
        for (int chapter = 1; chapter <= 2; chapter++) {
            Fixture isolated = new Fixture(chapter, 0, 1, true); isolated.target(0, -20);
            int hp = isolated.p(0).getHealth(); isolated.ticks(100);
            check(isolated.p(0).getHealth() == hp, "Idle/walking boss body contact drains health");
            check(!isolated.boss().isContactDamageEnabled(), "Passive boss contact flag enabled");
        }
        for (int chapter = 1; chapter <= 2; chapter++) for (int hero = 0; hero < 3; hero++) {
            Fixture f = new Fixture(chapter, hero, 1, false); Player p = f.p(0);
            p.x = 500; p.y = f.level.groundY() - p.height; p.grounded = true;
            f.boss().x = 900; f.boss().y = f.level.groundY() - f.boss().height - (chapter == 2 ? 24 : 0);
            p.update(0, f.screen); f.boss().update(0, f.screen);
            key(f.input, KeyEvent.VK_D, true);
            for (int n = 0; n < 120 && f.boss().x - p.right() > 45; n++) f.tick();
            key(f.input, KeyEvent.VK_D, false);
            check(f.boss().x - p.right() <= 60, "Live approach could not enter melee range");
            int hp = f.boss().getHealth(), playerHP = p.getHealth();
            f.press(KeyEvent.VK_J); waitHit(f, hp); link(f, p, 0, KeyEvent.VK_J);
            waitHit(f, hp - 30); link(f, p, 1, KeyEvent.VK_J); waitHit(f, hp - 70);
            check(f.boss().getHealth() == hp - 105, "Running AI broke an earned three-hit opening");
            check(p.getHealth() == playerHP, "Boss retaliated inside the confirmed basic chain");
            f.capture("chapter" + chapter + "-hero" + hero + "-live-chain");
            boolean bossResponded = false;
            for (int n = 0; n < 180 && f.game.getState() == GameState.PLAYING; n++) {
                f.tick();
                bossResponded |= get(f.boss(), "action") != null || !f.shots().isEmpty() || p.getHealth() < playerHP;
            }
            check(bossResponded, "Boss remained locked after the combo instead of counterattacking");
            check(f.game.saves == 0, "Live fixture wrote save data");
        }
        System.out.println("Real live AI: approach/confirmed chain without body-trade, then boss counterattack; no permanent lock PASS");
    }

    static Object beginDive(Fixture f) throws Exception {
        // Run the actual alternating burst -> dive cycle, rather than setting a
        // private diving flag. Only projectile lists are cleared to isolate contact.
        for (int attempt = 0; attempt < 3; attempt++) {
            f.boss().specialAttack(f.screen);
            for (int n = 0; n < 150 && get(f.boss(), "action") != null; n++) {
                if (f.boss().getContactActionId() != null) return f.boss().getContactActionId();
                f.tick(); f.shots().clear();
            }
        }
        throw new AssertionError("Void Core never entered its real contact dive");
    }

    static void diveContactDedup() throws Exception {
        Fixture f = new Fixture(2, 0, 2, true);
        f.p(0).x = 150; f.p(1).x = 300; f.boss().x = 1200;
        Object firstAction = beginDive(f);
        Player p = f.p(0), guard = f.p(1);
        for (Player player : f.screen.players()) {
            player.x = f.boss().centerX() - player.width / 2; player.y = f.boss().y + 10;
            player.update(0, f.screen); set(player, "invuln", 0);
        }
        ((PlayerDefense)get(guard, "defense")).activate();
        int hp = p.getHealth(), guardHP = guard.getHealth();
        call(f.screen, "resolveContact");
        check(p.getHealth() == hp - f.boss().getContactDamage(), "Dive did not deal its one contact hit");
        check(guard.getHealth() == guardHP && !guard.isBlocking(), "Dive did not consume exactly one guard");
        hp = p.getHealth();
        for (int n = 0; n < 8; n++) {
            set(p, "invuln", 0); set(guard, "invuln", 0); call(f.screen, "resolveContact");
        }
        check(p.getHealth() == hp && guard.getHealth() == guardHP, "Same dive repeatedly hurt/consumed a player after invulnerability ended");
        p.x = 0; call(f.screen, "resolveContact");
        p.x = f.boss().centerX() - p.width / 2; call(f.screen, "resolveContact");
        check(p.getHealth() == hp, "Separating/re-entering the same dive reset its contact hit");
        for (Player player : f.screen.players()) player.x = 100;
        for (int n = 0; n < 150 && get(f.boss(), "action") != null; n++) { f.tick(); f.shots().clear(); }
        Object secondAction = beginDive(f);
        check(secondAction != firstAction, "New dive reused the old contact identity");
        p.x = f.boss().centerX() - p.width / 2; p.y = f.boss().y + 10; set(p, "invuln", 0);
        hp = p.getHealth(); call(f.screen, "resolveContact");
        check(p.getHealth() == hp - f.boss().getContactDamage(), "A distinct later dive could never hurt again");
        System.out.println("Actual Core dive: one contact per action/player, guard consumed once, no re-entry farming; later dive can hit PASS");
    }

    static void boundedStaggerAndVisualIndependence() throws Exception {
        for (int chapter = 1; chapter <= 2; chapter++) {
            Fixture f = new Fixture(chapter, 0, 1, true); f.p(0).x = 150; f.boss().x = 1100;
            set(f.boss(), "attackTimer", 0);
            int hp = f.boss().getHealth(), acceptedHits = 0, longestLock = 0, lock = 0;
            boolean resistant = false, responseDuringResistance = false;
            for (int n = 0; n < 150; n++) {
                if (n % 3 == 0) {
                    f.boss().takeDamage(new DamageInfo(1, 0, 0, true).withMeleeChain(new DamageInfo.MeleeChain(), 1)); acceptedHits++;
                }
                f.tick();
                if ((double)get(f.boss(), "staggerTimer") > 0 || (double)get(f.boss(), "stun") > 0) lock++;
                else { longestLock = Math.max(longestLock, lock); lock = 0; }
                resistant |= f.boss().isStaggerResistant();
                responseDuringResistance |= f.boss().isStaggerResistant() && get(f.boss(), "action") != null;
            }
            longestLock = Math.max(longestLock, lock);
            check(f.boss().getHealth() == hp - acceptedHits, "Stagger resistance incorrectly became damage immunity");
            check(resistant && responseDuringResistance, "Repeated distinct/co-op-style hits permanently locked boss AI");
            check(longestLock <= 55, "Continuous combat control exceeded .90s budget");

            Fixture visual = new Fixture(chapter, 0, 1, true); visual.p(0).x = 150; visual.boss().x = 1100;
            ((AnimationController)get(visual.boss(), "anim")).add("hurt", new chronorunner.animation.Animation(60, 1.0, false));
            set(visual.boss(), "attackTimer", .35);
            visual.boss().takeDamage(new DamageInfo(1, 0, 0, true));
            boolean responded = false;
            for (int n = 0; n < 60; n++) { visual.tick(); responded |= get(visual.boss(), "action") != null; }
            check(responded, "Combat Hurt duration still depends on completing the 60-second visual clip");
        }
        System.out.println("Continuous stagger is bounded; resistance preserves incoming damage and AI response; Hurt clip duration cannot lock combat PASS");
    }

    static void diagnosticScreenshots() throws Exception {
        try {
            System.setProperty("chronorunner.debugCollision", "true");
            for (int chapter = 1; chapter <= 2; chapter++) {
                Fixture f = new Fixture(chapter, 0, 1, true); f.target(0, 90);
                key(f.input, KeyEvent.VK_D, true); f.press(KeyEvent.VK_J);
                for (int n = 0; n < 60 && !attack(f.p(0), 0).isActive(); n++) f.tick();
                check(attack(f.p(0), 0).isActive(), "Diagnostic scene has no active melee");
                // Suppress only the short white flash in this in-memory scene,
                // so sprite torso and orange receiver can both be inspected.
                set(f.boss(), "hurtFlash", 0);
                f.capture("chapter" + chapter + "-melee-active-debug");
                key(f.input, KeyEvent.VK_D, false);
            }
        } finally {
            System.clearProperty("chronorunner.debugCollision");
        }
        Fixture resistance = new Fixture(1, 0, 1, true); resistance.target(0, 40);
        for (int n = 0; n < 90 && !resistance.boss().isStaggerResistant(); n++) {
            if (n % 3 == 0) resistance.boss().takeDamage(new DamageInfo(1, 0, 0, true)
                    .withMeleeChain(new DamageInfo.MeleeChain(), 1));
            resistance.tick();
        }
        check(resistance.boss().isStaggerResistant(), "Diagnostic resistance cue never became active");
        set(resistance.boss(), "hurtFlash", 0);
        resistance.capture("guardian-stagger-resistance-cue");
        check(!Boolean.getBoolean("chronorunner.debugCollision"), "Diagnostic capture did not restore debug OFF");
        System.out.println("Diagnostic captures: yellow physics/orange hurtbox, active melee vs both bosses, resistance cue; debug OFF at finish");
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(OUTPUT);
        if (args.length > 0 && args[0].equals("--screenshots-only")) { diagnosticScreenshots(); return; }
        energyAndLinks(); invalidAndSkillEnergy(); movingHitboxAndGroundReach(); passiveContactAndLiveAI();
        diveContactDedup(); boundedStaggerAndVisualIndependence();
        check(!Boolean.getBoolean("chronorunner.debugCollision"), "Debug collision left enabled");
        String result = "PASS: " + checks + " combat fairness checks; real Game.update including boss AI; isolated cases labelled; no save writes; debug OFF.\n";
        Files.writeString(OUTPUT.resolve("results.txt"), result); System.out.print(result);
    }
}

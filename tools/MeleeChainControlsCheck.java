import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import chronorunner.combat.Attack;
import chronorunner.combat.DamageInfo;
import chronorunner.core.GameState;
import chronorunner.entity.Player;
import chronorunner.entity.boss.Boss;

/** Real input, player state and PlayScreen damage; fixtures intercept save writes. */
public final class MeleeChainControlsCheck {
    static int checks;
    static void check(boolean value, String why) { checks++; if (!value) throw new AssertionError(why); }
    static IdentitySkillsCheck.Fixture fixture(int chapter, int hero, int count) throws Exception {
        return new IdentitySkillsCheck.Fixture(chapter, hero, count);
    }
    static String clip(Player p) throws Exception { return IdentitySkillsCheck.anim(p); }
    static Attack attack(Player p, int step) throws Exception {
        return (Attack) IdentitySkillsCheck.get(p, step == 0 ? "basicAttack" : step == 1 ? "heavyAttack" : "launcherAttack");
    }
    static void done(IdentitySkillsCheck.Fixture f, Attack attack) throws Exception {
        int frames = 0;
        while (attack.isBusy() && frames++ < 150) f.tick();
        check(!attack.isBusy(), "Committed action never completed");
    }
    static void chains() throws Exception {
        String[] clips = {"light_1", "heavy", "launcher"};
        int[] damage = {30, 40, 35};
        for (int chapter = 1; chapter <= 2; chapter++) for (int hero = 0; hero < 3; hero++) for (int slot = 0; slot < 2; slot++) {
            var f = fixture(chapter, hero, 2); Player p = f.p(slot); f.target(p);
            int key = Player.keysFor(slot)[3], hp = f.level.boss().getHealth();
            double energy = p.getEnergy(); f.press(key);
            for (int step = 0; step < 3; step++) {
                check(clip(p).equals(clips[step]), "Wrong J stage: " + chapter + "/" + hero + "/" + slot + "/" + step);
                Attack action = attack(p, step);
                boolean queued = false, captured = false;
                for (int n = 0; action.isBusy() && n < 150; n++) {
                    if (!captured && action.isActive()) {
                        captured = true;
                        if (chapter == 1 && hero == 0 && slot == 0) f.capture("j-chain-" + clips[step]);
                    }
                    if (step < 2 && !queued && action.progress() >= 1 - 3.0 / action.getData().totalFrames()) {
                        f.press(key); queued = true;
                        check(clip(p).equals(clips[step]), "Buffered J cancelled recovery/changed animation early");
                    } else f.tick();
                }
                hp -= damage[step];
                check(f.level.boss().getHealth() == hp, "Distinct melee move lost/doubled damage at stage " + step);
                check(captured, "Missing active window");
            }
            f.ticks(80);
            check(f.level.boss().getHealth() == hp, "Chain repeated without a fresh press");
            check(p.getEnergy() == energy, "Melee chain consumed skill energy");
            check(f.p(1 - slot).getEnergy() == 100, "J affected other player's energy");
            f.press(key); check(clip(p).equals("light_1"), "Fourth fresh press did not restart at normal");
        }
        System.out.println("Three heroes x two slots x two chapters: buffered normal/heavy/launcher, actual damage30/40/35 once, unchanged windows PASS");
    }
    static void edgesAndReset() throws Exception {
        for (int hero = 0; hero < 3; hero++) {
            var held = fixture(1, hero, 1); Player p = held.p(0);
            IdentitySkillsCheck.key(held.input, KeyEvent.VK_J, true); held.tick();
            for (int n = 0; n < 150; n++) { // OS repeat while held must not create new edges.
                IdentitySkillsCheck.key(held.input, KeyEvent.VK_J, true); held.tick();
                check(!clip(p).equals("heavy") && !clip(p).equals("launcher"), "Held J auto chained");
            }
            IdentitySkillsCheck.key(held.input, KeyEvent.VK_J, false); held.tick();
            held.press(KeyEvent.VK_J); check(clip(p).equals("light_1"), "Expired chain did not reset");

            var early = fixture(1, hero, 1); early.press(KeyEvent.VK_J); early.press(KeyEvent.VK_J);
            done(early, attack(early.p(0), 0)); early.ticks(5);
            check(!attack(early.p(0), 1).isBusy(), "Expired early buffer replayed Heavy");
            early.press(KeyEvent.VK_J); check(clip(early.p(0)).equals("heavy"), "Fresh press within .6s did not link Heavy");

            for (int interrupt : new int[]{KeyEvent.VK_K, KeyEvent.VK_S, KeyEvent.VK_U, KeyEvent.VK_L}) {
                var f = fixture(1, hero, 1); f.press(KeyEvent.VK_J); done(f, attack(f.p(0), 0));
                f.press(interrupt); f.ticks(45); f.press(KeyEvent.VK_J);
                check(clip(f.p(0)).equals("light_1"), "Dash/block/skill/charge failed to reset chain: " + interrupt);
            }
            var hurt = fixture(1, hero, 1); hurt.press(KeyEvent.VK_J);
            Attack normal = attack(hurt.p(0), 0);
            while (normal.progress() < 1 - 4.0 / normal.getData().totalFrames()) hurt.tick();
            hurt.press(KeyEvent.VK_J); hurt.p(0).takeDamage(new DamageInfo(1, 0, 0, false)); hurt.ticks(40);
            check(!attack(hurt.p(0), 1).isBusy(), "Hurt retained buffered Heavy");
            hurt.press(KeyEvent.VK_J); check(clip(hurt.p(0)).equals("light_1"), "Hurt did not restart chain");
        }
        var f = fixture(1, 0, 1); Player p = f.p(0);
        f.press(KeyEvent.VK_H); check(!attack(p, 0).isBusy() && !attack(p, 1).isBusy(), "Retired H still attacks");
        f.press(KeyEvent.VK_SHIFT); check(!clip(p).equals("dash"), "Retired Shift still dashes");
        f.press(KeyEvent.VK_W); check(!p.grounded && p.vy < 0, "W did not jump");
        f.ticks(6); f.press(KeyEvent.VK_W); check(p.vy < -600, "W did not double jump");
        f.ticks(100); f.press(KeyEvent.VK_K); check(clip(p).equals("dash") && p.grounded, "K jumped instead of dashing");
        System.out.println("Press edges, buffer expiry, timeout, action/hurt reset, W double jump, K dash and retired bindings PASS");
    }
    static void damageGate() throws Exception {
        for (int chapter = 1; chapter <= 2; chapter++) {
            var f = fixture(chapter, 0, 1); Boss boss = f.level.boss();
            var chain = new DamageInfo.MeleeChain(); var foreign = new DamageInfo.MeleeChain();
            DamageInfo first = new DamageInfo(30, 0, 0, true).withMeleeChain(chain, 1);
            int hp = boss.getHealth(); boss.takeDamage(first); hp -= 30;
            boss.takeDamage(first);
            boss.takeDamage(new DamageInfo(40, 0, 0, true).withMeleeChain(foreign, 2));
            boss.takeDamage(new DamageInfo(35, 0, 0, true).withMeleeChain(chain, 3));
            boss.takeDamage(new DamageInfo(40, 0, 0, true));
            check(boss.getHealth() == hp, "Duplicate, unrelated source, skipped stage or skill bypassed iframe");
            DamageInfo second = new DamageInfo(40, 0, 0, true).withMeleeChain(chain, 2);
            boss.takeDamage(second); hp -= 40; boss.takeDamage(second);
            check(boss.getHealth() == hp, "Second distinct move must hit exactly once");
            boss.takeDamage(new DamageInfo(35, 0, 0, true).withMeleeChain(chain, 3)); hp -= 35;
            boss.takeDamage(new DamageInfo(30, 0, 0, true).withMeleeChain(foreign, 1));
            check(boss.getHealth() == hp, "Finisher/new chain iframe policy broken");
            Player p = f.p(0); int playerHP = p.getHealth(); p.takeDamage(first); p.takeDamage(second);
            check(p.getHealth() == playerHP - 30, "Melee tokens bypassed player invulnerability");
        }
        System.out.println("Boss iframe exception is per chain/target/ordered step; duplicates, skipped steps, other players/skills and player iframe protected PASS");
    }
    static void pauseAndEnd() throws Exception {
        var f = fixture(1, 0, 1); Player p = f.p(0); f.press(KeyEvent.VK_J);
        Attack normal = attack(p, 0);
        while (normal.progress() < .8) f.tick();
        f.press(KeyEvent.VK_J); double before = normal.progress();
        f.realPress(KeyEvent.VK_ESCAPE); check(f.game.getState() == GameState.PAUSED, "Pause failed");
        for (int n = 0; n < 30; n++) f.realTick();
        check(normal.progress() == before, "Pause advanced committed animation");
        f.realPress(KeyEvent.VK_ESCAPE); f.ticks(30);
        check(!attack(p, 1).isBusy(), "Pause retained queued Heavy");
        f.press(KeyEvent.VK_J); check(clip(p).equals("light_1"), "Resume retained chain position");
        p.endCombat(); f.ticks(1);
        check(!normal.isBusy() && !attack(p, 1).isBusy(), "End retained chain/attack");
        check(f.game.saves == 0, "Check wrote progress");
        System.out.println("Pause freezes current pose and drops pending J; resume/end clear chain; saves intercepted PASS");
    }
    public static void main(String[] args) throws Exception {
        Files.createDirectories(IdentitySkillsCheck.OUTPUT);
        chains(); edgesAndReset(); damageGate(); pauseAndEnd();
        check(!Boolean.getBoolean("chronorunner.debugCollision"), "Debug collision enabled");
        String result = "PASS: " + checks + " melee chain/control checks; real InputHandler and PlayScreen collisions; saves intercepted; debug OFF.\n";
        Files.writeString(Path.of("out/identity-v3-check/j-chain-results.txt"), result); System.out.print(result);
    }
}

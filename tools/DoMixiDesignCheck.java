import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import javax.imageio.ImageIO;
import chronorunner.animation.AnimationController;
import chronorunner.combat.Attack;
import chronorunner.combat.DamageInfo;
import chronorunner.core.Game;
import chronorunner.core.GameState;
import chronorunner.entity.Player;
import chronorunner.entity.DriedChickenJar;
import chronorunner.entity.LightningStrike;
import chronorunner.entity.SmokeBlast;
import chronorunner.entity.SwordVisualEffect;
import chronorunner.entity.boss.Boss;
import chronorunner.entity.character.DoMixi;
import chronorunner.gfx.Art;
import chronorunner.gfx.SwordEffects;
import chronorunner.gfx.PixelSprite;
import chronorunner.gfx.SpriteLayout;
import chronorunner.gfx.SpriteSet;

/** Exercises the installed Do Mixi art through the asset loader and regular game input. */
public final class DoMixiDesignCheck {
    private static final String HERO = "do_mixi";
    private static final Path OLD = Path.of("assets/players", HERO, "v4");
    private static final Path BANK = Path.of("assets/players", HERO, "v5");
    private static final Path OUTPUT = Path.of("out/mixi-design-check");
    private static final Map<String, Integer> COUNTS = Map.ofEntries(
            Map.entry("idle", 6), Map.entry("walk", 8), Map.entry("run", 16),
            Map.entry("land", 2), Map.entry("dash", 5), Map.entry("attack", 6),
            Map.entry("light_1", 6), Map.entry("heavy", 5), Map.entry("launcher", 5),
            Map.entry("hurt", 4), Map.entry("death", 6));
    private static final String[] SKILLS = {"skill_u", "skill_i", "ultimate"};
    private static int checks;

    public static void main(String[] args) throws Exception {
        Files.createDirectories(OUTPUT);
        Art.validateAll();
        assets();
        movement();
        normalAttacks();
        skills();
        jarThrow();
        smokeBlast();
        phoneCall();
        var picker = new IdentitySkillsCheck.Fixture(1, 2, 1);
        picker.game.setState(GameState.CHARACTER_SELECT);
        screenshot(picker, "character-selection.png");
        montage();
        check(picker.game.saves == 0, "The picker check wrote progress");
        System.out.println("PASS: " + checks + " Do Mixi design checks; captures in " + OUTPUT);
    }

    private static void assets() throws IOException {
        Properties metadata = properties(BANK);
        check("5".equals(metadata.getProperty("version")), "Missing Do Mixi v5 metadata");
        for (String clip : Art.PLAYER_ANIMATIONS) {
            SpriteSet active = Art.playerAnim(HERO, clip);
            int count = Integer.parseInt(metadata.getProperty(clip + ".frames"));
            check(count == COUNTS.getOrDefault(clip, count), "Incorrect authored count for " + clip);
            check(active.count() == count, "Runtime frame count differs for " + clip);
            SpriteLayout layout = active.layout();
            check(layout != null && layout.width == 192 && layout.height == 192
                    && layout.pivotX == 96 && layout.pivotY == 172
                    && layout.scale == 1 && !layout.centerAnchor, "Incorrect feet pivot for " + clip);
            for (int frame = 0; frame < count; frame++) {
                BufferedImage loaded = raster(active.frame(frame));
                BufferedImage file = readFrame(BANK, clip, frame);
                check(sameImage(loaded, file), "Wrong active pixels for " + clip + " frame " + frame);
                if (clip.equals("walk")) check(bottom(loaded) == 172, "Walk foot lifts off the floor");
                if (clip.equals("run")) check((bottom(loaded) == 172) == (frame % 8 < 4),
                        "Run contact/flight order changed at frame " + frame);
            }
        }
        check(sameImage(raster(Art.player(HERO).frame(0)), readFrame(BANK, "idle", 0))
                && !sameImage(raster(Art.player(HERO).frame(0)), readFrame(OLD, "idle", 0)),
                "Picker still uses the old Do Mixi art");
        check(sameImage(raster(Art.playerPortrait(HERO)),
                ImageIO.read(BANK.resolve("portrait.png").toFile())), "HUD does not use the new portrait");

        SpriteSet callArt = Art.playerAnim(HERO, "ultimate");
        check(callArt.count() == 7 && "5".equals(metadata.getProperty("ultimate.activeFrame"))
                && "6".equals(metadata.getProperty("ultimate.recoveryFrame")), "Incorrect call timing");
        Path original = Path.of("art/animations/do-mixi-design/phone-call/anh-goi-dien-goc.png");
        check(Files.mismatch(original, BANK.resolve("phone-call.png")) == -1, "Call lost the original photo");
        SpriteSet throwArt = Art.playerAnim(HERO, "skill_u");
        check(throwArt.count() == 7 && "4".equals(metadata.getProperty("skill_u.activeFrame")),
                "U needs four pickup/lift poses before release");
        check(!sameImage(raster(throwArt.frame(4)), readFrame(OLD, "skill_u", 4)),
                "The new throw still uses the old sound-wave pose");
        SpriteSet smokeArt = Art.playerAnim(HERO, "skill_i");
        check(smokeArt.count() == 6 && "4".equals(metadata.getProperty("skill_i.activeFrame"))
                && "5".equals(metadata.getProperty("skill_i.recoveryFrame")), "Incorrect smoke pose timing");
        BufferedImage prop = ImageIO.read(BANK.resolve("shisha.png").toFile());
        check(prop.getWidth() == 96 && prop.getHeight() == 96 && prop.getRGB(0,0) >>> 24 == 0,
                "Shisha prop has wrong size or transparency");
        for (String hero : new String[]{"phan_si_kien", "ronaldo"}) {
            for (String clip : new String[]{"idle", "run", "light_1"}) {
                Path other = Path.of("assets/players", hero, "v4");
                check(sameImage(raster(Art.playerAnim(hero, clip).frame(0)), readFrame(other, clip, 0)),
                        "Unrelated hero changed: " + hero + " / " + clip);
            }
        }
    }

    private static void movement() throws Exception {
        var f = new IdentitySkillsCheck.Fixture(1, 2, 1);
        Player p = f.p(0);
        check(HERO.equals(p.characterId()) && p.getMaxHealth() == 160 && p.width == 44 && p.height == 62,
                "Equipped hero, HP or collision geometry changed");
        screenshot(f, "idle-hud.png");
        checkCycle(f, "run", 250, 16);
        checkCycle(f, "walk", 112.5, 8);

        f = new IdentitySkillsCheck.Fixture(1, 2, 1);
        p = f.p(0);
        f.realPress(KeyEvent.VK_W);
        check(!p.grounded && p.vy < 0 && IdentitySkillsCheck.anim(p).equals("jump"),
                "Jump input did not select the new takeoff poses");
        screenshot(f, "jump.png");
        var airStates = new HashSet<String>();
        for (int n = 0; n < 100; n++) {
            f.realTick();
            airStates.add(IdentitySkillsCheck.anim(p));
        }
        check(airStates.contains("fall") && airStates.contains("land") && p.grounded,
                "Airtime did not progress through the fall and landing clips");
        f.realPress(KeyEvent.VK_K);
        check(IdentitySkillsCheck.anim(p).equals("dash"), "Dash startup did not select the new clip");
        f.realTick();
        check(IdentitySkillsCheck.anim(p).equals("dash") && Math.abs(p.vx) == 620,
                "Dash input, speed or clip changed");
        screenshot(f, "dash.png");
        check(f.game.saves == 0, "Movement check wrote progress");
    }

    private static void checkCycle(IdentitySkillsCheck.Fixture f, String clip, double speed, int count)
            throws Exception {
        Player p = f.p(0);
        if (clip.equals("walk")) p.applySlow(3);
        IdentitySkillsCheck.key(f.input, KeyEvent.VK_D, true);
        var seen = new HashSet<Integer>();
        for (int i = 0; i < 80; i++) {
            f.realTick();
            AnimationController anim = (AnimationController) IdentitySkillsCheck.get(p, "anim");
            check(anim.currentName().equals(clip) && Math.abs(p.vx - speed) < 1e-6,
                    "Wrong ground state or speed during " + clip);
            seen.add(anim.current().frame());
            if (i == 11) screenshot(f, clip + ".png");
        }
        check(seen.size() == count && seen.contains(0) && seen.contains(count - 1),
                "State skipped part of the " + clip + " cycle: " + seen);
        IdentitySkillsCheck.key(f.input, KeyEvent.VK_D, false);
    }

    private static void normalAttacks() throws Exception {
        Properties metadata = properties(BANK);
        String[] states = {"light_1", "heavy", "launcher"};
        String[] fields = {"basicAttack", "heavyAttack", "launcherAttack"};
        int[] damage = {30, 40, 35};
        for (int i = 0; i < 2; i++) {
            var f = new IdentitySkillsCheck.Fixture(1, 2, 2);
            Player p = f.p(i);
            Boss target = f.level.boss();
            for (int step = 0; step < states.length; step++) {
                f.target(p);
                int hp = target.getHealth();
                f.press(Player.keysFor(i)[3]);
                check(IdentitySkillsCheck.anim(p).equals(states[step]), "Wrong J combo state for slot " + i);
                int startup = ((Attack) IdentitySkillsCheck.get(p, fields[step])).getData().startupFrames;
                f.ticks(startup - 1);
                check(target.getHealth() == hp, "Normal attack hit before the active window");
                f.tick();
                check(target.getHealth() == hp - damage[step], "Normal attack damage or timing changed");
                AnimationController controller = (AnimationController) IdentitySkillsCheck.get(p, "anim");
                int active = Integer.parseInt(metadata.getProperty(states[step] + ".activeFrame"));
                int recovery = Integer.parseInt(metadata.getProperty(states[step] + ".recoveryFrame"));
                check(controller.current().frame() >= active && controller.current().frame() < recovery,
                        "Normal attack did not show the contact pose on hit");
                if (i == 0) screenshot(f, states[step] + ".png");
                f.ticks(50 - startup);
                check(target.getHealth() == hp - damage[step], "Normal attack hit twice");
            }
            check(f.game.saves == 0, "Combat check wrote progress");
        }
    }

    private static void skills() throws Exception {
        int[] costs = {20, 30, 100}, damage = {40, 45, 120};
        for (int slot = 0; slot < SKILLS.length; slot++) {
            var f = new IdentitySkillsCheck.Fixture(2, 2, 1);
            Player p = f.p(0);
            f.target(p);
            Boss target = f.level.boss();
            int hp = target.getHealth();
            f.press(IdentitySkillsCheck.skillKey(0, slot));
            check(IdentitySkillsCheck.anim(p).equals(SKILLS[slot]) && p.getEnergy() == 100 - costs[slot],
                    "Skill binding or energy cost changed");
            check(target.getHealth() == hp && f.hits().isEmpty() && f.shots().isEmpty(),
                    "Skill hit before the active window");
            int startup = ((Attack[]) IdentitySkillsCheck.get(p, "skills"))[slot].getData().startupFrames;
            for (int i = 0; i < Math.max(48, startup + 20); i++) {
                IdentitySkillsCheck.set(target, "invuln", 0);
                f.tick();
            }
            check(target.getHealth() == hp - damage[slot] && p.getEnergy() == 100 - costs[slot],
                    "Skill damage or energy changed");
            check(f.game.saves == 0, "Skill check wrote progress");
        }
    }

    private static void jarThrow() throws Exception {
        for (int chapter = 1; chapter <= 2; chapter++) {
            for (int slot = 0; slot <= 1; slot++) {
                int direction = slot == 0 ? 1 : -1;
                var f = new IdentitySkillsCheck.Fixture(chapter, 2, 2);
                f.game.getProgress().setScreenShake(slot == 0);
                Player p = f.p(slot);
                Boss boss = f.level.boss();
                boss.x = 935;
                boss.y = f.level.groundY() - boss.height;
                p.x = boss.centerX() - p.width / 2 - direction * 255;
                p.y = f.level.groundY() - p.height;
                p.grounded = true;
                IdentitySkillsCheck.field(p, "facing").setInt(p, direction);
                int health = boss.getHealth();
                f.press(IdentitySkillsCheck.skillKey(slot, 0));
                check(p.getSkillName(0).equals("Ném Khô Gà"), "Old name still in the skill HUD");
                f.ticks(24);
                check(f.shots().isEmpty() && boss.getHealth() == health, "Jar appeared before release");
                if (chapter == 1 && slot == 0) screenshot(f, "jar-lift.png");
                f.tick();
                check(f.shots().size() == 1 && f.shots().get(0) instanceof DriedChickenJar,
                        "Throw did not release exactly one jar");
                var jar = (DriedChickenJar) f.shots().get(0);
                check(Math.signum(jar.vx) == direction && jar.vy < 0 && shakeTrauma(f) == 0,
                        "Jar did not launch upward and forward, or shake came before impact");
                f.ticks(5);
                if (chapter == 1 && slot == 0) screenshot(f, "jar-flight.png");
                int remaining = 40;
                while (remaining-- > 0 && boss.getHealth() == health) f.tick();
                check(boss.getHealth() == health - 40 && f.shots().isEmpty(),
                        "Jar did not hit the grounded boss once in chapter " + chapter + ", facing " + direction);
                check(impactSmokeCount(f) == 1, "Missing or repeated smoke burst on the hit");
                check((shakeTrauma(f) > 0) == (slot == 0), "Impact ignored screen shake setting");
                f.ticks(6);
                if (chapter == 1 && slot == 0) screenshot(f, "jar-impact.png");
                check(boss.getHealth() == health - 40 && p.getEnergy() == 80 && f.game.saves == 0,
                        "Jar altered the cost or applied extra damage");
            }
        }
        var immune = new IdentitySkillsCheck.Fixture(1, 2, 1);
        immune.game.getProgress().setScreenShake(true);
        immune.target(immune.p(0));
        Boss protectedBoss = immune.level.boss();
        int health = protectedBoss.getHealth();
        IdentitySkillsCheck.set(protectedBoss, "invuln", 10);
        immune.press(KeyEvent.VK_U);
        immune.ticks(65);
        check(protectedBoss.getHealth() == health && impactSmokeCount(immune) == 0 && shakeTrauma(immune) == 0,
                "Jar triggered hit feedback on an immune target");
    }

    private static void smokeBlast() throws Exception {
        for (int chapter = 1; chapter <= 2; chapter++) {
            for (int slot = 0; slot <= 1; slot++) {
                var f = new IdentitySkillsCheck.Fixture(chapter, 2, 2);
                f.game.getProgress().setScreenShake(slot == 0);
                Player p = f.p(slot);
                Boss boss = f.level.boss();
                int facing = slot == 0 ? 1 : -1;
                boss.x = 935;
                boss.y = f.level.groundY() - boss.height;
                p.x = boss.centerX() - p.width / 2 - facing * 255;
                p.y = f.level.groundY() - p.height;
                p.grounded = true;
                IdentitySkillsCheck.field(p, "facing").setInt(p, facing);
                int hp = boss.getHealth();
                f.press(IdentitySkillsCheck.skillKey(slot, 1));
                check(p.getSkillName(1).equals("Phun Khói") && p.getEnergy() == 70, "Smoke name/cost mismatch");
                f.ticks(30);
                if (chapter == 2 && slot == 0) screenshot(f, "shisha-inhale.png");
                f.ticks(11);
                check(f.shots().isEmpty() && hp == boss.getHealth() && shakeTrauma(f) == 0,
                        "Smoke was released or shook during the inhale");
                f.tick();
                check(f.shots().size() == 1 && f.shots().get(0) instanceof SmokeBlast
                        && Math.signum(f.shots().get(0).vx) == facing, "Did not release one forward cloud");
                f.ticks(10);
                check(hp == boss.getHealth() && impactSmokeCount(f) == 0 && shakeTrauma(f) == 0,
                        "Impact effects appeared while smoke was still in flight");
                if (chapter == 2 && slot == 0) screenshot(f, "shisha-flight.png");
                for (int i = 0; i < 32; i++) {
                    IdentitySkillsCheck.set(boss, "invuln", 0);
                    f.tick();
                    if (boss.getHealth() < hp) break;
                }
                check(boss.getHealth() == hp - 45 && impactSmokeCount(f) == 1
                        && f.shots().isEmpty(), "Smoke missed or created multiple impacts");
                check((shakeTrauma(f) > 0) == (slot == 0), "Smoke ignored screen shake setting");
                f.ticks(6);
                if (chapter == 2 && slot == 0) screenshot(f, "shisha-impact.png");
                check(boss.getHealth() == hp - 45 && f.game.saves == 0, "Smoke repeated damage or wrote progress");
            }
        }
        var miss = new IdentitySkillsCheck.Fixture(1, 2, 1);
        miss.game.getProgress().setScreenShake(true);
        miss.target(miss.p(0));
        Boss boss = miss.level.boss();
        int hp = boss.getHealth();
        IdentitySkillsCheck.field(miss.p(0), "facing").setInt(miss.p(0), -1);
        miss.press(KeyEvent.VK_I);
        miss.ticks(100);
        check(boss.getHealth() == hp && impactSmokeCount(miss) == 0 && shakeTrauma(miss) == 0,
                "A cloud fired in the opposite direction hit or shook the camera");
    }

    private static void phoneCall() throws Exception {
        for (int chapter = 1; chapter <= 2; chapter++) {
            int slot = chapter - 1;
            var f = new IdentitySkillsCheck.Fixture(chapter, 2, 2);
            f.game.getProgress().setScreenShake(chapter == 1);
            DoMixi p = (DoMixi) f.p(slot);
            Boss boss = f.level.boss();
            boss.x = 1030;
            boss.y = f.level.groundY() - boss.height;
            p.x = 245;
            int hp = boss.getHealth();
            f.press(IdentitySkillsCheck.skillKey(slot, 2));
            check(p.getSkillName(2).equals("Gọi Điện") && p.getEnergy() == 0 && !p.isCallPhotoVisible(),
                    "Call started with wrong cost or photo state");
            f.ticks(27);
            check(p.isCallPhotoVisible() && boss.getHealth() == hp && shakeTrauma(f) == 0,
                    "The call photo must precede the strike");
            var before = f.screen.captureGameplayFrame().getSubimage(Game.WIDTH / 4,146,330,220);
            if (chapter == 2) screenshot(f, "call-windup.png");
            f.ticks(30);
            check(boss.getHealth() == hp, "Boss lost HP before the strike");
            f.tick();
            check(boss.getHealth() == hp - 120 && f.hits().isEmpty() && f.shots().isEmpty(),
                    "Call did not strike the distant boss directly");
            check(impactSmokeCount(f) == 1 && lightningCount(f) == 1
                    && (shakeTrauma(f) > 0) == (chapter == 1), "Wrong call impact or shake");
            var camera = IdentitySkillsCheck.get(f.screen, "camera");
            IdentitySkillsCheck.set(camera, "shakeX", 6);
            IdentitySkillsCheck.set(camera, "shakeY", -4);
            f.ticks(8);
            var during = f.screen.captureGameplayFrame().getSubimage(Game.WIDTH / 4,146,330,220);
            check(sameImage(before, during), "Photo moved with the gameplay camera");
            if (chapter == 2) screenshot(f, "call-impact.png");
            f.ticks(24);
            check(!p.isCallPhotoVisible() && boss.getHealth() == hp - 120 && f.game.saves == 0,
                    "Call stayed open or hit twice");
        }
        for (boolean interrupt : new boolean[]{true, false}) {
            var f = new IdentitySkillsCheck.Fixture(1, 2, 1);
            f.game.getProgress().setScreenShake(true);
            Boss boss = f.level.boss();
            int hp = boss.getHealth();
            if (!interrupt) IdentitySkillsCheck.set(boss, "invuln", 10);
            f.press(KeyEvent.VK_O);
            f.ticks(30);
            if (interrupt) f.p(0).takeDamage(new DamageInfo(1, 0, 0, false));
            f.ticks(75);
            check(boss.getHealth() == hp && lightningCount(f) == 0 && impactSmokeCount(f) == 0
                    && shakeTrauma(f) == 0 && !((DoMixi) f.p(0)).isCallPhotoVisible(),
                    "Interrupted or rejected call created an impact");
        }
    }

    private static long lightningCount(IdentitySkillsCheck.Fixture f) throws Exception {
        return ((java.util.List<?>) IdentitySkillsCheck.get(f.screen, "particles"))
                .stream().filter(LightningStrike.class::isInstance).count();
    }

    private static long impactSmokeCount(IdentitySkillsCheck.Fixture f) throws Exception {
        var effects = (java.util.List<?>) IdentitySkillsCheck.get(f.screen, "particles");
        return effects.stream().filter(effect -> effect instanceof SwordVisualEffect smoke
                && smoke.kind() == SwordEffects.Kind.SMOKE).count();
    }

    private static double shakeTrauma(IdentitySkillsCheck.Fixture f) throws Exception {
        return (double) IdentitySkillsCheck.get(IdentitySkillsCheck.get(f.screen, "camera"), "trauma");
    }

    private static Properties properties(Path bank) throws IOException {
        var data = new Properties();
        try (var stream = Files.newInputStream(bank.resolve("animation.properties"))) { data.load(stream); }
        return data;
    }

    private static BufferedImage readFrame(Path bank, String clip, int index) throws IOException {
        return ImageIO.read(bank.resolve(clip + "_" + (index + 1) + ".png").toFile());
    }

    private static BufferedImage raster(PixelSprite sprite) {
        var image = new BufferedImage(sprite.width(), sprite.height(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        sprite.draw(g, 0, 0, 1, false);
        g.dispose();
        return image;
    }

    private static boolean sameImage(BufferedImage a, BufferedImage b) {
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) return false;
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                int left = a.getRGB(x, y), right = b.getRGB(x, y);
                if ((left | right) >>> 24 != 0 && left != right) return false;
            }
        }
        return true;
    }

    private static int bottom(BufferedImage a) {
        for (int y = a.getHeight() - 1; y >= 0; y--) {
            for (int x = 0; x < a.getWidth(); x++) if (a.getRGB(x, y) >>> 24 != 0) return y + 1;
        }
        return 0;
    }

    private static void screenshot(IdentitySkillsCheck.Fixture f, String name) throws IOException {
        var image = new BufferedImage(Game.WIDTH, Game.HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        f.game.render(g);
        g.dispose();
        ImageIO.write(image, "png", OUTPUT.resolve(name).toFile());
    }

    private static void montage() throws IOException {
        String[] names = {"character-selection", "idle-hud", "run", "walk", "jump", "dash",
                "light_1", "heavy", "launcher"};
        int width = Game.WIDTH / 2, height = Game.HEIGHT / 2;
        var image = new BufferedImage(width * 3, height * 3, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for (int i = 0; i < names.length; i++) {
            var scene = ImageIO.read(OUTPUT.resolve(names[i] + ".png").toFile());
            g.drawImage(scene, i % 3 * width, i / 3 * height, width, height, null);
        }
        g.dispose();
        ImageIO.write(image, "png", OUTPUT.resolve("gameplay-and-picker.png").toFile());
    }

    private static void check(boolean ok, String error) {
        if (!ok) throw new AssertionError(error);
        checks++;
    }
}

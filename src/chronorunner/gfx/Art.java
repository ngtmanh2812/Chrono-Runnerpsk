package chronorunner.gfx;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

/**
 * Every hand-authored sprite in the game, in one place.
 *
 * <p>Grids are written as text blocks: one character per pixel, '.' transparent,
 * the letters mapped by the per-sprite palettes below. Rows are validated as
 * rectangles by {@link PixelSprite#of}, so a miscounted row fails loudly at
 * startup instead of silently shifting the art.
 *
 * <p>Everything is authored facing right and flipped at render time. Players are
 * built by {@link Humanoid} on a 44x62 canvas, exactly the collision box, so
 * they render at 1x; bosses are 24-28 wide and enemies 20 wide, and the render
 * scale is derived from the sprite width so both fill their hitbox alike.
 *
 * <p>Any sprite can be overridden without touching code: drop a PNG in
 * {@code assets/players}, {@code assets/bosses} or {@code assets/enemies} named
 * after the entity (lowercase, runs of other characters become {@code _}, e.g.
 * {@code phan_si_kien.png}). A single {@code .png} is
 * one frame; add {@code _1.png}, {@code _2.png}, ... for an animation. When no
 * file exists the built-in grid above is used, so the game still runs on a bare
 * checkout.
 *
 * <p>Bosses load named frames from {@code assets/bosses/<boss>/<state>_N.png}.
 * Playable characters additionally support one sprite set per animation:
 * {@code assets/players/<id>/<animation>_1.png}, {@code _2.png}, ... where
 * {@code <animation>} is one of {@link #PLAYER_ANIMATIONS}. If the per-id folder
 * is missing, the shared {@code assets/players/chrono_runner} folder is used.
 * That lets every selectable character share the same time-traveller art while
 * keeping their individual stats and skills.
 */
public final class Art {

    private Art() {
    }

    /** Folder scanned for optional sprite overrides, relative to the work dir. */
    private static final File ASSET_ROOT = new File("assets");

    /** Upper bound on {@code _1.png, _2.png, ...} frames read per entity. */
    private static final int MAX_ASSET_FRAMES = 16;

    /** Folder used when a selected character has no art folder of its own. */
    private static final String SHARED_PLAYER_ART = "chrono_runner";

    /** Animation names a player sprite set can provide. */
    public static final String[] PLAYER_ANIMATIONS = {
            "idle", "walk", "run", "jump", "attack", "special", "hurt", "death", "dash"
    };

    /** Outline / near-black / eye / white / grey, shared by every sprite. */
    private static Palette base() {
        return new Palette()
                .put('K', 0x12101C)
                .put('k', 0x34304E)
                .put('o', 0x18141F)
                .put('e', 0xF2F4FF)
                .put('w', 0xF5F6FF)
                .put('g', 0x9696A5);
    }

    // ----- Palettes --------------------------------------------------------

    // Colours below are sampled from the reference photographs of each person.
    private static Palette phanPalette() {
        return base()
                .put('s', 0xF0C9A8).put('S', 0xCE9C78)
                .put('h', 0x20242C).put('H', 0x3E4450)
                .put('b', 0x2E6BD6).put('B', 0x6FA6F0).put('n', 0x1B448C)
                .put('p', 0x343234).put('P', 0x54525A)
                .put('y', 0xFFCD46);
    }

    private static Palette ronaldoPalette() {
        return base()
                .put('s', 0xCFBEAB).put('S', 0xA6907A)
                .put('h', 0x140F12).put('H', 0x2E2830)
                .put('b', 0xB82028).put('B', 0xE8555A).put('n', 0x6E0E14)
                .put('p', 0x60664A).put('P', 0x868C68)
                .put('y', 0xE8C24A);
    }

    private static Palette doMixiPalette() {
        return base()
                .put('s', 0xCDD3C4).put('S', 0xA4928C)
                .put('h', 0x14101C).put('H', 0x2A2436)
                .put('b', 0x1D2244).put('B', 0x353E6E).put('n', 0x110921)
                .put('p', 0x2A2C3A).put('P', 0x444759)
                .put('y', 0xE0715A);
    }

    private static Palette guardianPalette() {
        return base()
                .put('m', 0x5A78C8).put('M', 0xAAC8FF).put('c', 0xF0E6B4)
                .put('h', 0xD9C79A)
                .put('e', 0x8CFFFF).put('r', 0xFF5A5A);
    }

    private static Palette beastPalette() {
        return base()
                .put('b', 0x50B46E).put('B', 0x96EBAF).put('n', 0x2C6E46)
                .put('r', 0xFF4646);
    }

    private static Palette knightPalette() {
        return base()
                .put('m', 0xAA8C5A).put('M', 0xFFDC8C).put('e', 0xFFC85A)
                .put('r', 0xC83C46);
    }

    private static Palette frostPalette() {
        return base()
                .put('b', 0x5A96D2).put('B', 0xC8F0FF).put('c', 0xE8FFFF)
                .put('n', 0x284672);
    }

    private static Palette paradoxPalette() {
        return base()
                .put('b', 0x8C46BE).put('B', 0xEB96FF).put('m', 0x8C46BE)
                .put('h', 0xE6D2B8)
                .put('r', 0xFF3C3C);
    }

    private static Palette batPalette() {
        return base()
                .put('b', 0xC88CF0).put('B', 0xE8C8FF).put('r', 0xFF4646);
    }

    private static Palette slimePalette() {
        return base()
                .put('g', 0x78DCAA).put('B', 0xB4F0D2).put('n', 0x46A878);
    }

    // ----- Players (44x62 template, scale 1 -> exactly the hitbox) ----------
    //
    // The three players share one anatomy and differ only in build, clothing
    // and palette. Widths come from the reference photographs: Ronaldo is a
    // broad-shouldered athlete, Do Mixi heavier still, Phan Si Kien a slim
    // teenager with a hooded top.

    /**
     * The build and clothing of a playable character. A pose of 0 stands still
     * and 1 strides, which is the pair of frames each player is drawn from.
     */
    public static Humanoid.Spec playerSpec(String id, int pose) {
        Humanoid.Spec s;
        switch (id) {
            case "ronaldo":
                s = new Humanoid.Spec();
                s.headWidth = 20;
                s.headHeight = 14;
                s.neckWidth = 9;
                s.shoulderWidth = 26;
                s.waistWidth = 21;
                s.hipWidth = 23;
                s.armWidth = 5;
                s.legWidth = 8;
                s.legGap = 3;
                s.sleeves = Humanoid.Sleeves.SHORT;
                s.lower = Humanoid.Lower.SHORTS;
                break;
            case "do_mixi":
                s = new Humanoid.Spec();
                s.headWidth = 21;
                s.headHeight = 15;
                s.neckWidth = 10;
                s.shoulderWidth = 28;
                s.waistWidth = 25;
                s.hipWidth = 27;
                s.armWidth = 6;
                s.legWidth = 9;
                s.legGap = 2;
                s.sleeves = Humanoid.Sleeves.SHORT;
                s.lower = Humanoid.Lower.TROUSERS;
                break;
            default:
                s = new Humanoid.Spec();
                s.headWidth = 19;
                s.headHeight = 14;
                s.neckWidth = 7;
                s.shoulderWidth = 21;
                s.waistWidth = 18;
                s.hipWidth = 21;
                s.armWidth = 4;
                s.legWidth = 7;
                s.legGap = 3;
                s.sleeves = Humanoid.Sleeves.LONG;
                s.lower = Humanoid.Lower.TROUSERS;
                s.hood = true;
                break;
        }
        s.pose = pose;
        return s;
    }

    private static Palette playerPalette(String id) {
        switch (id) {
            case "ronaldo":
                return ronaldoPalette();
            case "do_mixi":
                return doMixiPalette();
            default:
                return phanPalette();
        }
    }

    private static SpriteSet buildPlayer(String id) {
        return new SpriteSet(
                PixelSprite.of(playerPalette(id), Humanoid.render(playerSpec(id, 0))),
                PixelSprite.of(playerPalette(id), Humanoid.render(playerSpec(id, 1))));
    }


    // ----- Bosses (scale 4) -------------------------------------------------

    // Time Guardian: horned armour, a single glowing visor slit, chest clock.
    private static final String GUARDIAN = """
            ........................
            ..KK..............KK....
            ..KhK............KhK....
            ..KhK..KKKKKK..KhK......
            ...KMMMMMMMMMMMMK.......
            ...KMeeeeeeeeeeMK.......
            ...KMeeeeeeeeeeMK.......
            ...KMMKKKKKKKKMMK.......
            ...KMMMMMMMMMMMMK.......
            ....KmmmmmmmmmmK........
            ....KmmmmmmmmmmK........
            ..KKMMMMMMMMMMMMKK......
            ..KMMmmmmmmmmmmMMK......
            ..KMMmmmmmmmmmmMMK......
            ....KKmmmmmmmmKK........
            ....KmmmmmmmmmmK........
            ....KmmccccmmmmK........
            ....KmmmKcKcmmmK........
            ....KmmmmmmmmmmK........
            ....KmmmmmmmmmmK........
            ....KkkkkkkkkkkK........
            ....KmmmmKmmmmK.........
            ....KmmmmKmmmmK.........
            ....KmmmmKmmmmK.........
            ....KmmmmKmmmmK.........
            ....KmmmmKmmmmK.........
            ....KmmmmKmmmmK.........
            ...KKmmmmKmmmmKK........
            ...KMMMMKMMMMK..........
            ...KMMMMKMMMMK..........
            ...KkkkkKkkkkK..........
            """;

    // Chrono Beast: hunched quadruped, spiked back, red eyes, fanged maw.
    private static final String BEAST = """
            ............................
            .....K...K...K...K..........
            .....KbK.KbK.KbK.KbK........
            ....KbbKbKbbKbKbbKbK........
            ..KKbbbbbbbbbbbbbbbbbbbbKK..
            ..KBBbbbbbbbbbbbbbbbbbbbbK..
            ..KBbbbbbbbbbbbbbbbbbbbbbK..
            ..KbbbbbbbbbbbbbbbbbbrrbbK..
            ..KbbbbbbbbbbbbbbbbbbbbbbK..
            ..KbbbbbbbbbbbbbbnnnnnnK....
            ..KbbbbbbbbbbbbbbwwwwwwK....
            ..KbbbbbbbbbbbbbbKKKKKKK....
            ..KbbbbbbbbbbbbbbwwwwwwK....
            ..KbbbbbbbbbbbbbbbbnnnnK....
            ..KbbbbbbbbbbbbbbbbbbnnK....
            ..KbbbbbbbbbbbbbbbbbbbbbbK..
            ..KbbbbbbbbbbbbbbbbbbbbbbK..
            ..KbKkkKbbbbbbbbbbKkkKbbbK..
            ..KbKkkKbbbbbbbbbbKkkKbbbK..
            ..KbKKKKbbbbbbbbbbKKKKbbbK..
            ..KbwwwwbbbbbbbbbbwwwwbbbK..
            ............................
            """;

    // Temporal Knight: plumed helm, amber visor, pauldrons, greatsword.
    private static final String KNIGHT = """
            ..........rrrr..........
            .........KrrrrK.........
            .........KrrrrK.........
            ......KKKKKKKKKKKK......
            ......KmmmmmmmmmmK......
            ......KMeeeeeeeeMK...mM.
            ......KMeeeeeeeeMK...mM.
            ......KMMKKKKKKMMK...mM.
            .......KmmmmmmmmK....mM.
            .......KmmmmmmmmK....mM.
            ......KKkkkkkkkkKK...mM.
            .....KMMMmmmmmmMMMK..mM.
            .....KMMMmmmmmmMMMK..mM.
            .....KMMmmmmmmmmMMK..mM.
            .....KmmmmmmmmmmmmK..mM.
            .....KmmmmmmmmmmmmK..mM.
            .....KmmmmmmmmmmmmK..mM.
            .....KmmmmmmmmmmmmK..mM.
            ......KmmmmmmmmmmK...mM.
            ......KkkkkkkkkkkK...mM.
            ......KmmmmKmmmmK..KMMMK
            ......KmmmmKmmmmK..KmmK.
            ......KmmmmKmmmmK.......
            ......KmmmmKmmmmK.......
            ......KmmmmKmmmmK.......
            ......KmmmmKmmmmK.......
            ......KmmmmKmmmmK.......
            .....KMMMMKMMMMK........
            .....KMMMMKMMMMK........
            .....KkkkkKkkkkK........
            .....KkkkkKkkkkK........
            """;

    // Frost Paradox: jagged ice crown, hollow glowing eyes, frozen robe.
    private static final String FROST = """
            ....K...K...K...K.......
            ....KcK.KcK.KcK.KcK.....
            ...KcccKcccKcccKcccK....
            ...KccccccccccccccK.....
            ...KbbbbbbbbbbbbbbK.....
            ...KBBbbbbbbbbbbbbK.....
            ...KbbbbbbbbbbbbbbK.....
            ...KbbbbccbbccbbbbK.....
            ...KbbbbccbbccbbbbK.....
            ...KbbbbbbbbbbbbbbK.....
            ...KKbbbbbbbbbbbbKK.....
            ..KBBbbbbbbbbbbbbbbK....
            ..KbbbbbbbbbbbbbbbbK....
            ..KbbbbbbbbbbbbbbbbK....
            ..KbbbbbbbbbbbbbbnnK....
            ..KbbbbbbbbbbbbbbbbK....
            ..KbbbbbbbbbbbbbbbbK....
            ..KbbbbbbbbbbbbbbbbK....
            ..KbbbbbbbbbbbbbbbbK....
            ..KccccbbbbbbbbbbbbK....
            ..KccccbbbbbbbbbbbbK....
            ..KbnnbbbbbbbbbbbnnK....
            ..KbbbbbbbbbbbbbbbbK....
            ..KbbbbbbbbbbbbbbbbK....
            ..KbbbbbbbbbbbbbbbbK....
            ...KbbbbbbbbbbbbbbK.....
            ...KbbbbbbbbbbbbbbK.....
            ....KbbbbbbbbbbbbK......
            ....KbbbbbbbbbbbbK......
            .....KccccccccccK.......
            ......cc....cc....cc....
            .......c....c....c......
            """;

    // The Paradox: the final boss. Horned, many-eyed, winged, clawed.
    private static final String PARADOX = """
            ..KK....................KK..
            ..KhK..................KhK..
            ...KhK................KhK...
            ...KhK...KKKKKKKK...KhK.....
            ....KKKmmmmmmmmmmmmKKK......
            ....KmmmmmmmmmmmmmmK........
            ....KmmmmrrmmmmmmrrmK.......
            ....KmmmmrrmmmmmmrrmK.......
            ....KmmmmmmmrrmmmmmmK.......
            ....KmmmmmmmmmmmmmmK........
            ....KmmmmKKKKKKKKmmK........
            ....KmmmmwKwKwKwKmmK........
            ....KmmmmKKKKKKKKmmK........
            ....KmmmmmmmmmmmmmmK........
            ...KmmmmmmmmmmmmmmmmK.......
            KBBmmmmmmmmmmmmmmmmmmBBK....
            KBBBmmmmmmmmmmmmmmmmBBBK....
            KBBmmmmmmmmmmmmmmmmmmBBK....
            KBBmmmmmmmmmmmmmmmmmmBBK....
            .KBBmmmmmmmmmmmmmmmmBBK.....
            ..KBBmmmmmmmmmmmmmmBBK......
            ...KBBmmmmmmmmmmmmBBK.......
            ....KBmmmmmmmmmmmmBK........
            ....KmmmmmmmmmmmmmmK........
            ....KmmmmmmmmmmmmmmK........
            ....KmmmmmrrmmmmmmmK........
            ....KmmmmmmmmmmmmmmK........
            .....KmmmmmmmmmmmmK.........
            .....KkkkkkkkkkkkkK.........
            .....KmmmmmmKKKKKKmmmmmmK...
            .....KmmmmmmKKKKKKmmmmmmK...
            .....KmmmmmmKKKKKKmmmmmmK...
            .....KwwwwwwKKKKKKwwwwwwK...
            ......KwwwwwKKKKKKwwwwwK....
            .......KKKKKKKKKKKKKK.......
            """;

    // ----- Enemies (20 wide, scale 2) ---------------------------------------

    private static final String BAT_A = """
            ....KK........KK....
            ...KbbK......KbbK...
            ..KbbbbK....KbbbbK..
            ..KbbbbbK..KbbbbbK..
            ...KbbbbKbbKbbbbK...
            ....KKbbbbbbbbKK....
            ......KrrKKrrK......
            ......KbbbbbbK......
            .......KwwwwK.......
            .......KbKKbK.......
            .......KbbbbK.......
            ........KKKK........
            ........KbbK........
            ........KKKK........
            """;

    private static final String BAT_B = """
            ....................
            ....KK........KK....
            ...KbbK......KbbK...
            ....KbbK....KbbK....
            .....KKKbbbbKKK.....
            ......KbbbbbbK......
            ......KrrKKrrK......
            ......KbbbbbbK......
            .......KwwwwK.......
            .......KbKKbK.......
            .......KbbbbK.......
            ........KKKK........
            ........KbbK........
            ........KKKK........
            """;

    private static final String SLIME = """
            ........KKKK........
            ......KKggggKK......
            ....KKggggggggKK....
            ...KggggggggggggK...
            ..KggggggggggggggK..
            ..KggggKKggggKKggK..
            ..KgggKwwKggKwwKgK..
            ..KgggKwoKggKwoKgK..
            ..KggggggggggggggK..
            ..KgggKKKKKKKKgggK..
            ..KggggggggggggggK..
            ..KBBBggggggggBBBK..
            .KBBggggggggggggBBK.
            .KggggggggggggggggK.
            .KggggggggggggggggK.
            KggggggggggggggggggK
            KnnnnnnnnnnnnnnnnnnK
            """;

    // ----- Cache ------------------------------------------------------------

    private static final Map<String, Map<String, SpriteSet>> PLAYER_ANIMS = new HashMap<>();
    private static final Map<String, SpriteSet> BOSSES = new HashMap<>();
    private static final Map<String, Map<String, SpriteSet>> BOSS_ANIMS = new HashMap<>();
    private static final Map<String, SpriteSet> ENEMIES = new HashMap<>();

    /** Idle sprites for a playable character, keyed by {@code characterId()}. */
    public static SpriteSet player(String id) {
        return playerAnim(id, "idle");
    }

    /**
     * Sprites for one player animation. Falls back to {@code run} for
     * {@code dash} (the sheet has no dedicated dash row) and to {@code idle}
     * when a named animation has no art.
     */
    public static SpriteSet playerAnim(String id, String animation) {
        Map<String, SpriteSet> sets = PLAYER_ANIMS.get(id);
        if (sets == null) {
            sets = loadPlayerAnimations(id);
            PLAYER_ANIMS.put(id, sets);
        }
        SpriteSet set = sets.get(animation);
        if (set != null) return set;
        if ("dash".equals(animation)) {
            set = sets.get("run");
            if (set != null) return set;
        }
        set = sets.get("idle");
        if (set != null) return set;
        SpriteSet built = buildPlayer(id);
        sets.put("idle", built);
        return built;
    }

    private static Map<String, SpriteSet> loadPlayerAnimations(String id) {
        Map<String, SpriteSet> sets = new HashMap<>();
        File dir = new File(new File(ASSET_ROOT, "players"), slug(id));
        if (!dir.isDirectory()) {
            dir = new File(new File(ASSET_ROOT, "players"), SHARED_PLAYER_ART);
        }
        for (String animation : PLAYER_ANIMATIONS) {
            PixelSprite[] frames = animationFrames(dir, animation);
            if (frames.length > 0) {
                sets.put(animation, new SpriteSet(frames));
            }
        }
        if (sets.isEmpty()) {
            // Legacy flat override, then the built-in humanoid grid.
            PixelSprite[] asset = assetFrames("players", slug(id));
            SpriteSet built = asset.length > 0 ? new SpriteSet(asset) : buildPlayer(id);
            for (String animation : PLAYER_ANIMATIONS) {
                sets.put(animation, built);
            }
            return sets;
        }
        SpriteSet fallback = sets.containsKey("idle") ? sets.get("idle") : sets.values().iterator().next();
        for (String animation : PLAYER_ANIMATIONS) {
            // Leave dash unresolved so playerAnim() can borrow the run cycle
            // instead of the static idle frame.
            if (!sets.containsKey(animation) && !"dash".equals(animation)) {
                sets.put(animation, fallback);
            }
        }
        return sets;
    }

    /** Reads {@code <dir>/<animation>_1.png}, _2, ... until the first gap. */
    private static PixelSprite[] animationFrames(File dir, String animation) {
        List<PixelSprite> frames = new ArrayList<>();
        for (int i = 1; i <= MAX_ASSET_FRAMES; i++) {
            PixelSprite frame = loadSprite(new File(dir, animation + "_" + i + ".png"));
            if (frame == null) break;
            frames.add(frame);
        }
        return frames.toArray(new PixelSprite[0]);
    }

    /** Boss sprites, keyed by the boss's display name. */
    public static SpriteSet boss(String name) {
        SpriteSet cached = BOSSES.get(name);
        if (cached != null) return cached;
        PixelSprite[] asset = assetFrames("bosses", slug(name));
        SpriteSet built;
        if (asset.length > 0) {
            built = new SpriteSet(asset);
        } else {
            PixelSprite one;
            switch (name) {
                case "Chrono Beast":
                    one = sprite(beastPalette(), BEAST);
                    break;
                case "Temporal Knight":
                    one = sprite(knightPalette(), KNIGHT);
                    break;
                case "Frost Paradox":
                    one = sprite(frostPalette(), FROST);
                    break;
                case "The Paradox":
                case "The Void Core":
                    one = sprite(paradoxPalette(), PARADOX);
                    break;
                default:
                    one = sprite(guardianPalette(), GUARDIAN);
                    break;
            }
            built = new SpriteSet(one);
        }
        BOSSES.put(name, built);
        return built;
    }

    /** Loads a boss's named animation from assets/bosses/<slug>/<state>_N.png. */
    public static SpriteSet bossAnimation(String name, String animation) {
        Map<String, SpriteSet> sets = BOSS_ANIMS.computeIfAbsent(name, key -> new HashMap<>());
        SpriteSet cached = sets.get(animation);
        if (cached != null) return cached;

        File dir = new File(new File(ASSET_ROOT, "bosses"), slug(name));
        PixelSprite[] frames = animationFrames(dir, animation);
        if (frames.length == 0 && !"idle".equals(animation)) {
            // Some concept-sheet rows contain only standalone attack effects,
            // not boss poses. Keep the boss visible while its existing combat
            // effects continue through the gameplay systems.
            frames = animationFrames(dir, "idle");
        }
        SpriteSet built = frames.length > 0 ? new SpriteSet(frames)
                : new SpriteSet(boss(name).frame(0));
        sets.put(animation, built);
        return built;
    }

    /** Enemy sprites, keyed by the enemy's display name. */
    public static SpriteSet enemy(String name) {
        SpriteSet cached = ENEMIES.get(name);
        if (cached != null) return cached;
        PixelSprite[] asset = assetFrames("enemies", slug(name));
        SpriteSet built;
        if (asset.length > 0) {
            built = new SpriteSet(asset);
        } else if ("Chrono Bat".equals(name)) {
            built = new SpriteSet(sprite(batPalette(), BAT_A), sprite(batPalette(), BAT_B));
        } else {
            built = new SpriteSet(sprite(slimePalette(), SLIME));
        }
        ENEMIES.put(name, built);
        return built;
    }

    /**
     * Optional override art for an entity: {@code assets/<kind>/<slug>.png} as a
     * single frame, or {@code <slug>_1.png, _2.png, ...} for an animation. An
     * empty result means "no override", and the caller keeps the built-in grid.
     */
    private static PixelSprite[] assetFrames(String kind, String slug) {
        File dir = new File(ASSET_ROOT, kind);
        PixelSprite single = loadSprite(new File(dir, slug + ".png"));
        if (single != null) {
            return new PixelSprite[] { single };
        }
        List<PixelSprite> frames = new ArrayList<>();
        for (int i = 1; i <= MAX_ASSET_FRAMES; i++) {
            PixelSprite frame = loadSprite(new File(dir, slug + "_" + i + ".png"));
            if (frame == null) break;
            frames.add(frame);
        }
        return frames.toArray(new PixelSprite[0]);
    }

    /** Reads one sprite file, or returns null when it is absent. */
    private static PixelSprite loadSprite(File file) {
        if (!file.isFile()) return null;
        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) return null;
            return PixelSprite.fromImage(img);
        } catch (IOException e) {
            throw new IllegalStateException("cannot read sprite " + file, e);
        }
    }

    /** File-name form of an entity name: lowercase, other runs collapse to '_'. */
    private static String slug(String name) {
        StringBuilder sb = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = Character.toLowerCase(name.charAt(i));
            boolean word = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
            sb.append(word ? c : '_');
        }
        return sb.toString();
    }

    /**
     * Forces every sprite to be built, which runs the rectangle check on all of
     * them. Called at startup so a bad grid fails immediately and loudly rather
     * than on the first frame a boss happens to spawn.
     */
    public static void validateAll() {
        for (String id : new String[] {"phan_si_kien", "ronaldo", "do_mixi"}) {
            for (String animation : PLAYER_ANIMATIONS) {
                playerAnim(id, animation);
            }
        }
        for (String animation : new String[] {
                "idle", "run", "jump", "attack", "hurt", "death", "walk", "special"
        }) {
            bossAnimation("The Time Guardian", animation);
            bossAnimation("The Void Core", animation);
        }
        boss("Chrono Beast");
        boss("Temporal Knight");
        boss("Frost Paradox");
        enemy("Chrono Bat");
        enemy("Time Slime");
    }

    private static PixelSprite sprite(Palette palette, String block) {
        return PixelSprite.of(palette, block.split("\n"));
    }
}

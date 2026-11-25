package chronorunner.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import chronorunner.combat.DamageInfo;
import chronorunner.combat.CombatHitResult;
import chronorunner.combat.Hitbox;
import chronorunner.core.Camera;
import chronorunner.core.Game;
import chronorunner.core.GameSession;
import chronorunner.core.GameState;
import chronorunner.core.InputHandler;
import chronorunner.core.Screen;
import chronorunner.entity.Character;
import chronorunner.entity.FloatingText;
import chronorunner.entity.LightningStrike;
import chronorunner.entity.Particle;
import chronorunner.entity.Player;
import chronorunner.entity.Projectile;
import chronorunner.entity.SwordVisualEffect;
import chronorunner.entity.boss.Boss;
import chronorunner.entity.character.CharacterFactory;
import chronorunner.entity.character.DoMixi;
import chronorunner.entity.enemy.Enemy;
import chronorunner.gfx.PixelArt;
import chronorunner.gfx.CollisionDebug;
import chronorunner.obstacle.Platform;
import chronorunner.world.CombatWorld;
import chronorunner.world.Level;
import chronorunner.world.LevelFactory;
import chronorunner.world.TileType;
import chronorunner.util.Progress;

/**
 * The boss arena. Owns the entity lists, runs collisions/combat resolution and
 * implements {@link CombatWorld} so entities can spawn effects.
 */
public class PlayScreen extends Screen implements CombatWorld {

    /** Ceiling on enemies spawned during play, so a summon loop cannot flood the arena. */
    private static final int MAX_SUMMONED_ENEMIES = 32;

    private final InputHandler input;
    private final Hud hud = new Hud();

    private Level level;
    private Camera camera;

    private final List<Player> players = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<Hitbox> hitboxes = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<FloatingText> texts = new ArrayList<>();
    /** A blocked body contact remains consumed until that pair separates. */
    private final Map<Character, Set<Player>> blockedContacts = new IdentityHashMap<>();
    private Object bossContactAction;
    private final Set<Player> bossContactHits = new HashSet<>();

    private double endTimer = -1;
    private boolean victory;
    private boolean defeat;

    public PlayScreen(Game game) {
        super(game);
        this.input = game.getInput();
    }

    @Override
    public void onEnter() {
        GameSession session = game.getSession();
        level = LevelFactory.create(session.chapter);

        players.clear();
        enemies.clear();
        projectiles.clear();
        hitboxes.clear();
        particles.clear();
        texts.clear();
        blockedContacts.clear();
        bossContactAction = null; bossContactHits.clear();
        endTimer = -1;
        victory = false;
        defeat = false;

        for (int i = 0; i < session.playerCount; i++) {
            Player player = CharacterFactory.create(session.characterIndex[i], i, input,
                    level.playerSpawn(i), level.groundY() - 90);
            if (level.backdrop() != null) {
                player.y = level.groundY() - player.height;
                player.grounded = true;
            }
            players.add(player);
        }
        enemies.addAll(level.enemies());

        camera = new Camera(Game.WIDTH, Game.HEIGHT);
        double focus = players.isEmpty() ? level.width() / 2.0 : players.get(0).centerX();
        camera.snapTo(focus, Game.HEIGHT / 2.0, level.width(), level.height());
    }

    @Override
    public void onExit() {
        for (Player player : players) player.pauseCombatInput();
    }

    @Override
    public void update(double dt) {
        if (endTimer >= 0) {
            updateEnding(dt);
            updateEnd(dt);
            return;
        }
        if (level.boss() == null || level.boss().isDead() || !hasLivingPlayers()) {
            updateEnd(0);
            updateEnding(dt);
            return;
        }
        if (endTimer < 0 && input.wasPressed(KeyEvent.VK_ESCAPE)) {
            game.setState(GameState.PAUSED);
            return;
        }

        for (Player p : players) p.update(dt, this);
        for (Enemy e : enemies) e.update(dt, this);

        Boss boss = level.boss();
        if (boss != null) boss.update(dt, this);

        for (Projectile p : projectiles) p.update(dt, this);
        resolveProjectiles();

        // Resolve before ageing: a hitbox that expires this step must still get
        // its final chance to hit, otherwise a one-frame window deals nothing.
        resolveHitboxes();
        for (Hitbox h : hitboxes) h.update(dt);
        hitboxes.removeIf(h -> !h.isAlive());

        resolveContact();
        separateFighters();

        updateEffects(dt);

        projectiles.removeIf(p -> !p.alive);
        enemies.removeIf(e -> e.isDead());

        updateCamera(dt);
        updateEnd(dt);
    }

    private boolean hasLivingPlayers() {
        for (Player player : players) if (!player.isDead()) return true;
        return false;
    }

    private void updateEnding(double dt) {
        for (Player player : players) player.updateAfterCombat(dt, this);
        if (level.boss() != null) level.boss().updateAfterCombat(dt, this);
        updateEffects(dt);
        updateCamera(dt);
    }

    private void updateEffects(double dt) {
        for (Particle p : particles) p.update(dt, this);
        particles.removeIf(p -> !p.alive);
        for (FloatingText t : texts) t.update(dt, this);
        texts.removeIf(t -> !t.alive);
    }

    private void finishCombat() {
        for (Hitbox hitbox : hitboxes) hitbox.cancel();
        hitboxes.clear();
        projectiles.clear();
        enemies.clear();
        blockedContacts.clear();
        bossContactAction = null; bossContactHits.clear();
        for (Player player : players) player.endCombat();
        if (level.boss() != null) level.boss().endCombat();
    }

    private void updateCamera(double dt) {
        double cx = 0;
        double cy = Game.HEIGHT / 2.0;
        int count = 0;
        for (Player p : players) {
            if (p.isDead()) continue;
            cx += p.centerX();
            cy += p.centerY();
            count++;
        }
        if (count > 0) {
            camera.follow(cx / count, cy / count, level.width(), level.height(), dt);
        }
        camera.update(dt);
    }

    private void updateEnd(double dt) {
        Boss boss = level.boss();
        boolean bossDown = boss == null || boss.isDead();
        boolean allPlayersDown = true;
        for (Player p : players) {
            if (!p.isDead()) allPlayersDown = false;
        }

        if (endTimer < 0) {
            if (bossDown) {
                victory = true;
                endTimer = 1.8;
                celebrateBossDefeat(boss);
            } else if (allPlayersDown) {
                defeat = true;
                endTimer = 1.8;
            }
            // Preserve the existing boss-down priority if both sides are
            // already dead, then latch this outcome and disable combat.
            if (endTimer >= 0) finishCombat();
            return;
        }

        endTimer -= dt;
        if (endTimer > 0) return;

        if (victory) {
            int unlocked = Math.min(game.getSession().chapter + 1, Progress.TOTAL_CHAPTERS);
            game.getProgress().unlockThrough(unlocked);
            game.saveProgress();
            game.setState(GameState.VICTORY);
        } else {
            game.setState(GameState.GAME_OVER);
        }
    }

    private void celebrateBossDefeat(Boss boss) {
        if (boss == null) return;
        shake(0.9);
        for (int i = 0; i < 60; i++) {
            particles.add(new Particle(boss.centerX(), boss.centerY(),
                    (Math.random() - 0.5) * 620, (Math.random() - 0.5) * 620, 8,
                    boss.getName().isEmpty() ? Color.WHITE : new Color(255, 220, 140), 0.9, 0.4));
        }
        texts.add(new FloatingText(boss.centerX() - 70, boss.y - 20, "BOSS DEFEATED", Color.WHITE, 30, 1.6));
    }

    private void resolveHitboxes() {
        Boss boss = level.boss();
        for (Hitbox h : hitboxes) {
            if (!isCombatActive()) return;
            if (!h.isAlive()) continue;
            h.syncOwner();
            if (h.friendly) {
                if (boss != null && !boss.isDead() && !h.hasHit(boss)
                        && h.intersects(boss.hurtbox().rect())) {
                    resolveMeleeHit(h, boss);
                }
                for (Enemy e : enemies) {
                    if (e.isDead() || h.hasHit(e)) continue;
                    if (h.intersects(e.hurtbox().rect())) {
                        resolveMeleeHit(h, e);
                    }
                }
            } else {
                for (Player p : players) {
                    if (p.isDead() || h.hasHit(p)) continue;
                    if (h.intersects(p.hurtbox().rect())) {
                        resolveMeleeHit(h, p);
                    }
                }
            }
        }
    }

    private void resolveMeleeHit(Hitbox hitbox, Character target) {
        CombatHitResult result = applyDamage(target, hitbox.damage);
        if (result.landed() && hitbox.friendly && hitbox.owner instanceof Player player)
            player.confirmMeleeHit(hitbox.damage);
        // Immune overlap is not a hit-confirm. It may connect later in Active.
        if (result.consumed()) hitbox.markHit(target);
    }

    private void resolveProjectiles() {
        Boss boss = level.boss();
        for (Projectile pr : projectiles) {
            if (!isCombatActive()) return;
            if (!pr.alive) continue;

            boolean blocked = false;
            for (Platform p : level.platforms()) {
                if (pr.collidesWithPlatform(p)) {
                    blocked = true;
                    break;
                }
            }
            if (blocked) {
                pr.alive = false;
                pr.onImpact(this);
                burst(pr);
                continue;
            }

            if (pr.isFriendly()) {
                if (boss != null && !boss.isDead() && pr.bounds().intersects(boss.hurtbox().rect())) {
                    applyDamage(boss, pr.damage());
                    pr.alive = false;
                    burst(pr);
                    continue;
                }
                for (Enemy e : enemies) {
                    if (!e.isDead() && pr.bounds().intersects(e.hurtbox().rect())) {
                        applyDamage(e, pr.damage());
                        pr.alive = false;
                        burst(pr);
                        break;
                    }
                }
            } else {
                for (Player p : players) {
                    if (!p.isDead() && pr.bounds().intersects(p.hurtbox().rect())) {
                        if (pr.damagesOnContact()) applyDamage(p, pr.damage());
                        pr.onImpact(this);
                        pr.alive = false;
                        burst(pr);
                        break;
                    }
                }
            }
        }
    }

    private void resolveContact() {
        if (!isCombatActive()) return;
        blockedContacts.entrySet().removeIf(entry -> {
            Character source = entry.getKey();
            entry.getValue().removeIf(p -> p.isDead() || !p.bounds().intersects(source.bounds()));
            return source.isDead() || entry.getValue().isEmpty();
        });
        Boss boss = level.boss();
        Object contactAction = boss == null ? null : boss.getContactActionId();
        if (contactAction != bossContactAction) {
            bossContactAction = contactAction;
            bossContactHits.clear();
        }
        if (contactAction != null && boss.getContactDamage() > 0) {
            for (Player player : players) {
                if (player.isDead() || bossContactHits.contains(player)
                        || !player.bounds().intersects(boss.bounds())) continue;
                double dir = player.centerX() < boss.centerX() ? -1 : 1;
                CombatHitResult result = applyDamage(player,
                        new DamageInfo(boss.getContactDamage(), dir * 260, -260, false));
                if (result.consumed()) bossContactHits.add(player);
            }
        }
        for (Enemy e : enemies) {
            if (!e.isDead() && e.getContactDamage() > 0) {
                contactDamage(e, e.getContactDamage());
            }
        }
    }

    private void contactDamage(Character source, int damage) {
        for (Player p : players) {
            if (p.isDead() || p.isInvulnerable()) continue;
            Set<Player> consumed = blockedContacts.get(source);
            if (consumed != null && consumed.contains(p)) continue;
            if (p.bounds().intersects(source.bounds())) {
                double dir = p.centerX() < source.centerX() ? -1 : 1;
                if (applyDamage(p, new DamageInfo(damage, dir * 260, -260, false)).blocked()) {
                    blockedContacts.computeIfAbsent(source, key -> new HashSet<>()).add(p);
                }
            }
        }
    }

    /** Only accepted HP loss is a HIT; overlap and immunity do not confirm combos. */
    private CombatHitResult applyDamage(Character target, DamageInfo info) {
        if (!isCombatActive()) return CombatHitResult.NONE;
        if (target == null || info == null || info.amount <= 0 || target.isDead()) return CombatHitResult.NONE;
        if (target.isInvulnerableTo(info)) return CombatHitResult.IMMUNE;
        boolean guarding = target instanceof Player && ((Player) target).isBlocking();
        int before = target.getHealth();
        target.takeDamage(info);
        int dealt = before - target.getHealth();
        if (dealt <= 0) return guarding && !((Player) target).isBlocking()
                ? CombatHitResult.BLOCK : CombatHitResult.IMMUNE;

        if (info.slowSeconds > 0) {
            target.applySlow(info.slowSeconds);
        }

        Color color = info.fromPlayer ? new Color(255, 240, 130) : new Color(255, 130, 150);
        texts.add(new FloatingText(target.centerX() - 6, target.y + 2,
                String.valueOf(dealt), color, 20, 0.7));
        if (info.fromPlayer && info.feedback == DamageInfo.Feedback.DO_MIXI_JAR) {
            jarImpact(target, info.knockbackX);
        } else if (info.fromPlayer && info.feedback == DamageInfo.Feedback.DO_MIXI_SMOKE) {
            smokeImpact(target, info.knockbackX);
        } else if (info.fromPlayer && info.feedback == DamageInfo.Feedback.DO_MIXI_CALL) {
            lightningImpact(target);
        } else if (target instanceof Boss boss && info.fromPlayer && info.feedback != DamageInfo.Feedback.NONE) {
            swordImpact(boss, info.feedback);
        } else {
            for (int i = 0; i < 8; i++) {
                particles.add(new Particle(target.centerX() - 4, target.centerY() - 4,
                        (Math.random() - 0.5) * 280, (Math.random() - 0.5) * 280, 5,
                        color, 0.35, 0.6));
            }
            shake(info.fromPlayer ? 0.15 : 0.22);
        }
        return new CombatHitResult(CombatHitResult.Kind.HIT, dealt);
    }

    private void lightningImpact(Character target) {
        var torso = target.hurtbox().rect();
        double cx = torso.x + torso.w / 2, cy = torso.y + torso.h * .29;
        particles.add(SwordVisualEffect.graySmoke(cx, cy + 19, 182));
        particles.add(new LightningStrike(cx, cy));
        Color gold = new Color(255, 219, 103);
        Color white = new Color(255, 249, 212);
        for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI / 6;
            particles.add(new Particle(cx, cy, Math.cos(angle) * 205, Math.sin(angle) * 142,
                    3 + i % 3, i % 2 == 0 ? gold : white, .24 + .025 * (i % 4), .18));
        }
        shake(.68);
    }

    private void smokeImpact(Character target, double knockbackX) {
        var torso = target.hurtbox().rect();
        double cx = torso.x + torso.w * (.5 - .27 * Math.signum(knockbackX));
        double cy = torso.y + torso.h * .66;
        particles.add(SwordVisualEffect.graySmoke(cx, cy, 160));
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            Color tint = i % 2 == 0 ? new Color(220, 232, 238) : new Color(143, 157, 170);
            particles.add(new Particle(cx, cy, Math.cos(angle) * 170, Math.sin(angle) * 130,
                    3 + i % 3, tint, .24, .13));
        }
        shake(.55);
    }

    private void jarImpact(Character target, double knockbackX) {
        var torso = target.hurtbox().rect();
        double direction = Math.signum(knockbackX);
        double cx = torso.x + torso.w * (.5 - .27 * direction);
        double cy = torso.y + torso.h * .43;
        particles.add(SwordVisualEffect.graySmoke(cx, cy, 172));
        for (int i = 0; i < 16; i++) {
            double angle = i * 2 * Math.PI / 16;
            Color spark = i % 3 == 0 ? new Color(255, 241, 201) : new Color(246, 175, 84);
            particles.add(new Particle(cx, cy, 215 * Math.cos(angle), 165 * Math.sin(angle) - 38,
                    3 + i % 3, spark, .25 + (i % 4) * .03, .32));
        }
        shake(.60);
    }

    /** Only actual tagged HP loss reaches this path, including the killing hit. */
    private void swordImpact(Boss target, DamageInfo.Feedback feedback) {
        double size = switch (feedback) {
            case PHAN_SWORD_LIGHT -> 110;
            case PHAN_SWORD_MEDIUM -> 140;
            case PHAN_SWORD_ULTIMATE -> 190;
            default -> throw new IllegalArgumentException("Not a sword-impact profile");
        };
        double intensity = switch (feedback) {
            case PHAN_SWORD_LIGHT -> .24;
            case PHAN_SWORD_MEDIUM -> .30;
            case PHAN_SWORD_ULTIMATE -> .40;
            default -> 0;
        };
        // Core's vulnerable reactor is offset from its physics box. Anchor the
        // smoke to the receiving torso, never the halo, cape or a projectile.
        var torso = target.hurtbox().rect();
        double cx = torso.x + torso.w / 2, cy = torso.y + torso.h / 2;
        particles.add(SwordVisualEffect.impact(cx, cy, size));
        Color spark = new Color(130, 240, 255);
        for (int i = 0; i < 6; i++) {
            particles.add(new Particle(cx - 2, cy - 2,
                    (Math.random() - .5) * 320, (Math.random() - .5) * 320,
                    3, spark, .24, .25));
        }
        shake(intensity);
    }

    /** A horizontal pushbox keeps melee approaches out of passive body overlap. */
    private void separateFighters() {
        Boss boss = level.boss();
        if (boss == null || boss.isDead()) return;
        for (Player player : players) {
            if (player.isDead() || !player.bounds().intersects(boss.bounds())) continue;
            // Players can jump above/below a boss without a sideways teleport.
            if (Math.abs(player.bottom() - boss.bottom()) > player.height * .8) continue;
            double left = boss.x - player.width, right = boss.right();
            double preferred = player.centerX() < boss.centerX() ? left : right;
            double alternate = preferred == left ? right : left;
            double maximum = level.width() - player.width;
            if (preferred < 0 || preferred > maximum) preferred = alternate;
            player.x = Math.max(0, Math.min(maximum, preferred));
        }
    }

    private void burst(Projectile pr) {
        for (int i = 0; i < 6; i++) {
            particles.add(new Particle(pr.centerX(), pr.centerY(),
                    (Math.random() - 0.5) * 220, (Math.random() - 0.5) * 220, 5,
                    new Color(200, 235, 255), 0.3, 0.3));
        }
    }

    @Override
    public void render(Graphics2D g) {
        UiKit.background(g, level.tile().bgTop, level.tile().bgBottom);
        if (level.backdrop() == null) drawBackdrop(g);

        Graphics2D world = (Graphics2D) g.create();
        world.translate(-camera.offsetX(), -camera.offsetY());

        if (level.backdrop() != null) level.backdrop().render(world);

        for (Platform p : level.platforms()) p.render(world);
        for (Enemy e : enemies) e.render(world);

        Boss boss = level.boss();
        if (boss != null) boss.render(world);

        for (Player p : players) p.render(world);
        for (Projectile p : projectiles) p.render(world);
        for (Particle p : particles) p.render(world);
        for (FloatingText t : texts) t.render(world);

        CollisionDebug.render(world, level, players);

        world.dispose();

        for (Player player : players) {
            if (player instanceof DoMixi mixi && mixi.isCallPhotoVisible()) {
                PhoneCallOverlay.draw(g);
                break;
            }
        }
        hud.render(g, level, players);
    }

    /** The live arena and HUD only, captured once when an overlay takes over. */
    public BufferedImage captureGameplayFrame() {
        if (level == null) return null;
        BufferedImage frame = new BufferedImage(Game.WIDTH, Game.HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = frame.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        render(g);
        g.dispose();
        return frame;
    }

    /** Column pitch of the parallax skyline, in pixels. */
    private static final int SKYLINE_STEP = 96;
    /** Columns drawn per layer; enough to cover 1280px plus one off-screen. */
    private static final int SKYLINE_COLUMNS = 17;

    private void drawBackdrop(Graphics2D g) {
        TileType theme = level.tile();
        // Two layers at different scroll speeds. The far one is washed out
        // toward the sky colour and the near one sits close to the platform
        // colour, which is what sells the depth.
        drawSkyline(g, theme, (int) (camera.offsetX() * 0.20), 0.35, 0, 160);
        drawSkyline(g, theme, (int) (camera.offsetX() * 0.44), 0.95, 1, 235);
    }

    /**
     * Draws one parallax layer of hard-edged silhouettes. Shape and height are
     * derived from the column's world index, so they stay attached to the
     * level while scrolling rather than flickering frame to frame.
     */
    private void drawSkyline(Graphics2D g, TileType theme, int offset, double depth,
                             int layer, int alpha) {
        int baseY = Game.HEIGHT - 24;
        Color mixed = PixelArt.mix(theme.bgBottom, theme.platformColor, depth);
        Color body = withAlpha(mixed, alpha);
        Color edge = withAlpha(PixelArt.mix(mixed, Color.WHITE, 0.22), alpha);
        Color lit = withAlpha(theme.accent, layer == 0 ? 40 : 75);

        int shift = offset % SKYLINE_STEP;
        int base = offset / SKYLINE_STEP;
        for (int i = 0; i < SKYLINE_COLUMNS; i++) {
            int index = base + i;
            int x = i * SKYLINE_STEP - shift;
            int h = 80 + hash(index * 37 + layer * 1013) % 170;
            switch (theme) {
                case PAST:
                    drawRuins(g, x, baseY, h, body, edge, lit);
                    break;
                case FUTURE:
                    drawMonolith(g, x, baseY, h, body, edge, lit);
                    break;
            }
        }
    }

    /** Broken City: blocky towers with lit windows and the odd snapped crown. */
    private void drawTower(Graphics2D g, int x, int baseY, int h,
                           Color body, Color edge, Color lit, int index) {
        int w = 58 + hash(index * 7) % 30;
        int left = x + (SKYLINE_STEP - w) / 2;
        int top = baseY - h;
        PixelArt.rect(g, left, top, w, h + 24, body);
        PixelArt.rect(g, left, top, w, 2, edge);
        PixelArt.rect(g, left, top, 2, h + 24, edge);
        PixelArt.rect(g, left + w - 2, top, 2, h + 24, edge);

        for (int wy = top + 12; wy < baseY - 14; wy += 24) {
            for (int wx = left + 8; wx <= left + w - 12; wx += 16) {
                if (hash(wx * 131 + wy * 17 + index) % 5 < 2) {
                    PixelArt.rect(g, wx, wy, 5, 6, lit);
                }
            }
        }
        if (hash(index * 53) % 3 == 0) {
            PixelArt.rect(g, left + w / 4, top - 16, w / 3, 16, body);
            PixelArt.rect(g, left + w / 4, top - 16, w / 3, 2, edge);
        }
    }

    /** Forgotten Forest: thin trunks under broad, flat canopies. */
    private void drawTree(Graphics2D g, int x, int baseY, int h, Color body, Color edge) {
        int trunkW = 16 + h % 10;
        int trunkX = x + (SKYLINE_STEP - trunkW) / 2;
        int top = baseY - h;
        PixelArt.rect(g, trunkX, top + 26, trunkW, h + 24 - 26, body);
        PixelArt.rect(g, trunkX, top + 26, 2, h - 2, edge);
        PixelArt.rect(g, trunkX + trunkW - 2, top + 26, 2, h - 2, edge);

        int canopyW = 76 + h % 16;
        int canopyX = x + (SKYLINE_STEP - canopyW) / 2;
        PixelArt.rect(g, canopyX, top, canopyW, 34, body);
        PixelArt.rect(g, canopyX, top, canopyW, 2, edge);
        PixelArt.rect(g, canopyX, top, 2, 34, edge);
        PixelArt.rect(g, canopyX + canopyW - 2, top, 2, 34, edge);
    }

    /** Ancient Ruins: two standing columns joined by a lintel. */
    private void drawRuins(Graphics2D g, int x, int baseY, int h,
                           Color body, Color edge, Color lit) {
        int w = 62;
        int left = x + (SKYLINE_STEP - w) / 2;
        int top = baseY - h;
        int columnH = Math.max(24, h - 12);
        PixelArt.rect(g, left, top + 12, 14, columnH + 12, body);
        PixelArt.rect(g, left + w - 14, top + 12, 14, columnH + 12, body);
        PixelArt.rect(g, left, top, w, 14, body);
        PixelArt.rect(g, left, top, w, 2, edge);
        PixelArt.rect(g, left, top, 2, 14, edge);
        PixelArt.rect(g, left + w - 2, top, 2, 14, edge);
        PixelArt.rect(g, left, top + 12, 14, 2, edge);
        PixelArt.rect(g, left + w - 14, top + 12, 14, 2, edge);
        PixelArt.rect(g, left + 5, top + 34, 4, 7, lit);
        PixelArt.rect(g, left + w - 9, top + 52, 4, 7, lit);
    }

    /** Frozen Future: stacked, shrinking ice tiers. */
    private void drawSpire(Graphics2D g, int x, int baseY, int h, Color body, Color edge) {
        final int tiers = 4;
        int tierH = Math.max(12, h / tiers);
        int y = baseY;
        for (int t = 0; t < tiers; t++) {
            int tw = 84 - t * 18;
            int tx = x + (SKYLINE_STEP - tw) / 2;
            y -= tierH;
            PixelArt.rect(g, tx, y, tw, tierH + (t == 0 ? 24 : 0), body);
            PixelArt.rect(g, tx, y, tw, 2, edge);
            PixelArt.rect(g, tx, y, 2, tierH, edge);
            PixelArt.rect(g, tx + tw - 2, y, 2, tierH, edge);
        }
    }

    /** Chrono Core: monoliths hanging above the ground with a shard below. */
    private void drawMonolith(Graphics2D g, int x, int baseY, int h,
                              Color body, Color edge, Color lit) {
        int w = 56 + hash(x * 11) % 24;
        int left = x + (SKYLINE_STEP - w) / 2;
        int top = baseY - h;
        int blockH = Math.max(16, h - 30);
        PixelArt.rect(g, left, top, w, blockH, body);
        PixelArt.rect(g, left, top, w, 2, edge);
        PixelArt.rect(g, left, top, 2, blockH, edge);
        PixelArt.rect(g, left + w - 2, top, 2, blockH, edge);
        PixelArt.rect(g, left + w / 2 - 3, top + 14, 6, 16, lit);
        PixelArt.rect(g, left + w / 3, top + blockH + 8, w / 3, 9, body);
    }

    private static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    /** Cheap deterministic hash so silhouettes are stable but look varied. */
    private static int hash(int n) {
        n ^= n >>> 16;
        n *= 0x7feb352d;
        n ^= n >>> 15;
        n *= 0x846ca68b;
        n ^= n >>> 16;
        return n & 0x7fffffff;
    }

    // ----- CombatWorld -----

    @Override
    public boolean isCombatActive() {
        Boss boss = level.boss();
        return game.getState() == GameState.PLAYING && endTimer < 0
                && boss != null && !boss.isDead() && hasLivingPlayers();
    }

    @Override
    public List<Player> players() {
        return players;
    }

    @Override
    public List<Platform> platforms() {
        return level.platforms();
    }

    @Override
    public void addHitbox(Hitbox hitbox) {
        if (isCombatActive()) hitboxes.add(hitbox);
    }

    @Override
    public void addProjectile(Projectile projectile) {
        if (isCombatActive()) projectiles.add(projectile);
    }

    @Override
    public void addParticle(Particle particle) {
        particles.add(particle);
    }

    @Override
    public void addFloatingText(FloatingText text) {
        texts.add(text);
    }

    @Override
    public void addEnemy(Enemy enemy) {
        // Runtime summon budget only: level-authored enemies are appended by
        // LevelBuilder and never come through here. Kept generous so a boss
        // summon is not silently swallowed mid-fight.
        if (isCombatActive() && enemies.size() < MAX_SUMMONED_ENEMIES) {
            enemies.add(enemy);
        }
    }

    @Override
    public void strikeChapterBoss(DamageInfo damage) {
        applyDamage(level.boss(), damage);
    }

    @Override
    public void shake(double amount) {
        if (camera != null && game.getProgress().isScreenShake()) {
            camera.addShake(amount);
        }
    }

    @Override
    public double worldWidth() {
        return level.width();
    }

    @Override
    public double worldHeight() {
        return level.height();
    }
}

package chronorunner.entity;

import java.awt.Color;
import java.awt.Graphics2D;

import chronorunner.combat.DamageInfo;
import chronorunner.gfx.PixelArt;
import chronorunner.world.CombatWorld;

/** Simple damaging orb fired by players, bosses and enemies. */
public class Projectile extends GameObject {
    public enum Visual { ORB, FOOTBALL, SOUND_WAVE }

    private final DamageInfo damage;
    private final boolean friendly;
    private final Color color;
    private final double radius;
    private final double life;
    private double age;
    private final Visual visual;

    public Projectile(double x, double y, double vx, double vy, double radius,
                      DamageInfo damage, boolean friendly, Color color, double life) {
        this(x, y, vx, vy, radius, damage, friendly, color, life, Visual.ORB);
    }

    public Projectile(double x, double y, double vx, double vy, double radius,
                      DamageInfo damage, boolean friendly, Color color, double life, Visual visual) {
        super(x - radius, y - radius, radius * 2, radius * 2);
        this.vx = vx;
        this.vy = vy;
        this.radius = radius;
        this.damage = damage;
        this.friendly = friendly;
        this.color = color;
        this.life = life;
        this.visual = visual;
    }

    @Override
    public void update(double dt, CombatWorld world) {
        age += dt;
        x += vx * dt;
        y += vy * dt;
        if (age >= life || x + width < -40 || x > world.worldWidth() + 40
                || y > world.worldHeight() + 60 || y + height < -200) {
            alive = false;
        }
    }

    @Override
    public void render(Graphics2D g) {
        int cx = (int) centerX();
        int cy = (int) centerY();
        int r = Math.max(2, (int) radius);
        if (visual == Visual.FOOTBALL) {
            PixelArt.disc(g, cx, cy, r + 2, new Color(15, 24, 35));
            PixelArt.disc(g, cx, cy, r, new Color(245, 247, 238));
            int spin = (int)(age * 24) % 4;
            PixelArt.rect(g, cx - 3 + spin, cy - 3, 5, 5, new Color(22, 31, 42));
            PixelArt.rect(g, cx - r + 1, cy + 1, 4, 4, new Color(22, 31, 42));
            PixelArt.rect(g, cx + r - 4, cy - 4, 4, 4, new Color(22, 31, 42));
            for (int i = 1; i <= 3; i++) PixelArt.rect(g, cx - (vx > 0 ? 1 : -1) * (r + i * 9),
                    cy - 2, 6, 3, new Color(color.getRed(), color.getGreen(), color.getBlue(), 160 - i * 30));
            return;
        }
        if (visual == Visual.SOUND_WAVE) {
            for (int i = 0; i < 3; i++) {
                int xx = cx + (vx > 0 ? 1 : -1) * (i * 7 - 7);
                int half = r + i * 3;
                PixelArt.rect(g, xx, cy - half, 3, half * 2, color);
                PixelArt.rect(g, xx - 3, cy - half - 3, 3, 3, Color.WHITE);
                PixelArt.rect(g, xx - 3, cy + half, 3, 3, Color.WHITE);
            }
            return;
        }
        PixelArt.disc(g, cx, cy, r * 2,
                new Color(color.getRed(), color.getGreen(), color.getBlue(), 70));
        PixelArt.disc(g, cx, cy, r, color);
        PixelArt.disc(g, cx, cy, Math.max(1, r / 2), Color.WHITE);
    }

    public DamageInfo damage() {
        return damage;
    }

    public boolean isFriendly() {
        return friendly;
    }
}

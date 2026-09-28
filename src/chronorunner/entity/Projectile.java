package chronorunner.entity;

import java.awt.Color;
import java.awt.Graphics2D;

import chronorunner.combat.DamageInfo;
import chronorunner.gfx.PixelArt;
import chronorunner.world.CombatWorld;

/** Simple damaging orb fired by players, bosses and enemies. */
public class Projectile extends GameObject {

    private final DamageInfo damage;
    private final boolean friendly;
    private final Color color;
    private final double radius;
    private final double life;
    private double age;

    public Projectile(double x, double y, double vx, double vy, double radius,
                      DamageInfo damage, boolean friendly, Color color, double life) {
        super(x - radius, y - radius, radius * 2, radius * 2);
        this.vx = vx;
        this.vy = vy;
        this.radius = radius;
        this.damage = damage;
        this.friendly = friendly;
        this.color = color;
        this.life = life;
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

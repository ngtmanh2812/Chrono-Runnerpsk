package chronorunner.entity;

import java.awt.Color;
import java.awt.Graphics2D;
import chronorunner.gfx.PixelArt;
import chronorunner.world.CombatWorld;

/** A visual ring only. Damage belongs to the cast's single Hitbox. */
public final class SkillPulse extends Particle {
    private final Color color;
    private final double radius, duration;
    private double age;
    public SkillPulse(double x, double y, double radius, double duration, Color color) {
        super(x, y, 0, 0, 1, color, duration, 0);
        this.radius = radius; this.duration = duration; this.color = color;
    }
    @Override public void update(double dt, CombatWorld world) {
        age += dt;
        if (age >= duration) alive = false;
    }
    @Override public void render(Graphics2D g) {
        double t = Math.min(1, age / duration);
        int r = (int)(12 + radius * t);
        Color tint = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(220 * (1 - t)));
        for (int i = 0; i < 48; i++) {
            double angle = Math.PI * 2 * i / 48;
            PixelArt.rect(g, (int)x + (int)(Math.cos(angle) * r),
                    (int)y + (int)(Math.sin(angle) * r * .4), 5, 4, tint);
        }
    }
}

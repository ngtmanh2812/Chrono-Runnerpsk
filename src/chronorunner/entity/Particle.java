package chronorunner.entity;

import java.awt.Color;
import java.awt.Graphics2D;

import chronorunner.core.Physics;
import chronorunner.world.CombatWorld;

/** Tiny fading square used for hit sparks and dash trails. */
public class Particle extends GameObject {

    private final Color color;
    private final double maxLife;
    private double life;
    private final double gravityScale;

    public Particle(double x, double y, double vx, double vy, double size,
                    Color color, double life, double gravityScale) {
        super(x, y, size, size);
        this.vx = vx;
        this.vy = vy;
        this.color = color;
        this.maxLife = life;
        this.life = life;
        this.gravityScale = gravityScale;
    }

    @Override
    public void update(double dt, CombatWorld world) {
        life -= dt;
        if (life <= 0) {
            alive = false;
            return;
        }
        if (gravityScale > 0) {
            vy = Math.min(vy + Physics.GRAVITY * gravityScale * dt, Physics.MAX_FALL);
        }
        x += vx * dt;
        y += vy * dt;
    }

    @Override
    public void render(Graphics2D g) {
        int alpha = (int) (255 * Math.max(0, life / maxLife));
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
        g.fillRect((int) x, (int) y, (int) Math.max(1, width), (int) Math.max(1, height));
    }
}

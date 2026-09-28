package chronorunner.entity;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import chronorunner.world.CombatWorld;

/** Rising text used for damage numbers and skill names. */
public class FloatingText extends GameObject {

    private final String text;
    private final Color color;
    private final Font font;
    private final double maxLife;
    private double life;

    public FloatingText(double x, double y, String text, Color color, int fontSize, double life) {
        super(x, y, 0, 0);
        this.text = text;
        this.color = color;
        this.font = new Font("Dialog", Font.BOLD, fontSize);
        this.maxLife = life;
        this.life = life;
        this.vy = -60;
    }

    @Override
    public void update(double dt, CombatWorld world) {
        life -= dt;
        if (life <= 0) {
            alive = false;
            return;
        }
        vy += 90 * dt;
        x += vx * dt;
        y += vy * dt;
    }

    @Override
    public void render(Graphics2D g) {
        int alpha = (int) (255 * Math.max(0, Math.min(1, life / maxLife)));
        g.setFont(font);
        Color base = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
        g.setColor(new Color(0, 0, 0, alpha / 2));
        g.drawString(text, (int) x + 1, (int) y + 1);
        g.setColor(base);
        g.drawString(text, (int) x, (int) y);
    }

    public String getText() {
        return text;
    }
}

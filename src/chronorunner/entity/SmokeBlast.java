package chronorunner.entity;

import java.awt.Color;
import java.awt.Graphics2D;
import chronorunner.combat.DamageInfo;
import chronorunner.gfx.SwordEffects;
import chronorunner.world.CombatWorld;

/** One forward-moving puff. The overlapping clouds are visual layers. */
public final class SmokeBlast extends Projectile {
    private static final double RADIUS = 27;
    private static final double LIFE = .70;
    private final int direction;
    private double elapsed;

    public SmokeBlast(double mouthX, double mouthY, int direction, DamageInfo damage) {
        super(mouthX + direction * RADIUS, mouthY, 580 * direction, 0, RADIUS,
                damage, true, new Color(174, 184, 194), LIFE);
        this.direction = direction;
        height = 60;
        y = mouthY - height / 2;
    }

    @Override public void update(double dt, CombatWorld world) {
        elapsed += dt;
        super.update(dt, world);
    }

    @Override public void render(Graphics2D g) {
        double growth = Math.min(1, elapsed / .22);
        float fade = (float) Math.min(1, Math.max(0, (LIFE - elapsed) / .13));
        for (int layer = 3; layer >= 0; layer--) {
            double trail = (8 + 9 * growth) * layer;
            double px = centerX() + direction * (7 - trail);
            double py = centerY() + Math.sin(elapsed * 18 + layer * 2.4) * (4 + 5 * growth);
            int frame = 2 + (int) ((elapsed * 16 + layer) % 4);
            SwordEffects.draw(SwordEffects.Kind.SMOKE, frame, g, px, py,
                    .37 + .25 * growth - .05 * layer, direction < 0, fade * (1f - .16f * layer));
        }
    }
}

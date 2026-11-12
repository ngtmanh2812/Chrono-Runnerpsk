package chronorunner.entity;

import java.awt.Color;
import java.awt.Graphics2D;
import chronorunner.world.CombatWorld;

/** Pixel ring: cyan charge pulse or orange/gray barrel detonation. Visual only. */
public final class BossShockEffect extends Particle {
    private double age;
    private final double radius;
    private final boolean explosion;
    public BossShockEffect(double x,double y,double radius,boolean explosion) {
        super(x,y,0,0,1,Color.WHITE,.45,0);this.radius=radius;this.explosion=explosion;
    }
    @Override public void update(double dt,CombatWorld world){age+=dt;if(age>=.45)alive=false;}
    @Override public void render(Graphics2D g) {
        double t=Math.min(1,age/.45),r=radius*(.15+.85*t);
        g.setColor(explosion?new Color(255,172,65,(int)(255*(1-t))):new Color(75,245,235,(int)(255*(1-t))));
        for(int ring=0;ring<3;ring++)for(int i=0;i<96;i++) {
            double a=i*Math.PI/48,rr=Math.max(0,r-ring*7);
            g.fillRect((int)(x+Math.cos(a)*rr)-3,(int)(y+Math.sin(a)*rr)-3,6,6);
        }
    }
}

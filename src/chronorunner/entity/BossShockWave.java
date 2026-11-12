package chronorunner.entity;

import java.awt.Color;
import java.awt.Graphics2D;
import chronorunner.combat.DamageInfo;

/** Forward pressure wave, separate from the explosive rolling barrel. */
public final class BossShockWave extends Projectile {
    public BossShockWave(double x,double y,int direction,DamageInfo damage) {
        super(x,y,direction*480,0,22,damage,false,new Color(90,245,220),.95);
        width=38;height=80;this.x=x-19;this.y=y-40;
    }
    @Override public void render(Graphics2D g) {
        for(int j=0;j<3;j++)for(int i=-12;i<=12;i++) {
            double a=i*Math.PI/24;
            g.setColor(j==0?new Color(225,255,245):new Color(65,220-j*35,205));
            g.fillRect((int)(centerX()+Math.cos(a)*(18-j*7)*(vx<0?-1:1))-2,
                    (int)(centerY()+Math.sin(a)*38)-2,4,5);
        }
    }
}

package chronorunner.entity;

import java.awt.Color;
import java.awt.Graphics2D;
import chronorunner.combat.DamageInfo;

/** White electric shard or broad sonic front, matching Daddy's source FX. */
public final class DaddyBolt extends Projectile {
    private final boolean sonic;
    public DaddyBolt(double x,double y,double vx,double vy,DamageInfo damage,boolean sonic) {
        super(x,y,vx,vy,sonic?30:10,damage,false,new Color(220,245,224),sonic?1.5:2);
        this.sonic=sonic;
        if(sonic){width=60;height=150;this.x=x-30;this.y=y-75;}
    }
    @Override public boolean collidesWithPlatform(chronorunner.obstacle.Platform platform) {
        // Sound passes through arena slabs; players evade its travelling front.
        return !sonic && super.collidesWithPlatform(platform);
    }
    @Override public void render(Graphics2D g) {
        int dir=vx<0?-1:1,cx=(int)centerX(),cy=(int)centerY();
        if(sonic){
            for(int k=0;k<3;k++)for(int i=-18;i<=18;i++){
                double a=i*Math.PI/36;
                g.setColor(k==0?new Color(245,255,235):new Color(120,185-k*25,150));
                g.fillRect(cx+(int)(Math.cos(a)*(28-k*9))*dir,cy+(int)(Math.sin(a)*70),5,5);
            }
        }else{
            g.setColor(new Color(90,145,120));g.fillRect(cx-13,cy-6,26,12);
            g.setColor(new Color(240,255,230));g.fillRect(cx-11,cy-2,22,4);
            g.fillRect(cx-5,cy-6,8,12);
        }
    }
}

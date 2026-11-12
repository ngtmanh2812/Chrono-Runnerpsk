package chronorunner.entity;

import java.awt.Color;
import java.awt.Graphics2D;
import chronorunner.combat.DamageInfo;
import chronorunner.gfx.SwordEffects;
import chronorunner.gfx.SwordEffects.Kind;
import chronorunner.world.CombatWorld;

/** A moving crescent with its own readable blade hitbox, not an orb recolour. */
public final class SwordWave extends Projectile {
    private double waveAge;
    public SwordWave(double x,double y,int facing,DamageInfo damage) {
        super(x,y,facing*780,0,9,damage,true,new Color(70,220,255),1.6);
        width=56;height=72;this.x=x-width/2;this.y=y-height/2;
    }
    @Override public void update(double dt,CombatWorld world) {
        super.update(dt,world);waveAge+=dt;
    }
    @Override public void render(Graphics2D g) {
        // Loop the expanded travelling poses, leaving gathering/dissipation
        // to the stationary cast effect. Derive the range from the real bank.
        int count = SwordEffects.count(Kind.CRESCENT);
        int start = Math.max(0, (count-1)/5), end = Math.max(start+1, (count-1)*4/5);
        int frame = start+(int)(waveAge/.04)%(end-start);
        SwordEffects.draw(Kind.CRESCENT,frame,g,centerX(),centerY(),1,vx<0,
                (float)Math.min(1,Math.max(0,(1.6-waveAge)/.16)));
    }
}

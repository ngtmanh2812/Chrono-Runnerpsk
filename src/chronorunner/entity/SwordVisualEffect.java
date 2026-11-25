package chronorunner.entity;

import java.awt.Color;
import java.awt.Graphics2D;
import chronorunner.gfx.SwordEffects;
import chronorunner.gfx.SwordEffects.Kind;
import chronorunner.world.CombatWorld;

/** Visual-only blade trails and accepted-hit smoke; never a damaging entity. */
public final class SwordVisualEffect extends Particle {
    private final Kind kind;
    private final double duration, scale;
    private final boolean flip;
    private final boolean contactFlash;
    private double age;

    private SwordVisualEffect(Kind kind,double x,double y,double duration,double scale,boolean flip,boolean contactFlash) {
        super(x,y,0,0,1,Color.WHITE,duration,0);
        this.kind=kind;this.duration=duration;this.scale=scale;this.flip=flip;this.contactFlash=contactFlash;
    }

    public static SwordVisualEffect slash(Kind kind,double x,double y,int facing,double duration,double scale) {
        if (kind==Kind.SMOKE) throw new IllegalArgumentException("Use impact for smoke");
        return new SwordVisualEffect(kind,x,y,duration,scale,facing<0,false);
    }

    public static SwordVisualEffect impact(double x,double y,double size) {
        return impactSmoke(x,y,size,true);
    }

    public static SwordVisualEffect graySmoke(double x,double y,double size) {
        return impactSmoke(x,y,size,false);
    }

    private static SwordVisualEffect impactSmoke(double x,double y,double size,boolean contactFlash) {
        double duration=.38+Math.max(0,Math.min(.2,(size-110)*.0025));
        return new SwordVisualEffect(Kind.SMOKE,x,y,duration,size/160.0,false,contactFlash);
    }

    public Kind kind() { return kind; }

    @Override public void update(double dt,CombatWorld world) {
        age+=dt;
        if(age>=duration)alive=false;
    }

    @Override public void render(Graphics2D g) {
        double t=Math.min(1,age/duration);
        int frame=Math.min(SwordEffects.count(kind)-1,(int)(t*SwordEffects.count(kind)));
        float alpha=(float)(t<.75?1:Math.max(0,(1-t)/.25));
        SwordEffects.draw(kind,frame,g,x,y-(kind==Kind.SMOKE?age*12:0),scale,flip,alpha);
        if(contactFlash && age<.16) {
            int burstFrame=Math.min(SwordEffects.count(Kind.CROSS_BURST)-1,(int)(age/.16*SwordEffects.count(Kind.CROSS_BURST)));
            SwordEffects.draw(Kind.CROSS_BURST,burstFrame,g,x,y,scale*.27,false,(float)(1-age/.16));
        }
    }
}

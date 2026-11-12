package chronorunner.combat;

import chronorunner.entity.GameObject;
import chronorunner.util.Rect;

/** Exact circle versus receiving rectangle; corners of its square never hit. */
public final class RadialHitbox extends Hitbox {
    private final double cx, cy, radius;
    public RadialHitbox(double x,double y,double radius,DamageInfo damage,double life,GameObject owner) {
        super(new Rect(x-radius,y-radius,radius*2,radius*2),damage,life,false,owner);
        this.cx=x;this.cy=y;this.radius=radius;
    }
    @Override public boolean intersects(Rect target) {
        double dx=cx-Math.max(target.x,Math.min(cx,target.x+target.w));
        double dy=cy-Math.max(target.y,Math.min(cy,target.y+target.h));
        return dx*dx+dy*dy<=radius*radius;
    }
}

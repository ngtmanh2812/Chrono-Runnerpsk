package chronorunner.entity.boss;

import java.awt.Color;
import chronorunner.combat.DamageInfo;
import chronorunner.combat.RadialHitbox;
import chronorunner.entity.*;
import chronorunner.world.CombatWorld;

/** Chapter 1: forward shockwave, rolling barrel and charged stun pulse. */
public class TimeGuardian extends Boss {
    private int attackCycle;
    public static final double CHARGE_RADIUS=230, CHARGE_STUN=.70;
    public TimeGuardian(double x,double y) {
        super("The Time Guardian",x,y,96,124,700,12,150,new Color(80,235,220));
    }
    @Override protected void updateAI(double dt,CombatWorld world) {
        Player target=nearestPlayer(world);
        if(target==null){vx=0;return;}
        faceTarget(target);
        if(specialTimer<=0){specialAttack(world);specialTimer=4.2*cooldownScale();return;}
        if(attackTimer<=0){
            boolean barrel=attackCycle%2==1;
            if(beginAction(world,"attack",barrel?"skill_i":"light",barrel?.45:.30,.18,.28,()->{
                double sx=facing>=0?right()+50:x-50;
                if(barrel)world.addProjectile(new PhyschoProjectile(sx,bottom()-48,facing*350,0,48,
                        new DamageInfo(scaleDamage(18),facing*260,-220,false)));
                else world.addProjectile(new BossShockWave(facing>=0?right()+24:x-24,bottom()-44,facing,
                        new DamageInfo(scaleDamage(14),facing*280,-160,false)));
                world.shake(.18);
            })) {attackCycle++;attackTimer=(barrel?2.1:1.4)*cooldownScale();}
            return;
        }
        walkToward(target,speed(),150);
    }
    @Override public void specialAttack(CombatWorld world) {
        Player target=nearestPlayer(world);
        if(target==null||!canBeginAction(world))return;
        faceTarget(target);
        if(beginAction(world,"special","skill_u",.60,.30,.40,()->{
            world.addHitbox(new RadialHitbox(centerX(),centerY(),CHARGE_RADIUS,
                    new DamageInfo(scaleDamage(16),0,-100,false).withStun(CHARGE_STUN),.30,this));
            world.addParticle(new BossShockEffect(centerX(),centerY(),CHARGE_RADIUS,false));
            world.shake(.36);
        })) specialTimer=4.2*cooldownScale();
    }
}

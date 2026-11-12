package chronorunner.entity.boss;

import java.awt.Color;
import chronorunner.combat.*;
import chronorunner.entity.*;
import chronorunner.world.CombatWorld;

/** Daddy: grounded artillery, electric pulse, rush and a sonic scream. */
public class TheVoidCore extends Boss {
    private int cycle;
    public TheVoidCore(double x,double y) {
        super("The Void Core",x,y,116,140,1600,16,160,new Color(205,235,210));
    }
    @Override protected void updateAI(double dt,CombatWorld world) {
        Player target=nearestPlayer(world);
        if(target==null){vx=0;return;}
        faceTarget(target);
        if(specialTimer<=0){specialAttack(world);return;}
        if(attackTimer<=0){
            double tx=target.centerX(),ty=target.centerY();
            if(beginAction(world,"attack","light",.42,.15,.30,()->{
                int count=getPhase()==3?5:3;
                double angle=Math.atan2(ty-centerY(),tx-centerX());
                for(int i=0;i<count;i++) {
                    double a=angle+(i-(count-1)/2.0)*.13;
                    world.addProjectile(new DaddyBolt(centerX()+facing*65,centerY(),
                            Math.cos(a)*420,Math.sin(a)*420,new DamageInfo(scaleDamage(10),facing*170,-100,false),false));
                }
            })) attackTimer=1.8*cooldownScale();
            return;
        }
        walkToward(target,speed(),190);
    }
    @Override public void specialAttack(CombatWorld world) {
        Player target=nearestPlayer(world);
        if(target==null||!canBeginAction(world))return;
        faceTarget(target);
        int move=cycle%3;
        boolean accepted;
        if(move==0) accepted=beginAction(world,"special","skill_u",.65,.25,.40,()->{
            world.addHitbox(new RadialHitbox(centerX(),centerY(),210,
                    new DamageInfo(scaleDamage(18),0,-200,false,1.25),.25,this));
            world.addParticle(new BossShockEffect(centerX(),centerY(),210,false));
            world.shake(.32);
        });
        else if(move==1) accepted=beginAction(world,"special","skill_i",.60,.50,.40,()->{
            dash(facing,speed()*3.0,.50);
            melee(world,100,130,scaleDamage(20),420);
            world.addParticle(new SkillPulse(centerX(),bottom()-4,120,.35,secondary));
            world.shake(.22);
        });
        else accepted=beginAction(world,"special","ultimate",.85,.30,.45,()->{
            for(int dir:new int[]{-1,1})world.addProjectile(new DaddyBolt(centerX()+dir*80,bottom()-78,
                    dir*330,0,new DamageInfo(scaleDamage(14),dir*480,-180,false),true));
            world.addParticle(new BossShockEffect(centerX(),centerY(),140,false));
            world.shake(.45);
        });
        if(accepted){cycle++;specialTimer=4.8*cooldownScale();}
    }
}

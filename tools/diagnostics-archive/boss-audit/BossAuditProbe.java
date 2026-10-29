import java.awt.Color;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import chronorunner.animation.AnimationController;
import chronorunner.combat.*;
import chronorunner.core.*;
import chronorunner.entity.*;
import chronorunner.entity.Player;
import chronorunner.entity.boss.*;
import chronorunner.entity.character.PhanSiKien;
import chronorunner.entity.enemy.Enemy;
import chronorunner.obstacle.*;
import chronorunner.ui.PlayScreen;
import chronorunner.world.*;

/** Diagnostic reproductions only. Production source/assets/saves are not edited. */
public class BossAuditProbe {
    static final double DT=1.0/60;
    static int observations;
    static Object get(Object o,String name)throws Exception {
        for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())try {
            Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(o);
        }catch(NoSuchFieldException ignored) { }
        throw new NoSuchFieldException(name);
    }
    static void set(Object o,String name,double value)throws Exception {
        for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())try {
            Field f=c.getDeclaredField(name);f.setAccessible(true);f.setDouble(o,value);return;
        }catch(NoSuchFieldException ignored) { }
        throw new NoSuchFieldException(name);
    }
    static AnimationController anim(Object o)throws Exception { return (AnimationController)get(o,"anim"); }
    static void observe(boolean reproduced,String name,String details) {
        if(!reproduced)throw new AssertionError("Not reproduced: "+name+" | "+details);
        observations++;System.out.println("REPRODUCED "+name+" | "+details);
    }
    static class World implements CombatWorld {
        final List<Player> players=new ArrayList<>();
        final List<Platform> platforms=new ArrayList<>();
        final List<Hitbox> hits=new ArrayList<>();
        final List<Projectile> shots=new ArrayList<>();
        final List<Enemy> summons=new ArrayList<>();
        World() { platforms.add(new StaticPlatform(0,600,2000,80,Color.GRAY)); }
        public List<Player> players(){return players;} public List<Platform> platforms(){return platforms;}
        public double worldWidth(){return 2000;}public double worldHeight(){return 720;}
        public void addHitbox(Hitbox h){hits.add(h);}public void addProjectile(Projectile p){shots.add(p);}
        public void addParticle(Particle p){}public void addFloatingText(FloatingText f){}
        public void addEnemy(Enemy e){summons.add(e);}public void shake(double a){}
        Player target(double x,double y){Player p=new PhanSiKien(0,new InputHandler(),x,y);players.add(p);return p;}
    }
    static Boss boss(boolean core){Boss b=core?new TheVoidCore(500,460):new TimeGuardian(500,476);b.grounded=true;return b;}
    static void immediateAttacks()throws Exception {
        for(boolean core:new boolean[]{false,true}) {
            World w=new World();Boss b=boss(core);Player p=w.target(620,538);
            set(b,"attackTimer",0);set(b,"specialTimer",100);b.update(DT,w);
            observe(w.hits.size()==1&&w.hits.get(0).rect.intersects(p.bounds())&&anim(b).current().frame()==0,
                    "instant-damage/"+b.getName(),"hitbox exists at animation frame "+anim(b).current().frame());
            p.x=430;b.update(DT,w);
            observe(b.getFacing()==-1&&w.hits.get(0).rect.centerX()>b.centerX()&&anim(b).currentName().equals("attack"),
                    "unlatched-facing/"+b.getName(),"boss now faces left; same attack hitbox remains on its right");
        }
    }
    static void guardianDash()throws Exception {
        World w=new World();TimeGuardian b=(TimeGuardian)boss(false);w.target(1000,538);
        set(b,"attackTimer",100);set(b,"specialTimer",100);b.specialAttack(w);
        Hitbox hit=w.hits.get(0);double originalX=hit.rect.x;
        for(int i=0;i<12;i++){b.update(DT,w);hit.update(DT);}
        observe(b.x>590&&hit.rect.x==originalX&&!hit.isAlive()&&(double)get(b,"dashTimer")>0,
                "guardian-dash-hitbox",String.format("boss moved %.1fpx; hitbox moved %.1fpx; hitbox expired while dash has %.2fs left",b.x-500,hit.rect.x-originalX,(double)get(b,"dashTimer")));
    }
    static void hurtAI()throws Exception {
        for(boolean core:new boolean[]{false,true}) {
            World w=new World();Boss b=boss(core);w.target(1000,538);
            b.takeDamage(new DamageInfo(1,-300,0,true));double before=b.vx;
            set(b,"attackTimer",0);set(b,"specialTimer",100);b.update(DT,w);
            observe((double)get(b,"stun")>0&&w.shots.size()>0&&b.vx>0,
                    "stun-ignored/"+b.getName(),String.format("stun=%.3fs, projectile count=%d, vx %.0f -> %.0f",(double)get(b,"stun"),w.shots.size(),before,b.vx));
        }
    }
    static void verticalTarget()throws Exception {
        for(boolean core:new boolean[]{false,true}) {
            World w=new World();Boss b=boss(core);Player p=w.target(b.centerX()-22,200);
            set(b,"attackTimer",0);set(b,"specialTimer",100);b.update(DT,w);
            observe(w.hits.size()==1&&!w.hits.get(0).rect.intersects(p.bounds())&&w.shots.isEmpty()&&b.vx==0,
                    "vertical-range-ignored/"+b.getName(),"target directly above: melee whiffs; no ranged choice, no jump");
        }
    }
    static void coreMovement()throws Exception {
        World w=new World();TheVoidCore b=new TheVoidCore(500,100);w.target(1200,538);
        set(b,"attackTimer",100);set(b,"specialTimer",100);
        for(int i=0;i<90;i++)b.update(DT,w);
        observe(b.grounded&&Math.abs(b.bottom()-600)<.001,"core-no-hover","core fell from y=100 and became grounded at floor y="+b.bottom());
        w=new World();b=(TheVoidCore)boss(true);w.target(620,538);
        w.platforms.add(new StaticPlatform(400,500,600,12,Color.GRAY));
        b.specialAttack(w);b.specialAttack(w); // cycle 2 = teleport/dive
        set(b,"attackTimer",100);set(b,"specialTimer",100);
        for(int i=0;i<30;i++)b.update(DT,w);
        observe(b.grounded&&Math.abs(b.bottom()-500)<.001,"core-dive-intercepted","dive landed on slab y="+b.bottom()+"; target stands on floor y=600");
        w=new World();b=(TheVoidCore)boss(true);w.target(620,538);
        for(int i=0;i<3;i++)b.specialAttack(w);
        observe(w.summons.size()==2&&w.summons.get(0).getName().equals("Chrono Bat"),"core-still-summons-minions","third special spawns "+w.summons.size()+" Chrono Bat enemies");
    }
    static void airborneDeath()throws Exception {
        for(boolean core:new boolean[]{false,true}) {
            World w=new World();Boss b=boss(core);b.y=100;b.grounded=false;b.vy=400;
            b.takeDamage(new DamageInfo(99999,0,0,true));
            for(int i=0;i<90;i++)b.update(DT,w);
            observe(b.y==100&&!b.grounded&&anim(b).current().finished(),"airborne-corpse/"+b.getName(),"death finished while y stayed 100; no descent to floor");
        }
    }
    static class NoSave extends Game { @Override public void saveProgress(){} }
    static void postVictoryDamage()throws Exception {
        NoSave game=new NoSave();Method register=Game.class.getDeclaredMethod("registerScreens");register.setAccessible(true);register.invoke(game);
        game.setState(GameState.PLAYING);PlayScreen screen=(PlayScreen)game.screen(GameState.PLAYING);
        Level level=(Level)get(screen,"level");Player p=screen.players().get(0);
        level.boss().takeDamage(new DamageInfo(99999,0,0,true));game.update(DT);
        boolean inactive=!screen.isCombatActive();int hp=p.getHealth();
        screen.addProjectile(new Projectile(p.centerX(),p.centerY(),0,0,4,new DamageInfo(99999,0,0,false),false,Color.RED,1));
        game.update(DT);
        boolean dead=p.isDead();for(int i=0;i<120;i++)game.update(DT);
        observe(inactive&&hp>0&&dead&&game.getState()==GameState.VICTORY,"post-victory-damage","combat was inactive; lingering hostile projectile killed player; final screen is still VICTORY");
    }
    static void coopInvulnerability()throws Exception {
        Boss b=boss(false);int start=b.getHealth();
        b.takeDamage(new DamageInfo(30,0,0,true));b.takeDamage(new DamageInfo(30,0,0,true));
        observe(start-b.getHealth()==30,"shared-invulnerability","two distinct 30-damage calls in same tick remove only 30 HP; shared invulnerability="+get(b,"invuln"));
    }
    public static void main(String[] args)throws Exception {
        immediateAttacks();guardianDash();hurtAI();verticalTarget();coreMovement();airborneDeath();postVictoryDamage();coopInvulnerability();
        System.out.println("Confirmed diagnostic observations: "+observations+". No production changes; save calls intercepted.");
    }
}

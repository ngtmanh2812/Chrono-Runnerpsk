import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import chronorunner.animation.AnimationController;
import chronorunner.combat.*;
import chronorunner.core.*;
import chronorunner.entity.*;
import chronorunner.entity.Player;
import chronorunner.entity.boss.*;
import chronorunner.entity.character.PhanSiKien;
import chronorunner.entity.enemy.*;
import chronorunner.obstacle.*;
import chronorunner.ui.PlayScreen;
import chronorunner.world.*;

/** Regression scenarios from the boss audit. Saves and runtime art are untouched. */
public final class BossCombatFixCheck {
    static int checks;
    static final double DT=1.0/60;
    static final Canvas KEYS=new Canvas();
    static final Path OUTPUT=Path.of("out/boss-fix");
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static boolean near(double a,double b){return Math.abs(a-b)<1e-6;}
    static Field field(Object o,String name)throws Exception {
        for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f;}
        catch(NoSuchFieldException ignored){}
        throw new NoSuchFieldException(name);
    }
    static Object get(Object o,String name)throws Exception{return field(o,name).get(o);}
    static void set(Object o,String name,double value)throws Exception{field(o,name).setDouble(o,value);}
    static AnimationController anim(Object o)throws Exception{return (AnimationController)get(o,"anim");}
    static Attack action(Boss b)throws Exception{return (Attack)get(b,"action");}
    static Boss boss(boolean core){Boss b=core?new TheVoidCore(500,460):new TimeGuardian(500,476);b.grounded=true;return b;}
    static class World implements CombatWorld {
        final List<Player> players=new ArrayList<>();final List<Platform> platforms=new ArrayList<>();
        final List<Hitbox> hits=new ArrayList<>();final List<Projectile> shots=new ArrayList<>();int summons;
        World(){platforms.add(new StaticPlatform(0,600,2000,80,Color.GRAY));}
        public List<Player> players(){return players;}public List<Platform> platforms(){return platforms;}
        public double worldWidth(){return 2000;}public double worldHeight(){return 720;}
        public void addHitbox(Hitbox h){hits.add(h);}public void addProjectile(Projectile p){shots.add(p);}
        public void addParticle(Particle p){}public void addFloatingText(FloatingText t){}
        public void addEnemy(Enemy e){summons++;}public void shake(double a){}
        Player target(double x,double y){Player p=new PhanSiKien(players.size(),new InputHandler(),x,y);players.add(p);return p;}
        void age(double dt){for(Hitbox h:hits)h.update(dt);}
    }
    static void basicWindows()throws Exception {
        for(boolean core:new boolean[]{false,true})for(double dt:new double[]{1.0/120,DT,1.0/30}) {
            World w=new World();Boss b=boss(core);Player p=w.target(650,538);
            set(b,"attackTimer",0);set(b,"specialTimer",100);b.update(dt,w);
            check(action(b)!=null&&action(b).isStartup(),"No startup");
            check(w.hits.isEmpty()&&w.shots.isEmpty()&&!b.isContactDamageEnabled(),"Damage on first frame");
            int direction=b.getFacing();boolean sawActive=false,sawRecovery=false;
            for(int i=0;i<120&&action(b)!=null;i++) {
                Attack move=action(b);Attack.Phase phase=move.phase();int frame=anim(b).current().frame();
                check(b.getFacing()==direction,"Facing changed during committed move");
                if(phase==Attack.Phase.STARTUP)check(frame<2&&w.hits.isEmpty(),"Startup strike pose/damage");
                if(phase==Attack.Phase.ACTIVE){sawActive=true;check(frame>=2&&frame<4,"Active pose wrong");}
                if(phase==Attack.Phase.RECOVERY){sawRecovery=true;check(frame>=4&&!b.isContactDamageEnabled(),"Recovery damages");
                    for(Hitbox hit:w.hits)check(!hit.isAlive(),"Melee survived Active");}
                p.x=430; // Cross behind while the same action is committed.
                b.specialAttack(w);check(action(b)==move,"Special interrupted a committed attack");
                b.update(dt,w);w.age(dt);
            }
            check(sawActive&&sawRecovery&&w.hits.size()==1,"Missing window or duplicate strike");
        }
        System.out.println("Both bosses: startup/active/recovery, locked direction, no action interruption at 30/60/120 Hz PASS");
    }
    static void rangedHeightAndTargets()throws Exception {
        for(boolean core:new boolean[]{false,true}) {
            World w=new World();Boss b=boss(core);Player p=w.target(b.centerX()-22,200);
            set(b,"attackTimer",0);set(b,"specialTimer",100);b.update(DT,w);
            for(int i=0;i<18;i++)b.update(DT,w);
            check(w.hits.isEmpty()&&w.shots.size()==(core?3:1),"Melee chosen for unreachable height");
            check(w.shots.get(0).vy<0,"Volley not aimed toward high target");
            w=new World();b=boss(core);p=w.target(1100,538);
            set(b,"attackTimer",0);set(b,"specialTimer",100);b.update(DT,w);p.x=100;
            for(int i=0;i<18&&w.shots.isEmpty();i++)b.update(DT,w);
            check(b.getFacing()==1&&w.shots.get(0).vx>0,"Volley tracked behind locked wind-up");
            w=new World();b=boss(core);w.target(650,538);
            set(b,"attackTimer",100);set(b,"specialTimer",100);b.update(DT,w);
            check(near(b.vx,0),"Boss walked into player despite reachable melee");
            World targetWorld=new World();Boss targetingBoss=boss(core);
            targetWorld.target(targetingBoss.centerX()-22,30);
            Player closer=targetWorld.target(targetingBoss.centerX()+78,538);
            Method nearest=Boss.class.getDeclaredMethod("nearestPlayer",CombatWorld.class);nearest.setAccessible(true);
            check(nearest.invoke(targetingBoss,targetWorld)==closer,"Target selection ignores vertical distance");
        }
        System.out.println("Height-aware melee/ranged selection, fixed volley aim, spacing and 2D co-op target selection PASS");
    }
    static void hurtAndDeath()throws Exception {
        for(boolean core:new boolean[]{false,true}) {
            World w=new World();Boss b=boss(core);w.target(1000,538);
            b.takeDamage(new DamageInfo(1,-300,0,true));set(b,"attackTimer",0);set(b,"specialTimer",0);
            b.update(DT,w);
            check(b.vx<0&&w.shots.isEmpty()&&w.hits.isEmpty(),"Hurt overwrote knockback/fired");
            check(!b.isContactDamageEnabled(),"Hurt causes contact damage");
            for(int i=0;i<12;i++)b.update(DT,w);
            check(w.shots.isEmpty()&&w.hits.isEmpty(),"AI ran while hurt/stunned");
            w=new World();b=boss(core);w.target(650,538);
            set(b,"attackTimer",0);set(b,"specialTimer",100);b.update(DT,w);
            b.takeDamage(new DamageInfo(1,-100,0,true));
            check(action(b)==null,"Startup not cancelled by hurt");
            for(int i=0;i<14;i++)b.update(DT,w);
            check(w.hits.isEmpty(),"Cancelled wind-up fired late");
            w=new World();b=boss(core);w.target(650,538);
            set(b,"attackTimer",0);set(b,"specialTimer",100);b.update(DT,w);
            while(!action(b).isActive())b.update(DT,w);
            Hitbox hit=w.hits.get(0);b.takeDamage(new DamageInfo(1,-100,0,true));
            check(!hit.isAlive()&&action(b)==null,"Active hit survived interruption");
            w=new World();b=boss(core);b.y=100;b.grounded=false;b.vy=400;
            b.takeDamage(new DamageInfo(99999,0,0,true));double timer=(double)get(b,"attackTimer");
            for(int i=0;i<90;i++)b.update(DT,w);
            check(b.grounded&&near(b.bottom(),600),"Corpse stayed airborne/fell through floor");
            check(anim(b).current().finished()&&anim(b).current().frame()==5,"Death pose did not settle");
            check((double)get(b,"attackTimer")==timer&&w.hits.isEmpty()&&w.shots.isEmpty(),"Dead boss ran AI");
            check(b.getMaxHealth()==(core?1600:700),"HP changed");
        }
        System.out.println("Hurt cancels wind-up/active, preserves knockback; dead bosses fall/hold final pose without AI PASS");
    }
    static void dash()throws Exception {
        World w=new World();TimeGuardian b=(TimeGuardian)boss(false);w.target(1000,538);
        set(b,"attackTimer",100);set(b,"specialTimer",100);b.specialAttack(w);
        check(w.hits.isEmpty()&&near(b.x,500),"Dash hit before startup");
        while(!action(b).isActive())b.update(DT,w);
        Hitbox hit=w.hits.get(0);hit.syncOwner();double offset=hit.rect.x-b.x;int activeSteps=0;double start=b.x;
        while(action(b).isActive()) {
            hit.syncOwner();check(near(hit.rect.x-b.x,offset),"Dash hitbox left behind");
            if(activeSteps==12)check(hit.isAlive(),"Dash hitbox expired at old .18s boundary");
            b.update(DT,w);w.age(DT);activeSteps++;
        }
        check(b.x-start>150&&activeSteps>=20,"Dash motion truncated");
        check(!hit.isAlive()&&near(b.vx,0),"Dash damage/motion continues in recovery");
        System.out.println("Guardian dash: moving hitbox spans full .35s Active; recovery stops motion/damage PASS");
    }
    static void staggerBudget()throws Exception {
        for(boolean core:new boolean[]{false,true}) {
            World w=new World();Boss b=boss(core);w.target(650,538);
            set(b,"attackTimer",0);set(b,"specialTimer",100);
            b.takeDamage(new DamageInfo(1,0,0,true));
            // Finishing the presentation cannot finish the gameplay lock.
            anim(b).current().update(10);
            b.update(DT,w);
            check(action(b)==null&&w.hits.isEmpty()&&w.shots.isEmpty(),"Hurt art completion reopened AI before combat stagger expired");

            w=new World();b=boss(core);w.target(650,538);
            set(b,"attackTimer",0);set(b,"specialTimer",100);
            for(int tick=0;tick<120&&!b.isStaggerResistant();tick++) {
                if(tick%10==0) b.takeDamage(new DamageInfo(1,0,0,true)
                        .withMeleeChain(new DamageInfo.MeleeChain(),1));
                b.update(DT,w);
            }
            check(b.isStaggerResistant(),"Continuous hits never exhausted shared boss stagger budget");
            Attack response=action(b);
            check(response!=null&&response.isStartup(),"Boss did not regain an actionable response after stagger cap");
            int hp=b.getHealth();double vx=b.vx,vy=b.vy;boolean grounded=b.grounded;
            b.takeDamage(new DamageInfo(1,-500,-620,true)
                    .withMeleeChain(new DamageInfo.MeleeChain(),1));
            check(b.getHealth()==hp-1&&action(b)==response,"Resistance blocked damage or cancelled committed response");
            check(near(b.vx,vx)&&near(b.vy,vy)&&b.grounded==grounded,"Resistance still accepted forced launch/knockback");
            check(!"hurt".equals(anim(b).currentName()),"Resisted hit promised an interrupt through Hurt animation");
            for(int tick=0;tick<20;tick++) {
                if(tick%5==0)b.takeDamage(new DamageInfo(1,0,0,true)
                        .withMeleeChain(new DamageInfo.MeleeChain(),1));
                b.update(DT,w);
            }
            check(w.hits.size()==1,"Fresh melee pressure permanently suppressed boss's counterattack");
        }
        System.out.println("Combat-owned Hurt timing, shared stagger budget, damage during resistance and real counterattack under pressure PASS");
    }
    static void coreFlight()throws Exception {
        World w=new World();TheVoidCore b=new TheVoidCore(500,100);w.target(1200,538);
        set(b,"attackTimer",100);set(b,"specialTimer",100);
        for(int i=0;i<360;i++)b.update(DT,w);
        check(!b.grounded&&b.bottom()>570&&b.bottom()<580,"Core did not hover 24px above support");
        w=new World();b=(TheVoidCore)boss(true);Player p=w.target(620,538);
        w.platforms.add(new StaticPlatform(400,500,600,12,Color.GRAY));
        set(b,"attackTimer",100);set(b,"specialTimer",100);
        b.specialAttack(w);for(int i=0;i<90;i++)b.update(DT,w); // first = radial
        double oldX=b.x;b.specialAttack(w);p.x=900;
        check(near(b.x,oldX)&&!(boolean)get(b,"diving"),"Dive teleported during startup");
        boolean passed=false,landed=false;
        for(int i=0;i<90;i++) {
            b.update(DT,w);
            if((boolean)get(b,"diving")&&b.bottom()>500)passed=true;
            if(b.grounded){landed=true;check(near(b.bottom(),600),"Dive stopped on intermediate slab");break;}
        }
        check(passed&&landed,"Dive did not reach locked landing plane");
        check(near(b.centerX(),642),"Dive target changed after commitment");
        for(int i=0;i<90;i++)b.update(DT,w);
        for(int n=0;n<8;n++){b.specialAttack(w);for(int i=0;i<100;i++)b.update(DT,w);}
        check(w.summons==0,"Core spawned enemy minions");
        World high=new World();high.platforms.add(new StaticPlatform(400,180,600,12,Color.GRAY));
        TheVoidCore c=(TheVoidCore)boss(true);high.target(620,118);set(c,"attackTimer",100);set(c,"specialTimer",100);
        for(int n=0;n<2;n++){c.specialAttack(high);for(int i=0;i<100;i++)c.update(DT,high);}
        check(!(boolean)get(c,"diving")&&high.shots.size()>=16,"Invalid high dive not replaced by existing burst");
        System.out.println("Core hover, locked dive point/plane, passing intermediate caps, high-ledge safety and zero minions PASS");
    }
    static class NoSave extends Game {int saves;@Override public void saveProgress(){saves++;}}
    static final class Fixture {
        final NoSave game=new NoSave();final PlayScreen screen;final Level level;
        Fixture(int chapter,int count)throws Exception {
            game.getSession().chapter=chapter;game.getSession().playerCount=count;
            Method register=Game.class.getDeclaredMethod("registerScreens");register.setAccessible(true);register.invoke(game);
            game.setState(GameState.PLAYING);screen=(PlayScreen)game.screen(GameState.PLAYING);level=(Level)get(screen,"level");
        }
        void tick(){game.getInput().beginFrame();game.update(DT);game.getInput().endFrame();}
        void capture(String name)throws Exception {
            BufferedImage image=new BufferedImage(Game.WIDTH,Game.HEIGHT,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
            game.render(g);g.dispose();ImageIO.write(image,"png",OUTPUT.resolve(name+".png").toFile());
        }
    }
    static void endingsAndPause()throws Exception {
        for(int chapter=1;chapter<=2;chapter++)for(int count=1;count<=2;count++) {
            Fixture f=new Fixture(chapter,count);Player p=f.screen.players().get(0);Boss b=f.level.boss();
            Hitbox hazard=new Hitbox(p.bounds().copy(),new DamageInfo(99999,0,0,false),5,false,b);
            f.screen.addHitbox(hazard);
            f.screen.addProjectile(new Projectile(p.centerX(),p.centerY(),0,0,4,new DamageInfo(99999,0,0,false),false,Color.RED,5));
            f.screen.addEnemy(new TimeSlime(p.x,p.y));
            b.y=100;b.grounded=false;b.takeDamage(new DamageInfo(99999,0,0,true));int hp=p.getHealth();f.tick();
            check(!f.screen.isCombatActive()&&!hazard.isAlive(),"Outcome did not cancel combat");
            check(((List<?>)get(f.screen,"hitboxes")).isEmpty()&&((List<?>)get(f.screen,"projectiles")).isEmpty()
                    &&((List<?>)get(f.screen,"enemies")).isEmpty(),"Ending retained hostile objects");
            f.screen.addProjectile(new Projectile(p.centerX(),p.centerY(),0,0,4,new DamageInfo(99999,0,0,false),false,Color.RED,1));
            f.game.getInput().keyPressed(new KeyEvent(KEYS,KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_U,KeyEvent.CHAR_UNDEFINED));
            for(int i=0;i<100;i++)f.tick();
            check(p.getHealth()==hp&&!p.isDead(),"Post-win projectile/input changed player health");
            check(b.grounded&&b.bottom()<=f.level.groundY()+1e-6,"Ending froze airborne death");
            check(((List<?>)get(f.screen,"hitboxes")).isEmpty()&&((List<?>)get(f.screen,"projectiles")).isEmpty(),"Player cast during ending");
            f.capture("chapter"+chapter+"-"+count+"p-death-settled");
            for(int i=0;i<20;i++)f.tick();
            check(f.game.getState()==GameState.VICTORY&&f.game.saves==1,"Victory progression/save request changed");
            f.capture("chapter"+chapter+"-"+count+"p-victory");
            Fixture loss=new Fixture(chapter,count);for(Player dead:loss.screen.players())dead.takeDamage(new DamageInfo(99999,0,0,false));
            loss.tick();for(int i=0;i<120;i++)loss.tick();
            check(loss.game.getState()==GameState.GAME_OVER&&loss.game.saves==0,"Defeat progression changed");
            Fixture tie=new Fixture(chapter,count);for(Player dead:tie.screen.players())dead.takeDamage(new DamageInfo(99999,0,0,false));
            tie.level.boss().takeDamage(new DamageInfo(99999,0,0,true));tie.tick();for(int i=0;i<120;i++)tie.tick();
            check(tie.game.getState()==GameState.VICTORY,"Existing tie priority changed");
        }
        Fixture f=new Fixture(1,2);Boss b=f.level.boss();b.specialAttack(f.screen);f.tick();
        double progress=action(b).progress(),x=b.x;f.game.setState(GameState.PAUSED);
        for(int i=0;i<120;i++)f.tick();
        check(near(action(b).progress(),progress)&&near(b.x,x),"Pause advanced boss action");
        f.game.resumeState(GameState.PLAYING);f.tick();check(action(b).progress()>progress,"Resume rebuilt/froze action");
        Fixture flight=new Fixture(2,1);Boss core=flight.level.boss();
        set(core,"attackTimer",100);set(core,"specialTimer",100);
        for(int i=0;i<240;i++)flight.tick();
        check(!core.grounded&&core.bottom()<flight.level.groundY(),"Core grounded in real Chapter 2");
        flight.capture("chapter2-hover");
        Fixture charge=new Fixture(1,1);Boss guardian=charge.level.boss();Player target=charge.screen.players().get(0);
        guardian.x=850;target.x=650;target.y=charge.level.groundY()-target.height;target.grounded=true;
        guardian.specialAttack(charge.screen);charge.tick();
        check(action(guardian)!=null&&action(guardian).isStartup(),"Dash screenshot missed wind-up");
        charge.capture("guardian-dash-windup");
        int advance=0;
        while(action(guardian)!=null&&action(guardian).isStartup()&&advance++<180)charge.tick();
        check(action(guardian)!=null&&action(guardian).isActive(),"Dash screenshot did not reach Active");
        charge.capture("guardian-dash-active");
        System.out.println("Real PlayScreen: locked outcomes, no post-win damage/casts/minions, corpse physics, ties, 1P/2P endings and pause PASS");
    }
    public static void main(String[] args)throws Exception {
        Files.createDirectories(OUTPUT);basicWindows();rangedHeightAndTargets();hurtAndDeath();dash();staggerBudget();coreFlight();endingsAndPause();
        check(!Boolean.getBoolean("chronorunner.debugCollision"),"Debug left enabled");
        String result="PASS: "+checks+" boss regression checks; assets/HP preserved; saves intercepted; debug OFF.\n";
        Files.writeString(OUTPUT.resolve("results.txt"),result);System.out.print(result);
    }
}

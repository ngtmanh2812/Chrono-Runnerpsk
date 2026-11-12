import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.List;
import java.util.Properties;
import javax.imageio.ImageIO;
import chronorunner.animation.*;
import chronorunner.combat.*;
import chronorunner.core.*;
import chronorunner.entity.*;
import chronorunner.entity.Player;
import chronorunner.entity.boss.Boss;
import chronorunner.ui.PlayScreen;
import chronorunner.gfx.*;
import chronorunner.world.Level;

/** Exercises identity casts through real InputHandler, Player and PlayScreen collisions. No save writes. */
public final class IdentitySkillsCheck {
    static final double DT=1.0/60;
    static final Canvas KEYS=new Canvas();
    static final Path OUTPUT=Path.of("out/identity-v3-check");
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static boolean near(double a,double b){return Math.abs(a-b)<1e-7;}
    static Field field(Object o,String name)throws Exception{
        for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}
        throw new NoSuchFieldException(name);
    }
    static Object get(Object o,String name)throws Exception{return field(o,name).get(o);}
    static void set(Object o,String name,double value)throws Exception{field(o,name).setDouble(o,value);}
    static void call(Object o,String name)throws Exception{
        for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())try{Method m=c.getDeclaredMethod(name);m.setAccessible(true);m.invoke(o);return;}catch(NoSuchMethodException ignored){}
        throw new NoSuchMethodException(name);
    }
    static String anim(Player p)throws Exception{return ((AnimationController)get(p,"anim")).currentName();}
    static void key(InputHandler input,int code,boolean down){
        boolean numEnter=code==InputHandler.NUMPAD_ENTER;
        KeyEvent e=new KeyEvent(KEYS,down?KeyEvent.KEY_PRESSED:KeyEvent.KEY_RELEASED,0,0,
                numEnter?KeyEvent.VK_ENTER:code,KeyEvent.CHAR_UNDEFINED,
                numEnter?KeyEvent.KEY_LOCATION_NUMPAD:KeyEvent.KEY_LOCATION_STANDARD);
        if(down)input.keyPressed(e);else input.keyReleased(e);
    }
    static class NoSave extends Game{int saves;@Override public void saveProgress(){saves++;}}
    static final class Fixture{
        final NoSave game=new NoSave();final InputHandler input=game.getInput();final PlayScreen screen;final Level level;final Method effects;
        Fixture(int chapter,int character,int count)throws Exception{
            game.getSession().chapter=chapter;game.getSession().playerCount=count;
            game.getSession().characterIndex[0]=character;game.getSession().characterIndex[1]=character;
            call(game,"registerScreens");game.setState(GameState.PLAYING);screen=(PlayScreen)game.screen(GameState.PLAYING);level=(Level)get(screen,"level");effects=PlayScreen.class.getDeclaredMethod("updateEffects",double.class);effects.setAccessible(true);
            for(int i=0;i<count;i++){Player p=screen.players().get(i);p.x=200+i*400;p.y=level.groundY()-p.height;p.grounded=true;p.update(0,screen);}
            set(level.boss(),"attackTimer",100);set(level.boss(),"specialTimer",100);
        }
        Player p(int index){return screen.players().get(index);}
        @SuppressWarnings("unchecked") List<Projectile> shots()throws Exception{return (List<Projectile>)get(screen,"projectiles");}
        @SuppressWarnings("unchecked") List<Hitbox> hits()throws Exception{return (List<Hitbox>)get(screen,"hitboxes");}
        void tick()throws Exception{
            input.beginFrame();for(Player p:screen.players())p.update(DT,screen);input.endFrame();
            for(Projectile shot:shots())shot.update(DT,screen);
            call(screen,"resolveProjectiles");call(screen,"resolveHitboxes");for(Hitbox hit:hits())hit.update(DT);
            shots().removeIf(shot->!shot.alive);hits().removeIf(hit->!hit.isAlive());effects.invoke(screen,DT);
        }
        void ticks(int count)throws Exception{for(int n=0;n<count;n++)tick();}
        void press(int... keys)throws Exception{for(int k:keys)key(input,k,true);tick();for(int k:keys)key(input,k,false);}
        void realTick(){input.beginFrame();game.update(DT);input.endFrame();}
        void realPress(int code){key(input,code,true);realTick();key(input,code,false);}
        void target(Player p){Boss boss=level.boss();p.x=850;p.y=level.groundY()-p.height;p.grounded=true;boss.x=p.right()+25;boss.y=level.groundY()-boss.height;boss.update(0,screen);p.update(0,screen);}
        void capture(String name)throws Exception{BufferedImage image=new BufferedImage(Game.WIDTH,Game.HEIGHT,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();game.render(g);g.dispose();ImageIO.write(image,"png",OUTPUT.resolve(name+".png").toFile());}
    }
    static int skillKey(int player,int slot){return Player.keysFor(player)[slot==0?5:slot==1?10:11];}
    static void bankProof(int requiredVersion)throws Exception{
        for(String hero:new String[]{"phan_si_kien","ronaldo","do_mixi"}){
            Path bank=null; int version=0;
            for(int candidate:new int[]{4,3,2}){
                Path directory=Path.of("assets/players",hero,"v"+candidate);
                if(Files.isRegularFile(directory.resolve("animation.properties"))){bank=directory;version=candidate;break;}
            }
            check(version>=requiredVersion,"Required v"+requiredVersion+" bank not installed: "+hero);
            if(bank==null)continue;
            Properties metadata=new Properties();try(var in=Files.newInputStream(bank.resolve("animation.properties"))){metadata.load(in);}
            for(String clip:Art.PLAYER_ANIMATIONS){
                SpriteSet set=Art.playerAnim(hero,clip);
                check(set.layout()!=null&&Integer.toString(version).equals(((Properties)get(set.layout(),"properties")).getProperty("version")),"Runtime did not prefer latest bank: "+hero+"/"+clip);
                int expectedFrames=Integer.parseInt(metadata.getProperty(clip+".frames",Integer.toString(version>=4?6:clip.equals("fall")||clip.equals("land")?2:6)));
                check(set.count()==expectedFrames,"Wrong installed frame count: "+hero+"/"+clip);
                Path authored=bank.resolve(clip+"_1.png");check(Files.isRegularFile(authored),"Missing installed clip: "+authored);
                BufferedImage expected=ImageIO.read(authored.toFile());BufferedImage actual=new BufferedImage(set.layout().width,set.layout().height,BufferedImage.TYPE_INT_ARGB);
                Graphics2D g=actual.createGraphics();set.frame(0).draw(g,0,0,1,false);g.dispose();boolean same=true;
                check(actual.getWidth()==expected.getWidth()&&actual.getHeight()==expected.getHeight(),"Loaded layout disagrees with PNG dimensions");
                for(int y=0;y<actual.getHeight()&&same;y++)for(int x=0;x<actual.getWidth();x++){
                    int a=actual.getRGB(x,y),b=expected.getRGB(x,y);
                    if((a>>>24)!=(b>>>24)||((b>>>24)!=0&&a!=b)){same=false;break;}
                }
                check(same,"Runtime rendered fallback instead of latest installed frame: "+hero+"/"+clip);
                check(set.layout().width==(version>=4?192:128)&&set.layout().height==(version>=4?192:128),"Unexpected player pack canvas");
                check(set.layout().pivotX==(version>=4?96:64)&&set.layout().pivotY==(version>=4?172:116)&&set.layout().scale==1,"Player padding changed authored world anchor/scale");
            }
        }
    }
    static void casts()throws Exception{
        int[] costs={20,30,100},damage={40,45,120};String[] clips={"skill_u","skill_i","ultimate"};
        for(int chapter=1;chapter<=2;chapter++)for(int hero=0;hero<3;hero++)for(int index=0;index<2;index++)for(int slot=0;slot<3;slot++){
            Fixture f=new Fixture(chapter,hero,2);Player p=f.p(index);f.target(p);Boss boss=f.level.boss();int hp=boss.getHealth();
            int key=skillKey(index,slot);key(f.input,key,true);f.tick();
            check(near(p.getEnergy(),100-costs[slot]),"Cast must pay exactly once at acceptance");
            check(anim(p).equals(clips[slot]),"Wrong cast animation");
            check(f.shots().isEmpty()&&f.hits().isEmpty()&&boss.getHealth()==hp,"Cast damaged during startup");
            check(f.p(1-index).getEnergy()==100,"Cast energy leaked across player slots");
            for(int n=0;n<48;n++){set(boss,"invuln",0);f.tick();if(chapter==1&&index==0&&n==(slot==0?8:slot==1?12:24))f.capture("hero-"+hero+"-skill-"+slot+"-active");}
            check(boss.getHealth()==hp-damage[slot],"Wrong/doubled skill damage: chapter="+chapter+" hero="+hero+" P="+index+" slot="+slot+" got="+(hp-boss.getHealth()));
            check(near(p.getEnergy(),100-costs[slot]),"Holding skill repeated the energy cost");
            check(((List<?>)get(f.screen,"enemies")).isEmpty(),"Skill created enemy minions");
            key(f.input,key,false);
        }
        System.out.println("3 heroes x 2 slots x 2 chapters x U/I/O: startup, 40/45/120 once, costs, held edges, no enemies PASS");
    }
    static void energy()throws Exception{
        Fixture f=new Fixture(1,0,2);Player p=f.p(0);f.press(KeyEvent.VK_U);f.ticks(30);double e=p.getEnergy();
        f.press(KeyEvent.VK_U);check(near(p.getEnergy(),e),"Cooldown rejection charged energy");
        f.press(KeyEvent.VK_I);check(near(p.getEnergy(),50),"I not independent of U cooldown");f.ticks(50);
        f.press(KeyEvent.VK_O);check(near(p.getEnergy(),50)&&near(p.getSkillCooldownRemaining(2),0),"Unaffordable ultimate took energy/cooldown");
        key(f.input,KeyEvent.VK_L,true);f.ticks(60);check(near(p.getEnergy(),68)&&p.isCharging(),"Ground charge is not 18/s");
        key(f.input,KeyEvent.VK_D,true);f.tick();check(!p.isCharging(),"Movement did not stop charge");key(f.input,KeyEvent.VK_D,false);f.ticks(10);check(near(p.getEnergy(),68),"Held L restarted charge after movement");
        key(f.input,KeyEvent.VK_L,false);key(f.input,KeyEvent.VK_L,true);f.ticks(5);check(p.isCharging(),"Fresh charge press failed");
        p.takeDamage(new DamageInfo(1,0,0,false));check(!p.isCharging(),"Hit failed to stop charge");double hitEnergy=p.getEnergy();f.ticks(40);check(near(p.getEnergy(),hitEnergy),"Charge resumed after hurt while held");key(f.input,KeyEvent.VK_L,false);
        f.press(KeyEvent.VK_W);key(f.input,KeyEvent.VK_L,true);f.tick();check(!p.isCharging()&&near(p.getEnergy(),hitEnergy),"Charge allowed while airborne");key(f.input,KeyEvent.VK_L,false);f.ticks(100);
        key(f.input,KeyEvent.VK_L,true);f.ticks(300);check(near(p.getEnergy(),100),"Charge exceeded/lost max100");key(f.input,KeyEvent.VK_L,false);f.tick();
        Fixture p2=new Fixture(1,2,2);p2.press(KeyEvent.VK_NUMPAD4);p2.ticks(30);key(p2.input,KeyEvent.VK_ENTER,true);p2.ticks(5);check(!p2.p(1).isCharging()&&near(p2.p(1).getEnergy(),80),"Main Enter charged P2");key(p2.input,KeyEvent.VK_ENTER,false);key(p2.input,InputHandler.NUMPAD_ENTER,true);p2.ticks(30);check(near(p2.p(1).getEnergy(),89)&&near(p2.p(0).getEnergy(),100),"P2 Numpad Enter charge not independent");
        System.out.println("Energy affordability, independent cooldowns, charge18/s, max100 and movement/hurt/air interruption PASS");
    }
    static void controlsAndInterruptions()throws Exception{
        for(int hero=0;hero<3;hero++)for(int index=0;index<2;index++){
            Fixture f=new Fixture(1,hero,2);Player p=f.p(index);int[] keys=Player.keysFor(index);if(index==0){f.press(KeyEvent.VK_SPACE);check(p.grounded,"Unselected Space jump alias remains");}
            f.press(keys[2]);check(!p.grounded&&p.vy<0,"W/Numpad0 did not jump");f.ticks(100);
            f.press(keys[4]);check(anim(p).equals("dash"),"K/Numpad3 did not dash");f.ticks(30);
            int[] damages={30,40,35};String[] clips={"light_1","heavy","launcher"};
            for(int step=0;step<3;step++){
                f.target(p);int hp=f.level.boss().getHealth();f.press(keys[3]);
                check(anim(p).equals(clips[step]),"J/Numpad1 did not select melee stage "+step);
                f.ticks(50);check(f.level.boss().getHealth()==hp-damages[step],"Wrong chain damage at stage "+step);
            }
            f.press(keys[6]);check(p.isBlocking(),"Block binding changed");int hp=p.getHealth();p.takeDamage(new DamageInfo(10,0,0,false));check(p.getHealth()==hp&&!p.isBlocking(),"Block lost one-hit defense");
        }
        Fixture guarded=new Fixture(1,0,1);guarded.press(KeyEvent.VK_S,KeyEvent.VK_U);
        check(guarded.p(0).isBlocking()&&near(guarded.p(0).getEnergy(),80),"Skill removed existing guard compatibility");
        guarded.p(0).takeDamage(new DamageInfo(10,0,0,false));
        check(guarded.p(0).getHealth()==guarded.p(0).getMaxHealth()&&!guarded.p(0).isBlocking(),"Guard failed during committed cast");
        guarded.ticks(8);check(guarded.shots().size()==1,"Absorbed hit interrupted guarded cast");
        Fixture startup=new Fixture(1,0,1);startup.press(KeyEvent.VK_I);startup.p(0).takeDamage(new DamageInfo(1,0,0,false));startup.ticks(30);check(startup.hits().isEmpty(),"Interrupted startup emitted old skill hitbox");
        Fixture active=new Fixture(1,2,1);active.press(KeyEvent.VK_I);active.ticks(12);check(active.hits().size()==1&&active.hits().get(0).isAlive(),"I active not created");active.p(0).takeDamage(new DamageInfo(1,0,0,false));check(!active.hits().get(0).isAlive(),"Interrupted active hitbox survived");
        Fixture basic=new Fixture(1,0,1);basic.press(KeyEvent.VK_J);basic.ticks(9);check(basic.hits().size()==1,"Normal active missing");basic.p(0).takeDamage(new DamageInfo(1,0,0,false));check(!basic.hits().get(0).isAlive(),"Old basic hitbox survived hurt");
        Fixture simultaneous=new Fixture(1,0,1);simultaneous.press(KeyEvent.VK_J,KeyEvent.VK_U,KeyEvent.VK_I,KeyEvent.VK_O);check(near(simultaneous.p(0).getEnergy(),100),"Simultaneous skill escaped committed normal");
        System.out.println("1P/2P bindings, Heavy/Launcher, one-hit guard, committed actions and interruption cleanup PASS");
    }
    static void enterLocations(){
        InputHandler in=new InputHandler();key(in,InputHandler.NUMPAD_ENTER,true);in.beginFrame();
        check(in.wasPressed(InputHandler.NUMPAD_ENTER)&&in.wasPressed(KeyEvent.VK_ENTER),"Numpad Enter lost gameplay/menu edges");
        in.endFrame();key(in,KeyEvent.VK_ENTER,true);key(in,InputHandler.NUMPAD_ENTER,false);
        check(in.isDown(KeyEvent.VK_ENTER)&&!in.isDown(InputHandler.NUMPAD_ENTER),"Releasing num Enter lost held main Enter");
        key(in,KeyEvent.VK_ENTER,false);check(!in.isDown(KeyEvent.VK_ENTER),"Main Enter release stuck");
        key(in,InputHandler.NUMPAD_ENTER,true);in.clearAll();check(!in.isDown(InputHandler.NUMPAD_ENTER)&&!in.isDown(KeyEvent.VK_ENTER),"Focus clear retained Enter alias");
    }
    static void realScreens()throws Exception{
        for(int chapter=1;chapter<=2;chapter++)for(int count=1;count<=2;count++){
            Fixture f=new Fixture(chapter,0,count);f.realPress(KeyEvent.VK_U);for(int i=0;i<30;i++)f.realTick();key(f.input,KeyEvent.VK_L,true);f.realTick();check(f.p(0).isCharging(),"Real game charge failed");
            double energy=f.p(0).getEnergy(),cd=f.p(0).getSkillCooldownRemaining(0);f.realPress(KeyEvent.VK_ESCAPE);check(!f.p(0).isCharging()&&f.game.getState()==GameState.PAUSED,"Pause kept charge");
            for(int i=0;i<90;i++)f.realTick();check(near(f.p(0).getEnergy(),energy)&&near(f.p(0).getSkillCooldownRemaining(0),cd),"Pause advanced energy/cooldown");f.realPress(KeyEvent.VK_ESCAPE);f.realTick();check(!f.p(0).isCharging(),"Held charge restarted after resume");key(f.input,KeyEvent.VK_L,false);
            f.game.setState(GameState.PAUSED);f.game.setState(GameState.PLAYING);check(f.p(0).getEnergy()==100&&f.p(0).getSkillCooldownRemaining(0)==0,"Restart carried energy or cooldown");
            ((Level)get(f.screen,"level")).boss().takeDamage(new DamageInfo(99999,0,0,true));f.realTick();double endEnergy=f.p(0).getEnergy();f.realPress(KeyEvent.VK_O);key(f.input,KeyEvent.VK_L,true);for(int i=0;i<20;i++)f.realTick();check(!f.p(0).isCharging()&&near(f.p(0).getEnergy(),endEnergy)&&f.hits().isEmpty()&&f.shots().isEmpty(),"Ending accepted skill/charge");
            Fixture death=new Fixture(chapter,2,count);death.realPress(KeyEvent.VK_I);for(Player p:death.screen.players())p.takeDamage(new DamageInfo(99999,0,0,false));death.realTick();death.realPress(KeyEvent.VK_O);check(death.hits().isEmpty()&&!death.p(0).isCharging(),"Dead player cast");
        }
        Fixture render=new Fixture(1,2,2);render.realPress(KeyEvent.VK_U);render.capture("two-player-hud");render.game.setState(GameState.CHARACTER_SELECT);render.capture("character-select");
        System.out.println("Real Game 1P/2P, both chapters: pause/resume, restart, ending/death guards, HUD and select rendering PASS");
    }
    public static void main(String[] args)throws Exception{
        int requiredVersion=java.util.Arrays.asList(args).contains("--require-v4")?4:java.util.Arrays.asList(args).contains("--require-v3")?3:0;
        Files.createDirectories(OUTPUT);bankProof(requiredVersion);casts();energy();controlsAndInterruptions();enterLocations();realScreens();check(!Boolean.getBoolean("chronorunner.debugCollision"),"Debug overlay enabled");
        String result="PASS: "+checks+" identity skill checks; actual controls/collisions; saves intercepted; debug OFF.\n";
        Files.writeString(OUTPUT.resolve("results.txt"),result);System.out.print(result);
    }
}

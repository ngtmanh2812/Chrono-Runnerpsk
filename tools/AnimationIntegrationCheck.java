import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import chronorunner.animation.*;
import chronorunner.combat.*;
import chronorunner.core.*;
import chronorunner.entity.Player;
import chronorunner.entity.boss.Boss;
import chronorunner.gfx.*;
import chronorunner.ui.PlayScreen;
import chronorunner.world.Level;

/** Exercise the installed art through real input, physics and end-screen code. */
public final class AnimationIntegrationCheck {
    static final double DT=1.0/60;
    static final Canvas KEYS=new Canvas();
    static final Path OUTPUT=Path.of("out/animation-v2-check");
    static int checks;
    static void check(boolean ok,String why) { checks++; if(!ok)throw new AssertionError(why); }
    static Object get(Object o,String name)throws Exception {
        for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())try {
            Field f=c.getDeclaredField(name); f.setAccessible(true); return f.get(o);
        }catch(NoSuchFieldException ignored) { }
        throw new NoSuchFieldException(name);
    }
    static void set(Object o,String name,double v)throws Exception {
        for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())try {
            Field f=c.getDeclaredField(name);f.setAccessible(true);f.setDouble(o,v);return;
        }catch(NoSuchFieldException ignored) { }
        throw new NoSuchFieldException(name);
    }
    static AnimationController anim(Object o)throws Exception { return (AnimationController)get(o,"anim"); }
    static class NoSaveGame extends Game { @Override public void saveProgress() { } }
    static final class Fixture {
        final NoSaveGame game=new NoSaveGame();
        final InputHandler input=game.getInput();
        final PlayScreen screen; final Level level; final Player player;
        Fixture(int chapter,int character)throws Exception {
            game.getSession().chapter=chapter;game.getSession().playerCount=1;
            game.getSession().characterIndex[0]=character;
            Method register=Game.class.getDeclaredMethod("registerScreens");register.setAccessible(true);register.invoke(game);
            game.setState(GameState.PLAYING);screen=(PlayScreen)game.screen(GameState.PLAYING);
            level=(Level)get(screen,"level");player=screen.players().get(0);
            player.x=750;player.y=level.groundY()-player.height;player.grounded=true;
            tick();
        }
        void key(int code,boolean down) {
            KeyEvent e=new KeyEvent(KEYS,down?KeyEvent.KEY_PRESSED:KeyEvent.KEY_RELEASED,0,0,code,KeyEvent.CHAR_UNDEFINED);
            if(down)input.keyPressed(e);else input.keyReleased(e);
        }
        void tick() { input.beginFrame();player.update(DT,screen);input.endFrame(); }
        void ticks(int n) { for(int i=0;i<n;i++)tick(); }
        void press(int code) { key(code,true);tick();key(code,false); }
        void gameTick() { input.beginFrame();game.update(DT);input.endFrame(); }
        void snapshot(String name)throws Exception {
            BufferedImage image=new BufferedImage(Game.WIDTH,Game.HEIGHT,BufferedImage.TYPE_INT_RGB);
            Graphics2D g=image.createGraphics();game.render(g);g.dispose();
            ImageIO.write(image,"png",OUTPUT.resolve(name+".png").toFile());
        }
    }
    static String state(Player p)throws Exception { return anim(p).currentName(); }
    static void assets()throws Exception {
        Art.validateAll();
        for(String actor:new String[]{"phan_si_kien","ronaldo","do_mixi","The Time Guardian","The Void Core"}) {
            boolean boss=actor.startsWith("The ");
            Path bank=Path.of("assets",boss?"bosses":"players",boss?actor.toLowerCase(java.util.Locale.ROOT).replace(' ','_'):actor,actor.equals("The Time Guardian")?"v5":"v4");
            if(!Files.isRegularFile(bank.resolve("animation.properties")))bank=bank.getParent().resolve(boss?"v2":"v3");
            Properties metadata=new Properties();try(var in=Files.newInputStream(bank.resolve("animation.properties"))){metadata.load(in);}
            int version=Integer.parseInt(metadata.getProperty("version"));
            for(String state:boss?Art.BOSS_ANIMATIONS:Art.PLAYER_ANIMATIONS) {
                SpriteSet set=boss?Art.bossAnimation(actor,state):Art.playerAnim(actor,state);
                check(set.layout()!=null,"Legacy art still active: "+actor+"/"+state);
                int expected=Integer.parseInt(metadata.getProperty(state+".frames",Integer.toString(version>=4?6:state.equals("fall")||state.equals("land")?2:6)));
                check(set.count()==expected,"Stale/missing frames: "+actor+"/"+state);
                check(set.frame(0).width()==Integer.parseInt(metadata.getProperty("frameWidth"))&&set.frame(0).height()==Integer.parseInt(metadata.getProperty("frameHeight")),"Wrong measured geometry");
                check(set.animation(state,.1,true).frameCount()==set.count(),"Animation count drift");
            }
        }
    }
    static void players()throws Exception {
        for(int character=0;character<3;character++) {
            Fixture f=new Fixture(1,character);Player p=f.player;
            check(state(p).equals("idle"),"Spawn not idle");
            f.press(KeyEvent.VK_D);check(state(p).equals("run"),"Run not mapped");
            p.applySlow(1);f.key(KeyEvent.VK_D,true);f.tick();f.key(KeyEvent.VK_D,false);
            check(state(p).equals("walk"),"Slow walk not distinct");
            f=new Fixture(1,character);p=f.player;f.press(KeyEvent.VK_W);
            check(state(p).equals("jump"),"Jump not mapped");
            Set<String> states=new HashSet<>();
            for(int i=0;i<100;i++) { f.tick();states.add(state(p)); }
            check(states.containsAll(Set.of("fall","land","idle")),"Jump/fall/landing transitions missing");
            f=new Fixture(1,character);p=f.player;f.press(KeyEvent.VK_K);
            check(state(p).equals("dash"),"Dash not mapped");f.ticks(20);
            check(!state(p).equals("dash"),"Dash stuck");
            f=new Fixture(1,character);p=f.player;f.press(KeyEvent.VK_J);
            Attack attack=(Attack)get(p,"basicAttack");Set<Attack.Phase> phases=new HashSet<>();
            while(attack.isBusy()) {
                phases.add(attack.phase());int frame=anim(p).current().frame();
                int window=switch(attack.phase()){case STARTUP->0;case ACTIVE->1;case RECOVERY->2;default->-1;};
                Properties layout=(Properties)get(Art.playerAnim(p.characterId(),"light_1").layout(),"properties");
                int active=Integer.parseInt(layout.getProperty("combat.activeFrame","2"));
                int recovery=Integer.parseInt(layout.getProperty("combat.recoveryFrame","4"));
                int first=window==0?0:window==1?active:recovery;
                int limit=window==0?active:window==1?recovery:Art.playerAnim(p.characterId(),"light_1").count();
                check(state(p).equals("light_1")&&frame>=first&&frame<limit,
                        "Attack pose outside damaging window: "+character+"/"+attack.phase()+" frame "+frame);
                f.tick();
            }
            check(phases.containsAll(Set.of(Attack.Phase.STARTUP,Attack.Phase.ACTIVE,Attack.Phase.RECOVERY)),"Swing window lost");
            f=new Fixture(1,character);p=f.player;f.press(KeyEvent.VK_U);
            check(state(p).equals("skill_u"),"Skill not mapped");f.ticks(22);
            check(anim(p).current().frame()==Art.playerAnim(p.characterId(), "skill_u").count()-1,"Skill recovery pose not reached");f.ticks(3);
            check(!state(p).equals("skill_u"),"Skill stuck");
            f=new Fixture(1,character);p=f.player;f.press(KeyEvent.VK_S);
            check(p.isBlocking()&&state(p).equals("block"),"Guard not animated");
            int hp=p.getHealth();p.takeDamage(new DamageInfo(10,100,-100,false));f.tick();
            check(p.getHealth()==hp&&state(p).equals("block")&&anim(p).current().frame()==3,"Guard impact not shown");
            f=new Fixture(1,character);p=f.player;p.takeDamage(new DamageInfo(10,0,0,false));f.tick();
            check(state(p).equals("hurt")&&p.getHealth()==p.getMaxHealth()-10,"Hurt damaged behavior");
            p.takeDamage(new DamageInfo(99999,0,0,false)); // invulnerability must still reject this
            check(!p.isDead(),"Animation removed invulnerability");
            f.ticks(40);p.takeDamage(new DamageInfo(99999,0,0,false));f.ticks(60);
            check(p.isDead()&&state(p).equals("death")&&anim(p).current().frame()==5&&anim(p).current().finished(),"Death did not hold final pose");
            f.snapshot("player-"+character+"-defeated");
        }
    }
    static void bossesAndScreens()throws Exception {
        for(int chapter=1;chapter<=2;chapter++) {
            Fixture f=new Fixture(chapter,0);Boss b=f.level.boss();
            if(chapter==1)b.x=1050; // Keep the entire pose visible in review captures.
            b.y=f.level.groundY()-b.height;b.grounded=true;
            f.snapshot("chapter-"+chapter+"-idle");
            set(b,"attackTimer",0);set(b,"specialTimer",100);b.update(DT,f.screen);
            check(anim(b).currentName().equals("skill_u"),"Ranged boss action did not select cast animation");
            check(((Attack)get(b,"action")).getData().name.equals("attack"),"Visual clip changed normal combat action identity");
            // A committed normal attack now owns its recovery; a Special must
            // wait for that action to finish instead of interrupting its pose.
            set(b,"attackTimer",100);
            for(int i=0;i<90&&get(b,"action")!=null;i++)b.update(DT,f.screen);
            set(b,"attackTimer",100);set(b,"specialTimer",0);b.update(DT,f.screen);
            check(anim(b).currentName().equals(chapter==1?"skill_i":"ultimate"),"Boss special did not select action-specific animation");
            check(((Attack)get(b,"action")).getData().name.equals("special"),"Visual clip changed special combat action identity");
            f.snapshot("chapter-"+chapter+"-special");
            f=new Fixture(chapter,0);b=f.level.boss();b.y=f.level.groundY()-b.height;b.grounded=true;
            if(chapter==1)b.x=1050;
            b.takeDamage(new DamageInfo(99999,0,0,true));
            double timer=(double)get(b,"attackTimer");
            for(int i=0;i<33;i++)f.gameTick();
            check(anim(b).currentName().equals("death")&&anim(b).current().frame()>0,"PlayScreen froze boss death");
            check((double)get(b,"attackTimer")==timer,"Dead boss still runs AI");
            f.snapshot("chapter-"+chapter+"-boss-falling");
            for(int i=0;i<27;i++)f.gameTick();
            check(anim(b).current().finished()&&anim(b).current().frame()==Art.bossAnimation(b.getName(),"death").count()-1,"Boss death not held");
            check(f.game.getState()==GameState.PLAYING,"Victory cut off death");
            f.snapshot("chapter-"+chapter+"-boss-defeated");
            for(int i=0;i<60;i++)f.gameTick();
            check(f.game.getState()==GameState.VICTORY,"Victory progression changed");
            f.snapshot("chapter-"+chapter+"-victory");
        }
        Fixture f=new Fixture(1,0);f.press(KeyEvent.VK_S);
        Animation a=anim(f.player).current();double progress=a.progress();
        f.game.setState(GameState.PAUSED);for(int i=0;i<120;i++)f.game.update(DT);
        check(a.progress()==progress&&f.player.isBlocking(),"Pause advanced animation");f.snapshot("pause");
        f.game.setState(GameState.CHARACTER_SELECT);f.snapshot("character-select");
        f.game.setState(GameState.HOW_TO_PLAY);f.snapshot("how-to-play");
        f.game.setState(GameState.MAIN_MENU);f.snapshot("main-menu");
    }
    public static void main(String[] args)throws Exception {
        Files.createDirectories(OUTPUT);assets();players();bossesAndScreens();
        check(!Boolean.getBoolean("chronorunner.debugCollision"),"Debug left active");
        String report="PASS: "+checks+" animation integration checks; actual controls, attack windows, guard impact, death, pause, victory and UI portraits; no save writes; debug OFF.\n";
        Files.writeString(OUTPUT.resolve("results.txt"),report);System.out.print(report);
    }
}

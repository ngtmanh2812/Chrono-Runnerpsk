import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.Properties;
import javax.imageio.ImageIO;
import chronorunner.animation.AnimationController;
import chronorunner.combat.*;
import chronorunner.entity.*;
import chronorunner.gfx.*;

/** Loaded Daddy art and actual combat actions; progress writes intercepted. */
public final class DaddyBossCheck {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static IdentitySkillsCheck.Fixture fixture()throws Exception {
        var f=new IdentitySkillsCheck.Fixture(2,0,1);var b=f.level.boss();
        b.x=850;b.y=f.level.groundY()-b.height;b.grounded=true;
        f.p(0).x=600;return f;
    }
    static void tickBoss(IdentitySkillsCheck.Fixture f,int frames)throws Exception {
        for(int i=0;i<frames;i++){f.level.boss().update(1.0/60,f.screen);f.tick();}
    }
    static void capture(IdentitySkillsCheck.Fixture f,String name)throws Exception {
        BufferedImage im=new BufferedImage(1280,720,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=im.createGraphics();f.game.render(g);g.dispose();
        ImageIO.write(im,"png",Path.of("out/daddy-check",name+".png").toFile());
    }
    public static void main(String[] args)throws Exception {
        Path output=Path.of("out/daddy-check");Files.createDirectories(output);
        Art.validateAll();
        for(String clip:Art.BOSS_ANIMATIONS){
            var bank=Art.bossAnimation("The Void Core",clip);
            Properties p=(Properties)IdentitySkillsCheck.get(bank.layout(),"properties");
            check("5".equals(p.getProperty("version")),"Old Core art active: "+clip);
            check(bank.count()==Integer.parseInt(p.getProperty(clip+".frames")),"Missing strip frames: "+clip);
            check(!bank.layout().centerAnchor&&bank.layout().pivotY==232,"Wrong ground pivot");
            for(int i=0;i<bank.count();i++)check(bank.frame(i).width()==256&&bank.frame(i).height()==256,"Wrong geometry");
        }
        check(Art.bossAnimation("The Void Core","ultimate").count()==24,"Scream24 truncated");
        check(Art.bossAnimation("The Void Core","walk").count()==9,"Walk9 truncated");
        var f=fixture();var b=f.level.boss();tickBoss(f,3);
        check(b.grounded&&Math.abs(b.bottom()-f.level.groundY())<.01,"Daddy still hovers");
        check(b.getMaxHealth()==1600&&b.width==116&&b.height==140,"Body/HP changed");
        capture(f,"idle-gameplay");
        IdentitySkillsCheck.set(b,"attackTimer",0);tickBoss(f,20);
        check(f.shots().isEmpty(),"Shoot hit before its windup");tickBoss(f,8);
        check(f.shots().stream().filter(p->p instanceof DaddyBolt).count()==3,"Shoot volley missing");
        capture(f,"shoot-gameplay");
        f=fixture();b=f.level.boss();f.p(0).x=b.x-100;int hp=f.p(0).getHealth();
        b.specialAttack(f.screen);tickBoss(f,32);check(f.p(0).getHealth()==hp,"Pulse hit before windup");
        tickBoss(f,9);check(f.p(0).getHealth()==hp-18,"Electric pulse missing/duplicated");
        check((double)IdentitySkillsCheck.get(f.p(0),"slow")>1,"Pulse slow missing");
        capture(f,"charged-gameplay");
        f=fixture();b=f.level.boss();IdentitySkillsCheck.field(b,"cycle").setInt(b,1);
        b.specialAttack(f.screen);tickBoss(f,39);
        check(Math.abs(b.vx)>300&&!f.hits().isEmpty(),"Charged v2 rush did not begin");
        capture(f,"rush-gameplay");
        var rush=f.hits().get(0);b.takeDamage(new DamageInfo(1,0,0,true));
        check(!rush.isAlive(),"Interrupted rush kept its hitbox");
        f=fixture();b=f.level.boss();IdentitySkillsCheck.field(b,"cycle").setInt(b,2);
        b.specialAttack(f.screen);tickBoss(f,48);check(f.shots().isEmpty(),"Scream before warning");
        tickBoss(f,7);check(f.shots().stream().filter(p->p instanceof DaddyBolt).count()==2,"Scream needs two sonic fronts");
        capture(f,"scream-gameplay");
        for(var shot:f.shots())check(shot.height==150,"Scream silhouette not broad");
        b.takeDamage(new DamageInfo(99999,0,0,true));tickBoss(f,70);
        var anim=(AnimationController)IdentitySkillsCheck.get(b,"anim");
        check(b.isDead()&&anim.currentName().equals("death")&&anim.current().finished(),"Death did not finish");
        check(f.game.saves==0,"Checks wrote progress");
        String report="PASS: "+checks+" Daddy checks; complete V5 including24 Scream poses; grounded movement, volley, pulse slow, interruptible rush, two sonic fronts, death and no saves.\n";
        Files.writeString(output.resolve("results.txt"),report);System.out.print(report);
    }
}

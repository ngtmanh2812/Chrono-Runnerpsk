import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import chronorunner.animation.AnimationController;
import chronorunner.combat.DamageInfo;
import chronorunner.entity.*;
import chronorunner.gfx.*;

/** Accept supplied strips through the actual loader and gameplay renderer. */
public final class PhyschoBossCheck {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception {
        Path out=Path.of("out/physcho-check");Files.createDirectories(out);
        Art.validateAll();
        for(String clip:Art.BOSS_ANIMATIONS){
            SpriteSet bank=Art.bossAnimation("The Time Guardian",clip);
            Properties p=(Properties)IdentitySkillsCheck.get(bank.layout(),"properties");
            check("5".equals(p.getProperty("version")),"Old Guardian art loaded: "+clip);
            check(bank.count()==Integer.parseInt(p.getProperty(clip+".frames")),"Truncated strip: "+clip);
            for(int n=0;n<bank.count();n++){
                check(bank.frame(n).width()==256&&bank.frame(n).height()==256,"Inconsistent geometry");
                BufferedImage im=new BufferedImage(256,256,BufferedImage.TYPE_INT_ARGB);
                Graphics2D g=im.createGraphics();g.setComposite(AlphaComposite.Src);bank.frame(n).draw(g,0,0,1,false);g.dispose();
                BufferedImage expected=ImageIO.read(Path.of("assets/bosses/the_time_guardian/v5",clip+"_"+(n+1)+".png").toFile());
                int[] actual=im.getRGB(0,0,256,256,null,0,256),raw=expected.getRGB(0,0,256,256,null,0,256);
                for(int i=0;i<raw.length;i++)if((raw[i]>>>24)==0)raw[i]=0;
                check(Arrays.equals(actual,raw),"Fallback raster loaded: "+clip+"/"+n);
            }
        }
        check(Art.bossAnimation("The Time Guardian","walk").count()==13,"Lost Walk frames");
        check(Art.bossAnimation("The Time Guardian","skill_u").count()==10,"Lost Charged frames");
        check(Art.bossAnimation("The Void Core","idle").count()==6,"Boss 2 changed");
        var f=new IdentitySkillsCheck.Fixture(1,0,1);var boss=f.level.boss();
        boss.x=950;boss.y=f.level.groundY()-boss.height;boss.grounded=true;
        var torso=boss.hurtbox().rect();
        check(torso.w==140&&torso.h==188,"Existing hurtbox changed");
        check(boss.width==96&&boss.height==124&&boss.getMaxHealth()==700,"Physics/health changed");
        var anim=(AnimationController)IdentitySkillsCheck.get(boss,"anim");
        BufferedImage contact=new BufferedImage(1280,3*720,BufferedImage.TYPE_INT_RGB);
        Graphics2D sheet=contact.createGraphics();
        String[] clips={"idle","skill_u","skill_i"};
        for(int n=0;n<clips.length;n++){
            anim.restart(clips[n]);anim.current().setProgress(n==0?0:.5);
            BufferedImage game=new BufferedImage(1280,720,BufferedImage.TYPE_INT_RGB);
            Graphics2D g=game.createGraphics();f.game.render(g);g.dispose();
            ImageIO.write(game,"png",out.resolve(clips[n]+"-gameplay.png").toFile());sheet.drawImage(game,0,n*720,null);
        }
        sheet.dispose();ImageIO.write(contact,"png",out.resolve("gameplay-contact.png").toFile());
        var shot=new PhyschoProjectile(100,200,430,0,9,new DamageInfo(10,150,-140,false));
        check(shot.width==96&&shot.height==96&&!shot.isFriendly()&&shot.damage().amount==10,"Barrel size/damage mismatch");
        shot.update(.1,f.screen);check(Math.abs(shot.centerX()-143)<1e-7,"Barrel trajectory changed");
        check(f.game.saves==0,"Visual checks wrote save");
        String report="PASS: "+checks+" PhyschoBoss checks; all clips use supplied V5, full frame counts, exact raster load, unchanged combat geometry, barrel trajectory, real gameplay renders, no save writes.\n";
        Files.writeString(out.resolve("results.txt"),report);System.out.print(report);
    }
}

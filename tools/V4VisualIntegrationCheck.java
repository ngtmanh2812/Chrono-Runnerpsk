import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import javax.imageio.ImageIO;

import chronorunner.animation.AnimationController;
import chronorunner.combat.Attack;
import chronorunner.core.Game;
import chronorunner.core.GameState;
import chronorunner.entity.Player;
import chronorunner.entity.boss.Boss;
import chronorunner.gfx.Art;
import chronorunner.gfx.PixelSprite;
import chronorunner.gfx.SpriteSet;

/** V4 visual acceptance scenarios; physics/damage suites remain independent. */
public final class V4VisualIntegrationCheck {
    private static final Path OUTPUT=Path.of("out/animation-v4-check");
    private static final String[] HEROES={"phan_si_kien","ronaldo","do_mixi"};
    private static final String[] BOSSES={"The Time Guardian","The Void Core"};
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static AnimationController anim(Object o)throws Exception{return (AnimationController)IdentitySkillsCheck.get(o,"anim");}
    private static Rectangle visible(BufferedImage image){
        int x0=image.getWidth(),y0=image.getHeight(),x1=0,y1=0;
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)if((image.getRGB(x,y)>>>24)!=0){x0=Math.min(x0,x);y0=Math.min(y0,y);x1=Math.max(x1,x+1);y1=Math.max(y1,y+1);}
        return new Rectangle(x0,y0,Math.max(0,x1-x0),Math.max(0,y1-y0));
    }
    private static BufferedImage raw(PixelSprite frame){
        BufferedImage out=new BufferedImage(frame.width(),frame.height(),BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=out.createGraphics();frame.draw(g,0,0,1,false);g.dispose();return out;
    }
    private static BufferedImage anchored(SpriteSet bank,int frame,boolean flip,int feet,int center){
        BufferedImage out=new BufferedImage(800,700,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=out.createGraphics();bank.layout().draw(bank.frame(frame),g,400,feet,center,flip,PixelSprite.Mode.NORMAL,1);g.dispose();return out;
    }
    private static boolean same(BufferedImage a,BufferedImage b){
        return java.util.Arrays.equals(a.getRGB(0,0,a.getWidth(),a.getHeight(),null,0,a.getWidth()),b.getRGB(0,0,b.getWidth(),b.getHeight(),null,0,b.getWidth()));
    }
    private static void geometry()throws Exception{
        for(int actor=0;actor<5;actor++){
            boolean boss=actor>=3;
            String id=boss?BOSSES[actor-3]:HEROES[actor];
            String slug=boss?id.toLowerCase(java.util.Locale.ROOT).replace(' ','_'):id;
            SpriteSet bank=boss?Art.bossAnimation(id,"idle"):Art.playerAnim(id,"idle");
            Properties metadata=(Properties)IdentitySkillsCheck.get(bank.layout(),"properties");
            check((id.equals("The Time Guardian")?"5":"4").equals(metadata.getProperty("version")),"Latest idle bank not loaded: "+id);
            BufferedImage before=ImageIO.read(Path.of("assets",boss?"bosses":"players",slug,boss?"v2":"v3","idle_1.png").toFile());
            Rectangle oldBounds=visible(before),newBounds=visible(raw(bank.frame(0)));
            double heightRatio=newBounds.height/(double)oldBounds.height;
            check(heightRatio>=.8&&heightRatio<=1.2,"Idle art doubled/shrank relative to old game: "+id+" ratio "+heightRatio);
            check(newBounds.width>=oldBounds.width*.55&&newBounds.width<=oldBounds.width*1.6,"Idle width distorted: "+id);
            boolean core=id.equals("The Void Core");
            check(bank.layout().centerAnchor==core,"Wrong world anchor: "+id);
            for(int frame=0;frame<bank.count();frame++){
                BufferedImage right=anchored(bank,frame,false,500,300),left=anchored(bank,frame,true,500,300);
                boolean mirrored=true;
                for(int y=0;y<700&&mirrored;y++)for(int x=0;x<800;x++)if(right.getRGB(x,y)!=left.getRGB(799-x,y)){mirrored=false;break;}
                check(mirrored,"Facing flip shifted the world pivot: "+id+" frame "+frame);
                BufferedImage other=anchored(bank,frame,false,core?600:500,core?300:400);
                check(same(right,other),"Anchor follows wrong coordinate: "+id+" frame "+frame);
                Rectangle bounds=visible(right);
                check(bounds.width>0&&bounds.height>0,"Empty rendered idle: "+id);
                if(!core)check(Math.abs(bounds.y+bounds.height-500)<=3,"Idle feet miss the floor: "+id+" frame "+frame);
            }
            SpriteSet death=boss?Art.bossAnimation(id,"death"):Art.playerAnim(id,"death");
            Rectangle settled=visible(anchored(death,death.count()-1,false,500,core?430:300));
            if(id.equals("The Time Guardian")) {
                check(settled.width==0&&settled.height==0,"Physcho death must finish fully faded");
                Rectangle prior=visible(anchored(death,0,false,500,300));
                check(Math.abs(prior.y+prior.height-500)<=5,"Physcho death begins off floor");
            } else check(Math.abs(settled.y+settled.height-500)<=5,"Settled corpse floats or sinks below the floor: "+id+" bottom="+(settled.y+settled.height));
        }
        System.out.println("V4 visible size vs old banks, both-facing world pivots, ground feet and Core centre-anchor PASS");
    }
    private static void risePoses()throws Exception{
        for(int hero=0;hero<3;hero++){
            var f=new IdentitySkillsCheck.Fixture(1,hero,1);
            Player p=f.p(0);f.press(KeyEvent.VK_W);
            Set<Integer> seen=new HashSet<>();
            for(int n=0;n<90&&p.vy<0;n++){
                if(anim(p).currentName().equals("jump"))seen.add(anim(p).current().frame());
                f.tick();
            }
            check(seen.containsAll(Set.of(2,3,4,5)),"Rising motion still skips V4 apex poses: "+p.characterId()+" "+seen);
            check(f.game.saves==0,"Rise check wrote progress");
        }
        System.out.println("All three V4 jump clips use four distinct rising/apex poses through real W input PASS");
    }
    private static void waitForStrike(IdentitySkillsCheck.Fixture f,String clip)throws Exception{
        SpriteSet bank=Art.playerAnim(f.p(0).characterId(),clip);
        Properties metadata=(Properties)IdentitySkillsCheck.get(bank.layout(),"properties");
        int active=Integer.parseInt(metadata.getProperty(clip+".activeFrame",metadata.getProperty("combat.activeFrame","3")));
        for(int n=0;n<100&&anim(f.p(0)).currentName().equals(clip)&&anim(f.p(0)).current().frame()<active;n++)f.tick();
        check(anim(f.p(0)).currentName().equals(clip)&&anim(f.p(0)).current().frame()>=active,"Capture missed strike pose: "+clip);
    }
    private static void finish(IdentitySkillsCheck.Fixture f,String field)throws Exception{
        Attack attack=(Attack)IdentitySkillsCheck.get(f.p(0),field);
        for(int n=0;n<100&&attack.isBusy();n++)f.tick();
        check(!attack.isBusy(),"Melee stage never recovered: "+field);
    }
    private static BufferedImage gameplay(IdentitySkillsCheck.Fixture f,String label)throws Exception{
        // Only suppress the temporary hit flash in review images, so the newly
        // installed boss art remains visible beside a successful player strike.
        IdentitySkillsCheck.set(f.level.boss(),"hurtFlash",0);
        BufferedImage game=new BufferedImage(Game.WIDTH,Game.HEIGHT,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=game.createGraphics();f.game.render(g);g.dispose();
        BufferedImage tile=new BufferedImage(640,384,BufferedImage.TYPE_INT_RGB);
        g=tile.createGraphics();g.setColor(new Color(2,17,39));g.fillRect(0,0,640,384);g.setColor(new Color(56,218,255));g.drawString(label,12,17);
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(game,0,24,640,360,null);g.dispose();return tile;
    }
    private static void captureInputs()throws Exception{
        String[] clips={"idle","run","dash","light_1","heavy","launcher","skill_u","skill_i","ultimate"};
        for(int chapter=1;chapter<=2;chapter++)for(int hero=0;hero<3;hero++){
            BufferedImage sheet=new BufferedImage(640*3,384*3,BufferedImage.TYPE_INT_RGB);
            Graphics2D sheetG=sheet.createGraphics();
            for(int sample=0;sample<clips.length;sample++){
                String clip=clips[sample];var f=new IdentitySkillsCheck.Fixture(chapter,hero,1);
                Player p=f.p(0);Boss b=f.level.boss();p.x=500;p.y=f.level.groundY()-p.height;p.grounded=true;
                b.x=850;b.y=f.level.groundY()-b.height;b.grounded=true;p.update(0,f.screen);b.update(0,f.screen);
                if(clip.equals("run")){IdentitySkillsCheck.key(f.input,KeyEvent.VK_D,true);f.tick();waitForStrike(f,clip);}
                else if(clip.equals("dash")){f.press(KeyEvent.VK_K);waitForStrike(f,clip);}
                else if(clip.equals("light_1")||clip.equals("heavy")||clip.equals("launcher")){
                    f.press(KeyEvent.VK_J);
                    if(!clip.equals("light_1")){finish(f,"basicAttack");f.press(KeyEvent.VK_J);}
                    if(clip.equals("launcher")){finish(f,"heavyAttack");f.press(KeyEvent.VK_J);}
                    waitForStrike(f,clip);
                } else if(clip.equals("skill_u")||clip.equals("skill_i")||clip.equals("ultimate")){
                    f.press(clip.equals("skill_u")?KeyEvent.VK_U:clip.equals("skill_i")?KeyEvent.VK_I:KeyEvent.VK_O);waitForStrike(f,clip);
                }
                double px=p.x,py=p.y,pw=p.width,ph=p.height,bx=b.x,by=b.y,bw=b.width,bh=b.height;
                sheetG.drawImage(gameplay(f,clip),sample%3*640,sample/3*384,null);
                check(p.x==px&&p.y==py&&p.width==pw&&p.height==ph&&b.x==bx&&b.y==by&&b.width==bw&&b.height==bh,"Renderer changed physics coordinates");
                check(f.game.saves==0,"Visual capture wrote progress");
            }
            sheetG.dispose();ImageIO.write(sheet,"png",OUTPUT.resolve(HEROES[hero]+"-chapter"+chapter+".png").toFile());
        }
        System.out.println("Six real-renderer contact sheets: three heroes vs both chapters, J/Heavy/Launcher, U/I/O, run/dash; no gameplay/save writes PASS");
    }
    private static void coreTeleportPoses()throws Exception{
        var f=new IdentitySkillsCheck.Fixture(2,0,1);
        Boss core=f.level.boss();
        core.x=850;core.y=f.level.groundY()-core.height;core.grounded=true;
        IdentitySkillsCheck.field(core,"cycle").setInt(core,1);
        IdentitySkillsCheck.set(core,"attackTimer",100);
        IdentitySkillsCheck.set(core,"specialTimer",0);
        double originX=core.x;
        core.update(IdentitySkillsCheck.DT,f.screen);
        Attack action=(Attack)IdentitySkillsCheck.get(core,"action");
        check(action!=null&&action.getData().name.equals("special"),"Teleport renamed gameplay action");
        Set<Integer> windupFrames=new HashSet<>();boolean strike=false;
        for(int n=0;n<120&&action.isBusy();n++){
            if(action.isStartup()){
                check(anim(core).currentName().equals("skill_i"),"Teleport lost dedicated dissolve telegraph");
                int frame=anim(core).current().frame();windupFrames.add(frame);
                check(frame<=3,"Core reconstructed at the old position before teleport");
            }else if(action.isActive()){
                check(anim(core).currentName().equals("air_attack"),"Teleport did not switch to dive pose");
                check(Math.abs(core.x-originX)>50,"Dive pose started before gameplay teleport");
                check(anim(core).current().frame()>=3&&anim(core).current().frame()<5,"Dive contact pose outside Active");
                strike=true;break;
            }
            core.update(IdentitySkillsCheck.DT,f.screen);
        }
        check(windupFrames.containsAll(Set.of(0,1,2,3))&&strike,"Incomplete dissolve/dive sequence");
        check(f.game.saves==0,"Teleport review wrote progress");
        System.out.println("Core dissolves at old position, then teleports into active dive; combat action ID/timing preserved PASS");
    }
    public static void main(String[] args)throws Exception{
        Files.createDirectories(OUTPUT);geometry();risePoses();coreTeleportPoses();captureInputs();
        check(!Boolean.getBoolean("chronorunner.debugCollision"),"Debug collision left enabled");
        String report="PASS: "+checks+" V4 visual integration checks; latest art, preserved size/pivots, actual controls and game renderer; no save writes; debug OFF.\n";
        Files.writeString(OUTPUT.resolve("results.txt"),report);System.out.print(report);
    }
}

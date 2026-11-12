import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import javax.imageio.ImageIO;

import chronorunner.combat.DamageInfo;
import chronorunner.combat.Attack;
import chronorunner.core.Game;
import chronorunner.core.GameState;
import chronorunner.entity.Particle;
import chronorunner.entity.Player;
import chronorunner.entity.Projectile;
import chronorunner.entity.SwordVisualEffect;
import chronorunner.entity.SwordWave;
import chronorunner.entity.boss.Boss;
import chronorunner.gfx.Art;
import chronorunner.gfx.PixelSprite;
import chronorunner.gfx.SwordEffects;

/** Actual input and damage acceptance, not a render-only imitation of skills. */
public final class PhanSwordSkillsCheck {
    static final Path OUTPUT=Path.of("out/phan-sword-check");
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static boolean near(double a,double b){return Math.abs(a-b)<1e-6;}
    @SuppressWarnings("unchecked") static List<Particle> particles(IdentitySkillsCheck.Fixture f)throws Exception{
        return (List<Particle>)IdentitySkillsCheck.get(f.screen,"particles");
    }
    static double trauma(IdentitySkillsCheck.Fixture f)throws Exception{
        return (double)IdentitySkillsCheck.get(IdentitySkillsCheck.get(f.screen,"camera"),"trauma");
    }
    static boolean smoke(Particle p){return p instanceof SwordVisualEffect e&&e.kind().name().equals("SMOKE");}
    static int skillKey(int player,int slot){return Player.keysFor(player)[slot==0?5:slot==1?10:11];}
    static void expandedFrameBanks()throws Exception{
        String[] clips={"skill_u","skill_i","ultimate"};double[] totals={.4,.75,1.1};
        for(int slot=0;slot<clips.length;slot++){
            String clip=clips[slot];var bank=Art.playerAnim("phan_si_kien",clip);
            check(bank.count()==11,"New Phan sword asset did not load six original plus five added poses: "+clip);
            Properties metadata=(Properties)IdentitySkillsCheck.get(bank.layout(),"properties");
            check("6".equals(metadata.getProperty(clip+".activeFrame"))&&"10".equals(metadata.getProperty(clip+".recoveryFrame")),"11-pose contact/recovery mapping stale: "+clip);
            var animation=bank.animation(clip,.1,false);
            double frameTime=((Number)IdentitySkillsCheck.get(animation,"frameDuration")).doubleValue();
            check(Math.abs(frameTime*11-totals[slot])<1e-6,"Added poses retimed the existing skill: "+clip);
            Set<Integer> hashes=new HashSet<>();
            for(int n=0;n<bank.count();n++){
                PixelSprite frame=bank.frame(n);BufferedImage image=new BufferedImage(frame.width(),frame.height(),BufferedImage.TYPE_INT_ARGB);
                Graphics2D g=image.createGraphics();frame.draw(g,0,0,1,false);g.dispose();
                hashes.add(java.util.Arrays.hashCode(image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth())));
            }
            check(hashes.size()==11,"Expanded skill bank repeats raster frames instead of added drawings: "+clip);
            check(Art.playerAnim("ronaldo",clip).count()==6&&Art.playerAnim("do_mixi",clip).count()==6,"Phan-only frame upgrade changed other character banks");
        }
        for(SwordEffects.Kind kind:SwordEffects.Kind.values())check(SwordEffects.count(kind)==11,"Effect bank did not load +5 frames: "+kind);
        System.out.println("Phan U/I/O and four effect banks: 11 loaded frames, distinct drawings, preserved visual durations and contact mapping PASS");
    }
    static void feedbackOwnership(){
        DamageInfo plain=new DamageInfo(45,-320,-280,true,.8);
        check(plain.feedback==DamageInfo.Feedback.NONE,"Legacy damage acquired sword feedback");
        DamageInfo.MeleeChain chain=new DamageInfo.MeleeChain();
        DamageInfo tagged=plain.withFeedback(DamageInfo.Feedback.PHAN_SWORD_MEDIUM).withMeleeChain(chain,2);
        check(tagged.feedback==DamageInfo.Feedback.PHAN_SWORD_MEDIUM&&tagged.meleeChain==chain&&tagged.meleeStep==2,"Chain copy lost sword feedback ownership");
        check(tagged.amount==plain.amount&&near(tagged.slowSeconds,.8)&&near(tagged.knockbackX,-320),"Feedback changed damage or control effects");
        DamageInfo cleared=tagged.withFeedback(null);
        check(cleared.feedback==DamageInfo.Feedback.NONE&&cleared.meleeChain==chain&&cleared.meleeStep==2,"Clearing optional feedback lost combat identity");
    }
    static void target(IdentitySkillsCheck.Fixture f,int player,int facing,double hurtGap)throws Exception{
        Player p=f.p(player);Boss b=f.level.boss();
        p.x=facing>0?500:1050;p.y=f.level.groundY()-p.height;p.grounded=true;
        b.x=facing>0?850:650;b.y=f.level.groundY()-b.height;b.grounded=true;
        b.update(0,f.screen);p.update(0,f.screen);
        IdentitySkillsCheck.field(p,"facing").setInt(p,facing);
        var hurt=b.hurtbox().rect();
        b.x+=facing>0?p.right()+hurtGap-hurt.x:p.x-hurtGap-(hurt.x+hurt.w);
        b.hurtbox();
    }
    static void capture(IdentitySkillsCheck.Fixture f,String name)throws Exception{
        // Suppress the ordinary white hit flash only in the review picture so
        // the accepted boss hit and new grey plume can both be inspected.
        IdentitySkillsCheck.set(f.level.boss(),"hurtFlash",0);
        BufferedImage image=new BufferedImage(Game.WIDTH,Game.HEIGHT,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();f.game.render(g);g.dispose();
        ImageIO.write(image,"png",OUTPUT.resolve(name+".png").toFile());
    }
    static void smokePixels(Particle effect)throws Exception{
        BufferedImage image=new BufferedImage(512,512,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics();g.translate(256-effect.x,256-effect.y);effect.render(g);g.dispose();
        int grey=0,opaque=0;
        for(int y=0;y<512;y++)for(int x=0;x<512;x++){
            int p=image.getRGB(x,y);if((p>>>24)<64)continue;opaque++;
            int r=p>>>16&255,green=p>>>8&255,b=p&255;
            if(Math.max(r,Math.max(green,b))-Math.min(r,Math.min(green,b))<=24&&r>=40&&r<245)grey++;
        }
        check(opaque>20&&grey>10,"Accepted impact smoke did not render visible grey pixels");
        check((image.getRGB(0,0)>>>24)==0&&(image.getRGB(511,511)>>>24)==0,"Smoke renderer drew an opaque backing");
        ImageIO.write(image,"png",OUTPUT.resolve("smoke-alpha-proof.png").toFile());
    }
    static void allSkillHits()throws Exception{
        int[] damages={40,45,120},costs={20,30,100};double[] gaps={350,150,180};
        for(int chapter=1;chapter<=2;chapter++)for(int player=0;player<2;player++)for(int facing:new int[]{-1,1})for(int slot=0;slot<3;slot++){
            var f=new IdentitySkillsCheck.Fixture(chapter,0,2);Player p=f.p(player);Boss boss=f.level.boss();
            target(f,player,facing,gaps[slot]);int before=boss.getHealth();
            Set<Particle> plumes=Collections.newSetFromMap(new IdentityHashMap<>());
            int key=skillKey(player,slot);IdentitySkillsCheck.key(f.input,key,true);f.tick();
            check(near(p.getEnergy(),100-costs[slot]),"Wrong Phan cost P"+player+" slot"+slot);
            check(boss.getHealth()==before,"Skill dealt damage during Startup");
            check(particles(f).stream().noneMatch(PhanSwordSkillsCheck::smoke),"Smoke appeared before an accepted hit");
            boolean swordWave=false,captured=false;int firstDamage=-1;
            for(int n=0;n<125;n++){
                // Immunity cannot conceal a duplicate source: keep the target
                // vulnerable while Hitbox/Projectile ownership does dedup.
                IdentitySkillsCheck.set(boss,"invuln",0);f.tick();
                for(Projectile shot:f.shots())if(shot instanceof SwordWave)swordWave=true;
                for(Particle effect:particles(f))if(smoke(effect))plumes.add(effect);
                if(boss.getHealth()<before&&firstDamage<0)firstDamage=n;
                if(chapter==1&&player==0&&facing==1&&slot==1&&firstDamage>=0&&n==firstDamage+8){
                    // Inspect the developing plume after its initial white
                    // contact flash, using actual elapsed simulation frames.
                    smokePixels(particles(f).stream().filter(PhanSwordSkillsCheck::smoke).findFirst().orElseThrow());
                }
                if(!captured&&firstDamage>=0&&n>=firstDamage+3&&player==0&&facing==1){
                    capture(f,"chapter"+chapter+"-skill"+slot+"-impact");captured=true;
                }
            }
            check(firstDamage>=0,"Extended sword skill missed valid range: chapter"+chapter+" slot"+slot+" facing"+facing);
            check(before-boss.getHealth()==damages[slot],"Wrong/repeated Phan damage slot"+slot+" got"+(before-boss.getHealth()));
            check(plumes.size()==1,"Expected exactly one accepted-hit smoke plume, got "+plumes.size());
            check(near(p.getEnergy(),100-costs[slot]),"Held skill repeated cast/cost");
            check(near(f.p(1-player).getEnergy(),100),"Phan cast spent other player's energy");
            check(trauma(f)>0,"Skill cast/hit did not shake enabled screen");
            if(slot==0)check(swordWave,"U still emitted an orb instead of a sword wave");
            check(f.game.saves==0,"Skill checks wrote user progress");
            IdentitySkillsCheck.key(f.input,key,false);
        }
        System.out.println("Both chapters x both facings x both slots: U350px, I150px beyond old reach, O; 40/45/120 once, costs, one smoke plume and sword projectile PASS");
    }
    static void normalParticlePhases()throws Exception{
        String[] fields={"basicAttack","heavyAttack","launcherAttack"};
        for(int hero=0;hero<3;hero++){
            var f=new IdentitySkillsCheck.Fixture(1,hero,1);Player p=f.p(0);target(f,0,1,300);Boss boss=f.level.boss();
            int hp=boss.getHealth();
            for(int step=0;step<3;step++){
                Set<Particle> before=Collections.newSetFromMap(new IdentityHashMap<>());before.addAll(particles(f));
                f.press(KeyEvent.VK_J);Attack action=(Attack)IdentitySkillsCheck.get(p,fields[step]);
                check(action.isBusy(),"J did not start expected stage "+step+" for hero "+hero);
                for(int n=0;n<90&&action.isStartup();n++){
                    check(particles(f).stream().allMatch(before::contains),"Normal attack particles emitted during Startup");
                    f.tick();
                }
                check(action.isActive(),"Normal attack never entered Active");
                check(particles(f).stream().anyMatch(effect->!before.contains(effect)),"Normal attack lacks active sword/identity particles: hero "+hero+" step "+step);
                for(int n=0;n<100&&action.isBusy();n++){
                    check(particles(f).stream().noneMatch(PhanSwordSkillsCheck::smoke),"Whiffed normal attack created boss smoke");
                    f.tick();
                }
                check(action.isReady(),"Normal attack particles kept combat busy");
                check(hp==boss.getHealth()&&near(p.getEnergy(),100),"Visual normal particles dealt damage or gave energy on whiff");
            }
            check(f.game.saves==0,"Normal-particle review wrote progress");
        }
        System.out.println("Three heroes x J/Heavy/Launcher: no particles before Active, identity particles on swing, no visual-only damage/energy or whiff smoke PASS");
    }
    static void skillParticlePhases()throws Exception{
        int[] costs={20,30,100};
        for(int hero=0;hero<3;hero++)for(int slot=0;slot<3;slot++){
            var f=new IdentitySkillsCheck.Fixture(1,hero,1);target(f,0,1,350);
            Player p=f.p(0);Boss boss=f.level.boss();boss.y=30;int hp=boss.getHealth();
            f.press(skillKey(0,slot));Attack action=((Attack[])IdentitySkillsCheck.get(p,"skills"))[slot];
            check(action.isStartup(),"Skill did not retain its startup for hero "+hero+" slot "+slot);
            for(int n=0;n<90&&action.isStartup();n++){
                check(particles(f).isEmpty(),"Skill particles appeared before Active: hero "+hero+" slot "+slot);
                f.tick();
            }
            check(action.isActive(),"Skill never entered Active: hero "+hero+" slot "+slot);
            check(particles(f).stream().anyMatch(effect->effect.getClass()==Particle.class),"Skill lacks shared identity particles on release: hero "+hero+" slot "+slot);
            for(int n=0;n<120;n++){
                f.tick();check(particles(f).stream().noneMatch(PhanSwordSkillsCheck::smoke),"Whiffed skill particle created boss smoke");
            }
            check(hp==boss.getHealth()&&near(p.getEnergy(),100-costs[slot]),"Skill particles changed whiff damage or cast cost");
            check(f.game.saves==0,"Skill-particle review wrote progress");
        }
        System.out.println("Three heroes x U/I/O: particles begin on Active, preserved whiff costs and damage, no impact smoke on miss PASS");
    }
    static void rejectedHitsAndMisses()throws Exception{
        for(boolean immune:new boolean[]{false,true})for(int slot=0;slot<3;slot++){
            var f=new IdentitySkillsCheck.Fixture(1,0,1);target(f,0,1,slot==0?350:150);Boss boss=f.level.boss();
            if(immune)IdentitySkillsCheck.set(boss,"invuln",10);else boss.y=30;
            int hp=boss.getHealth();f.press(skillKey(0,slot));boolean castShook=false;
            double castTrauma=0;
            for(int n=0;n<120;n++){
                f.tick();
                check(particles(f).stream().noneMatch(PhanSwordSkillsCheck::smoke),"Miss/immune hit spawned boss smoke");
                double current=trauma(f);
                if(current>0&&!castShook){castTrauma=current;castShook=true;}
                if(castShook)check(near(current,castTrauma),"Rejected hit added impact shake");
            }
            check(hp==boss.getHealth(),"Miss/immune target lost HP");
            check(castShook,"Whiffed cast had no release shake");
        }
        var other=new IdentitySkillsCheck.Fixture(1,1,1);target(other,0,1,80);int hp=other.level.boss().getHealth();
        other.press(KeyEvent.VK_U);boolean lost=false;
        for(int n=0;n<90;n++){IdentitySkillsCheck.set(other.level.boss(),"invuln",0);other.tick();lost|=other.level.boss().getHealth()<hp;check(particles(other).stream().noneMatch(PhanSwordSkillsCheck::smoke),"Another actor's attack borrowed Phan smoke");}
        check(lost,"Control actor did not land a real hit");
        System.out.println("Misses, immune overlaps and another actor's actual hit do not create Phan smoke/extra impact shake PASS");
    }
    static void killingHit()throws Exception{
        var f=new IdentitySkillsCheck.Fixture(2,0,1);target(f,0,1,150);Boss boss=f.level.boss();
        IdentitySkillsCheck.field(boss,"health").setInt(boss,45);f.press(KeyEvent.VK_I);
        for(int n=0;n<45&&!boss.isDead();n++)f.tick();
        check(boss.isDead(),"Lethal sword strike did not kill boss");
        check(particles(f).stream().filter(PhanSwordSkillsCheck::smoke).count()==1,"Killing accepted hit lost its smoke feedback");
        f.realTick();
        check(particles(f).stream().anyMatch(PhanSwordSkillsCheck::smoke),"Combat ending erased the final hit smoke");
        check(f.game.saves==0,"Killing-hit review wrote progress");
    }
    static void optionsAndPause()throws Exception{
        var off=new IdentitySkillsCheck.Fixture(1,0,1);off.game.getProgress().setScreenShake(false);target(off,0,1,150);
        off.press(KeyEvent.VK_I);boolean plume=false;
        for(int n=0;n<80;n++){IdentitySkillsCheck.set(off.level.boss(),"invuln",0);off.tick();plume|=particles(off).stream().anyMatch(PhanSwordSkillsCheck::smoke);check(near(trauma(off),0),"Screen-shake OFF setting ignored");}
        check(plume,"Disabling shake also disabled actual-hit smoke");
        var pause=new IdentitySkillsCheck.Fixture(1,0,1);target(pause,0,1,150);pause.realPress(KeyEvent.VK_I);
        for(int n=0;n<13;n++)pause.realTick();
        SwordVisualEffect effect=(SwordVisualEffect)particles(pause).stream().filter(p->p instanceof SwordVisualEffect).findFirst().orElseThrow();
        pause.realPress(KeyEvent.VK_ESCAPE);check(pause.game.getState()==GameState.PAUSED,"Failed to pause live cast");
        double age=((Number)IdentitySkillsCheck.get(effect,"age")).doubleValue(),energy=pause.p(0).getEnergy(),beforeTrauma=trauma(pause);
        for(int n=0;n<90;n++)pause.realTick();
        check(near(((Number)IdentitySkillsCheck.get(effect,"age")).doubleValue(),age),"Pause advanced sword/smoke animation");
        check(near(trauma(pause),beforeTrauma)&&near(pause.p(0).getEnergy(),energy),"Pause advanced shake or energy");
        pause.realPress(KeyEvent.VK_ESCAPE);pause.realTick();
        check(((Number)IdentitySkillsCheck.get(effect,"age")).doubleValue()>age,"Sword effect did not resume");
        var ended=new IdentitySkillsCheck.Fixture(1,0,1);ended.level.boss().takeDamage(new DamageInfo(99999,0,0,true));ended.realTick();
        double endEnergy=ended.p(0).getEnergy();Set<Particle> existing=Collections.newSetFromMap(new IdentityHashMap<>());existing.addAll(particles(ended));
        ended.realPress(KeyEvent.VK_U);ended.realPress(KeyEvent.VK_I);ended.realPress(KeyEvent.VK_O);
        for(int n=0;n<60;n++)ended.realTick();
        check(near(ended.p(0).getEnergy(),endEnergy)&&ended.shots().isEmpty()&&ended.hits().isEmpty(),"Ending accepted a new sword cast");
        check(particles(ended).stream().noneMatch(p->p instanceof SwordVisualEffect&&!existing.contains(p)),"Ending spawned new sword/smoke feedback");
        check(off.game.saves==0&&pause.game.saves==0&&ended.game.saves==0,"Lifecycle checks wrote saves");
        System.out.println("Shake option OFF, pause/resume effect and trauma freeze, ending cast rejection PASS");
    }
    public static void main(String[] args)throws Exception{
        Files.createDirectories(OUTPUT);expandedFrameBanks();feedbackOwnership();normalParticlePhases();skillParticlePhases();allSkillHits();rejectedHitsAndMisses();killingHit();optionsAndPause();
        check(!Boolean.getBoolean("chronorunner.debugCollision"),"Debug left on");
        String result="PASS: "+checks+" Phan sword skill checks; accepted-hit smoke, long-range sword casts, preserved costs/damage, controls and camera options; no save writes.\n";
        Files.writeString(OUTPUT.resolve("results.txt"),result);System.out.print(result);
    }
}

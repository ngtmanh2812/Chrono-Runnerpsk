import java.awt.Color;
import java.nio.file.*;
import chronorunner.combat.*;
import chronorunner.entity.*;
import chronorunner.entity.boss.*;
import chronorunner.obstacle.StaticPlatform;
import chronorunner.util.Rect;

/** Actual PlayScreen collisions for the new PhyschoBoss move set. No saves. */
public final class PhyschoMovesCheck {
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static IdentitySkillsCheck.Fixture fixture()throws Exception {
        var f=new IdentitySkillsCheck.Fixture(1,0,2);
        var b=f.level.boss();b.x=850;b.y=f.level.groundY()-b.height;b.grounded=true;
        f.p(0).x=650;f.p(1).x=400;return f;
    }
    static void bossTicks(IdentitySkillsCheck.Fixture f,int n)throws Exception {
        for(int i=0;i<n;i++){f.level.boss().update(1.0/60,f.screen);f.tick();}
    }
    public static void main(String[] args)throws Exception {
        Path output=Path.of("out/physcho-moves");Files.createDirectories(output);
        var circle=new RadialHitbox(0,0,100,new DamageInfo(1,0,0,false),1,null);
        check(circle.intersects(new Rect(90,-5,10,10)),"Circle edge missed");
        check(!circle.intersects(new Rect(85,85,10,10)),"Square corner wrongly inside circular pulse");
        var f=fixture();var b=f.level.boss();
        f.p(0).x=b.x-110;f.p(1).x=b.x-400;
        int hp=f.p(0).getHealth(),farHP=f.p(1).getHealth();
        b.specialAttack(f.screen);bossTicks(f,30);
        check(f.p(0).getHealth()==hp,"Charge hit during windup");
        bossTicks(f,8);
        check(f.p(0).getHealth()==hp-16,"Charged pulse missing damage");
        check((double)IdentitySkillsCheck.get(f.p(0),"stun")>.60,"Charged pulse did not stun .70 sec");
        check(f.p(1).getHealth()==farHP,"Charge hit outside its radius");
        int after=f.p(0).getHealth();bossTicks(f,18);
        check(f.p(0).getHealth()==after,"Charge damaged twice");
        bossTicks(f,48);check((double)IdentitySkillsCheck.get(f.p(0),"stun")<=0,"Stun did not expire");
        // Real AI emits a forward wave first, then the barrel clip on next attack.
        f=fixture();b=f.level.boss();IdentitySkillsCheck.set(b,"attackTimer",0);
        bossTicks(f,20);check(f.shots().stream().anyMatch(p->p instanceof BossShockWave),"Basic attack did not create forward wave");
        bossTicks(f,40);IdentitySkillsCheck.set(b,"attackTimer",0);bossTicks(f,29);
        check(f.shots().stream().anyMatch(p->p instanceof PhyschoProjectile),"Barrel Roll did not release barrel");
        // Rolling floor contact remains harmless to the barrel itself.
        f=fixture();double floor=f.level.groundY();
        f.p(0).x=200;f.p(1).x=350;
        var barrel=new PhyschoProjectile(600,floor-48,350,0,48,new DamageInfo(18,0,0,false));
        f.screen.addProjectile(barrel);f.ticks(2);
        check(barrel.width==96&&barrel.height==96,"Barrel must be four times old 24px size");
        check(barrel.alive&&Math.abs(barrel.bottom()-floor)<.01,"Barrel exploded on supporting floor");
        // Impact routes explosion damage through a radial hitbox, once per player.
        f.shots().clear();f.p(0).x=650;f.p(1).x=700;
        barrel=new PhyschoProjectile(672,floor-48,0,0,48,new DamageInfo(18,0,0,false));
        f.screen.addProjectile(barrel);hp=f.p(0).getHealth();farHP=f.p(1).getHealth();
        f.tick();check(!barrel.alive,"Barrel did not explode on player");f.tick();
        check(f.p(0).getHealth()==hp-18&&f.p(1).getHealth()==farHP-18,"Explosion not radial/co-op or direct+blast damage duplicated");
        int count=f.hits().size();barrel.onImpact(f.screen);check(f.hits().size()==count,"Repeated impact exploded twice");
        IdentitySkillsCheck.OUTPUT.toFile().mkdirs();f.capture("physcho-barrel-explosion");
        f=fixture();barrel=new PhyschoProjectile(650,floor-48,0,0,48,new DamageInfo(18,0,0,false));
        var wall=new StaticPlatform(630,floor-100,50,100,Color.GRAY);
        check(barrel.collidesWithPlatform(wall),"Barrel ignored wall");
        f.level.platforms().add(wall);f.screen.addProjectile(barrel);f.tick();
        check(!barrel.alive&&!f.hits().isEmpty(),"Wall did not trigger explosion");
        check(f.game.saves==0,"Move tests wrote save");
        String result="PASS: "+checks+" Physcho move checks: windup, circular230px stun/expiration, normal wave, barrel attack, 96px rolling floor/wall/player explosion, co-op and dedup; no saves.\n";
        Files.writeString(output.resolve("results.txt"),result);System.out.print(result);
    }
}

package chronorunner.entity.character;

import java.awt.Color;
import chronorunner.util.Rect;
import chronorunner.combat.Hitbox;

import chronorunner.combat.Attack;
import chronorunner.combat.AttackData;
import chronorunner.combat.DamageInfo;
import chronorunner.combat.DamageInfo.Feedback;
import chronorunner.core.InputHandler;
import chronorunner.entity.Player;
import chronorunner.entity.SwordWave;
import chronorunner.entity.SwordVisualEffect;
import chronorunner.gfx.SwordEffects.Kind;
import chronorunner.world.CombatWorld;
import chronorunner.entity.SkillPulse;

/** Time swordsman: travelling blade, extended cleave and clock-cross finisher. */
public class PhanSiKien extends Player {

    public PhanSiKien(int playerIndex, InputHandler input, double x, double y) {
        super(playerIndex, input, "Phan Sĩ Kiên", x, y, keysFor(playerIndex));
        this.maxHealth = 110;
        this.health = maxHealth;
        this.moveSpeed = 330;
        this.jumpSpeed = 780;
        this.dashSpeed = 780;
        this.dashDuration = 0.15;
        this.dashCooldown = 0.6;
        this.maxJumps = 2;
        this.accentColor = new Color(150, 230, 255);
        configureSkill(0, "Chrono Shot", 1.4, 8, 4, 12);
        configureSkill(1, "Time Slash", 3.0, 12, 15, 18);
        configureSkill(2, "Timeline Break", 8.0, 24, 12, 30);
        // Balanced: 9f wind-up, 6f active (frames 9-14), 11f recovery. This is
        // the reference attack: pressing J winds up for frames 0-8, deals
        // damage on frames 9-14 and is committed through frame 25.
        this.basicAttack = new Attack(new AttackData("Chrono Slash", PLAYER_BASE_ATTACK_DAMAGE, 84, 54, 9, 6, 11, 100, 0));
    }

    @Override
    protected void performSkill(int slot, CombatWorld world) {
        if (slot == 0) {
            double sx = facing >= 0 ? right() + 28 : x - 28;
            world.addProjectile(new SwordWave(sx,centerY()-8,facing,
                    new DamageInfo(40,200*facing,-150,true).withFeedback(Feedback.PHAN_SWORD_LIGHT)));
            world.addParticle(SwordVisualEffect.slash(Kind.CRESCENT,sx,centerY()-8,facing,.24,.55));
            world.shake(.22);
        } else if (slot == 1) {
            double sx = facing >= 0 ? right() : x - 220;
            addSkillHitbox(world,new Hitbox(new Rect(sx,y-10,220,height+20),
                    new DamageInfo(45,320*facing,-280,true,.8).withFeedback(Feedback.PHAN_SWORD_MEDIUM),.25,true,this));
            world.addParticle(SwordVisualEffect.slash(Kind.LONG_SLASH,sx+110,centerY()-5,facing,.32,1));
            world.shake(.30);
        } else {
            addSkillHitbox(world, new Hitbox(new Rect(x - 220, y - 140, width + 440, height + 200),
                    new DamageInfo(120,0,-420,true,1.2).withFeedback(Feedback.PHAN_SWORD_ULTIMATE),.2,true,this));
            world.addParticle(SwordVisualEffect.slash(Kind.CROSS_BURST,centerX(),centerY()-30,facing,.58,1));
            world.addParticle(SwordVisualEffect.slash(Kind.LONG_SLASH,centerX(),centerY()-10,facing,.34,1.6));
            world.addParticle(new SkillPulse(centerX(),centerY(),240,.55,skillEffectColor));
            world.shake(.48);
        }
    }

    @Override protected void emitMeleeTrail(CombatWorld world,int direction,int step) {
        Kind kind=step==2?Kind.LONG_SLASH:Kind.CRESCENT;
        double reach=step==2?70:step==3?45:40;
        world.addParticle(SwordVisualEffect.slash(kind,centerX()+direction*reach,
                centerY()-(step==3?20:0),direction,.18,step==2?.38:.42));
    }

    @Override
    public String characterId() {
        return "phan_si_kien";
    }
}

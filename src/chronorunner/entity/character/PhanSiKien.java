package chronorunner.entity.character;

import java.awt.Color;
import chronorunner.util.Rect;
import chronorunner.combat.Hitbox;

import chronorunner.combat.Attack;
import chronorunner.combat.AttackData;
import chronorunner.combat.DamageInfo;
import chronorunner.core.InputHandler;
import chronorunner.entity.Player;
import chronorunner.entity.Projectile;
import chronorunner.world.CombatWorld;
import chronorunner.entity.SkillPulse;

/** Balanced all-rounder with a long range Chrono Shot. */
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
            double sx = facing >= 0 ? right() + 10 : x - 10;
            world.addProjectile(new Projectile(sx, centerY(), facing * 780, 0, 9,
                    new DamageInfo(40, 200 * facing, -150, true), true, skillEffectColor, 1.6));
        } else if (slot == 1) {
            double sx = facing >= 0 ? right() : x - 135;
            addSkillHitbox(world, new Hitbox(new Rect(sx, y - 10, 135, height + 20),
                    new DamageInfo(45, 320 * facing, -280, true, .8), .25, true, this));
            world.addParticle(new SkillPulse(centerX() + facing * 75, centerY(), 90, .35, skillEffectColor));
        } else {
            addSkillHitbox(world, new Hitbox(new Rect(x - 220, y - 140, width + 440, height + 200),
                    new DamageInfo(120, 0, -420, true, 1.2), .2, true, this));
            world.addParticle(new SkillPulse(centerX(), centerY(), 240, .65, skillEffectColor));
            world.addParticle(new SkillPulse(centerX(), centerY(), 160, .5, new Color(180, 245, 255)));
            world.shake(.45);
        }
    }

    @Override
    public String characterId() {
        return "phan_si_kien";
    }
}

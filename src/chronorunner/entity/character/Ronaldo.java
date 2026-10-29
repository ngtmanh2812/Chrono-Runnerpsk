package chronorunner.entity.character;

import java.awt.Color;
import chronorunner.entity.Projectile;

import chronorunner.combat.Attack;
import chronorunner.combat.AttackData;
import chronorunner.combat.DamageInfo;
import chronorunner.combat.Hitbox;
import chronorunner.core.InputHandler;
import chronorunner.entity.Player;
import chronorunner.util.Rect;
import chronorunner.world.CombatWorld;
import chronorunner.entity.SkillPulse;

/** Footballer: fast footwork, power shots and an overhead bicycle kick. */
public class Ronaldo extends Player {

    public Ronaldo(int playerIndex, InputHandler input, double x, double y) {
        super(playerIndex, input, "Ronaldo", x, y, keysFor(playerIndex));
        this.maxHealth = 85;
        this.health = maxHealth;
        this.moveSpeed = 420;
        this.jumpSpeed = 800;
        this.dashSpeed = 950;
        this.dashDuration = 0.13;
        this.dashCooldown = 0.38;
        this.maxJumps = 2;
        this.accentColor = new Color(250, 240, 90);
        this.skillEffectColor = new Color(60, 230, 180);
        configureSkill(0, "Power Shot", 1.1, 8, 4, 12);
        configureSkill(1, "Step-over Burst", 3.0, 12, 15, 18);
        configureSkill(2, "Siuu Bicycle Kick", 8.0, 24, 12, 30);
        // Fastest: 5f wind-up, 6f active (frames 5-10), 7f recovery.
        this.basicAttack = new Attack(new AttackData("Quick Kick", PLAYER_BASE_ATTACK_DAMAGE, 48, 46, 5, 6, 7, 180));
    }

    @Override
    protected void performSkill(int slot, CombatWorld world) {
        if (slot == 0 || slot == 2) {
            int damage = slot == 0 ? 40 : 120;
            double sx = facing >= 0 ? right() + 16 : x - 16;
            world.addProjectile(new Projectile(sx, centerY() - (slot == 2 ? 10 : 0), facing * (slot == 2 ? 1000 : 850),
                    0, slot == 2 ? 17 : 11, new DamageInfo(damage, 400 * facing, -260, true),
                    true, slot == 2 ? accentColor : skillEffectColor, 1.8, Projectile.Visual.FOOTBALL));
            if (slot == 2) {
                world.addParticle(new SkillPulse(centerX(), bottom() - 8, 170, .5, accentColor));
                world.shake(.4);
            }
        } else {
            startSkillDash(.25);
            double hx = facing >= 0 ? x : x - 100;
            addSkillHitbox(world, new Hitbox(new Rect(hx, y, width + 100, height),
                    new DamageInfo(45, 430 * facing, -220, true), .25, true, this, true));
            world.addParticle(new SkillPulse(centerX(), bottom() - 6, 90, .28, skillEffectColor));
        }
    }

    @Override
    public String characterId() {
        return "ronaldo";
    }
}

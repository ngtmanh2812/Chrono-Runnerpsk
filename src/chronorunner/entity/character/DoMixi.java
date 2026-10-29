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

/** Streamer tank: microphone waves, ground slams and a community rally. */
public class DoMixi extends Player {

    public DoMixi(int playerIndex, InputHandler input, double x, double y) {
        super(playerIndex, input, "Độ Mixi", x, y, keysFor(playerIndex));
        this.maxHealth = 160;
        this.health = maxHealth;
        this.moveSpeed = 250;
        this.jumpSpeed = 720;
        this.dashSpeed = 620;
        this.dashDuration = 0.18;
        this.dashCooldown = 0.9;
        this.maxJumps = 2;
        this.accentColor = new Color(255, 205, 70);
        this.skillEffectColor = new Color(80, 220, 245);
        configureSkill(0, "Sóng Âm", 2.0, 8, 4, 12);
        configureSkill(1, "Đập Bàn", 3.0, 12, 15, 18);
        configureSkill(2, "Đại Hội Tộc", 8.0, 24, 12, 30);
        // Heaviest: 14f wind-up, 10f active (frames 14-23), 18f recovery.
        this.basicAttack = new Attack(new AttackData("Mic Strike", PLAYER_BASE_ATTACK_DAMAGE, 72, 66, 14, 10, 18, 380));
    }

    @Override
    protected void performSkill(int slot, CombatWorld world) {
        if (slot == 0) {
            double sx = facing >= 0 ? right() + 14 : x - 14;
            world.addProjectile(new Projectile(sx, centerY(), facing * 660, 0, 14,
                    new DamageInfo(40, 180 * facing, -160, true), true, skillEffectColor, 1.7,
                    Projectile.Visual.SOUND_WAVE));
        } else {
            double range = slot == 1 ? 145 : 310;
            int damage = slot == 1 ? 45 : 120;
            addSkillHitbox(world, new Hitbox(new Rect(x - range, y - 35, width + range * 2, height + 55),
                    new DamageInfo(damage, 0, slot == 1 ? -340 : -440, true),
                    slot == 1 ? .25 : .2, true, this));
            world.addParticle(new SkillPulse(centerX(), bottom() - 6, range, .5, new Color(255, 193, 75)));
            world.addParticle(new SkillPulse(centerX(), bottom() - 6, range * .78, .65, new Color(180, 110, 255)));
            if (slot == 2) {
                world.addParticle(new SkillPulse(centerX(), bottom() - 6, range * 1.1, .75, skillEffectColor));
                world.shake(.5);
            }
        }
    }

    @Override
    public String characterId() {
        return "do_mixi";
    }
}

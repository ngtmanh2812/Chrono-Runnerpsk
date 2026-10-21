package chronorunner.entity.character;

import java.awt.Color;

import chronorunner.combat.Attack;
import chronorunner.combat.AttackData;
import chronorunner.combat.DamageInfo;
import chronorunner.combat.Hitbox;
import chronorunner.core.InputHandler;
import chronorunner.entity.Particle;
import chronorunner.entity.Player;
import chronorunner.util.Rect;
import chronorunner.world.CombatWorld;

/** Slow tank with heavy hits and the "Alo Vũ à Vũ" area shockwave. */
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
        this.skillCooldown = 2.0;
        this.accentColor = new Color(255, 205, 70);
        this.skillName = "Alo Vũ à Vũ";
        // Heaviest: 14f wind-up, 10f active (frames 14-23), 18f recovery.
        this.basicAttack = new Attack(new AttackData("Heavy Swing", PLAYER_BASE_ATTACK_DAMAGE, 72, 66, 14, 10, 18, 380));
    }

    @Override
    protected void performSkill(CombatWorld world) {
        announceSkill(world);
        Rect rect = new Rect(x - 90, y - 12, width + 180, height + 24);
        world.addHitbox(new Hitbox(rect, new DamageInfo(26, 0, -320, true), 0.2, true, this));
        for (int i = 0; i < 20; i++) {
            double angle = Math.PI * 2 * i / 20.0;
            world.addParticle(new Particle(centerX(), centerY(),
                    Math.cos(angle) * 260, Math.sin(angle) * 260, 8, skillEffectColor, 0.4, 0));
        }
    }

    @Override
    public String characterId() {
        return "do_mixi";
    }
}

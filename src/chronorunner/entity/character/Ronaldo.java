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

/** Glass cannon: fastest movement and dash, signature "Siuu" burst. */
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
        this.attackPower = 9;
        this.skillCooldown = 1.1;
        this.accentColor = new Color(250, 240, 90);
        this.skillName = "Siuu";
        // Fastest: 5f wind-up, 6f active (frames 5-10), 7f recovery.
        this.basicAttack = new Attack(new AttackData("Quick Jab", 9, 48, 46, 5, 6, 7, 180));
    }

    @Override
    protected void performAttack(CombatWorld world) {
        basicAttack.start(this, facing, true, world);
    }

    @Override
    protected void performSkill(CombatWorld world) {
        announceSkill(world);
        startSkillDash(0.22);
        double hx = facing >= 0 ? x : x - 120;
        Rect rect = new Rect(hx, y, 120, height);
        // The burst rides along with the dash, otherwise it would land ~200px
        // behind the spot Ronaldo actually ends up in.
        world.addHitbox(new Hitbox(rect, new DamageInfo(22, 430 * facing, -220, true),
                0.22, true, this, true));
        for (int i = 0; i < 12; i++) {
            world.addParticle(new Particle(centerX(), centerY(),
                    (Math.random() - 0.5) * 320, (Math.random() - 0.5) * 320, 6,
                    accentColor, 0.35, 0));
        }
    }

    @Override
    public String characterId() {
        return "ronaldo";
    }
}

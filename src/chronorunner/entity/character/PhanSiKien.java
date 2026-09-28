package chronorunner.entity.character;

import java.awt.Color;

import chronorunner.combat.Attack;
import chronorunner.combat.AttackData;
import chronorunner.combat.DamageInfo;
import chronorunner.core.InputHandler;
import chronorunner.entity.Player;
import chronorunner.entity.Projectile;
import chronorunner.world.CombatWorld;

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
        this.attackPower = 12;
        this.skillCooldown = 1.4;
        this.accentColor = new Color(150, 230, 255);
        this.skillName = "Chrono Shot";
        // Balanced: 9f wind-up, 6f active (frames 9-14), 11f recovery. This is
        // the reference attack: pressing J winds up for frames 0-8, deals
        // damage on frames 9-14 and is committed through frame 25.
        this.basicAttack = new Attack(new AttackData("Chrono Slash", 12, 60, 54, 9, 6, 11, 260));
    }

    @Override
    protected void performAttack(CombatWorld world) {
        basicAttack.start(this, facing, true, world);
    }

    @Override
    protected void performSkill(CombatWorld world) {
        announceSkill(world);
        double sx = facing >= 0 ? right() + 8 : x - 8;
        double sy = centerY();
        world.addProjectile(new Projectile(sx, sy, facing * 780, 0, 9,
                new DamageInfo(20, 200 * facing, -150, true), true, accentColor, 1.6));
    }

    @Override
    public String characterId() {
        return "phan_si_kien";
    }
}

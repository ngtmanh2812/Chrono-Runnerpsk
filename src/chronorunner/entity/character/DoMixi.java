package chronorunner.entity.character;

import java.awt.Color;
import java.awt.Graphics2D;

import chronorunner.combat.Attack;
import chronorunner.combat.AttackData;
import chronorunner.combat.DamageInfo;
import chronorunner.core.InputHandler;
import chronorunner.entity.DriedChickenJar;
import chronorunner.entity.Player;
import chronorunner.entity.SmokeBlast;
import chronorunner.gfx.ShishaProp;
import chronorunner.world.CombatWorld;

/** Streamer tank: jar throw, forward smoke and a call that strikes the boss. */
public class DoMixi extends Player {
    public static final String JAR_SKILL_NAME = "Ném Khô Gà";
    public static final String SMOKE_SKILL_NAME = "Phun Khói";
    public static final String CALL_SKILL_NAME = "Gọi Điện";
    private static final int[] LIFT_X = {23, 26, 30, 34};
    private static final int[] LIFT_HEIGHT = {57, 66, 77, 84};
    private static final int[] LIFT_ANGLE = {-8, -3, 3, DriedChickenJar.RELEASE_ANGLE};
    private static final int RELEASE_POSE = LIFT_X.length - 1;

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
        configureSkill(0, JAR_SKILL_NAME, 2.0, 25, 6, 14);
        configureSkill(1, SMOKE_SKILL_NAME, 3.0, 42, 14, 24);
        configureSkill(2, CALL_SKILL_NAME, 8.0, 58, 19, 15);
        // Heaviest: 14f wind-up, 10f active (frames 14-23), 18f recovery.
        this.basicAttack = new Attack(new AttackData("Mic Strike", PLAYER_BASE_ATTACK_DAMAGE, 96, 66, 14, 10, 18, 140, 0));
    }

    @Override
    protected void performSkill(int slot, CombatWorld world) {
        if (slot == 0) {
            world.addProjectile(new DriedChickenJar(jarX(RELEASE_POSE), jarY(RELEASE_POSE), facing,
                    new DamageInfo(40, 180 * facing, -160, true).withFeedback(DamageInfo.Feedback.DO_MIXI_JAR)));
        } else if (slot == 1) {
            world.addProjectile(new SmokeBlast(centerX() + facing * ShishaProp.MOUTH_X,
                    bottom() + ShishaProp.MOUTH_Y, facing,
                    new DamageInfo(45, 240 * facing, -180, true).withFeedback(DamageInfo.Feedback.DO_MIXI_SMOKE)));
        } else {
            world.strikeChapterBoss(new DamageInfo(120, 0, -440, true)
                    .withFeedback(DamageInfo.Feedback.DO_MIXI_CALL));
        }
    }

    public boolean isCallPhotoVisible() {
        if (isDead() || !"ultimate".equals(anim.currentName())) return false;
        int pose = anim.current().frame();
        return pose >= 2 && pose <= 5;
    }

    private double jarX(int pose) { return centerX() + facing * LIFT_X[pose]; }
    private double jarY(int pose) { return bottom() - LIFT_HEIGHT[pose]; }

    @Override public void render(Graphics2D g) {
        if (!isDead() && "skill_i".equals(anim.currentName())) {
            ShishaProp.draw(g, centerX(), bottom(), facing, anim.current().frame());
        }
        super.render(g);
        if (!isDead() && "skill_u".equals(anim.currentName())) {
            int pose = anim.current().frame();
            if (pose < LIFT_X.length) {
                DriedChickenJar.draw(g, jarX(pose), jarY(pose), facing * LIFT_ANGLE[pose]);
            }
        }
    }

    @Override
    public String characterId() {
        return "do_mixi";
    }
}

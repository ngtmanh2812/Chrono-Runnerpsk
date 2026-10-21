    package chronorunner.entity.boss;

    import java.awt.Color;

    import chronorunner.combat.DamageInfo;
    import chronorunner.combat.Hitbox;
    import chronorunner.entity.Player;
    import chronorunner.entity.enemy.TimeSlime;
    import chronorunner.util.Rect;
    import chronorunner.world.CombatWorld;

    /** Chapter 2: fast chaser with leap shockwaves and slime summons. */
    public class ChronoBeast extends Boss {

        private boolean alternate;

        public ChronoBeast(double x, double y) {
            super("Chrono Beast", x, y, 112, 88, 900, 14, 215,
                    new Color(170, 250, 180));
        }

        @Override
        protected void updateAI(double dt, CombatWorld world) {
            Player target = nearestPlayer(world);
            if (target == null) {
                vx = 0;
                return;
            }
            faceTarget(target);
            double distance = Math.abs(target.centerX() - centerX());

            if (specialTimer <= 0) {
                specialAttack(world);
                specialTimer = 4.6 * cooldownScale();
                return;
            }

            if (attackTimer <= 0 && distance < 130) {
                melee(world, 96, 80, scaleDamage(12), 300);
                attackTimer = 0.9 * cooldownScale();
            }
            walkToward(target, speed(), distance < 120 ? 0 : 60);
        }

        @Override
        public void specialAttack(CombatWorld world) {
            Player target = nearestPlayer(world);
            alternate = !alternate;
            if (alternate && target != null) {
                // Leap attack: hop toward the player and slam the ground.
                faceTarget(target);
                hop(950);
                dash(facing, speed() * 2.6, 0.45);
                Rect slam = new Rect(centerX() - 150, y + height - 40, 300, 90);
                world.addHitbox(new Hitbox(slam, new DamageInfo(scaleDamage(16), 0, -420, false), 0.35, false, this));
                world.shake(0.5);
            } else {
                // Summon two slimes from the edges of the arena.
                summon(world, new TimeSlime(Math.max(40, centerX() - 220), y - 40));
                summon(world, new TimeSlime(Math.min(world.worldWidth() - 80, centerX() + 180), y - 40));
            }
        }
    }

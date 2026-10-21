package chronorunner.world;

import java.util.List;

import chronorunner.combat.Hitbox;
import chronorunner.entity.FloatingText;
import chronorunner.entity.Particle;
import chronorunner.entity.Player;
import chronorunner.entity.Projectile;
import chronorunner.entity.enemy.Enemy;
import chronorunner.obstacle.Platform;

/**
 * The narrow view of the arena that entities are allowed to touch. Keeps the
 * entity classes decoupled from the rendering screen.
 */
public interface CombatWorld {

    /** False while the round is over; paused screens do not tick entities. */
    default boolean isCombatActive() { return true; }

    List<Player> players();

    List<Platform> platforms();

    void addHitbox(Hitbox hitbox);

    void addProjectile(Projectile projectile);

    void addParticle(Particle particle);

    void addFloatingText(FloatingText text);

    void addEnemy(Enemy enemy);

    void shake(double amount);

    double worldWidth();

    double worldHeight();
}

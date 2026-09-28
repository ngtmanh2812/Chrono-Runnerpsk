package chronorunner.combat;

import java.util.HashSet;
import java.util.Set;

import chronorunner.entity.GameObject;
import chronorunner.util.Rect;

/**
 * A short lived damage region. Each target can only be hit once per hitbox.
 *
 * <p>By default the region is fixed where it was spawned, which is what most
 * attacks want. A hitbox built with {@code followsOwner} instead rides along
 * with its owner at the offset it was created with, for moves that are supposed
 * to damage the path they travel (a dashing strike) rather than the spot they
 * started from. Such a hitbox mutates the rect it was handed in place, so pass
 * it a rect you do not reuse.
 */
public class Hitbox {

    /**
     * Slack allowed on the lifetime, matching the tolerance {@code Attack} uses
     * for its frame windows. A lifetime such as 6/60 s arrives as the sum of six
     * 1/60 dt's and can land a few ULPs above zero, which would otherwise keep
     * an expired hitbox alive for one extra update: the attack would already be
     * in recovery while its hitbox still dealt damage.
     */
    private static final double EPS = 1e-9;

    public final Rect rect;
    public final DamageInfo damage;
    public final boolean friendly;
    public final GameObject owner;

    private final boolean followsOwner;
    private final double offsetX;
    private final double offsetY;

    private double life;
    private final Set<GameObject> alreadyHit = new HashSet<>();

    public Hitbox(Rect rect, DamageInfo damage, double life, boolean friendly, GameObject owner) {
        this(rect, damage, life, friendly, owner, false);
    }

    public Hitbox(Rect rect, DamageInfo damage, double life, boolean friendly, GameObject owner,
                  boolean followsOwner) {
        this.rect = rect;
        this.damage = damage;
        this.life = life;
        this.friendly = friendly;
        this.owner = owner;
        this.followsOwner = followsOwner && owner != null;
        this.offsetX = this.followsOwner ? rect.x - owner.x : 0;
        this.offsetY = this.followsOwner ? rect.y - owner.y : 0;
    }

    public void update(double dt) {
        life -= dt;
        // Treat the float residue left after the final whole frame as expired,
        // so the hitbox dies on the same frame its window ends.
        if (life < EPS) life = 0;
        if (followsOwner) {
            rect.set(owner.x + offsetX, owner.y + offsetY, rect.w, rect.h);
        }
    }

    public boolean isAlive() {
        return life > 0;
    }

    public boolean hasHit(GameObject target) {
        return alreadyHit.contains(target);
    }

    public void markHit(GameObject target) {
        alreadyHit.add(target);
    }
}

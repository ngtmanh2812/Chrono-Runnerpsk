package chronorunner.entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import chronorunner.combat.DamageInfo;
import chronorunner.combat.RadialHitbox;
import chronorunner.world.CombatWorld;
import chronorunner.obstacle.Platform;

/** PhyschoBoss's supplied barrel art around the original projectile collider. */
public final class PhyschoProjectile extends Projectile {
    private boolean exploded;
    private double roll;
    private static final BufferedImage ART = load();

    private static BufferedImage load() {
        try {
            BufferedImage image = ImageIO.read(new File("assets/bosses/the_time_guardian/v5/projectile.png"));
            if (image == null) throw new IOException("Unreadable Physcho projectile");
            return image;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load PhyschoBoss projectile", e);
        }
    }

    public PhyschoProjectile(double x, double y, double vx, double vy, double radius, DamageInfo damage) {
        super(x, y, vx, vy, radius, damage, false, new Color(130, 165, 170), 3.0);
        width=96;height=96;this.x=x-48;this.y=y-48;
    }

    @Override public void update(double dt,CombatWorld world) {
        double previousBottom=bottom();
        vy=Math.min(900,vy+1500*dt);
        super.update(dt,world);roll+=vx*dt/48;
        double surface=Double.POSITIVE_INFINITY;
        for(Platform p:world.platforms()) {
            if(right()>p.x&&x<p.right()&&previousBottom<=p.y+.1&&bottom()>=p.y&&vy>=0)
                surface=Math.min(surface,p.y);
        }
        if(Double.isFinite(surface)){y=surface-height;vy=0;}
        if(x<0||right()>world.worldWidth())onImpact(world);
    }

    @Override public boolean collidesWithPlatform(Platform p) {
        if(Math.abs(bottom()-p.y)<.1&&vy>=0)return false; // Roll on top, explode on walls.
        return super.collidesWithPlatform(p);
    }

    @Override public boolean damagesOnContact() { return false; }

    @Override public void onImpact(CombatWorld world) {
        if(exploded||!world.isCombatActive())return;
        exploded=true;alive=false;
        world.addHitbox(new RadialHitbox(centerX(),centerY(),120,damage(),.14,this));
        world.addParticle(new BossShockEffect(centerX(),centerY(),120,true));
        world.shake(.38);
        for(int i=0;i<24;i++) {
            double a=i*Math.PI*2/24;
            world.addParticle(new Particle(centerX(),centerY(),Math.cos(a)*230,Math.sin(a)*230,
                    4+i%4,i%3==0?new Color(255,184,74):new Color(115,119,125),.35+i%3*.09,.4));
        }
    }

    @Override public void render(Graphics2D g) {
        Graphics2D copy=(Graphics2D)g.create();
        copy.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        copy.translate(centerX(),centerY());copy.rotate(roll);
        copy.drawImage(ART,-48,-48,96,96,null);
        copy.dispose();
    }
}

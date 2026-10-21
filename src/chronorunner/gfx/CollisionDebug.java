package chronorunner.gfx;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import chronorunner.entity.GameObject;
import chronorunner.entity.Player;
import chronorunner.obstacle.Platform;
import chronorunner.world.Level;

/** Opt-in development overlay; normal launches never enable this property. */
public final class CollisionDebug {
    private CollisionDebug() { }

    public static void render(Graphics2D g, Level level, List<Player> players) {
        if (!Boolean.getBoolean("chronorunner.debugCollision")) return;
        Graphics2D debug = (Graphics2D) g.create();
        for (Platform p : level.platforms()) {
            debug.setColor(p.y == level.groundY() ? Color.GREEN : Color.CYAN);
            outline(debug, p);
        }
        debug.setColor(Color.YELLOW);
        for (Player p : players) outline(debug, p);
        if (level.boss() != null) outline(debug, level.boss());
        debug.setColor(Color.MAGENTA);
        debug.draw(new Rectangle2D.Double(0, 0, level.width(), level.height()));
        debug.dispose();
    }

    private static void outline(Graphics2D g, GameObject object) {
        g.draw(new Rectangle2D.Double(object.x, object.y, object.width, object.height));
    }
}

package chronorunner.entity.boss;

import chronorunner.gfx.Art;
import chronorunner.gfx.SpriteLayout;
import chronorunner.util.Rect;

/** Stable torso envelopes measured on the installed 256px boss banks.
 * Excludes cloak, sword, halo and VFX; never changes floor/platform collision.
 */
final class BossHurtGeometry {
    private BossHurtGeometry() { }

    static Rect bounds(String name, double x, double y, double width, double height, int facing) {
        SpriteLayout layout = Art.bossAnimation(name, "idle").layout();
        if (layout == null || layout.width != 256 || layout.height != 256)
            return new Rect(x, y, width, height);
        int sx, sy, sw, sh;
        if ("The Time Guardian".equals(name)) {
            sx = 94; sy = 138; sw = 70; sh = 94;
        } else if ("The Void Core".equals(name)) {
            if (!layout.centerAnchor) { // Grounded Daddy bank, same-sized torso as Guardian.
                sx = 94; sy = 138; sw = 70; sh = 94;
            } else { sx = 88; sy = 108; sw = 88; sh = 68; }
        } else return new Rect(x, y, width, height);
        double left = facing < 0 ? layout.pivotX - sx - sw : sx - layout.pivotX;
        double anchorY = layout.centerAnchor ? y + height / 2 : y + height;
        return new Rect(x + width / 2 + left * layout.scale,
                anchorY + layout.offsetY + (sy - layout.pivotY) * layout.scale,
                sw * layout.scale, sh * layout.scale);
    }
}

import chronorunner.gfx.Art;
import chronorunner.gfx.Humanoid;
import chronorunner.gfx.PixelSprite;
import chronorunner.gfx.SpriteSet;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Dev-only check. Prints the generated player sprites as ASCII glyphs so the
 * template geometry can be eyeballed, and proves that every glyph the template
 * emits is actually resolved by the palette: a glyph with no palette entry
 * renders transparent, which would punch a hole through the character. Not part
 * of the game build.
 */
public final class DumpPlayers {

    private static final String[] IDS = { "phan_si_kien", "ronaldo", "do_mixi" };

    public static void main(String[] args) {
        boolean holes = false;
        for (String id : IDS) {
            SpriteSet set = Art.player(id);
            System.out.println("=== " + id + "  frames=" + set.count() + " ===");
            for (int f = 0; f < set.count(); f++) {
                PixelSprite sprite = set.frame(f);
                Humanoid.Spec spec = Art.playerSpec(id, f);
                String[] glyphs = Humanoid.render(spec);
                System.out.println("frame " + f + ": " + sprite.width() + "x" + sprite.height()
                        + "  scaleFor(44)=" + sprite.scaleFor(44)
                        + "  scaleFor(28)=" + sprite.scaleFor(28));
                print(glyphs);
                holes |= checkHoles(id, f, glyphs, sprite);
                holes |= checkMargin(id, f, glyphs);
            }
        }
        System.out.println(holes ? "GLYPH CHECK: FAIL (unmapped glyph punches a hole)" : "GLYPH CHECK: OK");
        if (holes) System.exit(1);
    }

    private static void print(String[] glyphs) {
        for (String row : glyphs) {
            System.out.println(row);
        }
    }

    /**
     * Nothing may touch the canvas edge: if it does, a limb has been clipped and
     * lost its outline. The border ring must be entirely empty.
     */
    private static boolean checkMargin(String id, int frame, String[] glyphs) {
        boolean bad = false;
        for (int y = 0; y < glyphs.length; y++) {
            for (int x = 0; x < glyphs[y].length(); x++) {
                boolean border = x == 0 || y == 0 || x == glyphs[y].length() - 1 || y == glyphs.length - 1;
                if (border && glyphs[y].charAt(x) != Humanoid.EMPTY) {
                    System.out.println("  CLIPPED " + id + " frame " + frame + " at " + x + "," + y
                            + " glyph='" + glyphs[y].charAt(x) + "'");
                    bad = true;
                }
            }
        }
        return bad;
    }

    /**
     * A glyph that the palette does not know renders fully transparent, so the
     * sprite and the template disagree on that pixel. Comparing them catches a
     * missing palette entry without reaching into the palette.
     */
    private static boolean checkHoles(String id, int frame, String[] glyphs, PixelSprite sprite) {
        BufferedImage img = new BufferedImage(sprite.width(), sprite.height(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        sprite.draw(g, 0, 0, 1, false, PixelSprite.Mode.NORMAL, 1f);
        g.dispose();

        boolean bad = false;
        for (int y = 0; y < glyphs.length; y++) {
            for (int x = 0; x < glyphs[y].length(); x++) {
                boolean glyphOpaque = glyphs[y].charAt(x) != Humanoid.EMPTY;
                boolean drawnOpaque = (img.getRGB(x, y) >>> 24) != 0;
                if (glyphOpaque != drawnOpaque) {
                    System.out.println("  MISMATCH " + id + " frame " + frame + " at " + x + "," + y
                            + " glyph='" + glyphs[y].charAt(x) + "' drawn=" + drawnOpaque);
                    bad = true;
                }
            }
        }
        return bad;
    }
}

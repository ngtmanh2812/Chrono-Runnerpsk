package chronorunner.gfx;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps the single characters used inside a sprite grid to colours. A key with
 * no entry is transparent, which is how '.', ' ' and any spare letter read.
 */
public final class Palette {

    private final Map<Character, Color> colors = new HashMap<>();

    public Palette put(char key, Color color) {
        colors.put(key, color);
        return this;
    }

    public Palette put(char key, int rgb) {
        return put(key, new Color(rgb));
    }

    /** Copies every entry of {@code other} that this palette does not already define. */
    public Palette fallback(Palette other) {
        for (Map.Entry<Character, Color> e : other.colors.entrySet()) {
            colors.putIfAbsent(e.getKey(), e.getValue());
        }
        return this;
    }

    Color colorOf(char key) {
        return colors.get(key);
    }
}

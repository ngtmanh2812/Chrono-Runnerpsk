package chronorunner.ui;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

import chronorunner.core.InputHandler;

/** Vertical menu with keyboard navigation, shared by all menu screens. */
public class Menu {

    private final String[] items;
    private int index;
    private boolean confirmed;
    private boolean cancelled;

    public Menu(String... items) {
        this.items = items;
    }

    public void update(InputHandler input) {
        confirmed = false;
        cancelled = false;
        int n = items.length;
        if (n == 0) return;
        if (input.wasPressed(KeyEvent.VK_UP) || input.wasPressed(KeyEvent.VK_W)) {
            index = (index - 1 + n) % n;
        }
        if (input.wasPressed(KeyEvent.VK_DOWN) || input.wasPressed(KeyEvent.VK_S)) {
            index = (index + 1) % n;
        }
        if (input.wasPressed(KeyEvent.VK_ENTER)) {
            confirmed = true;
        }
        if (input.wasPressed(KeyEvent.VK_ESCAPE)) {
            cancelled = true;
        }
    }

    public boolean confirmed() {
        return confirmed;
    }

    public boolean cancelled() {
        return cancelled;
    }

    public int index() {
        return index;
    }

    public void setIndex(int value) {
        index = Math.max(0, Math.min(items.length - 1, value));
    }

    public String selected() {
        return items[index];
    }

    /**
     * Draws the rows in the shared arcade voice: the current row is cream with
     * a gold triangle beside it, the rest recede to dim.
     */
    public void render(Graphics2D g, int centerX, int topY, int spacing) {
        int scale = UiKit.ARC_H2_SCALE;
        for (int i = 0; i < items.length; i++) {
            boolean selected = i == index;
            int baseline = topY + i * spacing;
            int left = centerX - UiKit.pixelWidth(items[i], scale, UiKit.ITEM_TRACKING) / 2;
            if (selected) {
                UiKit.arcadeArrow(g, left - UiKit.arrowWidth(scale) - 24,
                        baseline - 2 * scale, scale, UiKit.GOLD_TOP);
            }
            UiKit.pixelLeft(g, items[i], scale,
                    selected ? UiKit.CREAM : UiKit.ARC_DIM, left, baseline, UiKit.ITEM_TRACKING);
        }
    }
}

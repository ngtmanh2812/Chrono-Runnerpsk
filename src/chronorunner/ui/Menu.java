package chronorunner.ui;

import java.awt.Color;
import java.awt.FontMetrics;
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

    public void render(Graphics2D g, int centerX, int topY, int spacing) {
        g.setFont(UiKit.H2);
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i < items.length; i++) {
            String label = (i == index ? "> " : "  ") + items[i];
            int w = fm.stringWidth(label);
            int y = topY + i * spacing;
            if (i == index) {
                UiKit.panel(g, centerX - w / 2 - 26, y - 30, w + 52, 44,
                        new Color(60, 120, 200, 120), UiKit.ACCENT);
                g.setColor(Color.WHITE);
            } else {
                g.setColor(UiKit.MUTED);
            }
            g.drawString(label, centerX - w / 2, y);
        }
    }
}

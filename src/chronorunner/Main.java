package chronorunner;

import javax.swing.SwingUtilities;

import chronorunner.core.Game;
import chronorunner.gfx.Art;

/** Entry point. */
public class Main {

    public static void main(String[] args) {
        Art.validateAll();
        SwingUtilities.invokeLater(() -> new Game().start());
    }
}

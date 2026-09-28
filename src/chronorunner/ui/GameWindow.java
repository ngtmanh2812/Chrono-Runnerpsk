package chronorunner.ui;

import java.awt.BorderLayout;

import javax.swing.JFrame;

/** Top level window. */
public class GameWindow extends JFrame {

    public GameWindow(GamePanel panel) {
        super("Chrono Runner");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        panel.requestFocusInWindow();
    }
}

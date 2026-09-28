package chronorunner.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;

import chronorunner.core.Game;
import chronorunner.core.InputHandler;

/** Swing surface that runs the fixed 60 FPS game loop and paints the game. */
public class GamePanel extends JPanel implements Runnable {

    private static final long FRAME_NANOS = 1_000_000_000L / 60L;

    /**
     * The simulation always advances in this exact step. Attack windows are
     * specified in frames, so a constant step makes one update one frame and
     * keeps jump arcs and landing behaviour identical on every machine.
     */
    private static final double FIXED_DT = 1.0 / 60.0;

    /** Catch-up steps allowed per frame; beyond this the loop drops time instead of spiralling. */
    private static final int MAX_STEPS = 5;

    /**
     * Guards the arena's mutable entity lists. The loop thread mutates them in
     * {@code update} while the AWT event thread iterates them in
     * {@code paintComponent}; without this the paint can throw
     * ConcurrentModificationException or draw a half-updated frame.
     */
    private final Object frameLock = new Object();

    private final Game game;
    private final InputHandler input;
    private Thread thread;
    private volatile boolean running;

    public GamePanel(Game game, InputHandler input) {
        this.game = game;
        this.input = input;
        setPreferredSize(new Dimension(Game.WIDTH, Game.HEIGHT));
        setBackground(java.awt.Color.BLACK);
        setFocusable(true);
        addKeyListener(input);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
            }
        });
    }

    public void start() {
        if (thread != null) return;
        running = true;
        thread = new Thread(this, "chrono-runner-loop");
        thread.setDaemon(true);
        thread.start();
        requestFocusInWindow();
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        long last = System.nanoTime();
        double accumulator = 0;
        while (running) {
            long frameStart = System.nanoTime();
            double elapsed = (frameStart - last) / 1_000_000_000.0;
            last = frameStart;
            // Cap the backlog so a long stall (window drag, GC pause) cannot
            // queue up an unbounded burst of catch-up steps.
            accumulator += Math.min(elapsed, MAX_STEPS * FIXED_DT);

            input.beginFrame();
            int steps = 0;
            synchronized (frameLock) {
                while (accumulator >= FIXED_DT && steps < MAX_STEPS) {
                    game.update(FIXED_DT);
                    accumulator -= FIXED_DT;
                    steps++;
                    // Only the first step may see this frame's key edges,
                    // otherwise one jump press would be consumed twice.
                    if (steps == 1) input.endFrame();
                }
            }
            if (steps == MAX_STEPS) accumulator = 0;

            repaint();

            long sleep = FRAME_NANOS - (System.nanoTime() - frameStart);
            if (sleep > 0) {
                try {
                    Thread.sleep(sleep / 1_000_000L, (int) (sleep % 1_000_000L));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        synchronized (frameLock) {
            game.render(g2);
        }
        g2.dispose();
    }
}

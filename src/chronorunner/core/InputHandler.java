package chronorunner.core;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * Central keyboard state. Tracks keys that are held down plus keys that were
 * pressed during the current frame (edge detection for jump/attack/menus).
 *
 * <p>Key events arrive on the AWT event thread while the game loop reads them
 * from its own thread, so every access is guarded by {@link #lock}.
 *
 * <p>Edge state is split in two: {@link #pending} collects presses coming from
 * the event thread, and {@link #frame} is the snapshot the loop consumes. A
 * press is moved from one to the other by {@link #beginFrame} and survives until
 * some update actually reads it, so a key tapped during a long frame is seen on
 * the next one instead of being dropped.
 */
public class InputHandler extends KeyAdapter {

    private final Object lock = new Object();
    private final Set<Integer> down = new HashSet<>();
    private final Set<Integer> pending = new HashSet<>();
    private final Set<Integer> frame = new HashSet<>();

    @Override
    public void keyPressed(KeyEvent e) {
        synchronized (lock) {
            int code = e.getKeyCode();
            if (down.add(code)) {
                pending.add(code);
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        synchronized (lock) {
            down.remove(e.getKeyCode());
        }
    }

    public boolean isDown(int keyCode) {
        synchronized (lock) {
            return down.contains(keyCode);
        }
    }

    public boolean wasPressed(int keyCode) {
        synchronized (lock) {
            return frame.contains(keyCode);
        }
    }

    /** Publishes the presses collected since the last frame. Call once per frame, before update. */
    public void beginFrame() {
        synchronized (lock) {
            frame.addAll(pending);
            pending.clear();
        }
    }

    /**
     * Marks the current frame's presses as consumed. Keys held down stay in
     * {@link #down}, so a held key is still reported by {@link #isDown} but does
     * not fire {@link #wasPressed} again until it is released and pressed anew.
     */
    public void endFrame() {
        synchronized (lock) {
            frame.clear();
        }
    }

    /** Drops edge-triggered keys without forgetting which keys are held. */
    public void clearPressed() {
        synchronized (lock) {
            frame.clear();
            pending.clear();
        }
    }

    public void clearAll() {
        synchronized (lock) {
            down.clear();
            frame.clear();
            pending.clear();
        }
    }
}

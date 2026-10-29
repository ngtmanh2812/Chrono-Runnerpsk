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
    /** Separate gameplay binding; raw Enter remains an alias for menu input. */
    public static final int NUMPAD_ENTER = 0x10000 + KeyEvent.VK_ENTER;

    private final Object lock = new Object();
    private final Set<Integer> down = new HashSet<>();
    private final Set<Integer> pending = new HashSet<>();
    private final Set<Integer> frame = new HashSet<>();
    private boolean mainEnterDown, numpadEnterDown;
    private int pointerX = -1, pointerY = -1;
    private int framePointerX = -1, framePointerY = -1;
    private int pendingClickX, pendingClickY, frameClickX, frameClickY;
    private boolean pendingPointerMove, framePointerMove, pendingClick, frameClick;

    @Override
    public void keyPressed(KeyEvent e) {
        synchronized (lock) {
            int code = e.getKeyCode();
            if (code == KeyEvent.VK_ENTER) {
                if (e.getKeyLocation() == KeyEvent.KEY_LOCATION_NUMPAD) {
                    if (!numpadEnterDown) {
                        numpadEnterDown = true;
                        down.add(NUMPAD_ENTER); pending.add(NUMPAD_ENTER);
                    }
                } else mainEnterDown = true;
            }
            if (down.add(code)) {
                pending.add(code);
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        synchronized (lock) {
            int code = e.getKeyCode();
            if (code == KeyEvent.VK_ENTER) {
                if (e.getKeyLocation() == KeyEvent.KEY_LOCATION_NUMPAD) {
                    numpadEnterDown = false;
                    down.remove(NUMPAD_ENTER);
                } else mainEnterDown = false;
                if (!mainEnterDown && !numpadEnterDown) down.remove(KeyEvent.VK_ENTER);
            } else down.remove(code);
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

    /** Mouse events use the same pending/frame boundary as keyboard edges. */
    public void movePointer(int x, int y) {
        synchronized (lock) {
            if (pointerX != x || pointerY != y) pendingPointerMove = true;
            pointerX = x;
            pointerY = y;
        }
    }

    public void primaryPressed(int x, int y) {
        synchronized (lock) {
            movePointer(x, y);
            if (!pendingClick) {
                pendingClickX = x;
                pendingClickY = y;
            }
            pendingClick = true;
        }
    }

    public boolean wasPointerMoved() { synchronized (lock) { return framePointerMove; } }
    public int pointerX() { synchronized (lock) { return framePointerX; } }
    public int pointerY() { synchronized (lock) { return framePointerY; } }
    public boolean wasPrimaryClicked() { synchronized (lock) { return frameClick; } }
    public int clickX() { synchronized (lock) { return frameClickX; } }
    public int clickY() { synchronized (lock) { return frameClickY; } }

    /** Publishes the presses collected since the last frame. Call once per frame, before update. */
    public void beginFrame() {
        synchronized (lock) {
            frame.addAll(pending);
            pending.clear();
            framePointerX = pointerX;
            framePointerY = pointerY;
            framePointerMove |= pendingPointerMove;
            pendingPointerMove = false;
            if (!frameClick && pendingClick) {
                frameClickX = pendingClickX;
                frameClickY = pendingClickY;
            }
            frameClick |= pendingClick;
            pendingClick = false;
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
            framePointerMove = false;
            frameClick = false;
        }
    }

    /** Drops edge-triggered keys without forgetting which keys are held. */
    public void clearPressed() {
        synchronized (lock) {
            frame.clear();
            pending.clear();
            pendingPointerMove = framePointerMove = pendingClick = frameClick = false;
        }
    }

    public void clearAll() {
        synchronized (lock) {
            down.clear();
            mainEnterDown = numpadEnterDown = false;
            frame.clear();
            pending.clear();
            pendingPointerMove = framePointerMove = pendingClick = frameClick = false;
            pointerX = pointerY = framePointerX = framePointerY = -1;
        }
    }
}

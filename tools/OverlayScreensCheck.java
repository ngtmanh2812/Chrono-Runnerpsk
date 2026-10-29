import java.awt.Canvas;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.imageio.ImageIO;

import chronorunner.combat.DamageInfo;
import chronorunner.core.*;
import chronorunner.entity.Player;
import chronorunner.ui.GamePanel;
import chronorunner.ui.Menu;
import chronorunner.ui.PlayScreen;
import chronorunner.world.Level;

/** Runs the real screen transitions, input adapters and renderer without changing saves. */
public final class OverlayScreensCheck {
    static final double DT = 1.0 / 60;
    static final Canvas KEYS = new Canvas();
    static final File OUTPUT = new File("out/end-screen-integration");
    static int checks;

    static class NoSaveGame extends Game {
        int saves;
        @Override public void saveProgress() { saves++; }
    }

    static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
    static Object get(Object target, String name) throws Exception {
        return field(target, name).get(target);
    }
    static Field field(Object target, String name) throws Exception {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try { Field f = type.getDeclaredField(name); f.setAccessible(true); return f; }
            catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name);
    }
    static long hash(BufferedImage image) {
        long hash = 0xcbf29ce484222325L;
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++)
            hash = (hash ^ image.getRGB(x, y)) * 0x100000001b3L;
        return hash;
    }
    static void key(InputHandler input, int code, boolean down) {
        KeyEvent event = new KeyEvent(KEYS, down ? KeyEvent.KEY_PRESSED : KeyEvent.KEY_RELEASED,
                0, 0, code, KeyEvent.CHAR_UNDEFINED);
        if (down) input.keyPressed(event); else input.keyReleased(event);
    }

    static final class Fixture {
        final NoSaveGame game = new NoSaveGame();
        final InputHandler input = game.getInput();
        final PlayScreen play;
        final int chapter, count;
        Fixture(int chapter, int count) throws Exception {
            this.chapter = chapter; this.count = count;
            game.getSession().chapter = chapter; game.getSession().playerCount = count;
            game.getSession().characterIndex[0] = 0; game.getSession().characterIndex[1] = 1;
            game.getProgress().setScreenShake(false);
            Method register = Game.class.getDeclaredMethod("registerScreens"); register.setAccessible(true); register.invoke(game);
            game.setState(GameState.PLAYING); play = (PlayScreen) game.screen(GameState.PLAYING);
        }
        Level level() throws Exception { return (Level) get(play, "level"); }
        Player p() { return play.players().get(0); }
        void step() { input.beginFrame(); game.update(DT); input.endFrame(); }
        void press(int key) { key(input, key, true); step(); key(input, key, false); }
        void move(int x, int y) { input.movePointer(x, y); step(); }
        void click(int x, int y) { input.primaryPressed(x, y); step(); }
        void settle(GameState expected) {
            for (int i = 0; i < 140 && game.getState() == GameState.PLAYING; i++) step();
            check(game.getState() == expected, "Failed transition to " + expected);
        }
        Menu menu() throws Exception { return (Menu) get(game.screen(game.getState()), "menu"); }
        BufferedImage render() {
            BufferedImage image = new BufferedImage(Game.WIDTH, Game.HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics(); game.render(g); g.dispose(); return image;
        }
        void snapshot(String name) throws Exception {
            ImageIO.write(render(), "png", new File(OUTPUT, "chapter" + chapter + "-players" + count + "-" + name + ".png"));
        }
        void lost() throws Exception {
            if (count == 2) {
                p().takeDamage(new DamageInfo(100000, 0, 0, false)); step();
                check(game.getState() == GameState.PLAYING, "Co-op ended while P2 was alive");
            }
            for (Player p : play.players()) p.takeDamage(new DamageInfo(100000, 0, 0, false));
            settle(GameState.GAME_OVER);
        }
        void won() throws Exception {
            int before = game.saves;
            level().boss().takeDamage(new DamageInfo(100000, 0, 0, true));
            settle(GameState.VICTORY);
            check(game.saves == before + 1, "Victory must still request one progress save");
            check(game.getProgress().getUnlockedChapters() == 2, "Chapter unlock regression");
        }
    }

    static void frozenAndNeutral(Fixture f, BufferedImage expected) throws Exception {
        Object overlay = f.game.screen(f.game.getState());
        BufferedImage captured = (BufferedImage) get(overlay, "gameplayFrame");
        check(captured != null && hash(captured) == hash(expected), "Background is not the exact current arena/HUD frame");
        long before = hash(f.render());
        double x = f.p().x, cooldown = f.p().getBlockCooldownRemaining();
        for (int i = 0; i < 90; i++) f.step();
        check(hash(f.render()) == before, "Overlay background or text moved while frozen");
        check(f.p().x == x && f.p().getBlockCooldownRemaining() == cooldown, "Overlay advanced player simulation");
        BufferedImage rendered = f.render();
        for (int[] point : new int[][] {{12,110}, {1200,550}, {100,400}}) {
            int source = captured.getRGB(point[0], point[1]), actual = rendered.getRGB(point[0], point[1]);
            for (int shift : new int[] {0,8,16}) {
                int wanted = (int) Math.round(((source >> shift) & 255) * .4);
                check(Math.abs(((actual >> shift) & 255) - wanted) <= 2, "Coloured wash or duplicate dimming on backdrop");
            }
        }
    }

    static void pause(int chapter, int count) throws Exception {
        Fixture f = new Fixture(chapter, count); f.press(KeyEvent.VK_S);
        BufferedImage before = f.play.captureGameplayFrame();
        Level original = f.level(); Player player = f.p();
        f.press(KeyEvent.VK_ESCAPE); check(f.game.getState() == GameState.PAUSED, "Esc did not pause");
        check(f.menu().size() == 3 && f.menu().index() == 0, "Pause actions/default selection wrong");
        frozenAndNeutral(f, before); f.snapshot("pause");
        f.move(640,335); f.press(KeyEvent.VK_S);
        check(f.menu().index() == 1, "Keyboard cannot override hover");
        f.step(); check(f.menu().index() == 1, "Stationary pointer undid keyboard selection");
        f.press(KeyEvent.VK_W); check(f.menu().index() == 0, "W navigation failed");
        f.press(KeyEvent.VK_UP); check(f.menu().index() == 2, "Up wrap failed");
        f.press(KeyEvent.VK_DOWN); check(f.menu().index() == 0, "Down wrap failed");
        f.click(640,377); check(f.game.getState() == GameState.PAUSED, "Gap between buttons accepted a click");
        f.click(10,10); check(f.game.getState() == GameState.PAUSED, "Outside click activated a button");
        f.press(KeyEvent.VK_ESCAPE);
        check(f.game.getState() == GameState.PLAYING && f.level() == original && f.p() == player, "Esc resume rebuilt arena/player");

        f.p().x += 25; f.p().update(0, f.play); before = f.play.captureGameplayFrame();
        f.press(KeyEvent.VK_ESCAPE); frozenAndNeutral(f, before);
        f.press(KeyEvent.VK_ENTER); check(f.game.getState() == GameState.PLAYING && f.p() == player, "Enter resume failed");
        f.press(KeyEvent.VK_ESCAPE); f.click(640,420);
        check(f.game.getState() == GameState.PLAYING && f.level() != original && f.p() != player, "Pause restart did not rebuild level");
        check(f.p().getHealth() == f.p().getMaxHealth() && f.p().getBlockCooldownRemaining() == 0, "Restart retained old HP/guard");
        check(get(f.game.screen(GameState.PAUSED), "gameplayFrame") == null, "Snapshot retained after leaving pause");
        f.press(KeyEvent.VK_ESCAPE);
        check(f.menu().index() == 0, "Reopening pause retained stale selection");
        f.click(640,505); check(f.game.getState() == GameState.MAIN_MENU, "Pause menu action failed");
    }

    static void endings(int chapter, int count) throws Exception {
        Fixture loss = new Fixture(chapter, count); loss.lost();
        check(loss.menu().size() == 2 && loss.menu().index() == 0, "Game Over menu wrong");
        frozenAndNeutral(loss, loss.play.captureGameplayFrame()); loss.snapshot("game-over");
        Level old = loss.level(); loss.press(KeyEvent.VK_ENTER);
        check(loss.game.getState() == GameState.PLAYING && loss.level() != old, "Game Over retry failed");
        loss.lost(); loss.click(640,411);
        check(loss.game.getState() == GameState.PLAYING, "Game Over mouse retry failed");
        loss.lost(); loss.press(KeyEvent.VK_DOWN); loss.press(KeyEvent.VK_ENTER);
        check(loss.game.getState() == GameState.MAIN_MENU, "Game Over keyboard menu failed");
        loss.game.setState(GameState.PLAYING); loss.lost(); loss.press(KeyEvent.VK_ESCAPE);
        check(loss.game.getState() == GameState.MAIN_MENU, "Game Over Esc failed");

        Fixture win = new Fixture(chapter, count); win.won();
        check(win.menu().size() == (chapter == 1 ? 2 : 1), "Final-chapter actions wrong");
        frozenAndNeutral(win, win.play.captureGameplayFrame()); win.snapshot("victory");
        win.press(KeyEvent.VK_ENTER);
        check(win.game.getState() == (chapter == 1 ? GameState.LEVEL_SELECT : GameState.MAIN_MENU), "Victory Enter action failed");
        check(win.game.getSession().chapter == 2, "Invalid next chapter");
        check(get(win.game.screen(GameState.VICTORY), "gameplayFrame") == null, "Victory snapshot leaked");
        win.game.getSession().chapter = chapter; win.game.setState(GameState.PLAYING); win.won();
        win.click(640, chapter == 1 ? 411 : 445);
        check(win.game.getState() == (chapter == 1 ? GameState.LEVEL_SELECT : GameState.MAIN_MENU), "Victory mouse action failed");
        win.game.getSession().chapter = chapter; win.game.setState(GameState.PLAYING); win.won();
        win.press(KeyEvent.VK_ESCAPE); check(win.game.getState() == GameState.MAIN_MENU, "Victory Esc failed");
        if (chapter == 1) {
            win.game.getSession().chapter = chapter; win.game.setState(GameState.PLAYING); win.won();
            win.click(640,496);
            check(win.game.getState() == GameState.MAIN_MENU, "Victory secondary mouse action failed");
        }
    }

    static void mouseAdapterAndEdges() throws Exception {
        Fixture f = new Fixture(1,1); f.press(KeyEvent.VK_ESCAPE);
        GamePanel panel = new GamePanel(f.game, f.input);
        MouseEvent click = new MouseEvent(panel,MouseEvent.MOUSE_PRESSED,0,0,640,335,1,false,MouseEvent.BUTTON1);
        for (java.awt.event.MouseListener listener : panel.getMouseListeners()) listener.mousePressed(click);
        f.input.movePointer(640,505); f.input.beginFrame();
        // An outer render frame with no fixed update must retain its click edge.
        f.input.beginFrame(); f.game.update(DT); f.input.endFrame();
        check(f.game.getState() == GameState.PLAYING, "Mouse adapter or retained/latched click failed");
        check(!f.input.wasPrimaryClicked(), "Click leaked through screen transition");
        f.press(KeyEvent.VK_ESCAPE); check(f.menu().index() == 0, "Old mouse click activated reopened screen");
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("chronorunner.debugCollision","false"); OUTPUT.mkdirs();
        for (int chapter=1; chapter<=2; chapter++) for (int count=1; count<=2; count++) {
            pause(chapter,count); endings(chapter,count);
            System.out.println("Chapter " + chapter + ", " + count + " player(s): overlays/input/transitions PASS");
        }
        mouseAdapterAndEdges();
        check(!Boolean.getBoolean("chronorunner.debugCollision"), "Debug left on");
        System.out.println("PASS: " + checks + " overlay checks; save writes intercepted; renderer snapshots in " + OUTPUT);
    }
}

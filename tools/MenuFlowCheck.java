import java.awt.Canvas;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import chronorunner.core.*;

/** Real menu input and screen transitions; no window and no save writes. */
public final class MenuFlowCheck {
    static final Canvas KEYS = new Canvas();
    static int checks;
    static class NoSave extends Game { int saves; @Override public void saveProgress() { saves++; } }
    static void check(boolean ok, String why) { checks++; if (!ok) throw new AssertionError(why); }
    static Object field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object);
    }
    static void press(Game game, int... keys) {
        for (int key : keys) game.getInput().keyPressed(new KeyEvent(KEYS, KeyEvent.KEY_PRESSED, 0, 0, key, KeyEvent.CHAR_UNDEFINED));
        game.getInput().beginFrame(); game.update(1.0 / 60); game.getInput().endFrame();
        for (int key : keys) game.getInput().keyReleased(new KeyEvent(KEYS, KeyEvent.KEY_RELEASED, 0, 0, key, KeyEvent.CHAR_UNDEFINED));
    }
    static NoSave game() throws Exception {
        NoSave game = new NoSave();
        var register = Game.class.getDeclaredMethod("registerScreens"); register.setAccessible(true); register.invoke(game);
        game.setState(GameState.MAIN_MENU); return game;
    }
    static void capture(Game game, String name) throws Exception {
        var image = new BufferedImage(Game.WIDTH, Game.HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics(); game.render(g); g.dispose();
        ImageIO.write(image, "png", Path.of("out/menu-flow-check", name + ".png").toFile());
    }
    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("out/menu-flow-check"));
        for (int count : new int[]{1, 2}) {
            NoSave game = game();
            String[] options = (String[])field(game.screen(GameState.MAIN_MENU), "OPTIONS");
            check(options.length == 5 && options[3].equals("HƯỚNG DẪN CHƠI"), "Old menu entries remain");
            if (count == 2) press(game, KeyEvent.VK_RIGHT);
            capture(game, "menu-" + count + "p");
            press(game, KeyEvent.VK_ENTER);
            check(game.getState() == GameState.CHARACTER_SELECT, "Play skipped character selection");
            check(game.getSession().playerCount == count, "Player count lost");
            press(game, KeyEvent.VK_ENTER);
            check(game.getState() == GameState.CHARACTER_SELECT, "Unconfirmed roster advanced");
            press(game, KeyEvent.VK_D); press(game, KeyEvent.VK_D); press(game, KeyEvent.VK_J);
            if (count == 2) {
                press(game, KeyEvent.VK_ENTER);
                check(game.getState() == GameState.CHARACTER_SELECT, "P2 confirmation was not required");
                press(game, KeyEvent.VK_LEFT); press(game, KeyEvent.VK_NUMPAD1);
            }
            press(game, KeyEvent.VK_ENTER);
            check(game.getState() == GameState.LEVEL_SELECT, "Ready roster did not open chapters");
            check(game.getSession().characterIndex[0] == 2, "P1 selection lost");
            if (count == 2) check(game.getSession().characterIndex[1] == 1, "P2 selection lost");
            press(game, KeyEvent.VK_ENTER);
            check(game.getState() == GameState.PLAYING, "Chapter did not start");
            check(game.saves == 0, "Flow wrote progress");
        }
        NoSave game = game();
        press(game, KeyEvent.VK_DOWN); press(game, KeyEvent.VK_DOWN); press(game, KeyEvent.VK_DOWN); press(game, KeyEvent.VK_ENTER);
        check(game.getState() == GameState.HOW_TO_PLAY, "Guide menu does not open");
        for (int i = 0; i < 3; i++) {
            check((int)field(game.screen(GameState.HOW_TO_PLAY), "page") == i, "Guide page mismatch");
            capture(game, "guide-" + i); press(game, KeyEvent.VK_RIGHT);
        }
        check((int)field(game.screen(GameState.HOW_TO_PLAY), "page") == 0, "Guide forward wrap failed");
        press(game, KeyEvent.VK_LEFT);
        check((int)field(game.screen(GameState.HOW_TO_PLAY), "page") == 2, "Guide backward wrap failed");
        press(game, KeyEvent.VK_ESCAPE); check(game.getState() == GameState.MAIN_MENU, "Guide Esc failed");
        press(game, KeyEvent.VK_ENTER);
        check((int)field(game.screen(GameState.HOW_TO_PLAY), "page") == 0, "Guide reopen retained page");
        press(game, KeyEvent.VK_ENTER); check(game.getState() == GameState.MAIN_MENU, "Guide Enter failed");
        press(game, KeyEvent.VK_UP); press(game, KeyEvent.VK_ENTER);
        check(game.getState() == GameState.SETTINGS, "Settings mapping broken");
        press(game, KeyEvent.VK_ESCAPE); press(game, KeyEvent.VK_UP); press(game, KeyEvent.VK_ENTER);
        check(game.getState() == GameState.LEVEL_SELECT, "Continue mapping broken");
        check(game.saves == 0, "Guide/settings navigation wrote progress");
        System.out.println("PASS: " + checks + " menu/guide checks; 1P/2P Play > Character > Chapter > Gameplay; no save writes.");
    }
}

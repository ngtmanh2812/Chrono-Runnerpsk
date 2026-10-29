package chronorunner.ui;

import chronorunner.core.Game;
import chronorunner.core.GameState;
import chronorunner.util.Progress;
import chronorunner.world.TileType;

/** Shown after the boss is defeated. Offers the next chapter. */
public class VictoryScreen extends GameplayOverlayScreen {

    private int chapter = 1;

    public VictoryScreen(Game game) {
        super(game, Theme.VICTORY, "VỀ MENU CHÍNH");
    }

    @Override
    public void onEnter() {
        chapter = game.getSession().chapter;
        if (chapter >= Progress.TOTAL_CHAPTERS) {
            menu = new Menu("VỀ MENU CHÍNH");
        } else {
            menu = new Menu("CHƯƠNG TIẾP THEO", "VỀ MENU CHÍNH");
        }
        super.onEnter();
    }

    @Override
    public void update(double dt) {
        updateMenu();
        if (menu.cancelled()) {
            game.setState(GameState.MAIN_MENU);
            return;
        }
        if (!menuConfirmed()) return;
        if (chapter < Progress.TOTAL_CHAPTERS && menu.index() == 0) {
            game.getSession().chapter = chapter + 1;
            game.setState(GameState.LEVEL_SELECT);
        } else {
            game.setState(GameState.MAIN_MENU);
        }
    }

    @Override
    protected String description() { return TileType.of(chapter).bossName + " đã bị đánh bại."; }

    @Override
    protected String subtitle() {
        return chapter >= Progress.TOTAL_CHAPTERS
                ? "Dòng thời gian đã được khôi phục." : "Một kỷ nguyên mới đang chờ bạn.";
    }

    @Override
    protected String footnote() {
        if (chapter >= Progress.TOTAL_CHAPTERS) return "CẢM ƠN BẠN ĐÃ CHƠI CHRONO RUNNER!";
        return "ĐÃ MỞ KHÓA: CHƯƠNG " + (chapter + 1) + " / "
                + TileType.of(chapter + 1).placeName.toUpperCase(java.util.Locale.ROOT);
    }
}

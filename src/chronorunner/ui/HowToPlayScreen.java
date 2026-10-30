package chronorunner.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import chronorunner.core.Game;
import chronorunner.core.GameState;
import chronorunner.core.InputHandler;
import chronorunner.core.Screen;

/** Read-only guide for the controls and combat rules actually installed in game. */
public final class HowToPlayScreen extends Screen {
    private static final String[] PAGES = {"PHÍM ĐIỀU KHIỂN", "COMBO / KỸ NĂNG", "MỤC TIÊU"};
    private static final Color PANEL = new Color(3, 15, 34, 240);
    private int page;

    public HowToPlayScreen(Game game) { super(game); }

    @Override public void onEnter() { page = 0; }

    @Override public void update(double dt) {
        InputHandler input = game.getInput();
        if (input.wasPressed(KeyEvent.VK_ESCAPE) || input.wasPressed(KeyEvent.VK_ENTER)) {
            game.setState(GameState.MAIN_MENU);
            return;
        }
        if (input.wasPressed(KeyEvent.VK_LEFT) || input.wasPressed(KeyEvent.VK_A))
            page = (page + PAGES.length - 1) % PAGES.length;
        else if (input.wasPressed(KeyEvent.VK_RIGHT) || input.wasPressed(KeyEvent.VK_D)
                || input.wasPressed(KeyEvent.VK_TAB)) page = (page + 1) % PAGES.length;
    }

    @Override public void render(Graphics2D g) {
        UiKit.background(g, new Color(2, 10, 26), new Color(8, 25, 50));
        UiKit.arcade(g, "CHRONO RUNNER", 2, UiKit.ACCENT, 640, 60, 3);
        UiKit.arcade(g, "HƯỚNG DẪN CHƠI", 5, UiKit.CREAM, 640, 125, 4);
        for (int i = 0; i < PAGES.length; i++) {
            int x = 70 + i * 390;
            UiKit.panel(g, x, 161, 360, 46, i == page ? new Color(10, 53, 78) : PANEL,
                    i == page ? UiKit.ACCENT : UiKit.MUTED);
            UiKit.arcade(g, PAGES[i], 2, i == page ? UiKit.CREAM : UiKit.MUTED, x + 180, 191, 1);
        }
        if (page == 0) controls(g);
        else if (page == 1) combo(g);
        else objectives(g);
        UiKit.arcade(g, "A/D HOẶC TRÁI/PHẢI: ĐỔI TRANG    ESC / ENTER: QUAY LẠI", 2,
                UiKit.ACCENT, 640, 681, 0);
    }

    private void line(Graphics2D g, String text, int x, int y) {
        UiKit.arcadeLeft(g, text, 2, UiKit.TEXT, x, y, 0);
    }

    private void controls(Graphics2D g) {
        UiKit.panel(g, 70, 228, 1140, 377, PANEL, UiKit.ACCENT);
        line(g, "HÀNH ĐỘNG", 100, 262);
        line(g, "NGƯỜI CHƠI 1", 540, 262);
        line(g, "NGƯỜI CHƠI 2", 865, 262);
        String[][] rows = {
                {"Di chuyển", "A / D", "Trái / Phải"},
                {"Nhảy / nhảy đôi", "W", "Numpad 0"},
                {"Dash / né đòn", "K", "Numpad 3"},
                {"Chuỗi ba đòn", "J > J > J", "Numpad 1 > 1 > 1"},
                {"Đỡ một hit", "S", "Mũi tên xuống"},
                {"Nạp năng lượng", "Giữ L", "Giữ Numpad Enter"},
                {"Ba kỹ năng riêng", "U / I / O", "Numpad 4 / 5 / 6"},
                {"Tạm dừng", "Esc", "Esc"}
        };
        for (int i = 0; i < rows.length; i++) {
            int y = 307 + i * 35;
            line(g, rows[i][0], 100, y); line(g, rows[i][1], 540, y); line(g, rows[i][2], 865, y);
        }
        line(g, "Ở menu: chọn CHƠI, dùng trái/phải để chọn 1P hoặc 2P rồi nhấn Enter.", 85, 641);
    }

    private void combo(Graphics2D g) {
        String[] names = {"J: ĐÒN THƯỜNG", "J: HEAVY", "J: LAUNCHER"};
        String[] details = {"30 DAMAGE / +5 EN", "40 DAMAGE / +7 EN", "35 DAMAGE / +10 EN"};
        for (int i = 0; i < names.length; i++) {
            int x = 70 + i * 390;
            UiKit.panel(g, x, 240, 360, 124, PANEL, UiKit.ACCENT);
            UiKit.arcade(g, names[i], 3, UiKit.CREAM, x + 180, 286, 0);
            UiKit.arcade(g, details[i], 2, UiKit.GOLD_PALE, x + 180, 334, 0);
        }
        String[] lines = {
                "Nhấn J riêng: thường > Heavy > Launcher. Chỉ trúng mới nhận EN.",
                "Trúng: nối sớm ở phần thu đòn. Hết động tác: có 0,60 giây để nối.",
                "Giữ J không tự nối. P2 dùng Numpad 1 theo cùng cách.",
                "Dash, block, dùng skill hoặc bị đánh sẽ đặt lại chuỗi.",
                "U: 20 EN    I: 30 EN    O: 100 EN. Mỗi nhân vật có bộ chiêu riêng.",
                "Giữ L khi đứng yên trên đất để nạp; P2 giữ Numpad Enter.",
                "Kỹ năng cần đủ năng lượng và hết hồi chiêu: xem các ô trên HUD."
        };
        for (int i = 0; i < lines.length; i++) line(g, lines[i], 85, 411 + i * 33);
    }

    private void objectives(Graphics2D g) {
        String[] lines = {
                "CHƠI > CHỌN NHÂN VẬT > CHỌN CHAPTER > VÀO ĐẤU TRƯỜNG",
                "P1: A/D chọn, J xác nhận, K chọn lại. P2: trái/phải, N1, N2.",
                "Khi mọi người đã xác nhận, nhấn Enter để sang chọn chapter.",
                "CHAPTER 1 - QUÁ KHỨ: đánh bại The Time Guardian.",
                "Thắng Chapter 1 để mở CHAPTER 2 - TƯƠNG LAI: The Void Core.",
                "Làm cạn thanh máu boss để chiến thắng. Né đòn và tận dụng bệ đá.",
                "Khi boss hiện KHÁNG CHOÁNG: chuẩn bị né hoặc đỡ đòn phản công.",
                "Block đỡ một hit trong 0,5 giây; hồi 2 giây. Dash dùng để né.",
                "Hết máu sẽ bị hạ gục; trận thua khi tất cả người chơi đều bị hạ.",
                "Trong trận nhấn Esc để tạm dừng, tiếp tục hoặc về menu."
        };
        UiKit.panel(g, 70, 235, 1140, 418, PANEL, UiKit.ACCENT);
        for (int i = 0; i < lines.length; i++) line(g, lines[i], 90, 277 + i * 39);
    }
}

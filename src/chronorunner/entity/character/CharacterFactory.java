package chronorunner.entity.character;

import java.awt.Color;

import chronorunner.core.InputHandler;
import chronorunner.entity.Player;

/** Builds a playable character from a selection index. */
public final class CharacterFactory {

    public static final String[] NAMES = {"Phan Sĩ Kiên", "Ronaldo", "Độ Mixi"};

    private CharacterFactory() {
    }

    public static Player create(int index, int playerIndex, InputHandler input, double x, double y) {
        switch (index) {
            case 1:
                return new Ronaldo(playerIndex, input, x, y);
            case 2:
                return new DoMixi(playerIndex, input, x, y);
            default:
                return new PhanSiKien(playerIndex, input, x, y);
        }
    }

    public static String nameOf(int index) {
        return NAMES[Math.max(0, Math.min(NAMES.length - 1, index))];
    }

    public static String skinOf(int index) {
        switch (index) {
            case 1:
                return "Cầu thủ bóng đá\nTốc độ, sút mạnh";
            case 2:
                return "Streamer sóng âm\nChậm, HP cao";
            default:
                return "Kiếm sĩ thời gian\nCân bằng, đánh xa";
        }
    }

    public static String skillOf(int index) {
        switch (index) {
            case 1:
                return "Siuu Bicycle Kick";
            case 2:
                return "Đại Hội Tộc";
            default:
                return "Timeline Break";
        }
    }

    public static String[] skillsOf(int index) {
        switch (index) {
            case 1: return new String[]{"U Power Shot", "I Step-over Burst", "O Siuu Bicycle Kick"};
            case 2: return new String[]{"U Sóng Âm", "I Đập Bàn", "O Đại Hội Tộc"};
            default: return new String[]{"U Chrono Shot", "I Time Slash", "O Timeline Break"};
        }
    }

    /** Display-only stat bars: {HP, Damage, Speed} each out of 10. */
    public static int[] statsOf(int index) {
        switch (index) {
            case 1:
                return new int[]{4, 6, 10};
            case 2:
                return new int[]{10, 6, 3};
            default:
                return new int[]{6, 6, 6};
        }
    }

    /** Sprite id for a selection index; matches {@link Player#characterId()}. */
    public static String idOf(int index) {
        switch (index) {
            case 1:
                return "ronaldo";
            case 2:
                return "do_mixi";
            default:
                return "phan_si_kien";
        }
    }

    public static Color colorOf(int index) {
        switch (index) {
            case 1:
                return new Color(60, 220, 140);
            case 2:
                return new Color(235, 110, 60);
            default:
                return new Color(60, 150, 255);
        }
    }
}

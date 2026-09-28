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
                return "Tốc độ - HP thấp, dash nhanh";
            case 2:
                return "Tank - HP cao, damage cao, chậm";
            default:
                return "Cân bằng - dash + đánh xa";
        }
    }

    public static String skillOf(int index) {
        switch (index) {
            case 1:
                return "Siuu (burst lao tới)";
            case 2:
                return "Alo Vũ à Vũ (chấn động AOE)";
            default:
                return "Chrono Shot (đạn xa)";
        }
    }

    /** Display-only stat bars: {HP, Damage, Speed} each out of 10. */
    public static int[] statsOf(int index) {
        switch (index) {
            case 1:
                return new int[]{4, 5, 10};
            case 2:
                return new int[]{10, 9, 3};
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

package chronorunner.world;

import java.awt.Color;

/** Per-chapter theme: names, story text and palette. */
public enum TileType {

    PAST(1, "Quá Khứ", "The Time Guardian",
            "Trở về quá khứ để tìm hiểu nguyên nhân của sự kiện đã làm thay đổi dòng thời gian.\n\n"
                    + "Giữa những tàn tích và cỗ máy đồng hồ đổ vỡ, Phan Sĩ Kiên phải mở đường đến "
                    + "The Time Guardian (Vệ Thần Thời Gian).",
            new Color(72, 25, 26), new Color(24, 22, 38), new Color(115, 73, 57), new Color(255, 157, 83)),

    FUTURE(2, "Tương Lai", "The Void Core",
            "Đối mặt với những gì sẽ xảy ra để thay đổi tương lai.\n\n"
                    + "Trong thành phố neon bị máy móc kiểm soát, Phan Sĩ Kiên phải vượt qua hệ thống "
                    + "phòng thủ và đánh bại The Void Core.",
            new Color(8, 28, 72), new Color(23, 13, 58), new Color(46, 74, 132), new Color(47, 206, 255));

    public final int chapter;
    public final String placeName;
    public final String bossName;
    public final String story;
    public final Color bgTop;
    public final Color bgBottom;
    public final Color platformColor;
    public final Color accent;

    TileType(int chapter, String placeName, String bossName, String story,
             Color bgTop, Color bgBottom, Color platformColor, Color accent) {
        this.chapter = chapter;
        this.placeName = placeName;
        this.bossName = bossName;
        this.story = story;
        this.bgTop = bgTop;
        this.bgBottom = bgBottom;
        this.platformColor = platformColor;
        this.accent = accent;
    }

    public static TileType of(int chapter) {
        for (TileType t : values()) {
            if (t.chapter == chapter) return t;
        }
        return PAST;
    }
}

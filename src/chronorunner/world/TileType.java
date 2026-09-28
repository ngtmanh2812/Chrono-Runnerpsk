package chronorunner.world;

import java.awt.Color;

/** Per-chapter theme: names, story text and palette. */
public enum TileType {

    BROKEN_CITY(1, "Broken City", "Time Guardian",
            "Phan Sĩ Kiên tỉnh dậy giữa một thành phố xa lạ đang bị phá hủy bởi sự bất ổn của dòng thời gian.\n\n"
                    + "Không biết mình đang ở đâu, anh nhận ra con đường duy nhất để trở về là tìm lại "
                    + "sức mạnh của Chrono Core.\n\n"
                    + "Nhưng trước khi tiến xa hơn, anh phải đối mặt với Time Guardian — kẻ đang bảo vệ khu vực này.",
            new Color(24, 26, 42), new Color(58, 62, 92), new Color(96, 104, 150), new Color(120, 190, 255)),

    FORGOTTEN_FOREST(2, "Forgotten Forest", "Chrono Beast",
            "Sau khi rời khỏi thành phố, Phan Sĩ Kiên tiến vào một khu rừng đã bị dòng thời gian nuốt chửng.\n\n"
                    + "Những gì từng tồn tại ở đây đã biến đổi hoàn toàn. Một sinh vật khổng lồ được tạo ra từ "
                    + "năng lượng thời gian đang kiểm soát khu rừng.\n\n"
                    + "Phan Sĩ Kiên phải đánh bại Chrono Beast để tiếp tục hành trình.",
            new Color(12, 32, 22), new Color(30, 74, 48), new Color(64, 120, 76), new Color(140, 240, 160)),

    ANCIENT_RUINS(3, "Ancient Ruins", "Temporal Knight",
            "Phan Sĩ Kiên đến những tàn tích cổ đại, nơi lưu giữ một phần sức mạnh của Chrono Core.\n\n"
                    + "Tại đây, một chiến binh cổ đại đã bị mắc kẹt giữa các thời đại và trở thành người bảo vệ tàn tích.\n\n"
                    + "Để tiến vào sâu hơn, Phan Sĩ Kiên phải đối đầu với Temporal Knight.",
            new Color(34, 28, 20), new Color(96, 78, 48), new Color(150, 126, 84), new Color(255, 210, 120)),

    FROZEN_FUTURE(4, "Frozen Future", "Frost Paradox",
            "Phan Sĩ Kiên bước vào một tương lai đã bị đóng băng.\n\n"
                    + "Mọi thứ xung quanh gần như ngừng chuyển động, trong khi một thực thể bí ẩn đang "
                    + "hấp thụ năng lượng của Chrono Core.\n\n"
                    + "Đó là Frost Paradox. Anh phải đánh bại nó để lấy lại phần năng lượng cuối cùng.",
            new Color(18, 30, 52), new Color(70, 116, 168), new Color(120, 176, 220), new Color(190, 240, 255)),

    CHRONO_CORE(5, "Chrono Core", "The Paradox",
            "Cuối cùng, Phan Sĩ Kiên đã đến trung tâm của sự cố.\n\n"
                    + "Chrono Core nằm trước mắt, nhưng dòng thời gian đã hoàn toàn mất kiểm soát.\n\n"
                    + "Thực thể đứng sau tất cả xuất hiện — The Paradox. Để trở về thời đại của mình, "
                    + "anh phải đánh bại nó và khôi phục Chrono Core.",
            new Color(28, 16, 42), new Color(92, 40, 120), new Color(140, 80, 180), new Color(240, 150, 255));

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
        return BROKEN_CITY;
    }
}

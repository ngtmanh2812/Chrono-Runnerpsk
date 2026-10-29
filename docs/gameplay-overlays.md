# Pause, Game Over và Victory trong game — 27/10/2026

Ba mẫu đã được triển khai bằng Java trong hệ thống screen hiện có. Game sử dụng khung hình gameplay thực tế, bao gồm camera, nhân vật, boss, hiệu ứng và HUD tại thời điểm chuyển màn. Các ảnh JPEG trong bản demo không được đưa vào game.

## Giao diện

- Pause: tiêu đề **PAUSED**, màu cyan cho chữ và nút chính; Tiếp tục / Chơi lại / Về menu chính.
- Game Over: chữ đỏ và nút Chơi lại đỏ sẫm; nút Về menu chính.
- Victory: chữ và nút chính xanh lá; Chương tiếp theo / Về menu chính ở Chapter 1. Chapter cuối chỉ có Về menu chính và lời cảm ơn.
- Toàn bộ chữ của ba màn dùng `PixelFont`, với scale nguyên và hình nút có góc pixel. Không có lớp phủ đỏ, xanh lá hoặc cyan trên background. Chỉ làm tối khung hình bằng màu đen 60%, đúng cách đọc chữ trong mẫu cuối.
- Mỗi màn giữ một snapshot khi vào và giải phóng khi ra. Render không cập nhật thế giới nên vị trí, animation, thời gian Block và các cooldown đứng yên trên màn Pause/End.

## Điều khiển

- W/S hoặc ↑/↓: chọn nút, có quay vòng đầu/cuối danh sách.
- Enter: thực hiện lựa chọn.
- Chuột: di chuyển để chọn nút, nhấn chuột trái để thực hiện. Con trỏ đứng yên không kéo lựa chọn về nút cũ khi đang dùng bàn phím.
- Esc trong Pause: tiếp tục cùng trận, không tạo Player/Level mới.
- Esc trong Game Over/Victory: về menu chính.
- Chơi lại tạo lại arena của chương và các Player từ session hiện tại.
- Nút Chương tiếp theo giữ luồng cũ: chuyển sang màn chọn chương với chương kế tiếp đã chọn. Không tạo Chapter 3 sau khi hoàn thành Chapter 2.

Khoảng chờ cuối trận 1,8 giây và logic mở khóa/lưu tiến trình giữ nguyên. Trong khoảng chờ, diễn hoạt gameplay cuối trận tiếp tục; sau đó màn Game Over/Victory chụp và giữ khung hình đó. Lớp chữ kết thúc kiểu cũ trong PlayScreen đã được gỡ để tránh xuất hiện hai giao diện chồng nhau.

## File

| File | Thay đổi |
| --- | --- |
| `src/chronorunner/ui/GameplayOverlayScreen.java` | Mới: snapshot, palette, font, bố cục và nút dùng chung |
| `src/chronorunner/ui/PauseScreen.java` | Dùng giao diện chung, giữ resume/restart/menu |
| `src/chronorunner/ui/GameOverScreen.java` | Dùng giao diện chung với nền trận vừa thua |
| `src/chronorunner/ui/VictoryScreen.java` | Dùng giao diện chung với nền trận vừa thắng, nội dung theo chương |
| `src/chronorunner/ui/PlayScreen.java` | Chụp gameplay/HUD, bỏ lớp chữ kết thúc cũ |
| `src/chronorunner/ui/Menu.java` | Getter đọc số nút và nhãn để render dùng chung |
| `src/chronorunner/core/InputHandler.java` | Trạng thái di chuyển/click chuột theo cùng ranh giới frame với bàn phím |
| `src/chronorunner/ui/GamePanel.java` | Kết nối sự kiện chuột Swing vào input |
| `sources.txt` | Danh sách source được build tự sinh lại, thêm lớp mới |
| `tools/OverlayScreensCheck.java` | Mới: kiểm thử screen, input, snapshot và luồng chương |
| `docs/gameplay-overlays.md` | Báo cáo này |

## Kiểm chứng

```powershell
.\build.bat
javac -encoding UTF-8 -cp out -d out tools/OverlayScreensCheck.java tools/PlayerDefenseCheck.java
java '-Djava.awt.headless=true' -cp out OverlayScreensCheck
java '-Djava.awt.headless=true' -cp out PlayerDefenseCheck
```

- **Build: PASS.**
- **372 kiểm tra overlay: PASS.** Bao gồm cả hai chương, 1P/2P, chụp đúng khung hình, ảnh đứng yên, không thêm màu lên nền, resume không dựng lại trận, restart, menu chính, chuyển chương, giới hạn chương cuối, lưu mở khóa đúng một lần mỗi lần thắng, hover/click và bàn phím.
- Kiểm tra đường nối sự kiện `GamePanel.mousePressed()` thật; tọa độ click được giữ đúng nếu chuột di chuyển trước frame tiếp theo. Click chưa được xử lý vẫn tồn tại qua một render frame không có bước update và không bị lặp sau khi chuyển screen.
- **315 kiểm tra Player/Block/combat hiện có: PASS.**
- Đã xem ảnh do renderer production tạo ra cho Pause, Game Over và Victory; cả hai chương và chế độ hai người được xuất tại `out/end-screen-integration/`.
- SHA-256 so với snapshot đầu tác vụ xác nhận map, asset, AI/combat, Player/Hud và file save không bị thay đổi trong tác vụ này. Các thay đổi God Mode và save có sẵn trước đó vẫn được giữ.
- Debug collision tắt. Bài kiểm thử thay phương thức ghi save bằng bộ đếm trong một lớp Game dùng riêng cho kiểm thử; không ghi đè tiến trình của người chơi.

Việc xác minh gameplay/điều hướng ở trên chạy tự động với code production và render ngoài màn hình. Chưa có lượt kiểm tra thủ công bằng bàn phím/chuột trong cửa sổ game ở tác vụ này.

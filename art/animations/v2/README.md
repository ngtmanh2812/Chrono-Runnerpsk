# Chrono Runner — Animation v2

Bộ sprite pixel art đã tích hợp cho **Phan Sĩ Kiên, Ronaldo, Độ Mixi, The Time Guardian và The Void Core**. Thiết kế bám theo bộ concept nâng cấp trong `art/concepts/asset-upgrade-v2/`.

Mỗi đối tượng có 64 PNG frame và một sprite sheet 6 cột × 12 hàng. Tổng cộng **320 file frame**, gồm 300 tư thế gốc, thêm 20 file cho trạng thái rơi/đáp tái sử dụng tư thế đã vẽ trong Jump. Không dùng ảnh tĩnh dịch chuyển thay cho chuyển động tay/chân.

Mở [preview.html](preview.html) bằng trình duyệt để chọn trạng thái, tua frame và xem hướng trái/phải. [all-animations.gif](previews/all-animations.gif) phát lần lượt toàn bộ nhóm. Thư mục `runtime-previews/` chứa ảnh từ renderer gameplay thật, được xuất bằng bộ kiểm tra Java.

| Hàng (từ 0) | Trạng thái | Frame | Nội dung |
|---|---|---:|---|
| 0 | idle | 6 | Thở, chuyển động khăn/áo choàng, nhịp lõi |
| 1 | run | 6 | Chạy nghiêng người, sải chân dài; Core lướt nhanh |
| 2 | jump | 6 | Lấy đà, bật lên, đỉnh nhảy, rơi, đáp, hồi tư thế |
| 3 | attack | 6 | Chuẩn bị → đánh → thu đòn, mỗi giai đoạn 2 frame |
| 4 | hurt | 6 | Trúng đòn, giật lùi, lấy lại thăng bằng |
| 5 | death | 6 | Gục xuống/nằm bất động; Core vỡ và tắt lõi |
| 6 | walk | 6 | Đi chậm thẳng người, khác Run |
| 7 | special | 6 | Chrono Shot, Siuu Burst, đập đất, năng lượng thời gian, quá tải lõi |
| 8 | block | 6 | Giơ tay, dựng khiên, chịu va chạm, hạ khiên |
| 9 | dash | 6 | Lấy đà, lao nhanh, hãm, trở lại tư thế |
| 10 | fall | 2 | Từ jump_3 và jump_4 |
| 11 | land | 2 | Từ jump_5 và jump_6 |

Tám hàng đầu giữ thứ tự sprite sheet đã yêu cầu trước đó. Những ô không dùng ở cuối hàng Fall/Land hoàn toàn trong suốt; số frame thật được ghi trong manifest.

| Đối tượng | Kích thước frame | Sprite sheet | Pivot trong frame | Scale gameplay |
|---|---|---|---|---:|
| Phan Sĩ Kiên | 128 × 128 | 768 × 1536 | (64,116), chân | 1 |
| Ronaldo | 128 × 128 | 768 × 1536 | (64,116), chân | 1 |
| Độ Mixi | 128 × 128 | 768 × 1536 | (64,116), chân | 1 |
| Time Guardian | 256 × 256 | 1536 × 3072 | (128,232), chân | 2 |
| Void Core | 256 × 256 | 1536 × 3072 | (128,128), tâm lõi | 2 |

Void Core có offset render Y = -24 pixel thế giới để phù hợp ảnh lớn hơn và vị trí HUD; không đổi tọa độ vật lý. Cả hai chiều luôn dùng cùng một scale. Texture dùng alpha thật 0/255, một bảng màu 64 màu chung cho mỗi đối tượng, vẽ nearest-neighbour.

## Tài nguyên đang dùng trong game

- `assets/players/phan_si_kien/v2/`
- `assets/players/ronaldo/v2/`
- `assets/players/do_mixi/v2/`
- `assets/bosses/the_time_guardian/v2/`
- `assets/bosses/the_void_core/v2/`

Mỗi thư mục có frame PNG riêng, `sprite_sheet.png`, `manifest.json` và `animation.properties`. Runtime đọc PNG theo tên và dùng `animation.properties` để lấy pivot, scale, tốc độ và kiểu lặp; không phải đoán kích thước từ ảnh. `manifest.json` ghi tọa độ từng ô, kích thước PNG nguồn đã đo, lát cắt thực tế và đường dẫn nguồn tính từ gốc project. Asset cũ được giữ nguyên ngoài thư mục `v2/`.

## Cách nối với gameplay

- Frame count lấy từ file đã tải. Đánh thường ghim hai frame vào từng cửa sổ Startup/Active/Recovery hiện có, không đổi thời điểm tạo hitbox.
- Nhảy/rơi theo vận tốc, đáp chỉ chạy khi chạm đất; animation không điều khiển physics.
- Block của player dùng đúng cơ chế cũ: 0.5 giây, hấp thụ một đòn, cooldown 2 giây. Frame va chạm có phản hồi riêng.
- Death không lặp và giữ frame cuối. Boss vẫn cập nhật bộ đếm animation sau khi chết, AI không chạy. Màn Victory xuất hiện sau thời gian chờ cũ 1.8 giây.
- Boss có bộ frame Block để sử dụng sau này; chưa thêm cơ chế đỡ đòn. Sau đợt sửa bug, Core giữ volley/radial/teleport-dive và không triệu hồi enemy phụ. Đòn của cả hai boss dùng Startup/Active/Recovery chung để điều khiển animation; xem `docs/boss-fixes.md`. Sprite bank không thay đổi.
- Portrait ở HUD, chọn nhân vật, credits và icon menu được fit theo phần ảnh nhìn thấy, tránh padding của frame mới làm hình nổi lên hoặc tràn khung.

## Tạo lại và kiểm tra

Tạo hình bằng công cụ **image_gen tích hợp**. Toàn bộ prompt được lưu trong [prompts.md](prompts.md); nguồn PNG ở `source/`. Atlas nguồn được đo là 1374 × 1145; vì khoảng cách hàng không đều nên công cụ cắt theo alpha thực tế thay vì chia mù theo kích thước dự kiến. Frame apex Độ Mixi bị thiếu trong nguồn được vẽ riêng bằng image_gen và hiệu chỉnh scale đồng nhất khi đóng gói.

`tools/PrepareAnimationPack.java` chỉ xử lý kỹ thuật: cắt ô, chuẩn hóa pivot/scale, alpha, palette, đóng gói và xuất GIF. Không tạo tư thế giả bằng xoay/kéo giãn cả ảnh.

Từ gốc project:

```powershell
.\build.bat
javac -encoding UTF-8 -cp out -d out tools/PrepareAnimationPack.java
java -Djava.awt.headless=true -Xmx768m -cp out PrepareAnimationPack --install
javac -encoding UTF-8 -cp out -d out tools/AnimationIntegrationCheck.java
java -Djava.awt.headless=true -Xmx768m -cp out AnimationIntegrationCheck
```

Có thể thêm `--no-preview` khi chỉ cần đóng gói/cài asset. Dùng `run.bat` để chạy game; nếu đang mở bản game cũ, đóng cửa sổ đó rồi mở lại sau build. `run-godmode.cmd` vẫn dùng được để test bất tử.

Kết quả kiểm thử cuối cùng nằm trong [test-results.txt](test-results.txt). Debug collision không được bật trong game hoàn thiện.

## File code được sửa trong lần tích hợp này

- `src/chronorunner/animation/Animation.java`: ổn định chỉ số frame tại ranh giới số thực.
- `src/chronorunner/gfx/SpriteLayout.java` (mới), `SpriteSet.java`, `Art.java`: đọc metadata, chọn bộ v2, dùng frame count thật và pivot rõ ràng.
- `src/chronorunner/gfx/PixelSprite.java`: preview vừa khung theo vùng pixel có alpha.
- `src/chronorunner/entity/Player.java`: nối các trạng thái hiện có với animation mới.
- `src/chronorunner/combat/PlayerDefense.java`: chỉ thêm getter tiến trình đỡ đòn phục vụ hiển thị.
- `src/chronorunner/entity/boss/Boss.java`: frame count/timing/pivot và trạng thái rơi/đáp; không sửa hàm AI của hai boss.
- `src/chronorunner/ui/PlayScreen.java`: cập nhật/vẽ boss đã chết để phát hết clip.
- `src/chronorunner/ui/Hud.java`, `CharacterSelectScreen.java`, `CreditsScreen.java`, `UiKit.java`: thumbnail vừa khung.
- `sources.txt`: build tự cập nhật danh sách Java, gồm SpriteLayout.
- `tools/PrepareAnimationPack.java`, `AnimationIntegrationCheck.java` (mới), `ChapterTwoMapCheck.java`: chuẩn hóa/cài asset và kiểm tra tích hợp; kiểm tra vị trí Core đo vùng nhìn thấy thực tế.

Những thay đổi pause/game over/victory và phím đỡ đòn từ trước trong workspace được giữ lại.

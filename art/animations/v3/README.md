# Chrono Runner — bản sắc nhân vật và kỹ năng v3

Đã tích hợp ba bank v3 và bộ U/I/O riêng vào game Java hiện tại. Ronaldo và Độ Mixi có thiết kế cùng animation mới; Phan Sĩ Kiên giữ thiết kế cũ và bổ sung tư thế chiến đấu. Cập nhật 30/10: **W nhảy, K dash, J nối thường → Heavy → Launcher** bằng animation có sẵn. Chi tiết runtime và phạm vi combo: [identity-runtime-v3.md](../../../docs/identity-runtime-v3.md).

## Bản sắc và bộ chiêu đã chạy trong game

| Nhân vật | Thiết kế | U | I | O |
|---|---|---|---|---|
| Phan Sĩ Kiên | Kiếm sĩ thời gian; áo xanh, khăn đỏ, năng lượng cyan | Chrono Shot | Time Slash | Timeline Break |
| Ronaldo | Cầu thủ áo đỏ số 7, quần trắng, giày bóng đá; chân và bóng năng lượng xanh | Power Shot | Step-over Burst | Siuu Bicycle Kick |
| Độ Mixi | Streamer áo đen/viền vàng, headset và mic, găng phát sóng âm | Sóng Âm | Đập Bàn | Đại Hội Tộc |

U/I/O lần lượt gây **40/45/120 damage**, tiêu hao **20/30/100 EN**. Năng lượng tối đa/khởi đầu 100; giữ phím nạp khi đứng yên trên mặt đất sẽ hồi 18 EN/giây. Bộ chiêu có nguồn sát thương và hiệu ứng riêng: orb/kiếm thời gian, bóng/rê bóng, sóng mic/chấn động. Độ Mixi không triệu hồi enemy. Các vòng sóng lớn được game vẽ riêng, không nhúng nền hoặc che tư thế trong sprite.

Chuỗi J gây **30 / 40 / 35** damage, giữ nguyên frame và timing animation. Mỗi bước cần một lần nhấn; có buffer 8 frame và cửa sổ nối 0,60 giây sau đòn. Các clip dùng là Light 1 (hoặc Air Attack), Heavy, Launcher. Light 2/3 vẫn có asset để xem, chưa dùng trong route này. Không có hit-confirm cancel/juggle nâng cao.

## Điều khiển đã áp dụng

| Hành động | P1 | P2 |
|---|---|---|
| Di chuyển | A/D | ←/→ |
| Nhảy / nhảy kép | W | Numpad 0 |
| Thường → Heavy → Launcher | J → J → J | Numpad 1 → 1 → 1 |
| Dash | K | Numpad 3 |
| Block một hit | S | ↓ |
| Giữ để nạp năng lượng | L | Numpad Enter |
| U/I/O | U/I/O | Numpad 4/5/6 |
| Pause | Esc | Esc |

H/Shift không còn dùng cho Heavy/dash trong gameplay; Space không nhảy. Numpad Enter được nhận bằng vị trí phím; Enter chính không nạp P2. Menu/chọn nhân vật giữ phím cũ trong ngữ cảnh riêng.

## Bộ animation

Mỗi actor có **22 trạng thái, 124 PNG frame và một atlas 768 × 2816** (6 cột × 22 hàng). Tổng **372 PNG frame + 3 atlas**. Frame thống nhất 128 × 128, pivot chân (64,116), scale gameplay 1, standing body mục tiêu 64 pixel. Alpha thật 0/255, nearest-neighbour, không grid/nhãn/nền giả trong PNG runtime.

Thứ tự hàng: `idle, run, jump, attack, hurt, death, walk, special, block, dash, fall, land, light_1, light_2, light_3, heavy, launcher, air_attack, charge, skill_u, skill_i, ultimate`.

18 trạng thái nguồn có 6 frame riêng; alias `attack=light_1`, `special=skill_u` có 6 frame; `fall` và `land` có 2 tư thế từ Jump. Các ô không dùng ở cuối hàng Fall/Land trong atlas hoàn toàn trong suốt. Việc đếm file 124 có tính các alias, không có nghĩa 124 tư thế độc lập.

48 PNG của tám trạng thái nền Phan v2 (`idle/walk/run/jump/dash/block/hurt/death`) giữ nguyên byte. Ronaldo/Độ dùng đầy đủ nguồn mới. Death kết thúc nằm; Run/Walk, Light 1/2/3, Heavy, Launcher và từng U/I/O có hành động riêng.

Runtime đang tải:

- `assets/players/phan_si_kien/v3/`
- `assets/players/ronaldo/v3/`
- `assets/players/do_mixi/v3/`

Asset v2 và hai boss không bị ghi đè. `Art` chọn v3 trước v2; metadata `animation.properties` điều khiển cách tải/neo và nhịp mặc định, còn thời điểm gây damage bám cửa sổ combat.

## Xem và kiểm tra

- [GIF tổng](previews/all-animations.gif): ba nhân vật cạnh nhau, toàn bộ 22 trạng thái, 124 frame trình diễn thực.
- [Ronaldo](previews/ronaldo-contact-sheet.png), [Độ Mixi](previews/do_mixi-contact-sheet.png), [Phan](previews/phan_si_kien-contact-sheet.png): contact sheet để đối chiếu tư thế.
- `previews/<state>.gif`: từng trạng thái; GIF có nền kiểm tra và marker, các marker này không nằm trong PNG runtime/game.
- `metadata/validation.txt`, `install-verification.json`, `preview-verification.json`: alpha, bounds, uniqueness, manifest và SHA-256.
- `out/identity-v3-check/`: ảnh renderer game thật, HUD hai người và chín chiêu.

Xem [báo cáo runtime](../../../docs/identity-runtime-v3.md) cho energy, cooldown, timing, ngắt đòn và danh sách kiểm thử. Build đạt; Identity `--require-v3` 703, MeleeChainControlsCheck 692, Animation 590, Player 321, Boss 571, Overlay 372, Chapter 1 756, Chapter 2 11.573 kiểm tra. Các phép kiểm tra dùng input/physics/renderer production ở chế độ headless, không thay cho playtest cân bằng với người chơi.

## Nguồn và tạo lại

Concept ở `art/concepts/identity-v3/`; 11 atlas nguồn ở `source/`. Tạo bằng **image_gen tích hợp**, toàn bộ prompt ở [prompts.md](prompts.md). Phiên bản kỹ năng Độ có vòng quá lớn được lưu ở `source/variants/`, không dùng trong game.

`tools/PrepareIdentityAnimationPack.java` chỉ làm kỹ thuật: đo alpha, cắt ô/loại phần của ô lân cận theo tọa độ đã xác minh, đặt pivot, scale đều hai chiều, giảm palette và đóng gói. Không xoay ảnh tĩnh hay nhân bản frame để giả hành động. Mỗi `manifest.json` lưu kích thước PNG thực, tọa độ source, transform và override đã dùng. Cấu hình đo thủ công ở `metadata/measured-overrides.properties` được tool tự đọc.

```powershell
java '-Djava.awt.headless=true' tools/PrepareIdentityAnimationPack.java --install
.\build.bat
javac -encoding UTF-8 -cp out -d out tools/IdentitySkillsCheck.java
java '-Djava.awt.headless=true' -cp out IdentitySkillsCheck --require-v3
```

Đóng cửa sổ game đang mở rồi chạy `run.bat` để JVM mới tải bank v3. `run-godmode.cmd` vẫn dùng được khi test bất tử.

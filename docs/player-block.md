# Player base và Block — 21/10/2026

## Hành vi và cấu hình

- `Player.PLAYER_BASE_ATTACK_DAMAGE = 30`: nguồn duy nhất cho damage đánh thường của Phan Sĩ Kiên, Ronaldo và Độ Mixi, ở cả P1/P2.
- `PlayerDefense.BLOCK_DURATION_SECONDS = 0.5` và `BLOCK_COOLDOWN_SECONDS = 2.0`: thời gian tính bằng giây trong game, hồi chiêu bắt đầu ngay khi bật Block.
- Nhấn một lần để vào thế đỡ. Đỡ 100% một hit đối phương hợp lệ từ mọi hướng, sau đó kết thúc ngay. Hết 0,5 giây mà không trúng hit cũng kết thúc. Giữ phím không tự kích hoạt lại.
- Không mở đòn đánh thường mới khi đang đỡ. Block không hủy đòn đang ở startup/active/recovery và không kích hoạt trong lúc bị stun. Nếu S và J cùng được nhấn khi sẵn sàng, Block được ưu tiên.
- Di chuyển, nhảy kép, dash và kỹ năng riêng giữ cách vận hành hiện tại. Chrono Shot vẫn 20 damage, Siuu 22, chấn động Độ Mixi 26. Hình học, startup/active/recovery, knockback và tầm đánh thường giữ nguyên.
- Pause ngừng cập nhật timer. Hết trận hoặc chết hủy thế đỡ. Chơi lại/chuyển chương tạo Player mới với Block sẵn sàng; không thêm hệ thống hồi sinh mới.

## Điều khiển thực tế

| Hành động | P1 | P2 |
| --- | --- | --- |
| Trái / phải | A / D | ← / → |
| Nhảy / nhảy kép | W | ↑ |
| Đánh thường | J | Numpad 1 |
| Dash | K | Numpad 2 |
| Kỹ năng riêng | L | Numpad 3 |
| Block mới | **S** | **↓** |
| Pause / tiếp tục | Esc | Esc |

S và ↓ đã dùng để điều hướng menu. Đây là hai ngữ cảnh riêng: menu giữ nguyên điều hướng, trong trận hai phím chưa có hành động cũ nên không cần đổi phím. P2 có timer và thế đỡ độc lập với P1.

## Nhận sát thương và chống trúng lặp

Tất cả nguồn sát thương combat hiện có được giải quyết tại `PlayScreen`, gọi `Player.takeDamage()` trước khi đi vào `Character.takeDamage()`. Player loại hit null, damage không dương, trạng thái chết hoặc bất tử trước khi xét Block. `PlayerDefense.absorb()` chỉ tiêu hao Block với hit đối phương dương.

Nếu đỡ thành công, luồng trả về trước khi trừ HP, thêm knockback/stun/invulnerability hoặc phát Hurt. `PlayScreen` chỉ áp slow, số damage và rung màn hình khi HP thực sự giảm, vì vậy đòn bị đỡ cũng không áp các tác dụng này. Đòn độc lập tiếp theo vẫn gây damage bình thường; Block không cấp một cửa sổ bất tử bổ sung.

- Melee/energy hitbox: tái sử dụng `Hitbox.alreadyHit` và `markHit(player)` sau va chạm, kể cả hit đã đỡ. Một hitbox không đánh lại cùng Player.
- Projectile: đặt `alive = false` ngay tại va chạm, kể cả bị đỡ; không còn gây damage ở frame sau.
- Tiếp xúc cơ thể: chỉ với tiếp xúc đã được đỡ, lưu cặp nguồn–Player cho đến khi hai cơ thể tách ra. Đợt tiếp xúc sau khi tách là một hit mới. Các đòn melee/đạn riêng của cùng nguồn vẫn gây damage bình thường sau khi Block kết thúc.
- Thứ tự giải quyết giữ nguyên: projectile theo thứ tự trong danh sách, hitbox theo thứ tự trong danh sách, sau đó tiếp xúc boss/enemy. Các Player được xét theo thứ tự P1 rồi P2. Hit đầu tiên hợp lệ tiêu hao thế đỡ.
- Hệ thống hiện không có ID hit toàn cục, sát thương rơi hay hazard riêng cần chỉnh. Không thêm ID trùng với cơ chế ghi nhận hit đã có.

## Cấu trúc và hiển thị

`PlayerDefense` chỉ quản lý thời gian và tiêu hao một hit. Player giữ input/movement/animation đang có và chuyển tiếp damage tới thành phần này. Ba hàm `performAttack()` giống hệt nhau được gom vào Player; bỏ trường `attackPower` không được sử dụng. Cấu hình attack riêng vẫn ở các lớp nhân vật. Không tách thêm controller/combat class khi chưa cần.

Asset không có animation Block riêng, nên sử dụng một khiên pixel cyan nhỏ bên cạnh sprite. Khi đỡ trúng, khiên lóe trắng 0,18 giây với tia sáng ngắn. Không tạo frame animation giả hoặc đổi màu sprite. HUD thêm dải Block cao 22 px phía trên bảng Player: phím, trạng thái và tiến độ hồi chiêu. HP và hàng kỹ năng giữ nguyên vị trí.

## File thay đổi trong tác vụ này

| File | Thay đổi |
| --- | --- |
| `src/chronorunner/combat/PlayerDefense.java` | Mới: trạng thái, cấu hình và timer Block |
| `src/chronorunner/entity/Player.java` | Damage thường chung, phím Block, damage gate, dùng chung normal attack, hiệu ứng khiên |
| `src/chronorunner/entity/character/PhanSiKien.java` | Dùng damage chung; bỏ trường và dispatch thừa |
| `src/chronorunner/entity/character/Ronaldo.java` | Dùng damage chung; bỏ trường và dispatch thừa |
| `src/chronorunner/entity/character/DoMixi.java` | Dùng damage chung; bỏ trường và dispatch thừa |
| `src/chronorunner/entity/character/CharacterFactory.java` | Cân bằng chỉ báo Damage trong màn chọn nhân vật; mô tả Độ Mixi theo tầm đánh rộng |
| `src/chronorunner/world/CombatWorld.java` | Truy vấn trạng thái combat để Player không phụ thuộc trực tiếp vào screen |
| `src/chronorunner/ui/PlayScreen.java` | Gating theo trạng thái trận; chống lặp tiếp xúc đã đỡ; loại damage không dương |
| `src/chronorunner/ui/Hud.java` | Dải Block độc lập cho từng Player |
| `tools/PlayerDefenseCheck.java` | Mới: kiểm thử input, combat, timer, multiplayer và screen thật |
| `docs/player-block.md` | Mới: báo cáo này |

So sánh SHA-256 với snapshot trước chỉnh sửa xác nhận chỉ 8 file source có sẵn kể trên bị thay đổi; toàn bộ asset, code map/camera/physics, boss AI/HP/damage và animation controller giữ nguyên. Các thay đổi có sẵn từ tác vụ trước không bị hoàn tác.

## Kiểm thử

Lệnh chạy từ thư mục project bằng PowerShell:

```powershell
.\build.bat
javac -encoding UTF-8 -cp out -d out tools/PlayerDefenseCheck.java tools/ChapterOneMapCheck.java tools/ChapterTwoMapCheck.java
java '-Djava.awt.headless=true' -cp out PlayerDefenseCheck
java '-Djava.awt.headless=true' -cp out ChapterOneMapCheck
java '-Djava.awt.headless=true' -cp out ChapterTwoMapCheck
```

- Build: **PASS**.
- Player/combat: **315 assertions PASS**. Bao gồm normal hit/miss cho 3 nhân vật × 2 slot × 2 boss, startup và kích thước hitbox cũ, trúng một lần, S/↓, invalid hit, bất tử do dash, ranh giới 0,5/2 giây, không tự kích hoạt khi giữ phím, hai hit/đạn chồng nhau, thứ tự xử lý trong `PlayScreen.update()`, melee thật của boss, enemy/contact, slow và projectile disposal.
- Kiểm thử screen: pause/resume đóng băng timer cho hai Player, attack sau resume, restart/respawn/chapter transition reset state, victory/game-over chặn Block, kỹ năng riêng và điều khiển di chuyển/nhảy hoạt động.
- Chapter 1: **756 assertions PASS**; Chapter 2: **11.573 assertions PASS**. Bao gồm nền, bệ một chiều, sàn, camera, boss, hai chương và tải màn chơi.
- Render bằng `Game.render()`: ảnh guard, impact và cooldown của cả hai chương ở `out/player-defense-check/`. Đã kiểm tra cách hiển thị khiên và dải Block hai Player. Debug collision tắt.
- `git diff --check` đạt; chỉ có thông báo chuyển LF/CRLF của Git.

Đã khởi chạy cửa sổ game bằng `java -cp out chronorunner.Main`. Cửa sổ xuất hiện rồi đóng (process exit 0) trước khi công cụ thao tác trực tiếp chọn được nó. Vì vậy **chưa có lượt chơi kiểm tra bằng bàn phím trong cửa sổ game**; các kết quả gameplay trên là kiểm thử tự động với code production, và kiểm tra hình ảnh là render từ engine. Bài kiểm thử không ghi tiến trình chơi vào saves.

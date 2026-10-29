# Sửa bug boss — 29/10/2026

Đã xử lý nhóm lỗi xác nhận trong [báo cáo trước sửa](boss-audit-2026-10-29.md). Không thay asset, HP, thông số damage, cooldown cơ bản hoặc các phase HP. Chưa làm đợt nâng cấp chiêu và cân bằng mới.

## Các thay đổi

1. **Đồng bộ đòn đánh:** Guardian và Void Core dùng lại `Attack` với Startup → Active → Recovery. Chỉ tới Active mới tạo melee/projectile. Mỗi cửa sổ giữ đúng hai tư thế của animation; combat điều khiển frame. Thời gian gameplay được khai báo trong class boss, độc lập FPS của asset.
2. **Khóa hướng và điểm ngắm:** không đổi phía hoặc quay đòn sang player khác giữa action. Special không chen ngang đòn thường đã cam kết.
3. **Cú lao Guardian:** hitbox bám boss suốt Active 0.35s, đồng bộ vị trí trước khi xét collision; hết Active thì dừng chuyển động và hủy vùng damage. Không còn hitbox đứng lại phía sau hoặc hết sớm ở 0.18s.
4. **Hurt/stun:** ngắt action đang chuẩn bị hoặc đang gây damage, hủy hitbox tương ứng, giữ knockback và dừng AI trong phản ứng Hurt. Không còn vừa khựng vừa bắn. Contact damage cũng không hoạt động trong Hurt, Startup hoặc Recovery.
5. **Mục tiêu theo độ cao:** xét giao vùng melee thực tế và khoảng cách 2D. Mục tiêu ngoài vùng đánh được xử lý bằng volley hiện có. Giữ khoảng đánh thay vì tiếp tục đi vào tâm player. Guardian dùng đạn hiện có khi Special ngang không thể chạm mục tiêu trên bệ.
6. **Core bay và dive:** điều khiển độ cao bay theo mặt hỗ trợ của mục tiêu, giữ khoảng hở 24px ở vị trí ổn định. Dive khóa điểm và mặt đáp lúc cam kết, đi xuyên các bệ one-way trung gian, vẫn chạm đúng mặt đáp/sàn. Nếu không có khoảng trống hợp lệ phía trên bệ quá cao, dùng radial burst hiện có. Đáp xong chuyển Recovery.
7. **Bỏ summon:** chu kỳ Core chỉ luân phiên radial burst và teleport/dive hiện có; không gọi Chrono Bat hoặc enemy mới.
8. **Chết trên không:** tắt AI nhưng tiếp tục gravity/collision cho xác boss đến mặt đỡ, animation Death giữ frame cuối. Có thể đáp lên bệ hoặc sàn chính.
9. **Chốt kết quả trận:** dọn hitbox, projectile, enemy; chặn phát đòn và sát thương mới; chỉ chạy phần animation, death physics và VFX kết thúc. Không còn player bị giết bởi đạn sau khi boss chết. Thời gian chờ 1.8s giữ nguyên. Nếu hai bên đã chết cùng lúc khi kiểm tra kết quả, giữ ưu tiên Victory như trước.

## File đã sửa

| File | Vai trò |
|---|---|
| `src/chronorunner/combat/Attack.java` | Hook hiệu ứng Active, kết thúc Active sớm khi đáp, tiến trình animation chung |
| `src/chronorunner/combat/Hitbox.java` | Hủy hitbox và sync owner không làm giảm lifetime |
| `src/chronorunner/animation/AnimationController.java` | Khởi động lại clip khi bắt đầu một action mới |
| `src/chronorunner/entity/boss/Boss.java` | Action chung, Hurt, khóa hướng, chọn mục tiêu, body damage và death physics |
| `src/chronorunner/entity/boss/TimeGuardian.java` | Gắn melee/volley/dash với các cửa sổ combat |
| `src/chronorunner/entity/boss/TheVoidCore.java` | Hover, dive theo mặt đáp, bỏ summon |
| `src/chronorunner/core/Physics.java` | Bộ lọc mặt collision theo move; đường mặc định của player giữ nguyên |
| `src/chronorunner/entity/Character.java` | Hook death physics và update sau khi kết thúc trận |
| `src/chronorunner/entity/Player.java` | Dùng tiến trình Attack chung, hủy đòn và chặn input combat lúc hết trận |
| `src/chronorunner/ui/PlayScreen.java` | Chốt kết quả, dọn combat, sync hitbox và tiếp tục cảnh chết |
| `tools/BossCombatFixCheck.java` | Bộ kiểm tra hồi quy mới |
| `tools/AnimationIntegrationCheck.java` | Kiểm tra Special phải đợi đòn trước hoàn tất |
| `tools/ChapterTwoMapCheck.java` | Kiểm tra Core không còn summon |
| `tools/PlayerDefenseCheck.java` | Chặn lời gọi save trong test fixture |

## Kết quả kiểm thử

- `build.bat`: **BUILD OK**.
- `BossCombatFixCheck`: **571** kiểm tra qua, gồm các cửa sổ combat ở 30/60/120 Hz, hướng/aim khóa, Hurt, hitbox dash, hover/dive, death, kết thúc 1P/2P và pause/resume.
- `PlayerDefenseCheck`: **315** kiểm tra chiến đấu player qua; startup/active/recovery và damage cũ của ba nhân vật giữ nguyên.
- `AnimationIntegrationCheck`: **390** kiểm tra qua.
- `OverlayScreensCheck`: **372** kiểm tra qua.
- `ChapterOneMapCheck`: **756** kiểm tra qua, 10 bệ nổi còn tới được.
- `ChapterTwoMapCheck`: **11.573** kiểm tra qua, 9 bệ nổi còn tới được; Core không xuyên sàn hoặc ra khỏi arena.
- SHA-256: **516 file asset không thay đổi**.
- Debug collision **OFF** khi kết thúc. Kiểm tra dùng production input/physics/renderer ở chế độ headless; ảnh gameplay ở `out/boss-fix/`.
- Các kịch bản end-screen/boss và fixture chiến đấu chặn gọi lưu tiến trình trong lúc test.

Log chi tiết: `out/boss-fix/regression.log`, `player-combat.log`, `animations.log`, `overlays.log`, `chapter1.log`, `chapter2.log`.

Ảnh kiểm tra: `out/boss-fix/chapter2-hover.png`, `guardian-dash-windup.png`, `guardian-dash-active.png`, các ảnh Death/Victory 1P và 2P.

## Chưa thay đổi trong đợt sửa bug

Iframe chung 0.55s, cân bằng HP 1P/2P, vùng nhận đòn theo sprite mới, bản sắc phase và các clip riêng cho từng Special vẫn là việc cải tiến tiếp theo. Không thêm laser, chiêu hoặc animation mới. Các thời gian lấy đà hiện tại đã đồng bộ nhưng chưa được coi là đã cân bằng độ khó qua playtest người chơi.

Chạy `run.bat` sau khi đóng bản game đang mở để dùng code vừa build. `run-godmode.cmd` tiếp tục dùng được cho test bất tử.

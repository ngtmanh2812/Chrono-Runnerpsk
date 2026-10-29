# Đánh giá hai boss Chrono Runner — 29/10/2026

**Trạng thái:** đây là ảnh chụp phân tích trước sửa. Nhóm bug mục 1 đã được xử lý trong [đợt sửa boss](boss-fixes.md); các hạn chế cân bằng/thiết kế mục 2 vẫn được giữ để cải tiến sau. Số dòng bên dưới là số dòng tại thời điểm audit.

Đã đọc mã AI, animation, physics, hitbox, xử lý sát thương và kết thúc trận; chạy các kịch bản chẩn đoán headless trên class production. Tái hiện được **16 tình huống**. Đây là phân tích, chưa sửa gameplay. Đối chiếu SHA-256 trước/sau: 590 file trong `src/`, `assets/`, `saves/` không thay đổi.

Mã chẩn đoán: `out/boss-audit/BossAuditProbe.java`. Kết quả: `out/boss-audit/results.txt`. Các kịch bản movement đơn vị dùng sàn y=600 để cô lập lỗi; kịch bản sau chiến thắng dùng PlayScreen thật. Các lời gọi lưu tiến trình trong kịch bản kết thúc trận được chặn bằng NoSave.

## 1. Lỗi đã xác nhận

### P1 — Sát thương xuất hiện trước động tác đánh

**Cả hai boss.** `TimeGuardian.updateAI()` và `TheVoidCore.updateAI()` gọi `anim.play("attack")` rồi tạo melee/projectile ngay trong cùng lượt update. Tái hiện: hitbox đã chạm player trong khi animation vẫn ở frame 0. Player hiện có Startup/Active/Recovery; boss chưa dùng chu trình tương đương.

**Tác động:** người chơi khó phản ứng dựa trên hình ảnh, bị trúng lúc boss còn lấy đà. Sprite đẹp hơn không giải quyết được thời điểm sát thương.

**Khắc phục:** tạo Startup → Active → Recovery cho boss; chỉ sinh hitbox/projectile khi vào Active. Ghim frame theo trạng thái combat và phát dấu báo trước cho chiêu lớn. Tái sử dụng Attack/AttackData cho các đòn phù hợp; đòn đặc biệt cần thêm chuyển động/vùng ảnh hưởng riêng.

### P1 — Hitbox Special của Guardian đứng lại và hết hạn sớm

`TimeGuardian.specialAttack()` dash 0.35s nhưng gọi `Boss.melee()` tạo vùng cố định 0.18s, không dùng `followsOwner`.

**Tái hiện:** sau 0.20s boss đi 102px; hitbox đi 0px và đã hết hạn, trong khi dash còn 0.15s. Vì thế boss lao tới nhưng sát thương Special vẫn ở điểm xuất phát. Sát thương chạm thân có thể che khuất lỗi này bằng cách gây một loại damage khác.

**Khắc phục:** dùng hitbox bám boss trong cửa sổ Active của dash. Hệ thống Hitbox đã hỗ trợ `followsOwner`, Ronaldo đang sử dụng; không cần viết lại cơ chế. Gắn thời gian tồn tại, hướng, vị trí và điều kiện kết thúc cùng một action.

### P1 — Boss quay hướng giữa đòn nhưng hitbox vẫn ở phía cũ

**Cả hai boss.** AI gọi `faceTarget()` mỗi update trong khi hitbox đã sinh là vùng cố định. Tái hiện: cho player đi từ phải sang trái sau lúc boss ra đòn; sprite quay trái nhưng hitbox đòn đó vẫn nằm bên phải.

**Khắc phục:** khóa hướng và mục tiêu khi đòn được cam kết; chỉ chọn lại khi vào trạng thái cho phép. Nếu chiêu chủ đích theo dõi player thì phải thể hiện đường ngắm liên tục trước khi khóa.

### P1 — Vẫn chết sau khi hạ boss nhưng cuối cùng hiện Victory

`PlayScreen.update()` tiếp tục cập nhật enemy/projectile và giải quyết sát thương trong thời gian chờ kết thúc. `isCombatActive()` trả false nhưng các đường resolve damage chưa dùng nó để chặn. `updateEnd()` giữ cờ victory đã chốt.

**Tái hiện:** hạ boss → combat inactive → một projectile địch còn tồn tại giết player → cuối cùng vẫn VICTORY. Quy tắc ưu tiên Victory khi hai bên chết cùng frame cũng chưa được diễn đạt thành quy tắc thiết kế rõ ràng.

**Khắc phục:** khi chốt kết quả, vô hiệu sát thương địch và hành động chiến đấu, xử lý enemy triệu hồi, rồi chỉ cập nhật chuyển động kết thúc/VFX/death cần thiết. Quy định và kiểm thử rõ trường hợp cả hai phía chết cùng lượt.

### P2 — Stun và knockback của boss không khớp phản hồi Hurt

`Character.takeDamage()` đặt stun ít nhất 0.14s, cộng knockback; `Boss.updateBehavior()` vẫn chạy AI và ghi đè vx. Tái hiện khi stun còn 0.123s: Guardian vẫn bắn 1 viên, Core bắn 3 viên; vx -300 bị đổi ngay thành +150/+160.

**Khắc phục:** quyết định rõ đòn nào có thể làm boss khựng, đòn nào có giáp/chống choáng. Dùng ưu tiên trạng thái để xử lý; nếu dùng poise thì trạng thái đó phải có phản hồi riêng, thay vì animation báo khựng nhưng AI vẫn đánh như thường. Không nên mặc định mọi cú chạm đều ngắt toàn bộ chiêu lớn.

### P2 — Bỏ qua độ cao mục tiêu

`nearestPlayer()` và lựa chọn melee/ranged chỉ đo khoảng cách X. Tái hiện: player ngay phía trên boss, khác độ cao lớn; cả hai vẫn chọn melee đánh hụt, không chọn ranged hoặc nhảy.

**Khắc phục:** kiểm tra vùng đánh hai chiều, độ cao và bề mặt mục tiêu; không chọn melee nếu không có vùng giao nhau dự kiến. Guardian cần cách tiếp cận bệ hoặc sóng kiếm theo độ cao. Chọn mục tiêu co-op bằng khoảng cách 2D và giữ mục tiêu một khoảng thời gian, tránh đổi hướng liên tục.

### P2 — Core không có motor bay; cú dive có thể bị bệ chặn

Core dùng gravity và one-way collision chung trong Boss. Tái hiện: từ y=100, Core rơi xuống và đứng trên sàn. Với một bệ ở y=500 phía trên player đứng sàn y=600, chiêu teleport/dive đáp lên bệ đó thay vì tới khu vực player.

**Khắc phục:** tách hành vi Hover, Reposition, TelegraphDive, Dive, Recover. Hover giữ độ cao có giới hạn và chuyển động nhẹ. Khi dive, chọn mặt đáp hợp lệ; nếu cho xuyên bệ one-way thì dùng quy tắc collision riêng cho chiêu, vẫn giữ chặn sàn đấu trường. Dùng dấu báo điểm đáp trước dịch chuyển.

### P2 — Void Core vẫn sinh enemy phụ

Nhánh thứ ba trong chu kỳ Special gọi 2 Chrono Bat, hoặc 3 ở phase 3. Tái hiện sau 3 lần Special: có 2 Chrono Bat. Arena ban đầu không có quái phụ nhưng runtime summon vẫn còn; điều này lệch yêu cầu trước đó về bỏ enemy phụ.

**Khắc phục:** thay nhánh summon bằng laser, vùng năng lượng hoặc drone dạng bộ phận/VFX của boss. Nếu sau này muốn thêm enemy thật, cần quy định số lượng sống, thời gian tồn tại và dọn sau khi boss chết; hiện tại chỉ có trần tổng 32 enemy trong PlayScreen.

### P2 — Xác boss lơ lửng khi chết trên không

`Character.update()` chỉ chạy updateBehavior khi còn sống; physics nằm trong updateBehavior. Animation Death tiếp tục nhưng tọa độ bị đóng băng.

**Tái hiện ở cả hai boss:** bị hạ tại y=100 với vận tốc rơi; sau 1.5s Death đã kết thúc, y vẫn bằng 100.

**Khắc phục:** tắt AI nhưng tiếp tục physics của xác đến mặt đáp, hoặc dùng death trên không riêng. Với Core có thể vỡ trên không và cho mảnh vỡ rơi độc lập; với Guardian chuyển từ rơi sang gục/nằm khi chạm sàn.

## 2. Hạn chế thiết kế và cân bằng

Những mục này không tự động là bug; cần lựa chọn thiết kế và playtest.

- **Iframe chung 0.55s:** hai đòn riêng biệt 30 damage cùng lượt chỉ trừ 30 HP. Nó áp dụng cho mọi nguồn sát thương trên boss, nên người chơi thứ hai hoặc nhịp đánh nhanh có thể bị nuốt đòn. Nên cân nhắc chống trùng theo hitbox/nguồn đòn thay vì miễn sát thương toàn cục dài; giữ iframe chuyển phase nếu cần. Không đổi iframe của player cùng lúc.
- **Hai boss có cách truy đuổi gần giống nhau:** đi tới theo X, melee gần, bắn orb khi xa. Trong tầm melee, stopDistance được truyền thành 0 nên vẫn áp sát tâm player, dễ chồng thân và lật hướng. Cần giữ khoảng đánh, có khoảng lùi/hồi phục và hành vi riêng cho từng boss.
- **Phase chủ yếu tăng số:** tăng tốc độ, sát thương, giảm cooldown, thêm viên đạn/minion. Chưa có action chuyển phase riêng và biến đổi chiến thuật đủ rõ. Nên thêm một thay đổi dễ đọc ở mỗi phase, vẫn có thời gian né và trả đòn.
- **Chưa tách nhận diện các Special của Core:** radial/teleport/summon dùng cùng tên animation Special. Clip hiện tại dài 0.18s; người chơi khó đọc loại chiêu sắp tới. Nên có dấu báo/animation con riêng và thời gian lấy đà phù hợp.
- **Hình và hurtbox cần đánh giá lại:** vùng alpha Idle Guardian cao 194px, body collision/hurtbox cao 124px; Core tương ứng 198px và 140px. Đây là hệ quả giữ hitbox cũ khi nâng đồ họa, không có nghĩa mọi pixel của áo choàng/halo/VFX đều phải nhận đòn. Nên xác định rõ phần thân hoặc lõi được đánh, rồi tách body collision, vùng nhận đòn và vùng gây damage nếu cần.
- **Độ khó 1P/2P dùng cùng HP:** Guardian 700, Core 1600. Chưa đủ dữ liệu để kết luận nên tăng/giảm bao nhiêu; cần đo thời gian hạ boss, hit-rate, damage người chơi nhận và mức đóng góp từng người. Sửa lỗi mất đòn trước khi chỉnh HP.

## 3. Hướng cải tiến riêng

**Time Guardian:** giữ vai trò hiệp sĩ cận chiến có trọng lượng. Đòn thường là chém có lấy đà; Special là lao kiếm có đường báo hoặc nện kiếm tạo vòng thời gian. Cần chọn rõ chiêu khớp sprite/vùng damage: hiện code là dash còn bộ Special có tư thế kiếm/vòng năng lượng. Phase sau có thể thêm combo ngắn hoặc vùng làm chậm được báo trước; có khoảng hồi phục để player trả đòn.

**Void Core:** giữ vai trò boss bay kiểm soát không gian. Bay giữ khoảng cách, bắn quạt có khe né, laser có đường ngắm, teleport có điểm đáp báo trước. Thay Chrono Bat bằng một chiêu của chính Core để giữ trận đấu không có enemy phụ. Có cửa sổ lộ lõi sau chiêu lớn để tạo nhịp công/thủ, tránh chỉ kéo dài trận bằng lượng HP.

## 4. Thứ tự triển khai đề xuất

1. **Sửa tính chính xác:** khóa kết quả trận, Startup/Active/Recovery, hướng cam kết, hitbox dash, quy tắc Hurt/poise. Tái sử dụng hệ thống combat hiện có.
2. **Sửa di chuyển/địa hình:** kiểm tra độ cao, khoảng đánh, motor bay và điểm đáp Core, physics sau chết.
3. **Hoàn thiện thiết kế:** bỏ summon ngoài yêu cầu, dấu báo từng Special, bản sắc phase, vùng nhận đòn, cân bằng 1P/2P.

Tiêu chí nghiệm thu: không damage trong Startup/Recovery; hình và hướng hitbox khớp; dash không bỏ hitbox sau lưng; không có damage địch sau khi chốt thắng; player trên bệ được xử lý hợp lý; Core hover và dive đúng mặt đáp; xác không nằm bất động giữa không trung; hai player không nuốt damage ngoài quy tắc đã chọn.

## 5. Các điểm code chính

- `src/chronorunner/entity/boss/Boss.java`: updateBehavior 75, nearestPlayer 127, walkToward 147, melee 168, animation state 200.
- `src/chronorunner/entity/boss/TimeGuardian.java`: AI 17, attack 32, Special 50.
- `src/chronorunner/entity/boss/TheVoidCore.java`: AI 21, Special 54, summon 59, dive 73.
- `src/chronorunner/entity/Character.java`: update 48, takeDamage 66.
- `src/chronorunner/ui/PlayScreen.java`: update 106, updateEnd 158, hitbox 203, projectile 232, applyDamage 310.
- `src/chronorunner/combat/Hitbox.java`: hỗ trợ followsOwner sẵn.

Bộ kiểm tra trước đó tập trung việc tải/nối asset, input và hồi quy gameplay cơ bản. Các kịch bản chẩn đoán lần này kiểm tra thêm sự công bằng của đòn, tương tác độ cao và các trường hợp biên; kết quả trước đó không chứng minh AI đã bao phủ những tình huống này.

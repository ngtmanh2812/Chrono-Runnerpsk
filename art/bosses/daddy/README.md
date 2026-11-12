# Daddy — Boss 2 runtime V5

Nguồn người dùng gửi nằm nguyên trong `source/`. Đóng gói bằng
`python tools/PrepareDaddyBoss.py`, không cần công cụ tạo ảnh. Runtime ở
`assets/bosses/the_void_core/v5/`; ID chapter, tên HUD và tiến trình vẫn dùng
The Void Core. Boss đã đổi từ lõi bay thành quái đi trên mặt đất.

## Nguồn và mapping

| PNG nguồn | Kích thước thực | Frame | Dùng cho |
|---|---|---|---|
| Walk | 146×1620 | 9 × 146×180 | Đi/chạy |
| Shoot | 176×1408 | 8 × 176×176 | Bắn thường |
| Charged | 168×1408 | 8 × 168×176 | Xung điện |
| Charhedv2 | 160×1408 | 8 × 160×176 | Lao húc |
| Scream | 154×4224 | 24 × 154×176 | Sóng âm |

Scream được tải đủ 24 frame; giới hạn loader tăng 16 lên 32. Mỗi frame runtime
256×256, pivot chân (128,232), scale 2. Ảnh được cắt đúng theo strip, căn gốc
chân cố định theo từng nguồn, resample tỷ lệ đồng đều bằng nearest-neighbour
và lật ngang để đúng quy ước hướng của game. Alpha nguồn được giữ.
Idle lấy các pose nghỉ đầu Charged; Hurt mượn pose Charhedv2; Death tan biến
không loop vì bộ nguồn không có ba clip riêng này. Không dùng lại sprite Core.

## Chiêu thức

- Shoot: lấy đà 0,42 s, loạt 3 tia điện trắng (phase 3: 5 tia), 420 px/s,
  damage cơ bản 10/tia; khóa điểm nhắm khi bắt đầu. CD 1,8 s trước hệ số phase.
- Charged: báo trước 0,65 s, xung điện tròn bán kính 210 px, damage cơ bản 18,
  làm chậm 1,25 s, chỉ trúng một lần/mục tiêu. Có vòng điện và rung màn hình.
- Charhedv2: báo trước 0,60 s, lao theo hướng đã khóa trong 0,50 s, hitbox phía
  trước đi theo thân, damage cơ bản 20. Bị đánh ngắt sẽ hủy hitbox và dừng lao.
- Scream: báo trước 0,85 s, hai mặt sóng âm cao 150 px bay về hai phía ở
  330 px/s, damage cơ bản 14 và đẩy lùi mạnh. Sóng âm xuyên bệ đá; người chơi
  né bằng nhảy hoặc dash, hoặc chặn bằng block.

Ba chiêu special luân phiên, CD 4,8 s trước hệ số phase. Các hệ số phase,
1600 HP, collision thân 116×140, luật stagger/kháng choáng, block, pause và
kết thúc trận được tái sử dụng. Hurtbox căn theo thân mới trên mặt đất
(140×188 world px), thay vùng reactor nhỏ của boss bay cũ. Boss 1 không đổi.

## Kiểm thử

Build PASS; `DaddyBossCheck` 280 kiểm tra PASS, gồm đủ 24 frame Scream, load
V5 ở mọi trạng thái, grounded movement, windup, volley, slow, hủy rush khi
bị đánh, hai sóng âm và Death. `PhanSwordSkillsCheck` 3449 PASS;
`PhyschoMovesCheck` 18 PASS. Fixture chặn ghi save.

Ảnh renderer Java thật và kết quả: `out/daddy-check/`. Các bài test cũ dành
cho AI Core bay/dive mô tả bộ chiêu đã thay, không áp dụng cho Daddy.

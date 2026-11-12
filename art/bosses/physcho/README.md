# PhyschoBoss — Boss 1 runtime V5

Nguồn người dùng cung cấp từ PhyschoBoss được giữ nguyên trong `source/`.
`tools/PreparePhyschoBoss.py` đóng gói vào `assets/bosses/the_time_guardian/v5/`.
Game chọn V5 tự động; không dùng lại hình Guardian cũ. Bộ này có 22 clip/alias
runtime, frame 256×256, pivot chân (128,232), render scale 2. Các strip nguồn
thực tế rộng 128, mỗi frame cao 140: Idle 6, Walk 13, Dash 8, Barrelroll 8,
Charged 10. Alpha thật và nearest-neighbour; toàn bộ pose đã lật ngang theo
yêu cầu. Hurt mượn pose charged; Death dùng fade-out, vì nguồn không có hai
animation này. Nguồn không bị ghi đè.

## Bộ chiêu đang dùng

- Đánh thường (`light`, nguồn Dash): phát một sóng xung kích phía trước,
  tốc độ 480 px/s, damage cơ bản 14. Không còn hitbox chém cận chiến cũ.
- Barrel Roll (`skill_i`, nguồn Barrelroll): thùng lăn 350 px/s, hình và collider
  96×96 (gấp 4 cạnh hình cũ 24×24). Thùng rơi theo trọng lực, lăn trên mặt sàn,
  nổ khi va người chơi, cạnh platform/tường hoặc biên arena. Vụ nổ bán kính
  120 px, damage cơ bản 18, chỉ một lần/mục tiêu; không cộng thêm damage chạm
  thùng. Có khói, mảnh vỡ, vòng nổ và rung màn hình.
- Charged (`skill_u`, nguồn Charged): lấy đà 0,60 s rồi xung kích tròn quanh
  tâm boss, bán kính 230 px, damage cơ bản 16 và choáng 0,70 s khi thực sự
  trúng. Block và miễn thương vẫn chặn đòn/choáng. Cooldown 4,2 s trước hệ số
  phase. Vùng tròn dùng khoảng cách chính xác, không gây damage ở góc ô vuông.

AI luân phiên sóng thường và thùng; Charged dùng timer special riêng. Damage,
tốc độ tiếp cận và cooldown vẫn nhận hệ số phase hiện có. Máu, thân va chạm
địa hình, vùng nhận đòn boss, boss 2 và bộ chiêu player giữ nguyên.

`PhyschoMovesCheck` kiểm tra bộ chiêu mới qua PlayScreen thật; các bài kiểm tra
AI Guardian cũ dành cho melee/volley/dash không còn là hợp đồng của bộ chiêu
này. `PhyschoBossCheck` kiểm tra đủ frame, raster runtime, kích thước, pivot,
đạn và ảnh render thật. Kết quả/ảnh ở `out/physcho-moves/` và
`out/physcho-check/`. Các fixture chặn ghi save.

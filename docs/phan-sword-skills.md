# Phan Sĩ Kiên: kiếm, particles và phản hồi trúng đòn

Ba chiêu hiện có dùng động tác kiếm mới. Tên trong UI, nút bấm, damage,
nội năng, cooldown và các cửa sổ Startup/Active/Recovery giữ nguyên.

| Nút | Tên | Hình ảnh/hành vi | Damage | Nội năng | Cooldown |
|---|---|---|---:|---:|---:|
| U | Chrono Shot | Lưỡi kiếm cyan bay theo hướng nhìn, 780 px/s | 40 | 20 | 1,4 s |
| I | Time Slash | Chém kiếm kéo dài 220 px phía trước (trước đây 135 px) | 45 | 30 | 3 s |
| O | Timeline Break | Chém chữ X, vệt kiếm lớn và vòng thời gian trong vùng chiêu cũ | 120 | 100 | 8 s |

U có vùng va chạm lưỡi kiếm 56×72, cách sàn 3 px khi đứng trên đất. Đây là
vùng projectile; vùng nhận đòn của player và boss không đổi. I mở rộng tầm
chém theo yêu cầu. O giữ nguyên vùng hitbox hai phía, slow và knockback cũ.
Mỗi hitbox/projectile vẫn chỉ gây damage một lần lên cùng mục tiêu.

J → Heavy → Launcher của Phan có thêm vệt kiếm ngắn. Cả ba nhân vật đều có
particles khi đánh thường và tung U/I/O: Phan cyan, Ronaldo xanh lục, Độ Mixi
vàng; đòn nặng có thêm bụi chân. Hiệu ứng xuất hiện lúc Active, không thêm
nguồn damage, không tự hồi năng lượng và không đổi luật combo.

Chiêu kiếm Phan gây mất HP thực cho boss sẽ nổ khói xám tại tâm vùng thân
nhận đòn, thêm tia cyan và rung theo mức chiêu. Khói không xuất hiện khi
đánh hụt, boss đang miễn thương hoặc projectile va địa hình. Đòn kết liễu
vẫn có khói. Lệnh tung chiêu cũng rung nhẹ dù đánh hụt. Cài đặt tắt rung vẫn
được tôn trọng; khói và particles tiếp tục hiển thị. Pause dừng hiệu ứng.

## Frame và dữ liệu

Phan U/I/O và bốn hiệu ứng `crescent`, `long_slash`, `cross_burst`, `smoke`
đều tăng từ 6 lên 11 frame: mỗi asset thêm 5 hình vẽ khác nhau. Alias
`special` theo U. Các clip khác giữ nguyên. Ba clip pose dùng canvas 192×192,
pivot chân (96,172), thân chuẩn 64 px; VFX dùng pivot tâm và kích thước riêng
ghi trong `effects.properties`. Nền alpha thật, nearest-neighbour.

Mapping riêng U/I/O: F1–F6 lấy đà, F7–F10 ra đòn, F11 hồi thế. Metadata
`activeFrame=6`, `recoveryFrame=10` dùng chỉ số từ 0. Thời gian clip gốc
0,4 / 0,75 / 1,1 giây không đổi; gameplay vẫn quyết định cửa sổ sát thương.

Runtime: `assets/players/phan_si_kien/v4/` và
`assets/effects/phan_sword/v1/`. Nguồn, prompt built-in, số đo và ảnh rà hình
ở `art/effects/phan_sword/`. Hai tool đóng gói là
`tools/PreparePhanSwordEffects.py` và `tools/PrepareSwordFX.py`.

## Kiểm thử

`build.bat`: PASS. `PhanSwordSkillsCheck`: 3.449 kiểm tra PASS, gồm:

- 11 raster riêng biệt, thời lượng clip và mapping contact/recovery.
- Particles chỉ xuất hiện từ Active cho ba hero, J/Heavy/Launcher và U/I/O.
- 24 tổ hợp hai chapter × hai hướng × hai slot player × ba chiêu.
- Đúng damage/cost, không đánh lặp ngay cả khi cưỡng chế xóa iframe mỗi tick.
- Tầm U 350 px và I 150 px, khói khi trúng và khi kết liễu.
- Không khói khi hụt/miễn thương/va platform hoặc đòn của hero khác.
- Rung OFF, pause/resume, không tung chiêu sau kết thúc trận, không ghi save.

Ảnh chụp từ renderer Java thật và báo cáo ở `out/phan-sword-check/`.
Sau thay đổi này, 7 suite hồi quy cũng PASS (4.200 kiểm tra): IdentitySkills
877, AnimationIntegration 618, MeleeFairness 391, BossCombatFix 937,
PlayerDefense 321, MeleeChainControls 694 và V4VisualIntegration 362.
Tổng kiểm tra cho lượt nâng cấp này: 7.649.
Các kiểm tra tự động không thay cho đánh giá cảm giác chiến đấu khi chơi.

Sau khi cập nhật, đóng cửa sổ game cũ và mở lại bằng `run.bat`; JVM đã mở
giữ cache sprite cũ. `run.bat` chạy bản đã build trong `out/`.

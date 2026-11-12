# Animation V4 trong game

V4 thay hình ảnh và cách chọn pose cho ba nhân vật cùng hai boss. AI, damage,
cooldown, máu, điều khiển, va chạm địa hình và tiến trình chương giữ nguyên.
Các thông số combat vẫn theo `identity-runtime-v3.md` và `melee-fairness.md`.

## Tài nguyên đang tải

- `assets/players/phan_si_kien/v4/`
- `assets/players/ronaldo/v4/`
- `assets/players/do_mixi/v4/`
- `assets/bosses/the_time_guardian/v4/`
- `assets/bosses/the_void_core/v4/`

`Art` chọn thư mục `vN` mới nhất có `animation.properties` và Idle. V2/V3 được
giữ làm dự phòng. Frame count, canvas, pivot, nhịp mặc định và nhóm pose được
đọc từ metadata; render dùng nearest-neighbour.

Nguồn V4 và thông tin tạo lại được lưu trong `art/animations/v4/`. Tool chuẩn
hóa là `tools/PrepareV4Runtime.py`; game không phụ thuộc đường dẫn của bản
thiết kế bên ngoài repository.

## Kích thước, điểm neo và vùng nhận đòn

Hero dùng canvas 192 × 192, pivot chân (96,172), scale 1. Phần tăng so với
canvas V3 là khoảng trong suốt chứa kiếm/VFX; không phóng to thân hoặc collider.
Một tỷ lệ chuẩn hóa cố định được dùng cho toàn bộ pose mới của mỗi actor.
Không thu nhỏ riêng cơ thể khi một clip có hiệu ứng lớn.

Guardian dùng canvas 256 × 256, pivot chân (128,232), scale 2. Core dùng canvas
256 × 256, pivot tâm (128,128), scale 2, offsetY -24. Vùng thân được căn theo
envelope torso đang dùng; kiếm, áo choàng, halo và tia không làm phình hitbox.
Core giữ neo tâm khi bay và khi ảnh bị lật; không căn theo chân của hero.

## Hành động đang dùng

| Đối tượng | Hành động | Clip V4 |
|---|---|---|
| Hero | Đi/chạy, W, rơi, tiếp đất, K, L, S, Hurt, Death | Các clip riêng tương ứng |
| Hero | J → J → J | `light_1` → `heavy` → `launcher` |
| Hero | J khi trên không | `air_attack` |
| Hero | U / I / O | `skill_u` / `skill_i` / `ultimate` |
| Guardian | Chém / bắn / lao chém | `light` / `skill_u` / `skill_i` |
| Core | Đánh gần / volley / vòng xung | `light` / `skill_u` / `ultimate` |
| Core | Báo dịch chuyển / bổ xuống | `skill_i` / `air_attack` |

`attack` và `special` vẫn là tên hành động trong combat, dù clip hình ảnh được
chọn riêng. Vì vậy contact gate, hủy hành động khi bị đánh, cooldown và cửa sổ
sát thương không đổi theo tên clip mới. Bank cũ không có clip riêng sẽ dùng
`attack`/`special` tương ứng.

Core chỉ phát các pose tan biến F1–F4 của `skill_i` trong báo dịch chuyển;
không phát pose tái tạo ở vị trí cũ. Khi vị trí vật lý đổi ở đầu Active,
sprite chuyển sang `air_attack` theo cửa sổ bổ xuống hiện có.

`light_2` và `light_3` giữ artwork V3 được căn lại vào canvas V4, để không làm
mất các pose riêng đã có. Ở bank hero, alias `attack=light_1`, `special=skill_u`
được ghi rõ trong manifest. `new_skill` và `knockdown` có trong bank nhưng chưa thêm nút bấm,
logic counter/laser hoặc hành vi AI mới chỉ vì có ảnh mới.

## Đồng bộ pose với gameplay

Attack vẫn quyết định Startup, Active và Recovery. Metadata V4 chỉ ánh xạ hình
ảnh: F1–F3 lấy đà, F4–F5 ra đòn, F6 hồi thế. Nhờ đó lúc hitbox mở, sprite đã
sang pose đánh thay vì còn ở tư thế lấy đà. Những clip giữ từ V3 có override
nhóm pose cũ. Không tăng/giảm thời gian gây damage để khớp GIF thiết kế.

Riêng Phan U/I/O đã được thay tiếp bằng bộ kiếm 11 frame: F1–F6 lấy đà,
F7–F10 ra đòn, F11 hồi thế, giữ thời lượng chiêu. Xem
[bộ kiếm Phan](phan-sword-skills.md) cho tầm chém mới và VFX va chạm.

W dùng các pose đi lên F3–F6, lấy tiến độ từ vận tốc nhảy. Fall và Land có sáu
pose riêng. Physics không lấy tọa độ từ ảnh; toàn bộ frame được vẽ quanh điểm
neo của entity. Death không loop, giữ pose cuối; pause không tiến animation.

## Kiểm tra và chạy lại

Kết quả kiểm thử và ảnh render thực ở `out/animation-v4-check/` cùng các thư mục
kiểm tra combat hiện có. Các fixture kiểm thử chặn ghi save.

Ở lần tích hợp V4 nền ban đầu, build và 10 bộ kiểm thử dưới đây đều PASS,
tổng 16.559 kiểm tra tự động. Kết quả nâng cấp kiếm sau đó được ghi riêng
trong `phan-sword-skills.md`.

| Bộ kiểm thử | Số kiểm tra |
|---|---:|
| V4VisualIntegrationCheck | 362 |
| IdentitySkillsCheck `--require-v4` | 877 |
| AnimationIntegrationCheck | 618 |
| BossCombatFixCheck | 937 |
| MeleeFairnessCheck | 391 |
| PlayerDefenseCheck | 321 |
| MeleeChainControlsCheck | 694 |
| MenuFlowCheck | 30 |
| ChapterOneMapCheck | 756 |
| ChapterTwoMapCheck | 11.573 |

V4Visual kiểm tra kích thước cũ/mới, neo cả hai hướng, xác Core chạm sàn,
phát đủ pose đi lên, tan biến trước dịch chuyển và chuyển sang Dive đúng Active.
Sáu contact sheet render game thật có đủ ba hero ở hai chapter, J/Heavy/Launcher,
U/I/O, Run và Dash. Các kiểm tra này không thay cho cảm nhận cân bằng khi chơi.

Hai tiện ích map cũ cũng được đổi sang fixture chặn save để kiểm tra kết thúc
trận không ghi file tiến trình; đã đối chiếu hash save trước/sau hai suite.
Debug collision tắt khi hoàn thành.

Build: `build.bat`. Chạy: `run.bat` từ thư mục gốc. Cửa sổ game đã mở trước khi
cập nhật vẫn giữ sprite cache cũ; cần đóng bản đó và chạy lại để tải V4.

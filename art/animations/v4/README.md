# Chrono Runner — animation V4 trong game

Game hiện tải V4 cho ba hero và hai boss. Xem [báo cáo tích hợp](../../../docs/animation-v4-integration.md) cho mapping và kiểm thử. V2/V3 được giữ làm nguồn và dự phòng.

## Nguồn và tạo lại

- `source/`: 10 PNG nguồn và prompt tương ứng, tạo bằng công cụ ảnh tích hợp.
- `metadata/`: số đo nguồn, tọa độ cắt và báo cáo chuẩn hóa runtime.
- `previews/`: contact sheet để rà pose; nền/chữ không nằm trong PNG runtime.
- `assets/players/<id>/v4/` và `assets/bosses/<id>/v4/`: frame alpha, atlas, manifest cùng `animation.properties`.

Từ thư mục gốc repository chạy `python tools/PrepareV4Runtime.py` để đóng gói lại từ nguồn đã lưu; không cần gọi API tạo ảnh. Tool dùng Pillow, NumPy, SciPy; Arial của Windows chỉ dùng cho contact sheet. Game Java không cần Python. Cờ `--import-source <folder>` chỉ dùng khi nhập lại bộ nguồn thiết kế.

PNG nguồn được đo thực tế trước khi cắt: phần lớn 971 × 1619, Core A 793 × 1983. Tool loại nhiễu alpha nhỏ, bỏ ba pixel VFX tại đường chia cột đã xác minh của Core A, ngưỡng alpha 128 thành 0/255 và dùng palette chung 128 màu mỗi actor. PNG nguồn gốc vẫn được giữ nguyên.

## Canvas và chuyển động

Hero dùng 192 × 192, pivot chân (96,172), scale 1, chiều cao thân mục tiêu 64 px. Phần tăng canvas chứa vệt kiếm/VFX; không tăng collider hoặc thu thân theo từng hiệu ứng. Mỗi actor dùng một tỷ lệ resample đồng đều, nearest-neighbour.

Guardian dùng 256 × 256, pivot chân (128,232), scale 2 và căn theo torso đang dùng `[94,138,70,94]`. Core dùng 256 × 256, pivot tâm (128,128), scale 2, offsetY -24. Pose Death cuối Core được căn xuống sàn theo neo tâm, không đổi vật lý entity.

Mỗi actor có 20 nhóm nguồn mới, ban đầu 6 pose/nhóm. Hero có 24 hàng runtime: thêm alias `attack`/`special` và giữ `light_2`/`light_3` riêng từ V3, chỉ thêm padding để đồng bộ pivot. Boss có 22 hàng với alias tương thích. Phan U/I/O và alias `special` hiện có 11 frame, các clip khác giữ 6. Tổng 716 PNG frame actor runtime và 5 atlas; số này có tính alias. Bốn hiệu ứng kiếm/khói có thêm 44 frame riêng trong `assets/effects/phan_sword/v1/`.

Bộ kiếm 11 frame dùng nguồn/prompt riêng ở `art/effects/phan_sword/`.
`PrepareV4Runtime.py` tự áp lại bộ pose này sau khi đóng gói Phan.

J → Heavy → Launcher, Air Attack và U/I/O dùng artwork mới. Boss có clip riêng cho chém, bắn, lao, radial, dịch chuyển và bổ xuống. Metadata căn nhóm pose với Startup/Active/Recovery đang có. W dùng các pose bay F3–F6; Fall/Land có sáu pose riêng. Death giữ frame cuối; pause không tiến animation.

`new_skill` và `knockdown` được lưu nhưng chưa tạo phím/mechanic mới. Không thêm counter, rewind, laser AI, guard AI hoặc nguồn damage chỉ vì có ảnh thiết kế đó.

## Kiểm chứng

`tools/V4VisualIntegrationCheck.java` đối chiếu kích thước hiển thị cũ/mới, neo khi lật hướng, chân/sàn, tâm Core, pose nhảy và Death; chụp game renderer thật cho ba hero ở hai chapter. Các suite animation/combat/skill/map kiểm tra timing hit, điều khiển, pause và kết thúc trận. Ảnh/báo cáo ở `out/animation-v4-check/` cùng các thư mục kiểm thử hiện có; fixture chặn ghi save.

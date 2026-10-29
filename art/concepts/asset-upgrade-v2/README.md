# Chrono Runner — bản mẫu nâng cấp tạo hình v2

Ngày: 29/10/2026. Tạo bằng công cụ image_gen tích hợp, dùng asset hiện tại làm tham chiếu. Đây là **bản xem trước tạo hình**, chưa thay asset hoặc code trong game.

## Các mẫu

| Mẫu | File PNG | Kích thước ảnh thực tế | Hướng nâng cấp |
| --- | --- | --- | --- |
| Phan Sĩ Kiên | [phan-si-kien-v2.png](phan-si-kien-v2.png) | 1254 × 1254 | Giữ tóc tối, áo xanh, khăn đỏ; giáp và thiết bị thời gian rõ hơn, kiếm cyan |
| Ronaldo | [ronaldo-v2.png](ronaldo-v2.png) | 1254 × 1254 | Dáng nhanh nhẹn cùng họ tạo hình; điểm nhấn xanh lá ở thiết bị và trang phục |
| Độ Mixi | [do-mixi-v2.png](do-mixi-v2.png) | 1254 × 1254 | Vai rộng, giáp dày, găng tay lớn và lõi vàng cho vai trò chống chịu/đánh diện rộng |
| The Time Guardian | [time-guardian-v2.png](time-guardian-v2.png) | 1230 × 1278 | Giáp đen tím, áo choàng đỏ, lõi đồng hồ và kiếm vàng cam; các mảng giáp tách rõ |
| The Void Core | [void-core-v2.png](void-core-v2.png) | 1402 × 1122 | Lõi cyan/trắng và tím, vòng năng lượng phía trên, giáp cơ khí tách mảng, dáng bay |

Cả 5 file đều là PNG RGBA, có alpha thực sự trong khoảng 0–255. Không dùng nền caro giả. Đã kiểm tra trực quan đủ cơ thể, kiếm/áo choàng và các mảnh giáp; không có chữ hoặc khung trang trí trong từng mẫu. Một số pixel alpha rất thấp còn ở vùng trống nên bounding box alpha > 0 không đại diện cho phần cơ thể nhìn thấy.

## Tham chiếu

Kiểm tra `Art.loadPlayerAnimations()` cho thấy cả 3 nhân vật hiện cùng dùng bộ `assets/players/chrono_runner/` do chưa có thư mục animation riêng cho từng ID. Hai boss có bộ riêng tại `assets/bosses/the_time_guardian/` và `assets/bosses/the_void_core/`.

- Phan Sĩ Kiên: tham chiếu idle hiện tại và ảnh nguồn `art/sources/legacy/player_sprite_source.png`.
- Ronaldo, Độ Mixi: idle hiện tại để giữ họ tạo hình, kết hợp bản Phan Sĩ Kiên v2 để thống nhất nét vẽ.
- Hai boss: idle của từng boss để giữ nhận diện, kết hợp bản Phan Sĩ Kiên v2 làm tham chiếu phong cách.
- [Toàn bộ prompt](prompts.md). Chế độ: built-in image_gen, `transparent_background=true`.
- [Metadata ảnh](image-metadata.json).

## Phạm vi

Mỗi file là một pose mẫu. Các ảnh này chưa phải sprite sheet animation hoàn chỉnh, chưa có kích thước frame/pivot chung và không được engine nạp. Kích thước ảnh nêu trên là kích thước PNG đầu ra, không phải kích thước frame dự kiến khi dùng trong game. Nếu triển khai tiếp cần chuẩn hóa pixel grid, alpha, frame/pivot và xây dựng các trạng thái chuyển động theo hệ thống hiện có.

Source code và thư mục `assets/` đang được game sử dụng được giữ nguyên; đã đối chiếu SHA-256 với snapshot đầu tác vụ. Không chạy lại build hoặc kiểm thử gameplay vì tác vụ này chỉ tạo bản mẫu đồ họa độc lập.

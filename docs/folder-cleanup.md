# Dọn thư mục — 30/10/2026

## Cập nhật: sắp xếp ảnh PNG ở gốc

Không còn PNG rời tại thư mục gốc. Đã chuyển và kiểm tra SHA-256 không đổi:

- Ảnh main menu → `assets/ui/main_menu.png`.
- Ảnh chọn chapter → `assets/ui/chapter_select.png`.
- Nguồn cắt sprite player → `art/sources/legacy/player_sprite_source.png`.
- Nguồn cắt sprite boss → `art/sources/legacy/boss_sprite_source.png`.

Đã cập nhật loader Java, công cụ Python, manifest và tài liệu liên quan. Hai ảnh nền map ở gốc được xóa sau khi xác minh trùng SHA-256 với `assets/backgrounds/chapter_1.png` và `chapter_2.png`. Hai bản thiết kế boss cũ không còn được tham chiếu (`Dual Boss Pixel Art Sprite Sheets.png`, `Time Guardian and Void Core Sprite Sheets-1.png`) cũng đã xóa. Tổng bốn ảnh xóa: **11.269.929 byte**, khoảng **10,75 MiB**. Asset nguồn mà công cụ còn dùng vẫn được giữ.

Build Java thành công; hai screen thực tế tải được ảnh menu/chapter tại đường dẫn mới; cú pháp hai công cụ Python hợp lệ. Phần ghi chép dưới đây mô tả đợt dọn build/cache trước khi sắp xếp ảnh.

## Dọn build và cache

Đã dọn dữ liệu sinh tự động sau khi kiểm tra tham chiếu từ source và công cụ.

| Nhóm | Xử lý |
|---|---|
| `out/` | Xóa ảnh render kiểm thử, bản build trung gian, class chẩn đoán và bản build cũ; chạy lại `build.bat` để khôi phục class game dùng bởi `run.bat` |
| `tools/__pycache__/` | Xóa cache Python |
| `tools/out/` | Xóa class DumpPlayers có thể biên dịch lại từ source |
| `tools/dump.txt` | Xóa kết quả ASCII có thể tạo lại bằng DumpPlayers |
| 7 script/source nằm lẫn trong `out/` | Giữ nguyên nội dung tại `tools/diagnostics-archive/`, theo cấu trúc thư mục cũ |
| 21 báo cáo kiểm thử không rỗng | Giữ tại `docs/verification/cleanup-2026-10-30/`, theo cấu trúc thư mục cũ |

`out/` giảm từ **195,50 MB / 284 file** còn **0,27 MB / 87 file** sau build lại. Các file mã nguồn và báo cáo được lưu lại ngoài `out/`; dung lượng thu hồi khoảng **195 MB**. Thống kê ở `verification/cleanup-2026-10-30/summary.json`.

Đã đối chiếu SHA-256 của **978 file** thuộc `assets/`, `src/`, `saves/`: không đổi. Build thành công. Không chạy lại các bộ xuất ảnh kiểm thử sau khi dọn, vì chúng sẽ tạo lại dữ liệu vừa xóa.

Các ảnh PNG ở gốc được giữ: menu/chọn chapter tải trực tiếp hai ảnh gốc; một số công cụ cắt sprite dùng các ảnh gốc khác. `art/` chứa nguồn thiết kế, metadata và preview để chỉnh/tạo lại asset, không được xem là cache. `assets/` vẫn có các bank cũ dùng trong chuỗi fallback của loader. Giữ `tools/`, `backups/` và tài liệu để phục vụ sửa lỗi và khôi phục thiết kế.

Các đường dẫn ảnh kiểm thử `out/...` trong tài liệu cũ là vị trí đầu ra có thể tái tạo bằng các công cụ tương ứng trong `tools/`; ảnh không còn được lưu sau đợt dọn này. Source chẩn đoán BossAuditProbe trước ở `out/boss-audit/` nay ở `tools/diagnostics-archive/boss-audit/`.

Đã thêm ignore cho Python cache, `tools/out/` và `tools/dump.txt` để tránh đưa dữ liệu sinh tự động vào Git.

# Phan Sĩ Kiên — bộ kiếm và khói va chạm

Artwork được tạo bằng công cụ tạo ảnh tích hợp (built-in), theo mẫu VFX cyan
người dùng cung cấp và thiết kế Phan hiện có. Không cần API key để tạo bộ này.
Prompt gốc ở `source/*.prompt.txt`; PNG nguồn gốc được giữ nguyên cạnh prompt.

Mỗi animation có 6 key frame và 5 hình vẽ bổ sung, ghép xen kẽ thành 11 frame:
`key1, thêm1, key2, thêm2, …, key5, thêm5, key6`.

| Tài nguyên runtime | Frame | Canvas/frame | Pivot |
|---|---:|---|---|
| Phan `skill_u`, `skill_i`, `ultimate` | 11 mỗi clip | 192×192 | Chân (96,172) |
| `crescent` | 11 | 128×128 | Tâm (64,64) |
| `long_slash` | 11 | 256×128 | Tâm (128,64) |
| `cross_burst` | 11 | 384×256 | Tâm (192,128) |
| `smoke` | 11 | 192×192 | Tâm (96,96) |

Alias Phan `special` dùng cùng 11 frame với `skill_u`. Các animation khác của
Phan và các bank nhân vật/boss khác giữ số frame hiện có.

Nguồn pose cũ 1774×887 (6×3), pose bổ sung 1619×971 (5×3). Nguồn VFX cũ
1536×1024 (6×4), VFX bổ sung 1402×1122 (5×4). Các nguồn có khoảng cách hàng
không đều; không cắt bằng phép chia chiều cao cố định. Số đo và tọa độ cắt thực
tế ở `metadata/`, provenance từng frame ở manifest runtime.

Đóng gói lại từ thư mục gốc:

```powershell
python tools/PrepareSwordFX.py
python tools/PreparePhanSwordEffects.py
```

Các tool chỉ chuẩn hóa kỹ thuật: đo khoảng trống, cắt, lọc alpha ở ngưỡng 128,
palette 128 màu, resample nearest-neighbour theo tỷ lệ đồng đều, căn pivot và
padding trong suốt. Chúng không vẽ thêm pose. Khói được chuyển sang xám trung
tính. Alpha runtime là 0/255 thật, không có nền caro hoặc nhãn.

`PrepareV4Runtime.py` tự áp lại override pose kiếm sau khi xây bank Phan, để
đóng gói lại V4 không đưa các chiêu về hình cũ. Game Java không cần Python.
`pose-preview.png` và `fx-preview.png` chỉ dùng rà hình, không vào gameplay.

Chi tiết gameplay và kiểm thử: `docs/phan-sword-skills.md` ở thư mục gốc dự án.

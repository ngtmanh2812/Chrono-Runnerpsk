# Menu và hướng dẫn chơi

Menu chính gồm: Chơi, Tiếp tục, Cài đặt, Hướng dẫn chơi, Thoát. Đã bỏ mục Chọn nhân vật độc lập và thay Đội ngũ bằng hướng dẫn. Ảnh menu gốc được giữ; các nút được vẽ lại để không còn nhãn cũ.

Chơi mới đi theo luồng **Chơi → Chọn nhân vật → Chọn chapter → Gameplay**. Khi đang chọn Chơi ở menu, dùng trái/phải để đổi 1P/2P. Trong màn chọn nhân vật, P1 dùng A/D, J xác nhận, K chọn lại; P2 dùng trái/phải, Numpad 1 xác nhận, Numpad 2 chọn lại. Chỉ sau khi đủ người xác nhận mới nhấn Enter để chọn chapter. Tiếp tục giữ cách mở màn chọn chapter theo tiến trình đã lưu.

Hướng dẫn có ba trang: phím bấm P1/P2; combo J thường → Heavy → Launcher và U/I/O/năng lượng; mục tiêu hai chapter, boss, thắng/thua và cách bắt đầu trận. Chuyển trang bằng A/D, trái/phải hoặc Tab; Esc/Enter về menu. Nội dung phản ánh bộ phím W nhảy, K dash, J nối ba đòn và damage 30/40/35 đang chạy.

`HowToPlayScreen` thay `CreditsScreen`; state đăng ký là `HOW_TO_PLAY`. Không chỉnh combat hay dữ liệu save.

Kiểm tra: `build.bat`; biên dịch và chạy `tools/MenuFlowCheck.java` ở chế độ headless. Công cụ kiểm tra bằng InputHandler/Game thật, luồng 1P/2P, khóa xác nhận, bảo toàn nhân vật, vào trận, trang hướng dẫn/quay lại, Cài đặt và Tiếp tục; không ghi save. Ảnh kiểm tra ở `out/menu-flow-check/`.

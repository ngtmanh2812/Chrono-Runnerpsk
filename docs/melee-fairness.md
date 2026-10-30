# Cận chiến và tích nội năng — 30/10/2026

Đã sửa ba vấn đề người chơi gặp: đòn thường khó chạm hình boss, bị sát thương thụ động khi áp sát và thiếu cách tích nội năng trong combat. Giữ PNG/animation bank, body di chuyển, map, HP/damage boss, ngưỡng phase và các mẫu chiêu. Cửa sổ đánh boss đã cân lại để có thời gian đọc đòn.

## Vùng nhận đòn và tiếp cận

- Boss dùng hurtbox torso ổn định, độc lập hộp va chạm sàn: Guardian 140×188, Core 176×136 tại bank scale 2. Tọa độ tính từ pivot, anchor, offset và hướng của ảnh thật; không lấy toàn alpha/cape/halo. Các bank không đúng định dạng sẽ dùng body cũ làm fallback.
- Hitbox melee player bám vị trí owner suốt Active, giữ hướng đã cam kết; vùng VFX dùng cùng rect.
- Tầm đánh thường Phan/Ronaldo/Độ: 84/72/96; lực đẩy ngang 100/100/140, không hất lên. Giữ damage 30 và timing riêng của từng hero. Heavy: 40 damage, reach112, X120/Y0; Launcher: 35, reach104, X180/Y-620. Hai đòn đầu giữ mục tiêu gần; finisher mới hất tung.
- Pushbox tách ngang khi hai body chồng nhau gần cùng mặt đất, có giới hạn arena và tránh đẩy ngang khi player nhảy trên/dưới boss.
- Guardian không có body damage thụ động, kể cả dash đã có hitbox riêng. Core chỉ có body damage lúc dive Active; mỗi người trúng/đỡ tối đa một lần mỗi action, không reset khi rời rồi chạm lại. Enemy contact cũ giữ nguyên.

## Combo, chống trúng lặp và phản công

- `CombatHitResult` phân biệt HIT/BLOCK/IMMUNE/NONE. Chỉ HP giảm thực sự mới xác nhận combo và cấp nội năng; overlap đơn thuần không tính trúng.
- Buffer J 12/60 giây. Đòn trúng được nối sau ít nhất 2 frame Recovery; đòn hụt phải hết Recovery. Sau đòn có 0,60 giây để nối. Không tự đánh khi giữ J.
- Boss ghi bước melee đã nhận trên từng token bằng WeakHashMap. Đòn mới từ cả P1/P2 được nhận độc lập blanket iframe 0,55 giây; bước lặp/lùi cùng token bị từ chối. Projectile/skill vẫn dùng gate cũ, player/dash vẫn có iframe cũ.
- Choáng boss dùng timer combat riêng: thường 0,30s, Heavy0,36s, Launcher0,42s; không phụ thuộc độ dài clip Hurt. Giới hạn thực tế 0,90s choáng liên tục rồi có 0,45s kháng choáng. Trong khoảng kháng, boss vẫn mất HP nhưng không bị ngắt action/đẩy/hất lên; nhãn pixel vàng `KHÁNG CHOÁNG` báo cơ hội phản công.
- Khóa điều khiển player hết Hurt0,30s để không khởi động đòn vô hình dưới animation Hurt. Đạn boss đã bắn vẫn còn nguy hiểm.

| Boss | Normal Startup / Active / Recovery | Special Startup / Active / Recovery |
|---|---|---|
| Guardian | 11 / 11 melee hoặc6 ranged / 13 frame | 17 / 21 / 18 |
| Core | 10 / 11 melee hoặc3 ranged / 12 | 18 / 4 burst hoặc60 dive / 18 |

## Nội năng

Thường/Heavy/Launcher trúng thực cộng +5/+7/+10 EN, tổng22 mỗi chuỗi, chặn ở100. Mỗi bước chỉ cấp một lần cho đúng chủ đòn. Không cấp từ đánh hụt, miễn sát thương, hitbox lặp hay U/I/O. Nạp bằng L/Numpad Enter vẫn18 EN/giây; chi phí skill20/30/100 không đổi. Hướng dẫn trong game đã cập nhật.

## Kiểm tra

- `MeleeFairnessCheck`:391 assertion, gồm 6 tình huống áp sát/nối ba đòn với AI và timer mặc định thật, cả ba hero và hai chapter; boss phản công được sau chuỗi. Các fixture cô lập năng lượng/geometry có nhãn riêng, không được coi là live AI.
- Kiểm tra +5/+7/+10, đúng người, giới hạn100, trúng/hụt/trùng/skill; hitbox đi theo cả hai hướng; Core chạm melee từ mặt đất; contact dive được chống lặp theo action.
- Kiểm tra áp lực melee liên tục vẫn cho boss thoát choáng/phản công; thay Hurt clip thành60 giây trong bộ nhớ không khóa gameplay.
- Regression: IdentitySkills703, MeleeChainControls694, PlayerDefense321, BossCombatFix937, Animation590, ChapterOne756, ChapterTwo11573, Overlay372, MenuFlow30 đều qua. Tổng cùng suite mới:16367 assertion. Test headless dùng engine/input/collision production, chặn ghi save; chưa thay cho cảm nhận cân bằng khi người chơi trực tiếp điều khiển.
- Đã chụp/xem debug màu cam cho hurtbox torso và màu vàng cho body; debug OFF sau kiểm tra. Ảnh ở `out/melee-fairness-check/`, có cả cue kháng choáng. Game thường không hiện khung debug.

File chính: `Player.java`, `Character.java`, `Attack.java`, `CombatHitResult.java`, `PlayScreen.java`, ba class hero, ba class boss, `BossHurtGeometry.java`, `CollisionDebug.java`, `HowToPlayScreen.java`. Build bằng `build.bat`, đóng phiên JVM cũ và mở `run.bat` để chơi bản mới.

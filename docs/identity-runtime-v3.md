# Runtime kỹ năng riêng — v3

Đã triển khai ba kỹ năng riêng cho từng hero, năng lượng và animation bank v3. Cập nhật 30/10/2026: **W nhảy/nhảy đôi, K dash; ba lần nhấn J nối đòn thường → Heavy → Launcher**. P2 dùng Numpad 1 cho cùng chuỗi; các phím nhảy/dash của P2 giữ nguyên. Asset, frame, pivot, timing startup/active/recovery, animation boss và AI giữ nguyên.

| Hành động | P1 | P2 |
|---|---|---|
| Di chuyển | A / D | ← / → |
| Nhảy / nhảy đôi | W | Numpad 0 |
| Đòn thường → Heavy → Launcher | J → J → J | Numpad 1 → 1 → 1 |
| Dash | K | Numpad 3 |
| Đỡ một đòn | S | ↓ |
| Nạp năng lượng | Giữ L | Giữ Numpad Enter |
| Ba kỹ năng | U / I / O | Numpad 4 / 5 / 6 |
| Pause | Esc | Esc |

Guard vẫn dùng được cùng dash và kỹ năng như trước, hấp thụ đúng một hit, không mất năng lượng và có cooldown 2 giây.

Các phím ở menu/chọn nhân vật giữ nguyên. Numpad Enter được phân biệt bằng KeyEvent location để chỉ phím đó nạp P2; Enter chính không nạp P2. Trong menu, cả hai Enter vẫn xác nhận như trước. H và Shift không còn là đòn Heavy/dash trong trận; Space không là alias nhảy. Damage chuỗi là **30 / 40 / 35**, tổng **105** nếu cả ba trúng. Heavy giữ S/A/R 14/8/18; Launcher giữ 12/7/18 và lực đẩy lên -620; Light giữ cấu hình riêng của mỗi hero. Không đổi HP, tốc độ, dash hoặc iframe của player.

Mỗi đòn cần một cạnh nhấn riêng; giữ J hoặc auto-repeat bàn phím không tự nối. Có thể đệm một lần nhấn trong 8 frame (60 Hz) cuối đòn để bắt đầu đòn kế sau khi Recovery kết thúc; không cắt ngắn animation. Sau khi một đòn kết thúc, có 0,60 giây để nhấn tiếp. Quá thời gian, hoàn thành Launcher, bị Hurt/chết, dùng dash/block/skill/nạp hoặc kết thúc trận sẽ đặt lại chuỗi. Pause giữ nguyên hành động đang commit nhưng xóa lệnh đệm và bước nối; resume không tự phát Heavy. Đòn đầu trên không vẫn dùng `air_attack`; Heavy/Launcher dùng đúng các clip cũ. `light_2`/`light_3`, hit-confirm cancel, air juggle và guard counter nâng cao vẫn chưa triển khai.

Boss vẫn có iframe sau hit 0,55 giây với ngoại lệ hẹp: bước kế tiếp của **cùng một chuỗi melee đã trúng chính boss đó** được nhận damage. `DamageInfo.MeleeChain` dùng identity riêng cho mỗi chuỗi/player; Boss ghi bước cuối thực sự mất HP. Hitbox lặp, bước nhảy cóc, chuỗi khác, P2/skill không thuộc chuỗi vẫn bị chặn như cũ. Mỗi hitbox tiếp tục nhớ mục tiêu đã trúng. Player không nhận ngoại lệ này. Điều chỉnh này tránh nuốt Heavy khi nối từ đòn thường nhanh mà không bỏ toàn bộ iframe boss hoặc đổi AI.

| Hero | U — 40 damage / 20 EN | I — 45 damage / 30 EN | O — 120 damage / 100 EN |
|---|---|---|---|
| Phan Sĩ Kiên | Chrono Shot: đạn thời gian | Time Slash: chém trước mặt, slow 0.8 giây | Timeline Break: vùng thời gian quanh người, slow 1.2 giây |
| Ronaldo | Power Shot: bóng sút thẳng | Step-over Burst: rê bóng lao tới, hitbox đi cùng người | Siuu Bicycle Kick: cú sút bóng lớn |
| Độ Mixi | Sóng Âm: đạn sóng mic | Đập Bàn: chấn động quanh chân | Đại Hội Tộc: sóng AoE rộng; không sinh enemy |

Mỗi lần cast chỉ tạo một nguồn damage. Sóng/ring/hạt sáng chỉ là hình ảnh. Projectile biến mất sau lần va chạm đầu; mỗi hitbox nhớ mục tiêu đã trúng. U có cooldown 1.4 / 1.1 / 2 giây tương ứng Phan / Ronaldo / Độ; I 3 giây; O 8 giây. Cửa sổ startup/active/recovery (60 Hz) lần lượt là U 8/4/12, I 12/15/18, O 24/12/30 frame. Cooldown bắt đầu khi nhận lệnh, chạy cả lúc recovery, độc lập theo từng ô và từng người chơi.

Năng lượng tối đa 100 và bắt đầu mỗi lượt với 100. Chỉ trừ một lần khi lệnh được chấp nhận; đang cooldown, thiếu năng lượng hoặc còn hành động cam kết sẽ không mất năng lượng. Giữ phím kỹ năng không tự cast lại. Giữ L/Numpad Enter nạp 18 EN/giây khi đứng đất, không di chuyển, không block, không bị hurt và không có hành động khác. Nhảy, di chuyển, đỡ, tấn công, dash, trúng đòn, pause, chết hoặc kết thúc trận đều dừng nạp. Sau khi dừng cần thả rồi bấm lại phím nạp. Restart/chuyển chương tạo Player mới và đặt lại năng lượng/cooldown.

Đòn đang startup bị ngắt sẽ không sinh hitbox muộn; bị đánh/chết hoặc kết thúc trận hủy hitbox đang sở hữu. Projectile đã bắn ra tiếp tục bay như trước; khi kết thúc trận PlayScreen xóa toàn bộ projectile/hitbox. Skill dash dùng cơ chế dash có sẵn, không thêm cancel hay thay chính sách iframe.

`Art` ưu tiên `assets/players/<hero>/v3/animation.properties`, rồi v2, rồi legacy. Các clip mới gồm `light_1`, `light_2`, `light_3`, `heavy`, `launcher`, `air_attack`, `charge`, `skill_u`, `skill_i`, `ultimate`. Clip light/heavy/launcher/air thiếu sẽ mượn `attack`; skill thiếu mượn `special`; charge thiếu mượn `idle`. Chuỗi J dùng `light_1` (hoặc `air_attack`) → `heavy` → `launcher`. Không dùng vị trí/alpha sprite để thay body hitbox 44×62.

Kiểm tra bằng `cmd /c build.bat`, sau đó biên dịch công cụ:

```powershell
javac -encoding UTF-8 -cp out -d out tools/IdentitySkillsCheck.java tools/MeleeChainControlsCheck.java tools/PlayerDefenseCheck.java tools/BossCombatFixCheck.java tools/AnimationIntegrationCheck.java
java '-Djava.awt.headless=true' -cp out IdentitySkillsCheck --require-v3
java '-Djava.awt.headless=true' -cp out MeleeChainControlsCheck
java '-Djava.awt.headless=true' -cp out PlayerDefenseCheck
java '-Djava.awt.headless=true' -cp out BossCombatFixCheck
java '-Djava.awt.headless=true' -cp out AnimationIntegrationCheck
```

`IdentitySkillsCheck` dùng input thật, Player thật và collision PlayScreen cho 3 hero × 2 slot × 2 chương × U/I/O; kiểm tra năng lượng, damage một lần, cooldown, nạp, ngắt, 1P/2P pause/restart/chết/kết thúc và render HUD/chọn nhân vật. Các fixture chặn `saveProgress`, không ghi save. Ảnh và kết quả được xuất vào `out/identity-v3-check/`.

Kiểm tra cập nhật W/K/J: build OK; IdentitySkillsCheck `--require-v3` 703, MeleeChainControlsCheck 692, AnimationIntegrationCheck 590, PlayerDefenseCheck 321, BossCombatFixCheck 571. Kiểm tra map/overlay: ChapterOneMapCheck 756, ChapterTwoMapCheck 11.573, OverlayScreensCheck 372. Tổng 15.578 assertion qua trong harness headless, input và collision production; fixture chặn ghi save. Chain fixture cố định vị trí boss để đo sát thương từng bước, regression boss kiểm tra AI riêng. Runtime bank proof kiểm tra version 3, pivot, đúng số frame và đối chiếu PNG render của mọi clip; ảnh HUD/ba đòn ở `out/identity-v3-check/j-chain-*.png`.

Sau khi build, khởi động lại game qua `run.bat` để JVM mới tải bank v3; Art cache asset cho suốt phiên chạy.

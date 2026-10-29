# Thiết kế combat/combo v3 — bản đề xuất lưu trữ 29/10

> **Cập nhật 30/10/2026:** Bộ phím và các mô tả runtime dưới đây ghi lại phương án trước lần đổi điều khiển mới nhất. Cấu hình đã chạy hiện tại là **W nhảy, K dash, J → J → J = thường → Heavy → Launcher (30/40/35)**; có buffer 8 frame và ngoại lệ iframe giới hạn trong cùng chuỗi melee trên boss. Dùng [identity-runtime-v3.md](identity-runtime-v3.md) làm nguồn chính cho runtime. Các route H/W+H, Light1/2/3, hit-confirm, air juggle và victim budget ở bản đề xuất này không phải cấu hình hiện hành.

Ngày: 29/10/2026. Đối chiếu project Java/Swing hiện tại và bản yêu cầu combo được cung cấp trong phiên làm việc.

**Phạm vi người dùng đã chốt:** K nhảy, H Heavy, W+H Launcher; lượt này chốt luật combo và tích hợp asset/U/I/O. Đã đối chiếu source runtime có Heavy/Launcher đơn lẻ, chín skill, Energy/Charge và loader ưu tiên ba bank v3 đã cài. Chưa triển khai hệ thống nối chuỗi combo. Các luật iframe, hit-confirm cancel, input buffer, ComboManager và juggle bên dưới là **đề xuất cho giai đoạn tiếp theo**, không phải tính năng đã có trong runtime. Kết quả build, kiểm thử tự động và lượt chơi thật được ghi riêng trong báo cáo triển khai; tài liệu này không tự suy ra chúng từ việc source/asset đã có.

Heavy/Launcher đơn lẻ đã được nối cùng đợt asset/skill nhưng không đồng nghĩa Rising Combo hoặc ba-hit chain đã hoạt động. Lượt này không sửa iframe boss, poise hay victim budget. Không báo cáo năm combo hoàn tất khi chưa có ComboManager và kiểm thử nối chuỗi production. Mô tả runtime chi tiết: [identity-runtime-v3.md](identity-runtime-v3.md).

## 1. Nền tảng đã kiểm tra và những gì cần giữ

Game dùng Java2D/Swing, không dùng engine ngoài. `GamePanel` cập nhật cố định 1/60 giây, tối đa 5 bước catch-up; frame render không phải đồng hồ combo. `InputHandler` đã phân biệt cạnh nhấn với phím giữ và chỉ phát cạnh nhấn ở bước update đầu tiên của một lượt render. Có thể tái sử dụng, không cần viết lại vòng lặp hay bàn phím.

`Attack`/`AttackData` đã có Startup → Active → Recovery và `knockbackY` cấu hình riêng. `Attack` hiện giữ `activeHitbox`; cả `reset()` lẫn chuyển Active→Recovery đều hủy hitbox do nó tạo. `Player.interruptAction()` hủy các đòn và skill hitbox đang sở hữu khi Hurt/chết/hết trận, ngăn hitbox xuất hiện muộn sau khi bị ngắt. Projectile đã bắn vẫn tiếp tục lifetime rồi bị hủy khi va chạm; hết trận xóa nguồn damage theo `PlayScreen`. Đây là cleanup đã có, không phải phần còn thiếu của thiết kế tương lai.

`Hitbox` có `owner`, `alreadyHit`, `markHit()` và tùy chọn bám chủ đòn. `PlayerDefense` đã đỡ đúng một hit trong 0,5 giây, hồi 2 giây, không cấp iframe sau khi đỡ. `AnimationController` đã có `restart()` để bắt đầu lại cùng clip ở đòn mới.

Điểm còn thiếu cho combo: `DamageInfo` chỉ mang amount, knockback, cờ `fromPlayer` và slow; chưa mang chủ đòn/ID lần ra đòn. `Projectile` có visual riêng nhưng chưa có `owner`/`sourcePlayerIndex`; không thể từ cờ friendly phân biệt đạn P1/P2 để ghi combo. `Hitbox.owner` hiện có chưa thay thế được attribution và ID xuyên nguồn damage. `PlayScreen.applyDamage()` vẫn chỉ trả boolean biểu thị Block, chưa có `CombatHitResult`. `Character.takeDamage()` vẫn đặt iframe chung 0,55 giây. Boss kế thừa luật này nên các đòn mới hoặc P2 có thể bị nuốt damage. Đây là chính sách giữ nguyên trong lượt này, cần giải quyết theo mục 3 trước giai đoạn true combo; không tăng damage/HP để che vấn đề.

| Nhân vật | Vai trò phải giữ | HP hiện có | Tốc độ chạy | Dash hiện có | Light hiện có: startup/active/recovery |
| --- | --- | ---: | ---: | --- | --- |
| Phan Sĩ Kiên | Cân bằng, kiếm và đánh xa | 110 | 330 | 780; 0,15s; hồi 0,6s | 9/6/11 ticks; damage 30 |
| Ronaldo | Nhanh, HP thấp, áp sát linh hoạt | 85 | 420 | 950; 0,13s; hồi 0,38s | 5/6/7 ticks; damage 30 |
| Độ Mixi | Tank, chậm, tầm đánh rộng | 160 | 250 | 620; 0,18s; hồi 0,9s | 14/10/18 ticks; damage 30 |

Heavy đơn lẻ hiện chung cả ba hero: damage 45, S/A/R **14/8/18**, reach 84, height 68, knockbackX 380 theo hướng, knockbackY -260. Launcher hiện damage 45, S/A/R **12/7/18**, reach 60, height 96, knockbackX 180, knockbackY **-620**. H chọn Heavy; giữ W lúc nhấn H chọn đúng một Launcher. Khi ở trên không, J dùng clip `air_attack` nhưng vẫn dùng damage/timing/hitbox của basicAttack; chưa có Air Combo Manager. J hiện chỉ bắt đầu `light_1`, chưa chuyển sang `light_2`/`light_3` dù bank đã có các clip đó.

Giữ nhảy kép, map/chapter, camera, save, game over/victory và god mode. Không thay HP boss: Guardian 700, Void Core 1600. Giữ ngưỡng phase 70%/40%, hệ số hiện có, chiêu boss và hành vi kết thúc trận đã sửa. Không đưa enemy phụ trở lại qua Ultimate của Độ Mixi.

## 2. Bộ phím đã nối runtime

| Hành động | P1 | P2 |
| --- | --- | --- |
| Trái / phải | A / D | ← / → |
| Hướng lên, dùng làm modifier | W | ↑ |
| Nhảy / nhảy kép | K | Numpad 0 |
| Light | J | Numpad 1 |
| Heavy | H | Numpad 2 |
| Launcher | Giữ W + nhấn H | Giữ ↑ + nhấn Numpad 2 |
| Dash | Shift | Numpad 3 |
| Block một hit | S | ↓ |
| Giữ để nạp nội lực | L | Numpad Enter |
| Skill U / I / O | U / I / O | Numpad 4 / 5 / 6 |
| Pause | Esc | Esc |

K và Numpad 0 là hai phím nhảy riêng, W/↑ không nhảy. Rising Combo trở thành `J → W+H → K → J`; Heavy Finisher thành `J → J → H`; Guard Counter thành `S → J → H`.

`InputHandler.NUMPAD_ENTER` đã tách binding gameplay bằng `KeyEvent.getKeyLocation()`, vì `getKeyCode()` của hai Enter đều có thể là `VK_ENTER`. Enter chính không nạp P2; cả hai Enter vẫn có thể xác nhận menu. K/Numpad0 là binding nhảy, Space không là alias nhảy. Hành vi Numpad khi tắt Num Lock cần kiểm tra trên cửa sổ thật, vì có thể sinh mã điều hướng/Insert. Menu và gameplay dùng context riêng; khi có InputBuffer tương lai, chuyển context cũng phải xóa buffer đó. Hiện chưa có buffer gameplay để xóa.

## 3. Giải pháp (1): iframe boss và chống damage trùng

### 3.1. Tách hai mục đích đang bị gộp

**Chống trùng một lần ra đòn:** do nguồn damage sở hữu. **Miễn damage có chủ đích:** do trạng thái người nhận sở hữu. Một cú chém không được trừ HP mỗi tick Active; hai cú chém riêng hoặc hai người cùng đánh phải được xử lý độc lập.

Đề nghị bỏ iframe 0,55 giây tự động sau mọi hit **chỉ ở boss**, sau khi đã nối đầy đủ ID nguồn damage. Giữ nguyên iframe của player, dash và god mode. Chuyển phase hiện chỉ đổi phase/số; không tự thêm iframe phase. Nếu sau này có transition bất tử, phải dùng trạng thái có tên, thời lượng và VFX riêng, không tái dùng chống trùng đòn.

### 3.2. Định danh nguồn damage

| Trường đề xuất | Quy tắc |
| --- | --- |
| `sourceEntityId` | Entity gây đòn; P1/P2 và mỗi boss có ID ổn định trong trận |
| `sourcePlayerIndex` | 0/1 cho player, không có với địch; dùng HUD và nội lực |
| `attackId` | Loại đòn cấu hình, ví dụ `phan.light_2`; không phải ID một cú đánh |
| `attackInstanceId` | Tạo mới khi một action được commit; tăng đơn điệu trong trận, không tái dùng khi đổi animation |
| `hitGroupId` | 0 cho đòn một hit; chỉ tăng khi một action thật sự được thiết kế nhiều nhịp damage |
| `victimEntityId` | Mục tiêu nhận đòn |

Khóa chống trùng là `(sourceEntityId, attackInstanceId, hitGroupId, victimEntityId)`. P1 và P2 có thể cùng trúng boss trong một tick. Hai hitbox của cùng một nhát chém dùng cùng instance/group nên chỉ gây một hit lên boss. Đổi frame sprite, tạo lại hitbox hoặc đổi hình VFX không được tạo ID mới.

Projectile kế thừa instance/group của lần bắn; một projectile một hit bị hủy ngay khi chạm. Một vùng AoE một nhịp được đánh nhiều mục tiêu, mỗi mục tiêu đúng một lần. Nếu Ultimate có nhiều nhịp, các group phải được khai báo trong dữ liệu và có thời điểm phát cụ thể; không tăng group mỗi tick. Đòn chạm thân dùng contact episode riêng theo cặp nguồn–nạn nhân, kết thúc khi hai body tách; giữ cơ chế Block/contact hiện có.

Giữ `Hitbox.alreadyHit` như lớp chặn cục bộ và thêm ledger ở tầng resolve để chống trùng xuyên nhiều hitbox/projectile cùng nguồn. Ledger được dọn khi toàn bộ nguồn damage của instance đã hết; không dọn ngay khi animation người đánh kết thúc nếu đạn vẫn còn bay. Không dùng một `lastHitSource` đơn lẻ ở boss vì nó sẽ mất dấu khi P1/P2 đánh xen kẽ.

### 3.3. `CombatHitResult` thay boolean/đọc chênh HP rải rác

Đề nghị tầng resolve trả một kết quả duy nhất gồm: `outcome`, `sourceEntityId`, `sourcePlayerIndex`, `attackId`, `attackInstanceId`, `hitGroupId`, `victimEntityId`, `actualDamage`, `hitstunApplied`, `launchApplied`, `killed`, `combatTick` và lý do miễn khống chế nếu có.

| Outcome | Damage / combo / cancel |
| --- | --- |
| `HIT` | Có actualDamage > 0; tăng hit thực tế; có thể mở hit-confirm cancel theo dữ liệu đòn |
| `BLOCKED` | Damage 0; tiêu hao đúng một Block; mở token Guard Counter cho người đỡ; không tăng combo người đánh |
| `DUPLICATE` | Cùng khóa đã xử lý; không damage, hitstop, VFX hit hoặc combo lần nữa |
| `INVULNERABLE` | Player dash/iframe/god mode hoặc miễn damage có chủ đích; không hit-confirm |
| `INVALID` | Trận đã kết thúc, người nhận chết, damage không dương hoặc nguồn không hợp lệ; không tác dụng |

`HIT` vẫn hợp lệ nếu boss kháng launcher/hitstun: damage có thật, nhưng `launchApplied=false` để UI/VFX nói rõ. Không suy ra trúng đòn từ `hurtFlash`, `isStaggered()`, phím bấm hay việc hai hình chữ nhật giao nhau.

Trình tự resolve: kiểm tra combat/nguồn/mục tiêu → chống trùng → miễn damage có chủ đích → Block → tính scaling và trừ HP → áp phản ứng theo profile mục tiêu → ghi ledger/kết quả → phát combo, nội lực và VFX. Hit đã Block cũng được đánh dấu đã tiêu thụ. Để giữ hành vi hiện có của player, hitbox/đạn đã chạm trong iframe cũng tiêu thụ contact của group đó; không đợi iframe kết thúc rồi cùng nguồn tự đánh lại. `INVALID` trước khi có contact hợp lệ không chiếm khóa của một victim khác. Sự kiện từ nguồn damage được phát đúng một lần cho mỗi kết quả, không phát riêng ở cả `takeDamage()` và `PlayScreen`.

## 4. Giải pháp (3): buffer và combo window rõ ràng

### 4.1. Hai đồng hồ, hai khái niệm

**Buffer 8 ticks** chỉ giữ một ý định vừa nhấn để dùng khi hành động hợp lệ. Input được ghi tại tick T có hiệu lực ở T…T+7, hết hạn trước resolve input ở T+8. **Cancel window** là tập tick cụ thể của đòn hiện tại cho phép chuyển sang đòn kế. Buffer dài 8 ticks không có nghĩa mọi đòn được cancel trong 8 ticks bất kỳ.

Mọi thời gian chiến đấu dùng tick 60Hz. Hitstop được đề nghị là dừng combat toàn arena tối đa 6 ticks: đồng hồ action, stun, cooldown, charge và buffer TTL cùng dừng; vẫn đọc cạnh phím vào buffer để nối đòn sau hitstop. Nhiều hit cùng tick lấy hitstop lớn nhất, không cộng dồn. Pause xóa buffer và không nhận lệnh chiến đấu mới; resume không phát lại lệnh cũ.

InputBuffer của mỗi player lưu `{sequence, action, pressedTick, expiresTick, directionSnapshot, consumed}`. Cạnh J/H/K/U/I/O/Shift/S chỉ sinh một intent cho mỗi lần nhấn, giữ phím không sinh cạnh mới. L là hành động giữ; rời trạng thái neutral được phép nạp thì dừng charge. W/↑ chỉ là trạng thái hướng, không tự sinh đòn.

Khi có cạnh Heavy, lấy mẫu hướng hiện giữ trong cùng tick: có W/↑ thì chuẩn hóa thành một `LAUNCHER`, ngược lại là `HEAVY`. Không để lại thêm Heavy hoặc Jump từ cùng tổ hợp; thay đổi W sau lúc nhấn H không sửa intent đã lưu. Ưu tiên khi các cạnh cùng tick: Block → O → I → U → Launcher → Heavy → Light → Jump → Dash. Chỉ chọn một hành động mới; các cạnh thấp hơn cùng tick bị loại để tránh tự phát lại ngoài ý muốn. Di chuyển giữ và modifier hướng vẫn được lấy mẫu bình thường. Thứ tự này không thay thế luật state: Block không thể ngắt một đòn đã commit.

Buffer giữ thứ tự sequence; chỉ tiêu thụ intent khi transition thực sự thành công. Không lướt qua một intent đang chờ để phát các đòn sau nó thành combo tự động. Xóa intent bị cấm vĩnh viễn bởi context mới; intent chờ cancel có thể sống đến TTL. Clear toàn bộ khi chết, bị Hurt/Knockdown, hết trận, đổi chapter, restart, pause hoặc mất focus. Sau khi mất focus, cần nhả rồi nhấn lại phím hành động để tránh key-held bị mắc.

### 4.2. Hit-confirm cancel đã được chọn về mặt thiết kế

Chỉ mở route tấn công khi **đòn hiện tại đã nhận `HIT`**, đúng instance và đúng mục tiêu combo. Đánh hụt, bị Block, trúng iframe hoặc chỉ chạm VFX không mở cancel. Đầu Recovery mới thực thi cancel; input đến sớm trong 8 ticks vẫn được chờ. Kết quả hit được ghi ngay tick resolve, rồi transition diễn ra ở tick update hợp lệ kế tiếp; không chạy thêm startup/active của đòn mới trong cùng tick va chạm.

Đề nghị giữ Light1 theo timing hiện tại; Light2 ban đầu dùng cùng bộ timing riêng của hero. Các tick dưới đây tính từ lúc commit đòn, bắt đầu ở 0, dùng khoảng nửa mở `[bắt đầu, kết thúc)`. Chưa có asset/QA thì không coi bảng là cấu hình cân bằng cuối cùng.

| Hero | Light1/Light2: startup/active/recovery | Active | Window nối từ Light1/Light2 | Các route được mở |
| --- | --- | --- | --- | --- |
| Phan | 9/6/11 | [9,15) | [15,23), 8 ticks | Light1→Light2 hoặc Launcher; Light2→Light3, Heavy hoặc U |
| Ronaldo | 5/6/7 | [5,11) | [11,18), 7 ticks | Như trên; cửa sổ kết thúc cùng đòn vì Recovery hiện chỉ có 7 ticks |
| Độ Mixi | 14/10/18 | [14,24) | [24,34), 10 ticks | Như trên; startup chậm và tầm rộng được giữ |

Một lần nhấn J đã bắt đầu Light1 bị tiêu thụ, không được dùng lại làm Light2. Light3 và Heavy Finisher là điểm kết thúc, không có route quay về Light1. Nếu intent tới sau cancel window nhưng đòn còn Recovery, không biến nó thành một nhát đánh mới ngay khi READY; intent thuộc chain đã hết cửa sổ phải bị loại. Nhấn mới sau READY có thể bắt đầu chain mới.

Ví dụ Phan: J ở tick 0 bắt đầu Light1. Trúng boss ở tick 9, hitstop 3 ticks. J mới được đệm trước tick action 15 không quá 8 combat ticks thì có thể chuyển Light2 khi action tới 15. J giữ từ tick 0 không tạo Light2. Nếu Light1 hụt, J đệm không xuyên qua Recovery để tự động đánh tiếp.

### 4.3. Tuning cho giai đoạn combo tiếp theo — đề xuất

Bảng sau là phương án phân hóa timing theo role cho combo tương lai. **Không phải timing Heavy/Launcher đang chạy:** cấu hình đơn lẻ hiện tại là Heavy 14/8/18 và Launcher 12/7/18 chung cả ba hero, đã ghi tại mục 1. Light2/Light3 và route Counter/Jump cancel chưa có runtime.

| Đòn | Phan S/A/R | Ronaldo S/A/R | Độ Mixi S/A/R | Luật nối |
| --- | --- | --- | --- | --- |
| Light3, damage 30 | 11/6/15 | 7/6/11 | 16/10/22 | Kết thúc; knockback nhẹ |
| Heavy, damage 45 | 15/6/18 | 10/6/14 | 20/10/24 | Kết thúc; hitstop 5 ticks; knockback mạnh |
| Launcher, damage 45 | 13/5/16 | 9/5/13 | 18/8/20 | Chỉ sau `launchApplied=true` mới mở Jump cancel |
| Air Light, damage 30 | 7/5/11 | 5/5/9 | 11/7/15 | Chỉ khi player ở trên không; chịu budget mục tiêu |
| Guard Counter, damage 30 | 6/4/10 | 4/4/8 | 9/6/14 | Sau HIT, 8 ticks đầu Recovery cho phép Heavy |

Hitstun cơ sở đề xuất theo thứ tự Phan/Ronaldo/Độ: Light1/2 = 18/14/28 ticks; Light3 = 18/14/28; Heavy = 22/18/30; Launcher = 36/32/42; Air Light = 14/12/20; Guard Counter = 18/14/26. Đây là giá trị tối đa trước profile/budget boss. Window được phép nối không đảm bảo mọi input ở tick cuối window đều là true combo không có khe phản công; cần đo startup và knockback bằng playtest. `hurtFlash` và clip Hurt chỉ trình bày phản hồi, không được kéo dài hitstun hoặc giữ AI bị khóa ngoài thời gian cấu hình.

Launcher Jump cancel: Phan [18,30), Ronaldo [14,26), Độ [26,38). Chỉ dùng K/Numpad0, không cho giữ W nhảy theo. Jump cancel không hồi lại số lần nhảy còn lại. Mục tiêu miễn hất tung vẫn nhận damage nhưng không mở nhảy theo đặc biệt. Air Light chỉ có thể nối Air Light bằng một cạnh mới trong 8 ticks đầu Recovery, còn budget victim; không tự giữ lơ lửng người đánh.

Dash → Light1 là route di chuyển, không cần hit-confirm: mở sau 6 ticks dash với Phan, 4 với Ronaldo, 8 với Độ, đến cuối dash. Chuyển sang Light phải kết thúc motion dash và iframe dash theo luật hiện có, không cộng kéo dài iframe bằng animation attack. Lệnh Light ngoài cửa sổ chỉ sống 8 ticks; không làm dash tự tìm mục tiêu hoặc teleport.

Block thành công cấp `counterReady` 18 combat ticks cho người đỡ, dùng một lần để bắt đầu Guard Counter bằng J. Hết 18 ticks, bị Hurt, dash, skill, chết hoặc hết trận thì token bị xóa. Block không trúng hit không có token. Không tự mở phản công chỉ vì vừa nhấn S. Heavy sau Counter vẫn cần Counter trúng thật trong window của nó.

## 5. Giải pháp (5): phân trách nhiệm state/combo

Giữ các class đang làm đúng nhiệm vụ; thêm thành phần vào combat, không tạo engine/state layer thứ hai chạy song song.

| Thành phần | Sở hữu | Không tự làm |
| --- | --- | --- |
| `InputHandler` hiện có | Snapshot cạnh/giữ từ AWT, chuẩn hóa vị trí phím cần thiết | Nhận diện combo, trừ nội lực, tạo hitbox |
| `InputBuffer` mới, mỗi Player | Intent và TTL 8 ticks | Sửa state hoặc đọc HP boss |
| `AttackData` mở rộng | ID, damage, S/A/R, hitbox, hitstun/stop, knockback, launcher flag, chi phí, clip, cancel routes | Giữ tiến trình runtime của một cú đánh |
| `Attack` mở rộng | Một attackInstance, elapsed tick, phase, hướng commit, hit group và lifecycle hitbox | Tự suy đoán combo từ phím giữ |
| `CombatStateMachine` mới, mỗi Player | Một state hành động hợp lệ, transition và cleanup | Trừ HP hay tự render HUD |
| `ComboManager` mới, mỗi Player | Route Light1/2/3, hit-confirm, mục tiêu chain, counter token; thống kê HIT của player | Tạo iframe hoặc nhận ownership physics boss |
| `VictimComboState` mới, mỗi mục tiêu | Budget CC/juggle chung tất cả nguồn 1P/2P, damage scaling và recovery grace | Reset theo việc người đánh đổi phím/đổi player |
| Resolve tại `PlayScreen` | Va chạm, chống trùng, gọi nhận damage, `CombatHitResult` và dispatch sự kiện | Phát combo chỉ vì có overlap |
| Profile từng hero | Thông số role, hình dạng hitbox, clip và U/I/O đặc trưng | Copy ba hệ thống combo độc lập |

Các state chính: `IDLE`, `RUN`, `JUMP`, `FALL`, `DASH`, `ATTACK`, `BLOCK`, `HURT`, `KNOCKDOWN`, `SPECIAL`, `ULTIMATE`, `CHARGE`, `DEAD`. `grounded`, số nhảy còn lại và vận tốc thuộc locomotion; `ATTACK` có thể ở trên không. Không cố dùng đồng thời `JUMP` và `ATTACK` như hai chủ hành động cạnh tranh. Clip Air Light được chọn qua context của ATTACK.

Ưu tiên interruption: DEAD/kết quả trận → HURT/KNOCKDOWN thật sự → action đã commit → transition cancel hợp lệ → hành động neutral. Block chỉ bắt đầu khi neutral được phép. ULTIMATE không mặc định bất tử; lấy hit vẫn tuân luật player hiện có trừ khi sau này chiêu được mô tả rõ khác. Hurt hủy attack hiện tại và hitbox bám chủ; projectile đã phóng được xử lý theo lifetime, không bị tái cấp ID.

Mỗi transition có đúng một nơi entry/exit: `exit` đóng hitbox cần đóng và route cũ; `entry` tiêu thụ đúng một intent, trừ energy một lần, tạo instance mới và `anim.restart(clip)` một lần. Không gọi restart mỗi tick. Không trừ energy trong render/active-effect rồi lại trừ lúc bấm phím. Thiếu energy hoặc đang cooldown thì không bắt đầu startup và không tạo source damage.

Tick đề nghị: lấy snapshot input → đưa intent vào buffer → xử lý freeze/context → cập nhật action/timer đang có → chọn tối đa một transition → movement/physics → đồng bộ hurtbox/hitbox → resolve hit → phát kết quả → tính kết thúc trận → cleanup. Giữ quy tắc victory/game over hiện có; khi combat inactive phải xóa intent, cancel và nguồn damage chiến đấu cần tắt ngay.

Damage scaling đề xuất tính trên chain của **victim**, dùng chung P1/P2: hit 1–3 = 100%; hit 4 = 90%; hit 5 = 80%; từ hit 6 giảm tiếp 10 điểm phần trăm/hit, sàn 50%. Làm tròn một lần ở resolve, actualDamage không vượt HP còn lại. Combo UI mỗi player đếm số HIT/actualDamage của người đó; scaling chung không được khiến HUD gán damage P2 cho P1.

## 6. Giải pháp (7): juggle, boss và chống combo vô hạn

### 6.1. Budget theo nạn nhân, áp dụng xuyên 2P

Mỗi victim giữ `chainId`, `chainStartTick`, `lastHitTick`, `airHitCount`, `launchUsed`, `crowdControlTicksUsed` và `recoveryGraceUntil`. Một Launcher thành công bắt đầu `airHitCount=0`; mọi hit gây damage tiếp theo khi victim đang ở AIR_STAGGER đều tăng bộ đếm, bất kể đến từ P1/P2, melee, projectile hay skill.

Đề nghị tối đa **3 hit trên không sau Launcher**, tối đa **120 combat ticks từ lần launch**, và tối đa **60 ticks bị khống chế liên tục** trong một chain boss. Chạm một trong ba ngưỡng phải kết thúc quyền kéo dài CC. Giữ damage hợp lệ của hit chạm ngưỡng, sau đó buộc FALL/RECOVER và xóa mọi route tiếp tục juggle lên victim đó. Launcher thứ hai trong cùng chain gây damage như đòn thường nhưng không làm mới vận tốc hất tung hoặc budget.

Không reset budget khi người đánh đáp đất, dash, nhấn skill, đổi P1/P2, boss đổi phase, hoặc player reset ComboManager. Chỉ kết thúc chain sau khi victim thật sự thoát CC và có **24 ticks liên tục được điều khiển AI/di chuyển mà không bị CC**. Trong 24 ticks đó, boss vẫn nhận damage nhưng kháng thêm stun/launcher/knockback kéo dài; hit damage không làm lại đồng hồ grace. Đây là kháng khống chế, không phải iframe damage. Sau grace mới có chain CC mới.

CC budget còn cần chạy cho chuỗi dưới đất: bỏ iframe boss mà không giới hạn tổng stun sẽ khiến hai người thay phiên Light khóa boss mãi. `crowdControlTicksUsed` tăng theo thời gian boss thật sự bị khóa, không tăng bằng phép cộng hitstun khai báo; không dùng mỗi cú HIT đặt lại đầy hitstun. Khi hết budget, AI hiện có được trả quyền sau recovery đã quy định và có thể dùng các chiêu đang sẵn sàng.

### 6.2. Profile riêng cho hai boss

| Mục tiêu / context | Damage | Hitstun / Launcher / motor |
| --- | --- | --- |
| Guardian idle/run hoặc Recovery | Nhận bình thường | Nhận stagger và Launcher trong budget; gravity + platform collision hiện có |
| Guardian Startup/Active thường | Nhận bình thường | Có thể bị stagger theo budget; phải hủy action/hitbox qua một đường cleanup |
| Guardian Active của Special dash | Nhận bình thường | Đề nghị kháng Launcher/knockback lớn đến khi Active kết thúc; phản hồi `RESIST` ngắn, không cho Hurt clip hứa rằng dash đã bị ngắt |
| Void Core hover/Recovery | Nhận bình thường | Có thể bị air stagger trong budget; motor hover tạm nhường quyền cho combat motion |
| Void Core Active dive | Nhận bình thường | Đề nghị kháng Launcher, giữ surface dive đã cam kết; phản hồi trúng/kháng rõ |
| Boss đang recovery grace | Nhận bình thường | Không kéo dài stun/launch; AI/flight được hoạt động, VFX không giả Hurt kéo dài |

Các hàng kháng Special là lựa chọn thiết kế **chưa thay runtime**. Không được tự tuyên bố boss có armor sẵn; code hiện tại hủy action khi nhận damage và dùng `stun/hurtFlash/clip hurt` để quyết định stagger.

**Void Core flight takeover:** khi vào AIR_STAGGER, snapshot context hover, ngừng điều khiển `vy` về đích hover và ngừng AI áp lại `vx`. Combat motion sở hữu vận tốc trong hitstun, dùng gravity/giới hạn arena phù hợp và budget chung. Kết thúc hitstun/budget thì chuyển RECOVER, blend vận tốc về đích hover trong 12 ticks rồi trả quyền hover. Không đánh dấu `grounded=true` chỉ vì hết stun, không bắt Core phải đứng sàn mới được thoát combo, không kéo về một tọa độ cũ đã ra ngoài arena. Nếu thật sự chạm sàn/bệ hợp lệ, xử lý collision hiện có; sau đó Core vẫn được trở lại hover. Lúc Core đang dive, giữ motor/surface dive theo profile, không chạy hai motor cùng tick.

Guardian bị launch dùng trọng lực hiện có, không đóng băng y suốt hitstun. Ở hit trên không thứ ba, không cộng vận tốc bay lên nữa; loại thành phần đẩy lên và cho rơi có giới hạn. Core kết thúc air chain bằng flight recovery ở vị trí hợp lệ; không ép animation knockdown mặt đất. Khi boss chết, state DEAD thắng mọi takeover; physics/death hiện có vẫn chạy, không tái bật hover/AI.

Pushbox nên tách khỏi hurtbox/VFX, đẩy tách nhẹ ở mặt đất để tránh chồng thân lật hướng. Không sửa toàn bộ kích thước body boss chỉ để khớp halo/áo choàng. Launcher phải hit hurtbox thật; không dùng bounding box của sprite hoặc hiệu ứng kiếm/bóng/mic làm vùng nhận đòn.

## 7. U/I/O, nội lực và bản sắc ba hero

### 7.1. Cấu hình đã đối chiếu source runtime

Các giá trị dưới đây đã được đối chiếu `Player` và ba lớp hero hiện tại; **trạng thái kiểm thử phải được xác nhận ở báo cáo triển khai**, không suy ra từ bảng thiết kế.

| Thuộc tính | Giá trị được chọn cho lượt tích hợp |
| --- | --- |
| Nội lực | Tối đa 100, bắt đầu 100; mỗi player độc lập |
| Charge | Giữ L/NumpadEnter khi neutral và grounded, nạp 18 điểm/giây, dừng khi không còn hợp lệ |
| U | Damage 40, tốn 20; cooldown Phan 1,4s / Ronaldo 1,1s / Độ 2,0s |
| I | Damage 45, tốn 30; cooldown 3,0s |
| O | Damage 120, tốn 100; cooldown 8,0s |

`trySkill()` đã kiểm tra cooldown/energy rồi trừ cost một lần khi `Attack.start()` thành công; cooldown bắt đầu cùng lúc commit. Còn action đang commit thì không nhận skill mới. Skill callback chỉ sinh damage khi tới Active. Giữ L không nạp khi chạy, trên không, dash, Block, Hurt, Attack, Special, Ultimate hoặc đã chết. Charge bắt đầu bằng một cạnh nhấn hợp lệ rồi duy trì bằng giữ phím; sau khi bị dừng cần nhả và nhấn lại, kể cả pause/resume. Không có hồi nội lực thụ động hoặc thưởng nội lực mỗi hit. Restart/chuyển chương tạo Player mới và reset energy/cooldown.

### 7.2. Skill riêng và hình ảnh riêng

| Hero | U | I | O | Ngôn ngữ hình ảnh |
| --- | --- | --- | --- | --- |
| Phan Sĩ Kiên | **Chrono Shot:** đạn năng lượng xa | **Time Slash:** nhát kiếm trước mặt, slow 0,8s | **Timeline Break:** vùng thời gian quanh người, slow 1,2s | Cyan, kiếm, vòng thời gian; giữ thiết kế Phan v2 |
| Ronaldo | **Power Shot:** sút bóng, projectile là quả bóng | **Step-over Burst:** đảo chân rồi lao tới; hitbox bám chuyển động | **Siuu Bicycle Kick:** pose móc bóng, projectile bóng lớn tốc độ cao | Cầu thủ với áo số 7, quần/giày bóng đá; chân và bóng là trọng tâm, không kiếm/giáp |
| Độ Mixi | **Sóng Âm:** mic phát sóng âm | **Đập Bàn:** động tác nện tạo ground shockwave | **Đại Hội Tộc:** sóng âm AoE lớn | Streamer với tai nghe/mic, trang phục đời thường; waveform/camera-ring; không triệu hồi enemy |

Tên chiêu khác phải đi cùng pose, nguồn damage và VFX khác. Không chỉ đổi chuỗi tên của cùng một projectile/kiếm. O của Ronaldo cần dễ đọc tư thế bicycle kick; O của Độ cần vòng sóng âm/mic, không dùng hình lính/nhân vật phụ có thể bị hiểu là enemy triệu hồi. Độ vẫn là tank, Ronaldo vẫn nhanh HP thấp; redesign trang phục không xóa role cơ học.

Runtime hiện mỗi skill sinh đúng một nguồn damage: U là projectile; I là hitbox; O của Phan/Độ là một hitbox, O của Ronaldo là một projectile bóng lớn. Ring/sóng/hạt sáng bổ sung là VFX, không tạo damage riêng. Projectile và `alreadyHit` đang chặn trúng lặp cục bộ; chưa có `hitGroupId`/ledger chung. Khi triển khai ID tương lai, các nguồn này dùng một group mặc định. Chỉ chia O thành nhiều nhịp sau khi định nghĩa damage từng group, scaling và hitstop; không để nhiều hitbox vô tình mỗi cái gây 120.

S/A/R skill hiện đã có ở cả ba hero, tính bằng tick 60Hz: **U 8/4/12**, **I 12/15/18**, **O 24/12/30**. Projectile được phóng một lần ở đầu Active rồi tiếp tục bay sau Active theo lifetime; hitbox skill kết thúc khi rời Active hoặc bị ngắt. O không tự thêm trạng thái bất tử; skill dash của Ronaldo I dùng cơ chế dash/iframe hiện có. Không có hit-confirm cancel sang U/I/O trong lượt này.

## 8. Asset v3 và UI

Đã kiểm tra file cài trong `assets/players/<hero>/v3`: **mỗi hero có 22 animation rows và 124 frame PNG**, thêm một `sprite_sheet.png`; cả ba bank có `manifest.json` và `animation.properties`. Mỗi bank dùng frame 128×128, pivot [64,116], resampling nearest-neighbor. Loader `Art.animationDirectory()` ưu tiên v3 khi có properties, rồi v2, rồi legacy.

Ronaldo và Độ Mixi có **18 state mới × 6 frame**: năm locomotion `idle`, `walk`, `run`, `jump`, `dash`; năm combat `light_1`, `light_2`, `light_3`, `heavy`, `launcher`; năm bổ sung `air_attack`, `block`, `hurt`, `death`, `charge`; ba skill `skill_u`, `skill_i`, `ultimate`. Bốn row tương thích còn lại gồm `attack`, `special` và `fall`/`land` derive; không coi alias/derive là bốn animation mới độc lập. Ronaldo lấy source cầu thủ, Độ lấy source streamer với headset/mic.

Manifest Phan ghi giữ từ v2 các state `idle`, `run`, `jump`, `hurt`, `death`, `walk`, `block`, `dash`, `fall`, `land`; các clip combo/charge/skill mới đã có trong bank v3. Đã có asset `light_2`/`light_3` không đồng nghĩa gameplay đã chuyển đến chúng. Runtime hiện phát `light_1` hoặc `air_attack`, Heavy/Launcher và ba skill như mục 1/7.

Mỗi clip cần pivot chân/tâm cơ thể thống nhất, nền trong suốt, scale nguyên/nearest-neighbor, silhouette rõ ở scale trong game, không có VFX bị cắt mép. Khai báo manifest cho số frame, pivot, frame startup/active/recovery và file nguồn. Không coi một sheet nguồn AI là pack runtime hoàn chỉnh: phải cắt frame, kiểm tra alpha, alignment, kết nối loader và xem animation trong engine.

Tại baseline v2 chỉ có một `attack` và một `special` mỗi hero; hạn chế thiếu clip tách riêng đó đã được bổ sung bằng bank v3 hiện tại. Fallback loader vẫn giữ cho pack thiếu file: light/heavy/launcher/air mượn `attack`, skill mượn `special`, charge mượn `idle`. Không diễn giải fallback là asset mới; nếu phải rơi về fallback cần ghi rõ trong báo cáo asset/QA.

HUD source hiện giữ thanh máu boss/player và thông tin phase, thêm Energy 0–100 và ba ô skill với tên riêng, phím, cost, countdown/bar cooldown và màu sẵn sàng/thiếu điều kiện. P2 hiện nhãn N4/N5/N6; từng ô và từng player có cooldown độc lập. Tên skill dài có rút gọn trên ô. Chưa có icon đồ họa riêng cho từng skill; định hướng bổ sung là vết nứt thời gian cho O Phan, bóng + chân móc cho O Ronaldo, mic + vòng sóng âm cho O Độ. Khác biệt pose/nguồn damage/VFX O đã được nối trong runtime/asset, còn icon là phần trình bày đề xuất.

Sau khi triển khai ComboManager, Combo Counter mới tăng từ `HIT`; hiện `2 HIT`, `3 HIT` và tổng actualDamage, không đếm keypress/Block/duplicate. Phần counter này và token phản công thuộc giai đoạn combo tiếp theo, không nằm trong chứng nhận asset/U/I/O của lượt hiện tại. Trong 2P, mỗi người có số hit/damage riêng và cùng quy tắc scaling victim. Dòng Block và token phản công tương lai phải phân biệt `SẴN SÀNG`, `ĐANG ĐỠ`, `PHẢN CÔNG`, cooldown. Không đặt HUD lên boss hoặc che nhân vật ở sàn đấu.

VFX dùng ít lớp, hit spark nhỏ tại điểm tiếp xúc; Heavy đậm hơn Light; sóng âm, bóng và năng lượng thời gian khác hình. Hitstop dự kiến Light 3 ticks, Heavy 5, Launcher 4, U/I 4, O tối đa 6. Camera shake chỉ phản ánh `HIT`, dùng biên độ giới hạn khi nhiều nguồn trúng cùng tick. `RESIST` chỉ xuất hiện ngắn khi boss nhận damage nhưng kháng CC, không ngụ ý damage bị chặn.

## 9. Phạm vi triển khai và nghiệm thu

Nên nối theo từng bước còn build được: (A) định danh nguồn + CombatHitResult + boss damage gate; (B) buffer/state và Light1/2/3, Heavy; (C) Launcher/air, victim budget và Core takeover; (D) counter/U cancel; (E) nối asset, U/I/O, HUD và VFX. Asset/U/I/O có thể được thực hiện độc lập, nhưng không được coi việc có animation là chứng minh cancel/juggle đã hoạt động.

| Ca kiểm tra bắt buộc | Kết quả mong đợi sau khi triển khai luật tương ứng |
| --- | --- |
| Giữ J quá ba chu kỳ | Chỉ một Light; nhấn/nhả ba lần đúng window mới có ba đòn |
| J→J→J / J→J→H | Ba HIT thực tế; raw 90/105 khi chưa scaling, không thêm đòn thứ tư |
| Một action tạo hai hitbox chồng / tồn tại 10 ticks | Một hit trên cùng victim/group, không trùng VFX hoặc nội lực |
| P1/P2 cùng tick / hai attackInstance kế tiếp | Cả hai damage hợp lệ được nhận, không bị iframe boss 0,55s nuốt |
| Bắn đạn rồi đòn animation kết thúc | Ledger vẫn chống trùng đến khi đạn hết; projectile vẫn có source player đúng |
| Đánh hụt/Block/iframe rồi nhấn J/H/U | Không hit-confirm cancel; không tăng combo |
| Input ở TTL T+7/T+8 và mép window | T+7 còn hiệu lực; T+8 hết; window nửa mở áp dụng đúng |
| W+H / ↑+Numpad2 cùng tick | Một Launcher, không Heavy và không Jump riêng |
| Launcher→K→J | Chỉ nhảy theo khi launch thành công; vẫn tiêu thụ lượt nhảy |
| 2P thay phiên air hit, player đáp rồi đánh lại | Budget victim không reset; hit thứ ba chấm dứt quyền kéo dài juggle |
| Hai player Light liên tục dưới đất | Boss thoát CC theo budget/grace và phản công bằng AI hiện có |
| Guardian Active Special / Core Active dive | Damage vẫn đúng; CC theo profile, không hai motor/vùng damage mâu thuẫn |
| Core hết air stun giữa trời/chết giữa air stun | Trả hover có blend hoặc death physics; không teleport/đứng sàn giả |
| Block thành công→J→H / Block hụt | Counter chỉ có token sau Block thật; Block vẫn một hit, damage0 |
| U/I/O đủ/thiếu năng lượng và cooldown | Trừ cost một lần khi commit; không cast lén, không damage nhiều group ngoài cấu hình |
| Charge khi đứng/chạy/air/pause/hết trận | Chỉ nạp lúc neutral grounded trong combat; freeze/context đúng |
| 3 hero × P1/P2 | Role, hotkey, tên/icon O, damage attribution và cooldown độc lập |
| Pause/resume, focus lost, restart, đổi chapter | Không phát lại input cũ; không còn hitbox/counter/combo từ trận trước |
| Boss phase và save | HP/ngưỡng phase/chiêu và tiến trình giữ đúng; test không ghi đè save người dùng |
| Sprite trong engine | Pivot ổn định; hitbox khớp Active; nearest-neighbor; không frame rỗng/cắt mép |

Các file kiểm tra hiện có như `PlayerDefenseCheck`, `BossCombatFixCheck`, `AnimationIntegrationCheck`, `ChapterOneMapCheck`, `ChapterTwoMapCheck` là điểm hồi quy hữu ích. Bổ sung kiểm thử production cho ID trùng, boundary ticks, co-op và takeover; không dùng một mock combo riêng để chứng minh runtime. Cần một lượt bàn phím trong cửa sổ thật để xác minh Num Lock/NumpadEnter và cảm giác cancel. Báo cáo cuối phải tách rõ: đã build, test tự động đã chạy, kiểm tra hình ảnh, lượt chơi thật và các phần còn ở mức đề xuất.

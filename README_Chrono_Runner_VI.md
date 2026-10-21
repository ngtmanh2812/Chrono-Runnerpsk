# CHRONO RUNNER

> **Game hành động đi cảnh 2D phong cách pixel art về một người du hành thời gian mắc kẹt giữa Quá Khứ và Tương Lai.**

Chrono Runner là dự án game Java theo hướng lập trình hướng đối tượng (OOP), tập trung vào di chuyển đi cảnh, chiến đấu thời gian thực, đối đầu boss và các năng lực thao túng thời gian. Dự án được xây dựng xoay quanh **hai chương chính** có phong cách gameplay khác biệt rõ rệt:

- **Chương 01 — Quá Khứ:** chiến đấu cận chiến có tính toán, khám phá, canh thời điểm và phản công.
- **Chương 02 — Tương Lai:** di chuyển tốc độ cao, kẻ địch công nghệ, chướng ngại tầm xa và chiến đấu kết hợp công nghệ.

Hai chương dùng chung nền tảng nhân vật và chiến đấu, nhưng mỗi chương có môi trường, hành vi kẻ địch, cơ chế boss và năng lực thời gian riêng.

> **Trạng thái dự án:** README mô tả định hướng và thiết kế đã thống nhất. Không nên coi một tính năng được nhắc đến là đã triển khai cho đến khi tính năng đó được hoàn thành và kiểm thử trong dự án.

---

## Mục lục

- [Tầm nhìn dự án](#tầm-nhìn-dự-án)
- [Cốt truyện và bối cảnh](#cốt-truyện-và-bối-cảnh)
- [Nguyên tắc thiết kế cốt lõi](#nguyên-tắc-thiết-kế-cốt-lõi)
- [Thiết kế chương](#thiết-kế-chương)
- [Vòng lặp chơi](#vòng-lặp-chơi)
- [Luồng game và các màn hình](#luồng-game-và-các-màn-hình)
- [Điều khiển](#điều-khiển)
- [OOP và kiến trúc hệ thống](#oop-và-kiến-trúc-hệ-thống)
- [Công nghệ sử dụng](#công-nghệ-sử-dụng)
- [Cấu trúc dự án](#cấu-trúc-dự-án)
- [Lộ trình phát triển](#lộ-trình-phát-triển)
- [Quy trình làm việc nhóm](#quy-trình-làm-việc-nhóm)
- [Kiểm thử và tiêu chí nghiệm thu](#kiểm-thử-và-tiêu-chí-nghiệm-thu)
- [Biên dịch và chạy game](#biên-dịch-và-chạy-game)
- [Quy ước tài nguyên và giao diện](#quy-ước-tài-nguyên-và-giao-diện)
- [Quy ước Git và đóng góp](#quy-ước-git-và-đóng-góp)
- [Các quyết định thiết kế hiện tại](#các-quyết-định-thiết-kế-hiện-tại)

---

## Tầm nhìn dự án

Chrono Runner hướng đến trải nghiệm game hành động đi cảnh gọn gàng nhưng đáng nhớ. Thay vì dàn trải phát triển trên năm chương có nhiều điểm tương đồng, game tập trung vào **hai thời đại có chiều sâu và cơ chế chơi khác biệt**.

### Mục tiêu thiết kế

1. **Hai chương, hai phong cách chơi.** Quá Khứ và Tương Lai phải khác nhau về di chuyển, nhịp chiến đấu, thiết kế kẻ địch, chướng ngại và trận đấu boss.
2. **Một game thống nhất.** Hai chương dùng chung mô hình nhân vật, quy tắc máu và sát thương, hệ thống lưu game và ngôn ngữ giao diện khi phù hợp.
3. **Tiến trình tập trung vào chiến đấu.** Người chơi vượt địa hình, đánh bại kẻ địch, đối đầu boss phụ khi phù hợp và chiến đấu với boss cuối chương; không phát triển theo danh sách nhiệm vụ rời rạc hoặc chuỗi câu đố.
4. **Hình ảnh pixel art dễ đọc.** Sử dụng font pixel, sprite sắc nét, hoạt ảnh được kiểm soát tốt và giao diện thống nhất với xanh navy đậm cùng cyan neon.
5. **Mã OOP dễ bảo trì.** Tách luật gameplay, thực thể, dữ liệu màn chơi, giao diện, đầu vào và lưu trữ thay vì dồn mọi thứ vào một lớp lớn.

### Scope

**Trong phạm vi dự án**
- 2D side-scrolling/platforming gameplay.
- Single-player as the baseline experience.
- Player movement, combat, health, damage, enemy AI, and boss fights.
- Two chapters: Quá Khứ and Tương Lai.
- Chapter selection, settings, pause, victory, game-over, and save/progress flow.
- Pixel-art assets, animations, sound effects, and visual effects as they are produced.

**Không mặc định nằm trong phạm vi**
- Online multiplayer or networking.
- Five separate chapters.
- A separate “Chapter Gameplay” screen between chapter selection and gameplay.
- Any feature that has not yet been verified in the source project.

Nếu mã nguồn hiện tại có hành vi khác tài liệu này, hãy ghi nhận khác biệt và thống nhất với cả nhóm trước khi tái cấu trúc.

---

## Cốt truyện và bối cảnh

Nhân vật chính là một người du hành thời gian bị mắc kẹt trong dòng thời gian đứt gãy khi đang tìm đường trở về nhà. Một biến cố đã kết nối hai thời đại vốn không nên chồng lấn. Để tìm ra nguyên nhân và tìm đường trở về, nhân vật phải đối mặt với những thế lực kiểm soát từng thời đại.

Cốt truyện được kể thông qua môi trường, phần mở đầu chương, các cuộc chạm trán với kẻ địch, trận đấu boss và những đoạn chuyển cảnh ngắn. Cốt truyện cần hỗ trợ hành động thay vì làm gián đoạn nhịp chơi bằng những đoạn giải thích dài.

### Cấu trúc cốt truyện

1. **Quá Khứ:** Khám phá nguồn gốc của vết nứt thời gian và đối đầu với kẻ canh giữ bí mật của thế giới cổ đại.
2. **Tương Lai:** Bước vào thế giới công nghệ tiên tiến chịu ảnh hưởng từ vết nứt thời gian và đối đầu với trí tuệ nhân tạo hoặc hệ thống đang kiểm soát thế giới đó.
3. **Kết thúc:** Kết nối sự kiện của hai chương và giải quyết hành trình trở về nhà của nhân vật chính. Phần kết cụ thể sẽ được chốt sau khi thống nhất boss và các chi tiết cốt truyện.

Tên boss và nhân vật phụ bên dưới là tên tạm dùng trong thiết kế; nếu thay đổi, cần cập nhật đồng bộ trong mã nguồn, giao diện và tài nguyên.

---

## Nguyên tắc thiết kế cốt lõi

### Nền tảng dùng chung

Các hệ thống sau nên được tái sử dụng ở cả hai chương:

- Player health, damage, hit detection, and invulnerability rules.
- Core movement and collision handling.
- Animation state management.
- Common enemy and boss foundations.
- Damage and attack interfaces.
- Camera and level-transition behavior.
- HUD, pause, victory, and game-over flows.
- Settings and save/progress management.

### Bản sắc riêng của từng chương

Mỗi chương nên bổ sung hành vi riêng thông qua cấu hình, composition và các lớp chuyên biệt, thay vì sao chép toàn bộ bộ máy game. Ví dụ, cả hai chương có thể dùng chung cấu trúc cơ bản của kẻ địch nhưng định nghĩa cách di chuyển, đòn đánh và trạng thái AI khác nhau.

---

## Thiết kế chương

## Chương 01 — Quá Khứ

**Đặc trưng:** Chiến đấu cận chiến có tính toán, tập trung vào thời điểm ra đòn, vị trí và phản công.

### Bối cảnh và định hướng hình ảnh

- Ancient or abandoned city ruins.
- Broken clockwork structures and remnants of an early time-manipulation technology.
- Warm sunset, ember orange, muted red, and deep shadows, contrasted with the game's cyan time-energy effects.
- Platforms, collapsed structures, gaps, and environmental hazards.

### Lối chơi

- Standard platforming: run, jump, land, and avoid hazards.
- Sword-based melee combat.
- Dodging, reading enemy attack cues, and counterattacking during openings.
- A time-slowing ability that creates a brief opportunity to evade or punish an enemy.
- Enemies designed around readable attack patterns rather than overwhelming projectile volume.

### Vai trò kẻ địch đề xuất

| Enemy role | Behavior purpose |
|---|---|
| Melee fighter | Teaches spacing, dodging, and counterattacks. |
| Heavy guardian | Slow, high-impact attacks with clear recovery windows. |
| Ranged sentinel | Encourages movement and closing distance. |
| Elite guard | Combines familiar attacks to test mastery. |

Đây là các vai trò được đề xuất trong thiết kế, không có nghĩa các kẻ địch tương ứng đã được triển khai.

### Boss chương — The Time Guardian (Vệ Thần Thời Gian)

**Định hướng trận đấu:** Một kẻ canh giữ kiểm tra khả năng đọc hoạt ảnh tấn công và sử dụng làm chậm thời gian đúng thời điểm.

Các giai đoạn trận đấu có thể gồm:
1. **Guarded phase:** Deliberate melee attacks and clear recovery windows.
2. **Pressure phase:** Faster combinations and a larger threat area.
3. **Final phase:** A short, high-pressure sequence that rewards accurate dodging and counterattacks.

Đòn đánh báo trước cần dễ nhận biết; không thiết kế trận đấu phụ thuộc vào sát thương không thể tránh.

### Điều kiện hoàn thành

Đánh bại boss chương để kích hoạt trạng thái hoàn thành. Lưu kết quả và mở khóa Chương 02 theo quy tắc tiến trình đã thống nhất.

---

## Chương 02 — Tương Lai

**Đặc trưng:** Di chuyển tốc độ cao và chiến đấu dưới áp lực từ máy móc tiên tiến cùng các mối nguy tầm xa.

### Bối cảnh và định hướng hình ảnh

- A futuristic city, elevated platforms, industrial facilities, and automated defenses.
- Drones, robots, energy barriers, lasers, and machine-controlled spaces.
- Deep navy, electric blue, violet, and cyan neon.
- More vertical routes and movement challenges than the Past, while preserving fair platforming.

### Lối chơi

- Reuse the baseline movement system, then add a controlled high-speed ability such as a dash.
- Combine sword combat with technology-themed attacks and enemy hazards.
- Avoid projectiles, laser lines, turrets, and moving platforms.
- Use a future-specific time ability—such as a short-range temporal blink or time echo—if it proves fun and technically manageable.
- Reward movement and quick decisions without making every encounter a screen full of projectiles.

**Scope guard:** Choose one signature Future ability first. Do not implement blink, clone, ranged weapons, hacking, and multiple new movement systems simultaneously unless time and testing capacity support them.

### Vai trò kẻ địch đề xuất

| Enemy role | Behavior purpose |
|---|---|
| Scout drone | Tracks the player and pressures them to keep moving. |
| Turret | Controls a lane with telegraphed ranged attacks. |
| Heavy robot | Forces the player to combine positioning and attack timing. |
| Elite machine | Combines ranged pressure with close-range defense. |

Đây là các vai trò được đề xuất trong thiết kế, không có nghĩa các kẻ địch tương ứng đã được triển khai.

### Boss chương — The Void Core (Lõi Hư Vô)

**Định hướng trận đấu:** Một trí tuệ nhân tạo kiểm soát đấu trường, buộc người chơi liên tục di chuyển, tìm đường an toàn và tấn công khi có cơ hội.

Các giai đoạn trận đấu có thể gồm:
1. **Arena-control phase:** Telegraphs lasers or ranged attacks.
2. **Pursuit phase:** Adds moving hazards or drone support.
3. **Core-exposure phase:** Creates a clear, limited opportunity for the player to deal damage.

Boss cần kiểm tra kỹ năng di chuyển và phong cách chiến đấu đặc trưng của chương Tương Lai, thay vì chỉ có nhiều máu hơn boss Quá Khứ.

### Điều kiện hoàn thành

Đánh bại boss cuối, phát phần kết và lưu trạng thái hoàn thành game. Cần thống nhất quy tắc mở khóa và chơi lại trước khi triển khai hệ thống tiến trình.

---

## Vòng lặp chơi

**Không có màn hình trung gian “Chapter Gameplay”.** Chọn một chương đã mở khóa sẽ vào thẳng trận đấu của chương đó.

```text
Main Menu
   |
   v
CHƠI
   |
   v
Chapter Select
   |---------------------------|
   v                           v
Quá Khứ                     Tương Lai
   |                           |
   v                           v
Chapter boss
   |                           |
   v                           v
Chapter result / story transition / save progress
```

Chương Tương Lai có thể bị khóa cho đến khi người chơi hoàn thành Quá Khứ nếu nhóm chọn tiến trình cốt truyện tuyến tính. Giao diện chọn chương phải thể hiện rõ các trạng thái: bị khóa, đã mở khóa, đang chọn, đã hoàn thành và đang chơi.

---

## Luồng game và các màn hình

### Menu chính

Menu chính định hình phong cách hình ảnh của Chrono Runner.

Các mục hiện tại:
- **CHƠI** — bắt đầu lượt mới và mở màn chọn chương.
- **TIẾP TỤC** — mở màn chọn chương tại chương cao nhất đã mở khóa.
- **CHỌN NHÂN VẬT** — tùy chọn nhân vật trước khi vào màn chọn chương.
- **CÀI ĐẶT** — audio, display, and control options supported by the game.
- **ĐỘI NGŨ / CREDITS** — project contributors and asset credits.
- **THOÁT** — close the game.

Màn chọn chương xác nhận chương rồi vào gameplay trực tiếp. Chọn nhân vật là một nhánh tùy chọn từ menu chính; xác nhận nhân vật sẽ đưa người chơi về màn chọn chương.

### Màn hình chọn chương

Hiển thị chính xác hai thẻ chương:

| Chapter | Visual identity | Status |
|---|---|---|
| **QUÁ KHỨ** | Warm ruins, broken clockwork, ember-orange sky | Mở từ đầu |
| **TƯƠNG LAI** | Neon city, machines, electric-blue energy | Mở sau khi hoàn thành Quá Khứ |

Mỗi thẻ có thể hiển thị:
- Chapter number and title.
- A chapter-specific preview image.
- A short narrative description.
- Boss identity.
- Completion state or progress.
- A lock indicator where relevant.

### HUD trong game

HUD cần dễ đọc và thống nhất:
- Player health.
- Ability cooldown or energy state, if applicable.
- Boss health during boss encounters.
- Optional interaction prompts only when needed.

Tránh để các bảng trang trí che khuất chướng ngại hoặc diễn biến chiến đấu.

### Menu tạm dừng

- Resume.
- Restart current level or encounter, if supported.
- Settings.
- Return to Main Menu, with a confirmation if progress could be lost.

### Chiến thắng / Hoàn thành chương

- Show the result clearly.
- Save progress.
- Offer Continue, Replay, or Main Menu according to the final progression design.

### Thua cuộc

- Retry.
- Return to Main Menu.
- Preserve previously saved progress.

---

## Điều khiển

Phím điều khiển chính thức phải khớp với phần triển khai và được hiển thị trong Cài đặt. Bảng dưới đây là **cấu hình mặc định đề xuất**, chưa phải danh sách phím đã xác minh trong mã nguồn.

| Action | Proposed key |
|---|---|
| Move left / right | A / D or Left / Right |
| Jump | Space |
| Attack | J or left mouse button |
| Dodge / dash | Shift |
| Chapter-specific time ability | K or another configurable key |
| Pause | Esc |
| Confirm / select | Enter |
| Navigate menus | Arrow keys / WASD |

Controls should be centralized in the input layer or configurable input map, not hard-coded independently into every gameplay class.

---

## OOP và kiến trúc hệ thống

Mục tiêu là phân chia trách nhiệm rõ ràng và giúp dễ mở rộng gameplay riêng của từng chương.

### Phân chia trách nhiệm đề xuất

| Component | Responsibility |
|---|---|
| `Game` / game controller | Owns the main game state and coordinates updates/transitions. |
| `SceneManager` or equivalent | Switches between menu, chapter, pause, victory, and game-over states. |
| `InputHandler` | Reads input and translates it into game actions. |
| `Player` | Player state, movement requests, health, and combat coordination. |
| `Physics` / collision system | Movement integration, collision checks, and ground detection. |
| `Entity` | Shared foundation for entities, only if useful to the current codebase. |
| `Enemy` | Common enemy state and combat behavior. |
| Chapter-specific enemy classes | Special movement, attack patterns, and AI behavior. |
| `Boss` | Boss health, phases, attack selection, and phase transitions. |
| Chapter-specific boss classes | Past and Future boss patterns. |
| `Level` / level data | Platforms, hazards, spawn points, and encounter configuration. |
| `HUD` | Health bars, ability status, and boss health display. |
| `SaveManager` | Saved progress, unlocked chapters, and settings where appropriate. |
| `AssetManager` or equivalent | Loads and manages sprites, sounds, and other resources. |

Tên lớp ở trên chỉ là gợi ý kiến trúc. Hãy giữ tên lớp hiện có nếu chúng đã đảm nhiệm đúng trách nhiệm; không tạo lớp trùng lặp chỉ để khớp với bảng.

### Các nguyên lý OOP

- **Encapsulation:** keep mutable state private or protected behind clear methods.
- **Abstraction:** define small contracts for common behavior when multiple implementations need them.
- **Inheritance:** use only where there is a genuine shared “is-a” relationship, such as a specialized boss type.
- **Polymorphism:** let different enemies or bosses implement their own attack/update behavior through a shared interface or base type.
- **Composition:** assemble abilities, health, attacks, and chapter rules from components where this avoids deep inheritance.
- **Single responsibility:** keep input, rendering, physics, combat, UI, and saving separate where practical.

### Hướng kiến trúc cho sự khác biệt giữa hai chương

Ưu tiên nền tảng chiến đấu dùng chung kết hợp với hành vi riêng của từng chương. Ví dụ:

- Both chapters can share `Health`, damage handling, and hitbox rules.
- The Past can use a `TimeSlowAbility`.
- The Future can use one selected signature ability, such as `TemporalDash` or `TimeEcho`.
- Enemy attack patterns should be data-driven or separated into focused behavior classes where this simplifies testing.

Tránh tạo bản sao riêng của toàn bộ `Player`, hệ thống vật lý, HUD và lưu game cho từng chương.

---

## Công nghệ sử dụng

Cấu hình công nghệ của repository:

- **Language:** Java.
- **Runtime/UI:** Java 2D (`Graphics2D`) và Swing.
- **JDK:** Java.
- **Art direction:** 2D pixel art.
- **Version control:** Git and GitHub.

Game chạy như ứng dụng Java độc lập, không phụ thuộc Godot, Godot-JVM hoặc Gradle. Dùng `build.bat` và `run.bat` trong repository trên Windows.

---

## Cấu trúc dự án

Cấu trúc Java hiện có được chia theo trách nhiệm. Tiếp tục dùng các package này thay vì tạo scene Godot riêng:

```text
Chrono-Runner/
├── assets/players/                 # sprite animations
├── src/chronorunner/
│   ├── core/                        # game loop, state, input, camera
│   ├── combat/                      # attacks, hitboxes, damage
│   ├── entity/                      # player, enemies, bosses
│   ├── obstacle/                    # platforms and obstacles
│   ├── ui/                          # menus, chapter select, HUD, screens
│   ├── util/                        # save, collision, geometry
│   └── world/                       # chapter data and level construction
├── saves/
├── tools/
├── build.bat
└── run.bat
```

Các màn hình được quản lý qua `GameState` và `Screen`; không cần thư mục scene riêng.

### Quy ước đặt tên tài nguyên

Use clear, consistent names, for example:

```text
player_idle.png
player_run.png
player_attack.png
past_background_01.png
future_background_01.png
past_boss_idle.png
future_boss_idle.png
ui_chapter_past.png
ui_chapter_future.png
```

Use the same naming pattern for animation states and keep source files separate from generated exports where possible.

---

## Lộ trình phát triển

Trước tiên hãy triển khai phiên bản nhỏ nhất nhưng hoàn chỉnh và có thể kiểm thử. Không nên phát triển toàn bộ nội dung của cả hai chương song song trước khi nền tảng dùng chung ổn định.

### Giai đoạn 1 — Chốt thiết kế

- [ ] Confirm the two-chapter scope.
- [ ] Finalize the signature ability for each chapter.
- [ ] Agree on the chapter boss concepts and progression rule.
- [ ] Confirm controls and the main navigation flow.
- [ ] Record decisions in `docs/GAME_DESIGN.md`.

**Tiêu chí hoàn thành:** tất cả thành viên thống nhất tên chương, mục tiêu gameplay, luồng màn hình và phạm vi dự án.

### Giai đoạn 2 — Ổn định nền tảng dùng chung

- [ ] Verify player movement and collision.
- [ ] Verify health, damage, hitboxes, and attack timing.
- [ ] Verify animation transitions.
- [ ] Verify camera and level transitions.
- [ ] Verify pause, victory, game-over, and restart flows.
- [ ] Verify that existing save behavior is understood before modifying it.

**Tiêu chí hoàn thành:** có thể chơi hết một màn thử nghiệm nhỏ nhiều lần mà không gặp lỗi nghiêm trọng.

### Giai đoạn 3 — Phát triển Quá Khứ

- [ ] Create the Past level layout and environment.
- [ ] Implement and test the time-slowing ability.
- [ ] Add a small set of distinct enemy behaviors.
- [ ] Build the Past boss with readable phases.
- [ ] Add chapter completion and progress saving.

**Tiêu chí hoàn thành:** có thể chơi Quá Khứ từ lúc bắt đầu đến khi đánh bại boss và hoàn thành chương.

### Giai đoạn 4 — Phát triển Tương Lai

- [ ] Create the Future level layout and visual identity.
- [ ] Implement one Future signature ability.
- [ ] Add ranged hazards and a small set of machine enemies.
- [ ] Build the Future boss around movement and arena control.
- [ ] Connect the chapter to the agreed progression and ending flow.

**Tiêu chí hoàn thành:** Tương Lai có nhịp chiến đấu khác biệt rõ ràng và có thể hoàn thành ổn định.

### Giai đoạn 5 — Hoàn thiện giao diện, hình ảnh và âm thanh

- [ ] Match Chapter Select to the Main Menu's pixel-font and neon visual style.
- [ ] Show the two chapter cards, previews, boss information, and lock/completion states.
- [ ] Add UI focus/hover/pressed states and keyboard navigation.
- [ ] Add sound and visual feedback for combat and time abilities.
- [ ] Check readability and performance at the target resolution.

**Tiêu chí hoàn thành:** điều hướng và phản hồi nhất quán trong toàn bộ game.

### Giai đoạn 6 — Kiểm thử và phát hành

- [ ] Run the regression checklist.
- [ ] Test save/load and chapter unlocking.
- [ ] Test window resizing and the target display mode.
- [ ] Check missing assets, audio, and runtime errors.
- [ ] Prepare a release build and update the README with verified run instructions.

**Tiêu chí hoàn thành:** người chơi mới có thể mở game, chơi, hoàn thành các chương và hiểu cách điều khiển mà không cần nhà phát triển hướng dẫn trực tiếp.

---

## Quy trình làm việc nhóm

Với nhóm bốn người, hãy phân chia trách nhiệm theo hệ thống và sản phẩm bàn giao, đồng thời thống nhất giao diện kết nối giữa các phần từ trước. Điều chỉnh phân công theo thế mạnh thực tế của nhóm.

| Role | Primary ownership | Coordination points |
|---|---|---|
| **Member 1 — Core & Integration** | Game flow, shared state, scene transitions, integration, release stability | Defines interfaces and reviews cross-system changes. |
| **Member 2 — Player & Combat** | Player controls, movement, attacks, health, shared combat behavior | Coordinates hitboxes, animations, and enemy damage rules. |
| **Member 3 — Chapter Quá Khứ** | Past level, chapter-specific ability, Past enemies, Past boss | Reuses shared combat contracts; avoids modifying core systems without agreement. |
| **Member 4 — Chapter Tương Lai & UI/Assets** | Future level, Future ability/enemies/boss, chapter selection and UI integration | Coordinates with core and combat owners on ability and progression interfaces. |

Cách phân công này khiến Thành viên 4 có khối lượng khá lớn; nếu phần giao diện và tài nguyên trở nên nặng, hãy chia sẻ công việc đó cho cả nhóm hoặc chuyển giao một nhiệm vụ UI có phạm vi rõ ràng. Tất cả thành viên đều nên tham gia kiểm thử và review code.

### Tiêu chí xác nhận hoàn thành

Một tính năng chỉ được coi là hoàn thành khi:
- It meets the agreed behavior and acceptance criteria.
- It builds/runs in the team's agreed environment.
- It has been tested in the relevant scene or level.
- It does not break existing movement, combat, navigation, or saving.
- Temporary assets, debug controls, and known limitations are documented.
- The change is committed and reviewed.

---

## Kiểm thử và tiêu chí nghiệm thu

### Gameplay dùng chung

- [ ] Player movement and jumping behave consistently.
- [ ] Collision prevents walking through solid platforms and walls.
- [ ] Damage and invulnerability behavior are consistent.
- [ ] Attacks only hit valid targets and do not deal unintended repeated damage.
- [ ] Animation states transition correctly during movement, attack, hurt, and death.
- [ ] Pause stops gameplay updates appropriately.
- [ ] Restart and return-to-menu flows do not leave stale state behind.

### Đặc trưng từng chương

**Quá Khứ**
- [ ] The time-slowing ability activates and ends correctly.
- [ ] The ability has a clear duration, cooldown or resource rule.
- [ ] Enemy attacks have readable telegraphs and recovery windows.
- [ ] The boss can be defeated without unavoidable damage.

**Tương Lai**
- [ ] The Future signature ability behaves consistently with collision and damage rules.
- [ ] Ranged hazards are telegraphed and avoidable.
- [ ] Moving platforms and vertical routes do not trap the player unexpectedly.
- [ ] The boss encounter tests movement and hazard awareness.

### Giao diện và tiến trình

- [ ] Exactly two chapter cards are displayed.
- [ ] Keyboard navigation and confirmation work.
- [ ] Locked chapters cannot be started through normal UI interaction.
- [ ] Unlock state and completion state are displayed accurately.
- [ ] Continue behaves correctly when there is and is not a valid save.
- [ ] Victory and game-over screens provide working next actions.

### Hiệu năng và độ ổn định

- [ ] No recurring exceptions or error logs during a normal playthrough.
- [ ] Sprites and animations remain crisp at the target resolution.
- [ ] Repeated restarts do not cause duplicated entities or stale UI.
- [ ] Resource loading and scene transitions are stable.

---

## Biên dịch và chạy game

Dùng các script Java có sẵn trong repository:

```bat
build.bat
run.bat
```

Game không cần Godot, Godot-JVM hoặc Gradle. Không commit đường dẫn tuyệt đối trên máy cá nhân, file build sinh tự động hoặc file save chứa tiến trình thử nghiệm cá nhân, trừ khi nhóm thực sự cần.

---

## Quy ước tài nguyên và giao diện

### Nhận diện hình ảnh

- Use a consistent pixel font for titles, chapter names, buttons, and status text.
- Keep the Main Menu and Chapter Select aligned in typography, borders, spacing, and interaction states.
- Use dark navy panels with cyan neon accents for shared UI.
- Give the chapters distinct environment palettes: warm sunset/ember tones for the Past, cool blue/violet neon for the Future.
- Keep gameplay sprites readable against the background and preserve nearest-neighbor/pixel-crisp scaling where appropriate.

### Hành vi giao diện

- Show clear states for normal, focused, selected, disabled/locked, and completed.
- Support keyboard navigation in addition to mouse interaction where applicable.
- Avoid relying on color alone to communicate locked or completed states.
- Keep UI panels out of critical combat areas.

### Bảo toàn tài nguyên

Thiết kế nhân vật hiện tại được xem là tài nguyên dùng chung của dự án. Không thiết kế lại nhân vật trong quá trình chỉnh giao diện chọn chương trừ khi cả nhóm duyệt thay đổi tạo hình. Sự khác biệt giữa hai chương nên chủ yếu đến từ môi trường, kẻ địch, hiệu ứng và gameplay.

### Ghi công và giấy phép

Ghi lại nguồn, tác giả, giấy phép và yêu cầu ghi công của từng sprite, font, âm thanh hoặc tài nguyên bên thứ ba. Không mặc định một tài nguyên được phép phân phối lại miễn phí chỉ vì nó có trên Internet.

---

## Quy ước Git và đóng góp

### Nhánh làm việc

Đề xuất quy trình nhánh đơn giản:

- `main`: phiên bản ổn định đã tích hợp.
- `feature/<ten-ngan>`: một tính năng hoặc bản sửa lỗi có phạm vi rõ ràng.
- Tạo pull request hoặc nhờ thành viên khác review trước khi merge thay đổi lớn.

### Thông điệp commit

Dùng thông điệp ngắn gọn, mô tả hành động. Các tiền tố gợi ý:

```text
feat: add Past time-slow ability
feat: implement Future drone behavior
ui: align chapter select with main menu
fix: prevent repeated boss damage
refactor: separate input from player logic
docs: update gameplay design
test: cover chapter unlock flow
```

### Danh sách kiểm tra pull request

- [ ] The change has one clear purpose.
- [ ] Relevant scenes and gameplay were tested.
- [ ] No unrelated files or generated outputs are included.
- [ ] Shared interfaces and controls are documented if changed.
- [ ] README/design docs are updated when the agreed direction changes.
- [ ] Reviewers know any remaining limitations.

---

## Các quyết định thiết kế hiện tại

Các quyết định sau xác định định hướng hiện tại, trừ khi nhóm chính thức thay đổi:

1. **The game has two main chapters, not five:** Quá Khứ and Tương Lai.
2. **The chapters must differ in gameplay**, not only in background art or enemy appearance.
3. **The game is combat-focused:** traversal, enemy encounters, and bosses drive progression.
4. **There is no separate “Chapter Gameplay” screen** in the chapter-selection flow.
5. **Shared systems should be reused** to keep the Java OOP project maintainable.
6. **The Main Menu and Chapter Select share one visual language:** pixel typography, dark sci-fi panels, and cyan neon accents.
7. **The current player character asset should remain unchanged** unless a character redesign is explicitly approved.
8. **Unverified features remain proposals.** Update this README when implementation and testing confirm the final behavior.

---

## Ghi công

Chrono Runner là dự án nhóm. Trước khi phân phối game, hãy bổ sung tên thành viên, ghi công tài nguyên, giấy phép font, nguồn âm thanh và các thư viện bên thứ ba tại đây.

---

*Luôn giữ README khớp với repository thực tế. Khi phần triển khai khác thiết kế, hãy cập nhật mã nguồn hoặc sửa tài liệu một cách rõ ràng để cả nhóm tiếp tục làm việc dựa trên một nguồn thông tin thống nhất.*

# ⏳ Chrono Runner

**Chrono Runner** là game **2D Action Platformer + Boss Battle**, được xây dựng hoàn toàn bằng **Java và Java2D/Swing**.

Người chơi có thể lựa chọn **1 hoặc 2 Player**, chọn nhân vật và chiến đấu qua 5 Chapter. Mỗi Chapter có một Boss riêng với các phase, attack pattern và kỹ năng khác nhau.

---

# 📖 Story

Chrono Runner kể về một thế giới nơi dòng thời gian đã bị phá vỡ sau một sự cố liên quan đến **Chrono Core**.

Nhân vật chính **Phan Sĩ Kiên** vô tình bị cuốn vào sự cố và mắc kẹt trong một thế giới bị ảnh hưởng bởi sự hỗn loạn của dòng thời gian. Không thể trở về thời đại của mình, anh buộc phải tìm cách khôi phục Chrono Core để tìm lại con đường trở về.

Sự cố đã tạo ra những thực thể bảo vệ từng khu vực. Mỗi Chapter có một **Boss** sở hữu một phần sức mạnh liên quan đến Chrono Core.

Phan Sĩ Kiên phải chiến đấu qua từng khu vực, đánh bại các Boss và tiến gần hơn đến Chrono Core.

Cuối cùng, anh phải đối mặt với **The Paradox** – thực thể được sinh ra từ sự hỗn loạn của dòng thời gian.

### Mục tiêu

> **Chiến đấu qua từng Chapter → đánh bại các Boss → khôi phục Chrono Core → trở về dòng thời gian của mình.**

---

# 🎮 Gameplay

Chrono Runner được thiết kế xoay quanh **combat và Boss Battle**.

## Gameplay Loop

```text
                 ┌─────────────────┐
                 │    Main Menu    │
                 └────────┬────────┘
                          ↓
                 ┌─────────────────┐
                 │ Chọn chế độ chơi│
                 │     1P / 2P     │
                 └────────┬────────┘
                          ↓
                 ┌─────────────────┐
                 │ Chọn nhân vật   │
                 └────────┬────────┘
                          ↓
                 ┌─────────────────┐
                 │      Level      │
                 │   Boss Battle   │
                 └────────┬────────┘
                          ↓
                    Boss defeated?
                     ↙           ↘
                   NO             YES
                   ↓               ↓
             Continue fight    Victory
                                  ↓
                              Next Level
```

Mỗi Level có một Boss chính.

Người chơi không cần thực hiện nhiệm vụ phụ hoặc giải các câu đố phức tạp. Mục tiêu chính của mỗi Level là:

> **Đánh bại Boss và mở khóa Level tiếp theo.**

## Core Features

- ⚔️ 2D Action Platformer
- 👹 Boss Battle
- 🧑‍🤝‍🧑 1-2 Players
- 🧙 Nhiều nhân vật có lối chơi khác nhau
- 💥 Combat và Skill
- ❤️ Health System
- 🎞️ Character/Boss Animation
- 🗺️ 5 Chapter
- 🎵 Sound Effect và Visual Effect

---

# 🧑‍🤝‍🧑 1-2 Players

Game hỗ trợ hai chế độ.

## 1 Player

Người chơi chọn một nhân vật và chiến đấu với Boss.

```text
Player
   ↓
Character
   ↓
Fight Boss
   ↓
Defeat Boss
   ↓
Next Level
```

## 2 Players

Hai người chơi có thể lựa chọn hai nhân vật khác nhau và phối hợp trong cùng một màn.

```text
Player 1 ──┐
           ├──> Boss Battle ──> Victory
Player 2 ──┘
```

Mỗi người chơi có:

- Thanh máu riêng
- Kỹ năng riêng
- Điều khiển riêng
- Vị trí riêng

Nếu một người chơi bị hạ nhưng người còn lại vẫn chiến đấu, trận đấu có thể tiếp tục.

Khi cả hai người chơi đều bị hạ:

```text
GAME OVER
```

---

# 🧙 Character System

Người chơi có thể lựa chọn nhiều nhân vật.

Mỗi nhân vật có các chỉ số và phong cách chiến đấu khác nhau thay vì chỉ thay đổi hình ảnh.

## Phan Sĩ Kiên

**Phong cách:** Cân bằng

- HP trung bình
- Damage trung bình
- Tốc độ trung bình
- Dash
- Long-range attack

Phù hợp với người chơi mới.

## Ronaldo

**Phong cách:** Tốc độ

- HP thấp
- Damage khá
- Tốc độ di chuyển cao
- Dash nhanh
- Khả năng né tránh tốt
- Skill có Sound Effect `"Siuu"`

Phù hợp với lối chơi đánh nhanh và di chuyển liên tục.

## Độ Mixi

**Phong cách:** Tank / Damage

- HP cao
- Damage cao
- Tốc độ thấp
- Khả năng chống chịu tốt
- Đòn đánh mạnh
- Skill có Sound Effect `"Alo Vũ à Vũ"`

Phù hợp với lối chơi trực diện.

---

# 🕹️ Character Selection

Trước khi bắt đầu trận đấu, người chơi sẽ chọn nhân vật.

```text
┌────────────────────────────────────┐
│          CHARACTER SELECT          │
│                                    │
│        [ PHAN SĨ KIÊN ]            │
│                                    │
│    [ RONALDO ]    [ ĐỘ MIXI ]      │
│                                    │
│    PLAYER 1: PHAN SĨ KIÊN          │
│    PLAYER 2: ĐỘ MIXI               │
│                                    │
│             [ START ]              │
└────────────────────────────────────┘
```

Trong chế độ 2 Player:

```text
Player 1 → Character A
Player 2 → Character B
```

Hai người chơi có thể chọn hai nhân vật khác nhau để tạo ra các cách phối hợp khác nhau.

---

# ⚔️ Combat System

Combat là thành phần quan trọng nhất của Chrono Runner.

Người chơi có thể:

- Di chuyển
- Nhảy
- Double Jump
- Dash
- Tấn công
- Sử dụng Skill
- Né đòn

## Basic Attack

**Player 1:** `J`  
**Player 2:** `Numpad 1`

Người chơi thực hiện đòn tấn công cơ bản.

## Dash

**Player 1:** `K`  
**Player 2:** `Numpad 2`

Dash giúp:

- Né đòn Boss
- Tiếp cận Boss
- Thoát khỏi vùng nguy hiểm
- Kết hợp với combo

## Jump

**Player 1:** `W / Space`  
**Player 2:** `↑`

Jump hỗ trợ:

- Né attack
- Tiếp cận vị trí cao
- Tấn công từ trên không

## Movement

**Player 1**

- Left: `A`
- Right: `D`

**Player 2**

- Left: `←`
- Right: `→`

## Skill

**Player 1:** `L`  
**Player 2:** `Numpad 3`

Mỗi nhân vật có Skill riêng phù hợp với phong cách chiến đấu của mình.

---

# 👹 Boss Battle

Mỗi Chapter có một Boss riêng.

Boss có hệ thống:

- Movement
- Attack Pattern
- Hitbox
- Damage
- Phase
- Special Attack
- Enrage Phase

Mục tiêu của người chơi là quan sát pattern của Boss và tìm thời điểm thích hợp để tấn công.

## Boss Phase

Boss có thể thay đổi cách chiến đấu dựa trên lượng HP.

Ví dụ:

```text
100% ───────────────── Phase 1
          ↓
 70% ───────────────── Phase 2
          ↓
 40% ───────────────── Phase 3
          ↓
  0% ───────────────── DEFEATED
```

Mỗi Phase có thể thay đổi:

- Tốc độ
- Damage
- Attack Pattern
- Số lượng Projectile
- Special Attack

Điều này giúp trận đấu thay đổi theo từng giai đoạn thay vì chỉ tấn công Boss cho đến khi HP bằng 0.

---

# 👹 Bosses

Mỗi Chapter có một Boss chính.

## Chapter 1 — Broken City

### Time Guardian

Một chiến binh được tạo ra để bảo vệ thành phố trong quá khứ.

**Đặc điểm:**

- Melee Attack
- Dash Attack
- Projectile
- Shield
- Phase 2 tăng tốc độ

## Chapter 2 — Forgotten Forest

### Chrono Beast

Một sinh vật bị biến đổi do tiếp xúc với năng lượng thời gian.

**Đặc điểm:**

- Tốc độ cao
- Leap Attack
- Charge
- Area Attack
- Gọi các sinh vật nhỏ hỗ trợ

## Chapter 3 — Ancient Ruins

### Temporal Knight

Một hiệp sĩ cổ đại bị mắc kẹt giữa các dòng thời gian.

**Đặc điểm:**

- Sword Attack
- Teleport
- Counter Attack
- Projectile
- Các đòn tấn công đặc biệt

## Chapter 4 — Frozen Future

### Frost Paradox

Một thực thể được sinh ra từ một tương lai đã bị đóng băng.

**Đặc điểm:**

- Ice Projectile
- Freeze Attack
- Area Attack
- Teleport
- Multiple Attack Patterns

## Chapter 5 — Chrono Core

### The Paradox

Boss cuối cùng và là nguồn gốc của sự hỗn loạn thời gian.

**Đặc điểm:**

- Nhiều Phase
- Clone
- Projectile
- Area Attack
- Teleport
- Thay đổi Arena

Đây là trận đấu cuối cùng để khôi phục Chrono Core.

> Lưu ý: yếu tố thời gian ở đây chủ yếu phục vụ **cốt truyện và thiết kế Boss**, không phải một hệ thống Time Shift bắt buộc trong gameplay.

---

# ❤️ Health System

Mỗi Player và Boss đều có thanh HP.

```text
PLAYER 1
██████████████████  HP

PLAYER 2
██████████████████  HP

BOSS
████████████████████████████  HP
```

Khi HP của Player giảm về 0:

```text
Player Defeated
      ↓
Nếu còn Player khác
      ↓
Continue Battle
```

Nếu tất cả Player đều bị hạ:

```text
GAME OVER
```

Khi HP của Boss bằng 0:

```text
BOSS DEFEATED
      ↓
   VICTORY
      ↓
 NEXT LEVEL
```

---

# 🎮 Controls

## Player 1

| Action | Key |
|---|---|
| Move Left | `A` |
| Move Right | `D` |
| Jump | `W / Space` |
| Attack | `J` |
| Dash | `K` |
| Skill | `L` |

## Player 2

| Action | Key |
|---|---|
| Move Left | `←` |
| Move Right | `→` |
| Jump | `↑` |
| Attack | `Numpad 1` |
| Dash | `Numpad 2` |
| Skill | `Numpad 3` |

Các phím có thể được thay đổi trong quá trình phát triển.

---

# 🌍 Level System

Game được chia thành 5 Chapter.

```text
Chapter 1
Broken City
     ↓
Time Guardian
     ↓
Chapter 2
Forgotten Forest
     ↓
Chrono Beast
     ↓
Chapter 3
Ancient Ruins
     ↓
Temporal Knight
     ↓
Chapter 4
Frozen Future
     ↓
Frost Paradox
     ↓
Chapter 5
Chrono Core
     ↓
The Paradox
     ↓
END
```

Mỗi Level tập trung vào một trận Boss.

Một Level có thể bao gồm:

- Arena
- Platforms
- Obstacles
- Boss
- Combat Environment
- Background
- Music
- Visual Effects

---

# 🖥️ Game Screens

Game được chia thành các màn hình chính và phần cốt truyện theo từng Chapter.

```text
Main Menu
   │
   ├── Play
   │     ├── 1 Player
   │     └── 2 Players
   │
   ├── Character Select
   │
   ├── Level Select
   │
   ├── Settings
   │
   └── Exit
```

## Chapter Flow

```text
Level Select
     ↓
Chapter Story
     ↓
Character Select
     ↓
Gameplay
     ↓
Boss Battle
     ↓
Victory
     ↓
Next Chapter
```

Mỗi Chapter có một đoạn Story riêng.

## Chapter 1 — Broken City

Phan Sĩ Kiên tỉnh dậy giữa một thành phố xa lạ đang bị phá hủy bởi sự bất ổn của dòng thời gian.

Không biết mình đang ở đâu, anh nhận ra rằng con đường duy nhất để trở về là tìm lại sức mạnh của Chrono Core.

Nhưng trước khi tiến xa hơn, Phan Sĩ Kiên phải đối mặt với Time Guardian, kẻ đang bảo vệ khu vực này.

**Mục tiêu:** Đánh bại Time Guardian.

## Chapter 2 — Forgotten Forest

Sau khi rời khỏi thành phố, Phan Sĩ Kiên tiến vào một khu rừng đã bị dòng thời gian nuốt chửng.

Những gì từng tồn tại ở đây đã biến đổi hoàn toàn. Một sinh vật khổng lồ được tạo ra từ năng lượng thời gian đang kiểm soát khu rừng.

Phan Sĩ Kiên phải đánh bại Chrono Beast để tiếp tục hành trình.

**Mục tiêu:** Đánh bại Chrono Beast.

## Chapter 3 — Ancient Ruins

Phan Sĩ Kiên đến những tàn tích cổ đại, nơi lưu giữ một phần sức mạnh của Chrono Core.

Tại đây, một chiến binh cổ đại đã bị mắc kẹt giữa các thời đại và trở thành người bảo vệ tàn tích.

Để tiến vào sâu hơn, Phan Sĩ Kiên phải đối đầu với Temporal Knight.

**Mục tiêu:** Đánh bại Temporal Knight.

## Chapter 4 — Frozen Future

Phan Sĩ Kiên bước vào một tương lai đã bị đóng băng.

Mọi thứ xung quanh gần như ngừng chuyển động, trong khi một thực thể bí ẩn đang hấp thụ năng lượng của Chrono Core.

Đó là Frost Paradox.

Phan Sĩ Kiên phải đánh bại nó để lấy lại phần năng lượng cuối cùng trước khi tiến đến Chrono Core.

**Mục tiêu:** Đánh bại Frost Paradox.

## Chapter 5 — Chrono Core

Cuối cùng, Phan Sĩ Kiên đã đến trung tâm của sự cố.

Chrono Core nằm trước mắt, nhưng dòng thời gian đã hoàn toàn mất kiểm soát.

Thực thể đứng sau tất cả những gì đã xảy ra xuất hiện.

**The Paradox.**

Để trở về thời đại của mình, Phan Sĩ Kiên phải đánh bại The Paradox và khôi phục Chrono Core.

**Mục tiêu:** Đánh bại The Paradox và trở về dòng thời gian của mình.

## Complete Game Flow

```text
                    MAIN MENU
                        │
                       PLAY
                        │
                    1P / 2P
                        │
                        ↓
                  LEVEL SELECT
                        │
              ┌─────────┴─────────┐
              ↓                   ↓
          CHAPTER 1           CHAPTER 2 ...
              │
              ↓
         STORY SCREEN
              │
              ↓
      CHARACTER SELECT
              │
              ↓
          BOSS ARENA
              │
         ┌────┴────┐
         ↓         ↓
      VICTORY   GAME OVER
         │
         ↓
    NEXT CHAPTER
         │
         ↓
     STORY SCREEN
```

---

# 🎞️ Animation & Visual Effects

Game sử dụng hệ thống animation cho:

- Player
- Boss
- Enemy
- Attack
- Hit
- Dash
- Jump
- Death

Các hiệu ứng có thể bao gồm:

- Hit Effect
- Slash Effect
- Particle
- Screen Shake
- Flash
- Boss Effect
- Damage Number

Mục tiêu là làm cho combat có phản hồi rõ ràng và tạo cảm giác mạnh khi người chơi tấn công hoặc bị tấn công.

---

# 🧱 Project Architecture

Chrono Runner được xây dựng hoàn toàn bằng **Java và Java2D/Swing**.

Project được chia thành các module chính:

```text
src/
└── chronorunner/
    │
    ├── Main.java
    │
    ├── core/
    │   ├── Game.java
    │   ├── Camera.java
    │   ├── Physics.java
    │   ├── InputHandler.java
    │   ├── Screen.java
    │   └── GameState.java
    │
    ├── animation/
    │   ├── Animation.java
    │   ├── AnimationController.java
    │   └── SpriteSheet.java
    │
    ├── combat/
    │   ├── Attack.java
    │   ├── AttackData.java
    │   ├── Hitbox.java
    │   ├── Hurtbox.java
    │   └── DamageInfo.java
    │
    ├── entity/
    │   ├── GameObject.java
    │   ├── Character.java
    │   ├── Player.java
    │   ├── Projectile.java
    │   ├── Particle.java
    │   ├── FloatingText.java
    │   │
    │   ├── character/
    │   │   ├── PhanSiKien.java
    │   │   ├── Ronaldo.java
    │   │   └── DoMixi.java
    │   │
    │   ├── enemy/
    │   │   ├── Enemy.java
    │   │   ├── TimeSlime.java
    │   │   └── ChronoBat.java
    │   │
    │   └── boss/
    │       ├── Boss.java
    │       ├── TimeGuardian.java
    │       ├── ChronoBeast.java
    │       ├── TemporalKnight.java
    │       ├── FrostParadox.java
    │       └── TheParadox.java
    │
    ├── obstacle/
    │   ├── Obstacle.java
    │   ├── Platform.java
    │   ├── StaticPlatform.java
    │   ├── MovingPlatform.java
    │   ├── FallingPlatform.java
    │   └── Trap.java
    │
    ├── world/
    │   ├── Level.java
    │   ├── LevelBuilder.java
    │   ├── LevelFactory.java
    │   └── TileType.java
    │
    ├── ui/
    │   ├── GamePanel.java
    │   ├── GameWindow.java
    │   ├── Hud.java
    │   ├── MainMenuScreen.java
    │   ├── LevelSelectScreen.java
    │   ├── StoryScreen.java
    │   ├── CharacterSelectScreen.java
    │   ├── PlayScreen.java
    │   ├── PauseScreen.java
    │   ├── VictoryScreen.java
    │   ├── GameOverScreen.java
    │   └── UiKit.java
    │
    └── util/
        ├── Collision.java
        ├── Rect.java
        ├── SaveManager.java
        └── Progress.java
```

## Vai trò của từng module

| Module | Chức năng |
|---|---|
| `core` | Game loop, input, camera, physics và trạng thái game |
| `animation` | Quản lý sprite, animation và chuyển frame |
| `combat` | Attack, hitbox, hurtbox và damage |
| `entity` | Các đối tượng trong game |
| `entity/character` | Các nhân vật người chơi |
| `entity/enemy` | Enemy phụ |
| `entity/boss` | Boss của từng Chapter |
| `obstacle` | Platform và vật cản trong Arena |
| `world` | Quản lý Level và Arena |
| `ui` | Menu, Story, Character Select, HUD và các màn hình game |
| `util` | Collision, Save và các tiện ích dùng chung |

---

# 🧩 Object-Oriented Programming

Chrono Runner được thiết kế dựa trên bốn nguyên lý OOP cơ bản.

## Encapsulation

Dữ liệu của các đối tượng được quản lý bên trong class.

Ví dụ:

```java
class Player {
    private int health;
    private int damage;
    private double x;
    private double y;

    public void attack() {
        // ...
    }

    public int getHealth() {
        return health;
    }
}
```

## Inheritance

Các đối tượng trong game có thể kế thừa từ những class cơ sở.

```text
GameObject
    │
    └── Character
          ├── Player
          ├── Enemy
          └── Boss
```

Ví dụ:

```java
abstract class Character extends GameObject {
    protected int health;
    protected int damage;

    public abstract void attack();
}
```

Các Boss cụ thể kế thừa từ `Boss`:

```text
Boss
 ├── TimeGuardian
 ├── ChronoBeast
 ├── TemporalKnight
 ├── FrostParadox
 └── TheParadox
```

## Polymorphism

Các Character và Boss có thể triển khai hành vi khác nhau thông qua cùng một kiểu dữ liệu.

Ví dụ:

```java
Character player1 = new PhanSiKien();
Character player2 = new Ronaldo();

player1.attack();
player2.attack();
```

Cùng gọi `attack()` nhưng mỗi nhân vật có cách tấn công khác nhau.

## Abstraction

Các class abstract được sử dụng để mô tả hành vi chung.

Ví dụ:

```java
abstract class Boss extends Character {

    public abstract void updateAI();

    public abstract void specialAttack();
}
```

Các Boss cụ thể sẽ tự triển khai `updateAI()` và `specialAttack()` theo cách riêng.

---

# 🧠 Design Patterns

Một số Design Pattern được sử dụng hoặc dự kiến sử dụng trong project.

## Builder Pattern

`LevelBuilder` hỗ trợ xây dựng các Arena/Level.

Ví dụ:

```java
Level level = new LevelBuilder()
        .setBackground(...)
        .addPlatform(...)
        .addBoss(...)
        .build();
```

Builder giúp tạo Level theo từng thành phần và dễ mở rộng khi thêm Arena mới.

## State Pattern

Game sử dụng các trạng thái khác nhau:

```text
Main Menu
   ↓
Character Select
   ↓
Playing
   ↓
Pause
   ↓
Victory / Game Over
```

State Pattern giúp quản lý flow của game rõ ràng hơn và tránh việc tất cả logic màn hình nằm trong một class.

---

# ⚙️ Technical Details

## Language

**Java**

## Graphics

**Java2D / Swing**

Sử dụng:

- `Graphics2D`
- Double Buffering
- Anti-aliasing
- Sprite Rendering
- Camera System

## Game Loop

Game sử dụng game loop để cập nhật trạng thái và render.

```text
Input
  ↓
Update
  ↓
Physics
  ↓
Collision
  ↓
Combat / AI
  ↓
Animation
  ↓
Render
  ↓
Repeat
```

Mục tiêu là duy trì gameplay ổn định ở khoảng:

**60 FPS**

---

# 📷 Camera

Camera theo dõi Player và cho phép thế giới game lớn hơn kích thước cửa sổ.

```text
World
──────────────────────────────────────────────

          ┌───────────────┐
          │    Camera     │
          │      ↓        │
          │    Player     │
          └───────────────┘

──────────────────────────────────────────────
```

Camera có thể hỗ trợ:

- Boss Arena
- Screen Shake
- Camera Follow
- Camera Lock
- Multiplayer

---

# 💻 Requirements

## Java

Yêu cầu:

**JDK 17+**

Khuyến nghị:

**JDK 21+**

## Operating System

- Windows
- Linux
- macOS

## Resolution

Resolution nội bộ:

**1280 × 720**

---

# 🚀 Build & Run

## Windows

```bat
build.bat
run.bat
```

Hoặc biên dịch thủ công:

```bat
javac -encoding UTF-8 -d out @sources.txt
java -cp out chronorunner.Main
```

---

# 📁 Project Structure

```text
chrono-runner/
├── build.bat
├── run.bat
├── README.md
├── saves/
├── sources.txt
├── .gitignore
│
└── src/
    └── chronorunner/
        ├── Main.java
        ├── core/
        ├── animation/
        ├── combat/
        ├── entity/
        │   ├── character/
        │   ├── enemy/
        │   └── boss/
        ├── obstacle/
        ├── world/
        ├── ui/
        └── util/
```

---

# 🔮 Development Roadmap

## Phase 1 — Core

- [ ] Game Window
- [ ] Game Loop
- [ ] Input System
- [ ] Physics
- [ ] Collision
- [ ] Camera

## Phase 2 — Player

- [ ] Player Movement
- [ ] Jump
- [ ] Double Jump
- [ ] Dash
- [ ] Basic Attack
- [ ] Skill
- [ ] Health System

## Phase 3 — Combat

- [ ] Attack System
- [ ] Hitbox
- [ ] Hurtbox
- [ ] Damage System
- [ ] Projectile
- [ ] Combat Effects
- [ ] Screen Shake

## Phase 4 — Character

- [ ] PhanSiKien
- [ ] Ronaldo
- [ ] DoMixi
- [ ] Character Select
- [ ] Character Animation

## Phase 5 — Boss

- [ ] Boss Base Class
- [ ] Time Guardian
- [ ] Chrono Beast
- [ ] Temporal Knight
- [ ] Frost Paradox
- [ ] The Paradox
- [ ] Boss Phase
- [ ] Boss AI

## Phase 6 — Multiplayer

- [ ] Player 1
- [ ] Player 2
- [ ] Separate Controls
- [ ] Shared Boss Battle
- [ ] Multiplayer Camera

## Phase 7 — UI & Story

- [ ] Main Menu
- [ ] Level Select
- [ ] Story Screen
- [ ] Character Select
- [ ] Pause
- [ ] Victory
- [ ] Game Over
- [ ] HUD

## Phase 8 — Polish

- [ ] Sprite Animation
- [ ] Sound Effect
- [ ] Music
- [ ] Particle Effect
- [ ] Screen Shake
- [ ] Balance Boss
- [ ] Bug Fixing

---

# 📌 Summary

**Chrono Runner** tập trung vào trải nghiệm **2D Action Platformer và Boss Battle**.

Gameplay chính:

```text
SELECT MODE
     ↓
SELECT CHARACTER
     ↓
ENTER ARENA
     ↓
COMBAT
     ↓
BOSS BATTLE
     ↓
DEFEAT BOSS
     ↓
NEXT CHAPTER
```

Mục tiêu của project là kết hợp:

- Java OOP
- Java2D/Swing
- Game Loop
- Physics & Collision
- Combat System
- Character System
- Boss AI
- Animation
- 1-2 Player
- State Pattern
- Builder Pattern

để xây dựng một game 2D hoàn chỉnh với kiến trúc có thể mở rộng.

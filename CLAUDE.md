# 逃離中央大學 — 專案說明

## 基本資訊
- **課程**：計算機實習，期末專題，截止約 2026-06-07
- **GitHub**：https://github.com/Ethan-OwO/Dark-Haunted-House
- **類型**：JavaFX 俯視角射擊遊戲，類似元氣騎士（Soul Knight）
- **三人小組**，皆為 Java 初學者，用 GitHub Desktop 協作

## 技術架構
- **語言/框架**：Java 17 + JavaFX 21（Maven）
- **渲染**：Canvas + GraphicsContext，無 FXML
- **遊戲循環**：AnimationTimer（`core/GameLoop.java`）
- **操作**：WASD 移動，滑鼠瞄準 + 點擊射擊
- **執行**：`mvn javafx:run`

## 套件結構

```
com.escapencu
├── application/   GameApp.java（主視窗，800×600）
├── core/          GameLoop, SceneManager, GameState
├── ui/            GameScene, MainMenuScene, GameOverScene, VictoryScene
├── entity/        Entity, Player, Enemy, Bullet, EffectBullet, AreaChecker
│   ├── enemy/     Squirrel（松鼠）, Goose（鵝）, Wing, Termite（白蟻）
│   └── boss/      Boss, WuXiaoGuang, ChenQinHan, ShiGuoZhen, Decoy, Mine, TAEnemy
├── level/         Room, NormalRoom, BossRoom, Corridor, DungeonFloor, LevelManager
├── map/           MapGenerator（隨機地圖生成）
└── util/          CollisionUtil, ResourceLoader
```

## 關卡設計

| Stage | 地點 | 普通小怪 | Boss |
|-------|------|----------|------|
| 1 | 工程五館 | 松鼠（Squirrel） | 無小光（WuXiaoGuang） |
| 2 | 男13宿舍 | 鵝（Goose） | 沉沁汗（ChenQinHan） |
| 3 | 圖書館/操場 | 白蟻（Termite） | 濕幗針（ShiGuoZhen） |

每個 Stage 有 3 個 Floor，Floor 3 固定是 Boss 房。

## Boss 機制

**無小光（Stage 1）**
- 每 5 秒隱形 3 秒（隱形時無敵），同時生成 2 個分身（Decoy）
- 打到分身會召喚助教小怪（TAEnemy）
- 定時在地板放河內塔地雷（Mine），玩家靠近爆炸
- Phase 2（HP < 50%）：隱形更頻繁、地雷更密

**沉沁汗（Stage 2）**
- 每隔幾秒鑽入地板，在玩家位置顯示黃色警告圈，1 秒後冒出造成傷害+定身
- 浮出時若距離玩家 < 50px → 造成 20 傷害 + 定身 0.3 秒
- 射出邏輯閘子彈循環：AND（高速直射）→ OR（3 向擴散）→ XOR（5 向扇形）
- Phase 2：鑽地 CD 縮短、射擊頻率提高

**濕幗針（Stage 3，Final Boss）**
- 移動留下液體軌跡（綠色圓形），站在上面每 0.3 秒扣 2 HP
- 丟程式語言子彈循環：Python（定身 0.3s）→ C++（毒+緩速 3s）→ Java（燃燒 2s）
- Phase 2（HP < 50%）：移速提升

## 玩家狀態效果
- **定身（stun）**：無法移動也無法射擊
- **緩速（slow）**：移速 × 0.5
- **毒（poison）**：每 0.5 秒 -2 HP
- **燃燒（burn）**：每 0.4 秒 -3 HP

## 開發者模式
- `F1`：切換 Dev Mode（HUD 顯示提示）
- `F2`（Dev Mode 中）：切換 OP Mode（無限 HP、9999 傷害、免疫狀態效果）
- `N`（Dev Mode 中）：直接跳到下一個 Floor

## GameState 全域變數
```java
GameState.currentStage  // 1-3
GameState.playerHp / playerMaxHp
GameState.score
GameState.devMode / opMode
```

## 注意事項
- 所有渲染都在世界座標系，`GameScene` 用 `gc.translate(-camX, -camY)` 處理攝影機
- 未清除的戰鬥房（NORMAL/BOSS）會鎖住玩家，必須消滅所有敵人才能離開
- `EffectBullet` 用 lambda 傳入狀態效果，打到玩家時呼叫 `applyEffect(player)`
- Boss 的 `getPendingSpawns()` 每幀被 Room 呼叫，用來動態生成 Decoy/Mine 等實體

package com.escapencu.ui;

import com.escapencu.application.GameApp;
import com.escapencu.core.GameLoop;
import com.escapencu.core.GameState;
import com.escapencu.core.SceneManager;
import com.escapencu.entity.AreaChecker;
import com.escapencu.entity.Bullet;
import com.escapencu.entity.EffectBullet;
import com.escapencu.entity.Enemy;
import com.escapencu.entity.Entity;
import com.escapencu.entity.FirePatch;
import com.escapencu.entity.LeBronSkill;
import com.escapencu.entity.Player;
import com.escapencu.lebron.LeBronUltimate;
import com.escapencu.entity.boss.ChenQinHan;
import com.escapencu.entity.boss.ShiGuoZhen;
import com.escapencu.entity.boss.WuXiaoGuang;
import com.escapencu.entity.enemy.Goose;
import com.escapencu.entity.enemy.Squirrel;
import com.escapencu.entity.enemy.Termite;
import com.escapencu.level.DungeonFloor;
import com.escapencu.level.LevelManager;
import com.escapencu.level.NormalRoom;
import com.escapencu.level.RewardRoom;
import com.escapencu.level.Room;
import com.escapencu.util.CollisionUtil;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GameScene {
    private Canvas          canvas;
    private GraphicsContext gc;
    private Player          player;
    private LevelManager    levelManager;
    private DungeonFloor    dungeon;
    private Room            currentRoom;
    private GameLoop        gameLoop;

    private LeBronSkill            lebronSkill    = null;
    private final List<FirePatch>  firePatches    = new ArrayList<>();
    private StackPane              rootPane       = null;
    /** Dev-only: a SHOP RewardRoom overlaid on top of the current room. */
    private RewardRoom             devRewardOverlay = null;
    // ── Pause variables ──────────────────────────────────────────────────────
    private boolean isPaused = false;
    private javafx.scene.layout.VBox pauseMenu;
    //── Function checklist ──────────────────────────────────────────────────────
    private VBox controlsMenu;

    private final CommandConsole console = new CommandConsole();
    private final Set<KeyCode> pressedKeys    = new HashSet<>();
    private final Set<Room>    visitedRooms   = new HashSet<>();
    private double contactCooldown  = 0;
    private double mouseScreenX     = 0;
    private double mouseScreenY     = 0;
    private double prevMouseScreenX = -1;
    private double prevMouseScreenY = -1;
    private boolean devAdvancePending = false;

    // ── Camera shake ───────────────────────────────────────────────────────
    private double shakeTimer     = 0;
    private double shakeDuration  = 0;
    private double shakeIntensity = 10.0;

    // ── Room-clear banner ──────────────────────────────────────────────────
    private static final Font PIXEL_FONT_LARGE = Font.loadFont(
            GameScene.class.getResourceAsStream("/fonts/Cubic_11.ttf"), 52);
    private static final Font PIXEL_FONT_HUD   = Font.loadFont(
            GameScene.class.getResourceAsStream("/fonts/Cubic_11.ttf"), 18);
    private static final Font PIXEL_FONT_52    = Font.loadFont(
            GameScene.class.getResourceAsStream("/fonts/Cubic_11.ttf"), 52);
    private static final Font PIXEL_FONT_24    = Font.loadFont(
            GameScene.class.getResourceAsStream("/fonts/Cubic_11.ttf"), 24);
    private static final Font PIXEL_FONT_14    = Font.loadFont(
            GameScene.class.getResourceAsStream("/fonts/Cubic_11.ttf"), 14);
    private static final double CLEAR_BANNER_DURATION = 2.2; // seconds to show
    private double  clearBannerTimer    = 0;
    private boolean prevRoomCleared     = false;  // cleared state last frame
    private Room    prevTrackedRoom     = null;   // detects room changes

    // ── HUD coin animation ─────────────────────────────────────────────────
    private static final javafx.scene.image.Image HUD_COIN_SHEET =
            com.escapencu.util.ResourceLoader.getImage("/images/2D Chests & Coins/Coin.png", false);
    private static final int    HUD_COIN_FRAMES   = 6;
    private static final double HUD_COIN_FRAME_DUR = 0.10;
    private double hudCoinTimer = 0;
    private int    hudCoinFrame = 0;

    /** Trigger a camera shake. intensity = max pixel offset. */
    private void triggerShake(double duration, double intensity) {
        shakeTimer    = duration;
        shakeDuration = duration;
        shakeIntensity = intensity;
    }

    // ── Tab-completion data (update when new commands / entities are added) ──
    /** All available slash-commands. */
    private static final List<String> CMD_LIST = List.of("/help", "/summon", "/shop", "/closeshop", "/chest");

    /**
     * Summonable entity names — both English and Chinese so players can
     * Tab-complete in either language.
     * Format: English name first, then Chinese equivalent, paired by index.
     */
    private static final List<String> SUMMON_NAMES = List.of(
            "squirrel",     "松鼠",
            "goose",        "鵝",
            "termite",      "白蟻",
            "wuxiaoguang",  "無小光",
            "chenqinhan",   "沉沁汗",
            "shiguozhen",   "濕幗針"
    );

    // Corridor direction offsets: index matches hasCorridor[] (0=N,1=S,2=E,3=W)
    private static final int[] DIR_DX = { 0,  0, 1, -1};
    private static final int[] DIR_DY = {-1,  1, 0,  0};

    // ── Build ──────────────────────────────────────────────────────────────
    public Scene build() {
        canvas = new Canvas(GameApp.WIDTH, GameApp.HEIGHT);
        gc     = canvas.getGraphicsContext2D();

        levelManager = new LevelManager();
        loadFloor();

        canvas.setFocusTraversable(true);
        canvas.setOnKeyPressed(e -> {
            // ── Console open: intercept input ──────────────────────────────
            if (console.isOpen()) {
                switch (e.getCode()) {
                    case ENTER      -> { String cmd = console.submit(); executeCommand(cmd); }
                    case ESCAPE     -> console.close();
                    case BACK_SPACE -> console.backspace();
                    case TAB        -> { e.consume(); handleTabCompletion(); }
                    default -> {} // printable characters handled by setOnKeyTyped
                }
                return; // don't pass keys to game while console is open
            }

            if (e.getCode() == KeyCode.P) {
                togglePause();
                return; // 避免觸發其他按鍵行為
            }

            // ── Normal game input ──────────────────────────────────────────
            pressedKeys.add(e.getCode());
            if (e.getCode() == KeyCode.SPACE) {
                player.dash();}
            if (e.getCode() == KeyCode.F1) {
                GameState.devMode = !GameState.devMode;
                if (!GameState.devMode) GameState.opMode = false;
            }
            if (e.getCode() == KeyCode.F2 && GameState.devMode) {
                GameState.opMode = !GameState.opMode;
            }
            if (e.getCode() == KeyCode.N && GameState.devMode) {
                devAdvancePending = true;
            }
            if (e.getCode() == KeyCode.ENTER && GameState.devMode) {
                console.open();
            }
            if (e.getCode() == KeyCode.E)      { handleRewardInteract(); }
            if (e.getCode() == KeyCode.DIGIT1) { handleShopBuy(0); }
            if (e.getCode() == KeyCode.DIGIT2) { handleShopBuy(1); }
            if (e.getCode() == KeyCode.DIGIT3) { handleShopBuy(2); }
            if (e.getCode() == KeyCode.Q
                    && GameState.selectedTalent == GameState.Talent.LEBRON
                    && !GameState.talentUsedThisStage
                    && lebronSkill == null
                    && currentRoom != null) {
                GameState.talentUsedThisStage = true;
                double roomCX = currentRoom.worldX + currentRoom.worldW / 2.0;
                double roomCY = currentRoom.worldY + currentRoom.worldH / 2.0;
                double entryX = camX() - 80;
                lebronSkill = new LeBronSkill(entryX, roomCX, roomCY);
                // 全螢幕大招動畫 overlay
                if (rootPane != null) {
                    LeBronUltimate ult = new LeBronUltimate(GameApp.WIDTH, GameApp.HEIGHT);
                    rootPane.getChildren().add(ult);
                    ult.play(() -> rootPane.getChildren().remove(ult));
                }
            }
        });
        canvas.setOnKeyTyped(e -> console.typeChar(e.getCharacter()));
        canvas.setOnKeyReleased(e  -> pressedKeys.remove(e.getCode()));
        canvas.setOnMouseMoved(e   -> { mouseScreenX = e.getX(); mouseScreenY = e.getY(); });
        canvas.setOnMouseClicked(e -> {
            if (!console.isOpen()) {
                // 取得玩家當前踩著的房間
                Room detected = dungeon.getRoomAt(player.getCenterX(), player.getCenterY());
                // 如果玩家正在跨越未通關的戰鬥房門檻（還沒全身進去導致門沒關），則鎖定攻擊鍵
                if (detected != null && detected != currentRoom && !detected.isCleared()) {
                    return; // 門還沒關好，不給射擊！
                }
                player.shoot();
            }
        });

        gameLoop = new GameLoop(this);
        gameLoop.start();

        pauseMenu = buildPauseMenu();
        pauseMenu.setVisible(false); // 預設隱藏
        controlsMenu = buildControlsMenu();

        rootPane = new StackPane(canvas, pauseMenu, controlsMenu);
        Scene scene = new Scene(rootPane, GameApp.WIDTH, GameApp.HEIGHT);
        canvas.requestFocus();
        return scene;
    }
    //pause控制介面
    private javafx.scene.layout.VBox buildPauseMenu() {
        javafx.scene.text.Text title = new javafx.scene.text.Text("遊戲暫停");
        title.setFont(PIXEL_FONT_52 != null ? PIXEL_FONT_52 : Font.font(52));
        title.setFill(Color.WHITE);

        javafx.scene.control.Button resumeBtn = new javafx.scene.control.Button("繼續遊戲");
        resumeBtn.setFont(PIXEL_FONT_24 != null ? PIXEL_FONT_24 : Font.font(24));
        resumeBtn.setPrefWidth(220);
        resumeBtn.setOnAction(e -> togglePause());

        javafx.scene.control.Button controlsBtn = new javafx.scene.control.Button("操作說明");
        controlsBtn.setFont(PIXEL_FONT_24 != null ? PIXEL_FONT_24 : Font.font(24));
        controlsBtn.setPrefWidth(220);
        controlsBtn.setOnAction(e -> {
            pauseMenu.setVisible(false);     // 隱藏暫停選單
            controlsMenu.setVisible(true);   // 顯示操作說明
        });

        javafx.scene.control.Button menuBtn = new javafx.scene.control.Button("回主選單");
        menuBtn.setFont(PIXEL_FONT_24 != null ? PIXEL_FONT_24 : Font.font(24));
        menuBtn.setPrefWidth(220);
        menuBtn.setOnAction(e -> {
            gameLoop.stop();
            SceneManager.showMainMenu();
        });

        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(30, title, resumeBtn, controlsBtn, menuBtn);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        box.setStyle("-fx-background-color: rgba(0, 0, 0, 0.75);"); // 半透明黑色背景
        return box;
    }

    private void togglePause() {
        isPaused = !isPaused;
        pauseMenu.setVisible(isPaused);
        if (!isPaused) {
            canvas.requestFocus(); // 恢復時把焦點還給遊戲畫面，確保能繼續接收按鍵
        }
    }

    private javafx.scene.layout.VBox buildControlsMenu() {
        // 標題
        javafx.scene.text.Text title = new javafx.scene.text.Text("操作說明");
        title.setFont(PIXEL_FONT_52 != null ? PIXEL_FONT_52 : Font.font(52));
        title.setFill(Color.WHITE);

        // 鍵位內容 (使用 Text 支援多行，並設定置中)
        String controlsText = "W / A / S / D : 移動\n\n" +
                "滑鼠左鍵 : 射擊\n\n" +
                "Space (空白鍵) : 衝刺 / 翻滾\n\n";

        // ▼▼▼ 修改重點 2：在此替換成你實際判斷玩家是否擁有該天賦的程式碼 ▼▼▼
        // 例如：如果你的 player 實體有 boolean hasLeBronSkill 變數，可以寫成 if (player.hasLeBronSkill)
        if (GameState.selectedTalent == GameState.Talent.LEBRON) {
            controlsText += "Q : LeBron 技能 (砸地板)\n\n";}

        // 最後加上共同的暫停鍵
        controlsText += "P : 暫停遊戲";

        javafx.scene.text.Text text = new javafx.scene.text.Text(controlsText);
        text.setFont(PIXEL_FONT_24 != null ? PIXEL_FONT_24 : Font.font(24));
        text.setFill(Color.WHITE);
        text.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // 返回按鈕
        javafx.scene.control.Button backBtn = new javafx.scene.control.Button("返回");
        backBtn.setFont(PIXEL_FONT_24 != null ? PIXEL_FONT_24 : Font.font(24));
        backBtn.setPrefWidth(220);
        backBtn.setOnAction(e -> {
            controlsMenu.setVisible(false); // 隱藏操作說明
            pauseMenu.setVisible(true);     // 重新顯示暫停選單
        });

        // ▼▼▼ 修改重點：直接在建構子放入元件 (title, text, backBtn) ▼▼▼
        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(40, title, text, backBtn);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        box.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85);"); // 半透明黑底
        box.setVisible(false); // 預設隱藏

        return box;
    }

    // ── Load the current LevelManager floor ───────────────────────────────
    private void loadFloor() {
        dungeon = levelManager.getDungeonFloor();
        Room start = dungeon.startRoom;
        start.activate();
        currentRoom    = start;
        contactCooldown = 0;

        visitedRooms.clear();
        visitedRooms.add(start);

        double px = start.worldX + start.worldW / 2.0 - 16;
        double py = start.worldY + start.worldH / 2.0 - 16;
        player = new Player(px, py);
        GameState.playerHp = player.getHp();

        // 換 Stage 時補充 LeBron 技能次數
        int newStage = levelManager.getStage();
        if (newStage != GameState.currentStage) {
            GameState.talentUsedThisStage = false;
            GameState.currentStage = newStage;
        }
        firePatches.clear();
        lebronSkill    = null;
        devRewardOverlay = null;
    }

    // ── Camera helper ──────────────────────────────────────────────────────
    private double camX() { return player.getCenterX() - GameApp.WIDTH  / 2.0; }
    private double camY() { return player.getCenterY() - GameApp.HEIGHT / 2.0; }

    // ── Area checker ───────────────────────────────────────────────────────
    /**
     * Returns a movement checker that locks the player inside an uncleared
     * combat room — but only once the player's entire body has crossed into
     * that room (prevents entrance-stuck bug).
     */
    private AreaChecker buildAreaChecker() {
        double e = 0.5;
        double px = player.getX(), py = player.getY();
        double pw = player.getWidth(), ph = player.getHeight();

        // ── Step 1: base movement bounds ──────────────────────────────────
        AreaChecker base;
        if (currentRoom == null || currentRoom.isCleared()
                || (currentRoom.type != Room.Type.NORMAL && currentRoom.type != Room.Type.BOSS)) {
            base = dungeon::canMoveTo;
        } else {
            // Only enforce combat-room lock while the player is fully inside
            final Room r = currentRoom;
            boolean fullyInside = r.containsPoint(px + e,      py + e)
                    && r.containsPoint(px + pw - e, py + e)
                    && r.containsPoint(px + e,      py + ph - e)
                    && r.containsPoint(px + pw - e, py + ph - e);
            if (!fullyInside) {
                base = dungeon::canMoveTo;
            } else {
                base = (npx, npy, npw, nph) ->
                        r.containsPoint(npx + e,        npy + e)
                                && r.containsPoint(npx + npw - e,  npy + e)
                                && r.containsPoint(npx + e,        npy + nph - e)
                                && r.containsPoint(npx + npw - e,  npy + nph - e);
            }
        }

        // ── Step 2: layer on obstacle blocking (NormalRoom only) ──────────
        if (currentRoom instanceof NormalRoom nr) {
            AreaChecker finalBase = base;
            return (npx, npy, npw, nph) ->
                    finalBase.canMoveTo(npx, npy, npw, nph)
                            && !nr.blockedByObstacle(npx, npy, npw, nph);
        }
        return base;
    }

    // ── Update (called by GameLoop) ────────────────────────────────────────
    public void update(double deltaTime) {
        if (isPaused) return;//如果是暫停狀態，跳過所有邏輯更新

        console.update(deltaTime);

        // HUD coin spin animation (runs even while console is open)
        hudCoinTimer += deltaTime;
        if (hudCoinTimer >= HUD_COIN_FRAME_DUR) {
            hudCoinTimer -= HUD_COIN_FRAME_DUR;
            hudCoinFrame = (hudCoinFrame + 1) % HUD_COIN_FRAMES;
        }

        if (console.isOpen()) return; // 打指令時凍結遊戲

        // Dev mode: N key → skip to next floor
        if (devAdvancePending) {
            devAdvancePending = false;
            advanceFloor();
            return;
        }

        // Aim: convert mouse screen position → world position
        double mouseDelta = Math.hypot(mouseScreenX - prevMouseScreenX, mouseScreenY - prevMouseScreenY);
        boolean mouseMoved = mouseDelta > 3.0;
        prevMouseScreenX = mouseScreenX;
        prevMouseScreenY = mouseScreenY;
        player.setMouseMoved(mouseMoved);
        player.updateMouseWorldPos(mouseScreenX + camX(), mouseScreenY + camY());

        // Move player
        player.handleMovement(pressedKeys, deltaTime, buildAreaChecker());
        player.update(deltaTime);

        // LeBron 技能動畫
        if (lebronSkill != null) {
            List<Entity> skillEnemies = (currentRoom != null) ? currentRoom.getEnemies() : List.of();
            lebronSkill.update(deltaTime, skillEnemies);
            List<FirePatch> pending = lebronSkill.getPendingFire();
            if (!pending.isEmpty()) {
                firePatches.addAll(pending);
                pending.clear();
            }
            if (!lebronSkill.isAlive()) lebronSkill = null;
        }

        // 火焰效果更新
        if (!firePatches.isEmpty() && currentRoom != null) {
            List<Entity> fEnemies = currentRoom.getEnemies();
            firePatches.removeIf(f -> !f.isAlive());
            for (FirePatch f : firePatches) f.update(deltaTime, player, fEnemies);
        }

        // Track which room the player is in.
        // Activate enemies as soon as the player's center enters.
        // For uncleared combat rooms, delay the currentRoom switch until the player's
        // entire body is inside (avoids the entrance-stuck bug at the boundary).
        // ▼▼▼ 修改點 2：完美修正進門、關門與生怪順序 ▼▼▼
        Room detected = dungeon.getRoomAt(player.getCenterX(), player.getCenterY());
        if (detected != null) {
            // 注意：我們移除了原本提早執行的 detected.activate()

            if (detected != currentRoom) {
                boolean needsFullEntry = !detected.isCleared()
                        && (detected.type == Room.Type.NORMAL
                        || detected.type == Room.Type.BOSS);

                if (!needsFullEntry) {
                    // 安全房間 (無怪) 直接進入並啟動
                    currentRoom = detected;
                    visitedRooms.add(currentRoom);
                    if (!detected.isActivated()) detected.activate();
                } else {
                    // 戰鬥房間：必須等玩家「全身」都進入房間內
                    double e = 0.5;
                    double px = player.getX(), py = player.getY();
                    double pw = player.getWidth(), ph = player.getHeight();

                    if (detected.containsPoint(px + e,      py + e)
                            && detected.containsPoint(px + pw - e, py + e)
                            && detected.containsPoint(px + e,      py + ph - e)
                            && detected.containsPoint(px + pw - e, py + ph - e)) {

                        // 1. 強制將玩家往房間內部推 10px，避免關門瞬間邊界重疊導致卡牆
                        double pushDist = 10.0;
                        if (px < detected.worldX + pushDist) player.setX(detected.worldX + pushDist);
                        if (px + pw > detected.worldX + detected.worldW - pushDist) player.setX(detected.worldX + detected.worldW - pw - pushDist);
                        if (py < detected.worldY + pushDist) player.setY(detected.worldY + pushDist);
                        if (py + ph > detected.worldY + detected.worldH - pushDist) player.setY(detected.worldY + detected.worldH - ph - pushDist);

                        // 2. 固定位置後，正式將門關上 (鎖定 currentRoom)
                        currentRoom = detected;
                        visitedRooms.add(currentRoom);

                        // 3. 門確實關好後，才觸發房間啟動（生成怪物）
                        if (!detected.isActivated()) detected.activate();
                    }
                }
            }
        }
        // ▲▲▲ 修改結束 ▲▲▲

        dungeon.update(deltaTime, player);
        if (devRewardOverlay != null) devRewardOverlay.update(deltaTime, player);

        // ── Room-clear banner trigger ─────────────────────────────────────
        if (currentRoom != prevTrackedRoom) {
            // Player changed rooms — sync the "was cleared" baseline
            prevTrackedRoom  = currentRoom;
            prevRoomCleared  = (currentRoom != null && currentRoom.isCleared());
        }
        if (clearBannerTimer > 0) clearBannerTimer -= deltaTime;
        boolean nowCleared = (currentRoom != null && currentRoom.isCleared());
        if (nowCleared && !prevRoomCleared
                && currentRoom.type != Room.Type.START
                && currentRoom.type != Room.Type.REWARD
                && currentRoom.type != Room.Type.EXIT) {
            clearBannerTimer = CLEAR_BANNER_DURATION;
        }
        prevRoomCleared = nowCleared;

        if (contactCooldown > 0) contactCooldown -= deltaTime;
        if (shakeTimer      > 0) shakeTimer      -= deltaTime;
        resolveCollisions();
        if (GameState.opMode) player.fullHeal();
        GameState.playerHp = player.getHp();

        if (!player.isAlive()) {
            gameLoop.stop();
            SceneManager.showGameOver();
            return;
        }

        // Portal: EXIT always has one; BOSS only after cleared
        if (currentRoom != null) {
            boolean hasPortal = currentRoom.type == Room.Type.EXIT
                    || (currentRoom.type == Room.Type.BOSS && currentRoom.isCleared());
            if (hasPortal) {
                double cx = currentRoom.worldX + currentRoom.worldW / 2.0;
                double cy = currentRoom.worldY + currentRoom.worldH / 2.0;
                if (Math.hypot(player.getCenterX() - cx, player.getCenterY() - cy) < 50) {
                    advanceFloor();
                    return;
                }
            }
        }
    }

    // ── Render (called by GameLoop) ────────────────────────────────────────
    public void render() {
        gc.setFill(Color.rgb(10, 10, 18));
        gc.fillRect(0, 0, GameApp.WIDTH, GameApp.HEIGHT);

        // Camera shake offset (fades out as shakeTimer → 0)
        double shakeX = 0, shakeY = 0;
        if (shakeTimer > 0) {
            double t   = shakeTimer / shakeDuration; // 1.0 → 0.0
            double amp = shakeIntensity * t;
            shakeX = (Math.random() * 2 - 1) * amp;
            shakeY = (Math.random() * 2 - 1) * amp;
        }

        gc.save();
        gc.translate(-camX() + shakeX, -camY() + shakeY);
        dungeon.draw(gc);
        // Dev shop overlay (drawn over current room floor but under player)
        if (devRewardOverlay != null) devRewardOverlay.draw(gc);
        // 火焰（地板層，在玩家下方）
        for (FirePatch f : firePatches) f.draw(gc);
        // LeBron 技能動畫
        if (lebronSkill != null) lebronSkill.draw(gc);
        for (Bullet b : player.getBullets()) b.draw(gc);
        player.draw(gc);
        gc.restore();

        drawHUD();
        drawMiniMap();
        console.draw(gc, GameApp.WIDTH, GameApp.HEIGHT);
    }

    // ── Reward room interaction ────────────────────────────────────────────
    private void handleRewardInteract() {
        if (devRewardOverlay != null) { devRewardOverlay.onInteract(player); return; }
        if (currentRoom instanceof RewardRoom rr) rr.onInteract(player);
    }

    /** 1 / 2 / 3 keys — buy from shop (dev overlay or real reward room). */
    private void handleShopBuy(int index) {
        if (devRewardOverlay != null) { devRewardOverlay.buy(player, index); return; }
        if (currentRoom instanceof RewardRoom rr) rr.buy(player, index);
    }

    // ── Tab completion ─────────────────────────────────────────────────────

    /**
     * Called when Tab is pressed while the console is open.
     *
     * Behaviour:
     *   • Empty buffer or partial command (no space yet) → complete the command name.
     *   • After command + space → complete the argument for that command.
     *   • Each Tab press cycles to the next matching option.
     *   • Typing any character resets the cycle (handled inside CommandConsole).
     */
    private void handleTabCompletion() {
        // ── Already cycling: just advance, don't re-filter from buffer ────
        if (console.isInCompletionCycle()) {
            console.advanceCycle();
            return;
        }

        // ── Fresh Tab press: compute options from current buffer ───────────
        String input = console.getBuffer();

        // No slash yet → show all commands
        if (input.isEmpty() || !input.startsWith("/")) {
            String low = input.toLowerCase();
            List<String> opts = CMD_LIST.stream()
                    .filter(c -> c.startsWith(low.isEmpty() ? "/" : low))
                    .collect(Collectors.toList());
            console.applyTab(opts, "");
            return;
        }

        int spaceIdx = input.indexOf(' ');
        if (spaceIdx < 0) {
            // Still typing the command name
            String low = input.toLowerCase();
            List<String> opts = CMD_LIST.stream()
                    .filter(c -> c.startsWith(low))
                    .collect(Collectors.toList());
            // Exact match for a command that takes args → auto-add trailing space
            if (opts.size() == 1 && opts.get(0).equals("/summon")
                    && input.equalsIgnoreCase("/summon")) {
                console.applyTab(List.of("/summon "), "");
            } else {
                console.applyTab(opts, "");
            }
        } else {
            // Command is complete; complete the argument
            String cmd    = input.substring(0, spaceIdx).toLowerCase();
            String arg    = input.substring(spaceIdx + 1);    // partial arg typed so far
            String prefix = input.substring(0, spaceIdx + 1); // "/summon "
            List<String> opts = getArgCompletions(cmd, arg);
            if (!opts.isEmpty()) console.applyTab(opts, prefix);
        }
    }

    /** Returns matching argument completions for the given command and partial arg. */
    private List<String> getArgCompletions(String cmd, String partial) {
        String low = partial.toLowerCase();
        return switch (cmd) {
            case "/summon" -> SUMMON_NAMES.stream()
                    .filter(n -> n.toLowerCase().startsWith(low))
                    .collect(Collectors.toList());
            default -> new ArrayList<>();
        };
    }

    // ── Command console execution ──────────────────────────────────────────
    private void executeCommand(String cmd) {
        if (cmd == null || cmd.isBlank()) return;
        if (!cmd.startsWith("/")) { console.showOutput("❌ 指令必須以 / 開頭"); return; }

        String[] parts = cmd.substring(1).trim().split("\\s+");
        switch (parts[0].toLowerCase()) {
            case "help"      -> execHelp();
            case "summon"    -> execSummon(parts);
            case "shop"      -> execShop();
            case "chest"     -> execChest();
            case "closeshop" -> execCloseReward();
            default          -> console.showOutput("❌ 未知指令：/" + parts[0] + "\n輸入 /help 查看所有指令");
        }
    }

    private void execHelp() {
        console.showOutput("""
                ── 指令清單 ──────────────────────
                /help              顯示此清單
                /summon <名稱>     在玩家旁召喚實體
                  可召喚：squirrel 松鼠、goose 鵝、termite 白蟻
                          wuxiaoguang 無小光、chenqinhan 沉沁汗
                          shiguozhen 濕幗針
                /shop              在當前房間疊加商店（按 1/2/3 購買）
                /chest             在當前房間疊加寶箱（按 E 開啟）
                /closeshop         關閉疊加商店 / 寶箱""", 7.5);
    }

    private void execSummon(String[] parts) {
        if (parts.length < 2) { console.showOutput("用法：/summon <名稱>"); return; }
        if (currentRoom == null) { console.showOutput("❌ 目前沒有房間"); return; }

        double sx = player.getCenterX() + 70;
        double sy = player.getCenterY();
        int    st = levelManager.getStage();

        Entity entity = switch (parts[1].toLowerCase()) {
            case "squirrel", "松鼠"     -> new Squirrel(sx, sy, st);
            case "goose",    "鵝"       -> new Goose(sx, sy, st);
            case "termite",  "白蟻"     -> new Termite(sx, sy, st);
            case "wuxiaoguang", "無小光" -> {
                WuXiaoGuang b = new WuXiaoGuang(sx, sy, st);
                b.setRoomBounds(currentRoom.worldX, currentRoom.worldY,
                        currentRoom.worldW, currentRoom.worldH);
                yield b;
            }
            case "chenqinhan", "沉沁汗" -> {
                ChenQinHan b = new ChenQinHan(sx, sy, st);
                b.setRoomBounds(currentRoom.worldX, currentRoom.worldY,
                        currentRoom.worldW, currentRoom.worldH);
                yield b;
            }
            case "shiguozhen", "濕幗針" -> {
                ShiGuoZhen b = new ShiGuoZhen(sx, sy, st);
                b.setRoomBounds(currentRoom.worldX, currentRoom.worldY,
                        currentRoom.worldW, currentRoom.worldH);
                yield b;
            }
            default -> null;
        };

        if (entity == null) {
            console.showOutput("❌ 未知實體：" + parts[1] + "\n輸入 /help 查看可召喚清單");
            return;
        }
        currentRoom.getEnemies().add(entity);
        console.showOutput("✔ 已召喚：" + parts[1]);
    }

    // ── Dev shop overlay ───────────────────────────────────────────────────

    /**
     * Overlays a SHOP RewardRoom on top of the current room in world space.
     * The overlay shares the exact same worldX/worldY as the current room
     * (constructed with the same gridX/gridY), so the tables and merchant
     * appear at the room centre as if a real shop room had been generated there.
     *
     * The overlay is drawn after dungeon.draw(), E-key interactions are routed
     * to it first, and it is cleared automatically when advancing floors.
     */
    private void execShop() {
        if (currentRoom == null) { console.showOutput("❌ 目前沒有房間"); return; }
        devRewardOverlay = new RewardRoom(
                currentRoom.gridX, currentRoom.gridY, RewardRoom.RewardType.SHOP,
                GameState.currentStage);
        devRewardOverlay.activate();
        console.showOutput("✔ 商店已疊加於當前房間\n按 1/2/3 購買，/closeshop 關閉");
    }

    private void execChest() {
        if (currentRoom == null) { console.showOutput("❌ 目前沒有房間"); return; }
        devRewardOverlay = new RewardRoom(
                currentRoom.gridX, currentRoom.gridY, RewardRoom.RewardType.CHEST,
                GameState.currentStage);
        devRewardOverlay.activate();
        console.showOutput("✔ 寶箱已疊加於當前房間\n按 E 開啟，/closeshop 關閉");
    }

    private void execCloseReward() {
        if (devRewardOverlay == null) { console.showOutput("目前沒有開啟疊加物件"); return; }
        devRewardOverlay = null;
        console.showOutput("✔ 已關閉");
    }

    // ── Floor advancement ──────────────────────────────────────────────────
    private void advanceFloor() {
        if (levelManager.hasNextFloor()) {
            levelManager.advanceFloor();
            loadFloor();
        } else {
            gameLoop.stop();
            SceneManager.showVictory();
        }
    }

    // ── Collision resolution ───────────────────────────────────────────────
    private void resolveCollisions() {
        if (currentRoom == null) return;

        List<Entity> enemies = currentRoom.getEnemies();
        // 玩家目前所在的房間（走廊中為 null）
        Room playerRoom = dungeon.getRoomAt(player.getCenterX(), player.getCenterY());

        for (Bullet b : player.getBullets()) {
            if (!b.isAlive()) continue;

            boolean hitWall = !dungeon.canMoveTo(b.getX(), b.getY(), b.getWidth(), b.getHeight());
            if (!hitWall && currentRoom instanceof NormalRoom nr) {
                hitWall = nr.blockedByObstacle(b.getX(), b.getY(), b.getWidth(), b.getHeight());
            }

            if (hitWall) {
                b.hit();  // 呼叫 hit() 讓子彈失效/播放消失動畫
                continue; // 既然已經撞牆，就不需要再檢查有沒有打到敵人了，直接換下一顆子彈
            }

            // 子彈進入「玩家不在其中」的未通關戰鬥房間 → 視為撞牆消失
            Room bulletRoom = dungeon.getRoomAt(b.getCenterX(), b.getCenterY());
            if (bulletRoom != null && bulletRoom != playerRoom
                    && !bulletRoom.isCleared()
                    && (bulletRoom.type == Room.Type.NORMAL || bulletRoom.type == Room.Type.BOSS)) {
                b.hit();
                continue;
            }

            for (Entity e : enemies) {

                // ▼▼▼ 修改點 1：無敵狀態，子彈直接穿透正在生成的怪物 ▼▼▼
                if (e instanceof Enemy en && en.isSpawning()) continue;
                // ▲▲▲ 修改結束 ▲▲▲

                if (e.isAlive() && CollisionUtil.overlaps(b, e)) {
                    e.takeDamage(GameState.opMode ? 9999 : b.getDamage());
                    b.hit();
                    GameState.score += 10;
                    // ── Apply book effects on hit ──────────────────────────
                    if (e instanceof Enemy en) {
                        if (GameState.bookEffects.contains(GameState.BookEffect.SLOW_SHOT))
                            en.applySlow(2.0);
                        if (GameState.bookEffects.contains(GameState.BookEffect.BURN_SHOT))
                            en.applyBurn(2.0);
                    }
                    break;
                }
            }
        }

        for (Bullet b : currentRoom.getEnemyBullets()) {
            if (!b.isAlive()) continue;

            // ▼▼▼ 1. 先判斷敵人子彈是否撞牆或碰到障礙物 ▼▼▼
            boolean hitWall = !dungeon.canMoveTo(b.getX(), b.getY(), b.getWidth(), b.getHeight());
            if (!hitWall && currentRoom instanceof NormalRoom nr) {
                hitWall = nr.blockedByObstacle(b.getX(), b.getY(), b.getWidth(), b.getHeight());
            }

            // 如果撞牆了，子彈直接失效，換下一顆子彈
            if (hitWall && !b.isIgnoreWalls()) {
                b.hit();
                continue;
            }
            // ▲▲▲ 撞牆判定結束 ▲▲▲

            // 2. 如果沒撞牆，才判斷是否打中玩家
            if (CollisionUtil.overlaps(b, player)) {
                player.takeDamage(b.getDamage());
                b.hit();
                if (b instanceof EffectBullet eb) eb.applyEffect(player);
            }
        }

        if (contactCooldown <= 0) {
            for (Entity e : enemies) {

                // ▼▼▼ 修改點 2：生成中的怪物不會對玩家造成碰撞傷害 ▼▼▼
                if (e instanceof Enemy en && en.isSpawning()) continue;
                // ▲▲▲ 修改結束 ▲▲▲
                if (e.isAlive() && CollisionUtil.overlaps(e, player)) {
                    int dmg = (e instanceof Enemy en) ? en.getContactDamage() : 5;
                    player.takeDamage(dmg);
                    contactCooldown = 0.5;

                    // Goose charge hit: stun + camera shake
                    if (e instanceof Goose g && g.isCharging() && !GameState.opMode) {
                        player.applyStun(0.3);
                        triggerShake(0.3, g.isBerserk() ? 14.0 : 9.0);
                    }
                    break;
                }
            }
        }
    }

    // ── HUD ────────────────────────────────────────────────────────────────
    private void drawHUD() {
        double barW = 200, barH = 20;

        // HP bar
        gc.setFill(Color.DARKRED);
        gc.fillRect(10, 10, barW, barH);
        gc.setFill(Color.LIMEGREEN);
        gc.fillRect(10, 10, barW * (double) GameState.playerHp / GameState.playerMaxHp, barH);
        gc.setStroke(Color.WHITE);
        gc.strokeRect(10, 10, barW, barH);
        gc.setFill(Color.WHITE);
        gc.fillText("HP: " + GameState.playerHp + " / " + GameState.playerMaxHp, 15, 25);

        // Stage / Floor label
        String floorLabel = levelManager.getFloorNum() == 3
                ? "Boss Floor" : "Floor " + levelManager.getFloorNum();
        gc.setFill(Color.WHITE);
        gc.fillText("Stage " + levelManager.getStage() + "  " + floorLabel,
                GameApp.WIDTH / 2.0 - 60, 25);
        // ── Score: coin icon + number, below HP bar (top-left) ────────────
        double coinSz = 56;
        double coinX  = 10;
        double coinY  = 35;   // just below the HP bar (bar ends at y=30)
        if (HUD_COIN_SHEET != null) {
            double fw = HUD_COIN_SHEET.getWidth() / HUD_COIN_FRAMES;
            double fh = HUD_COIN_SHEET.getHeight();
            gc.drawImage(HUD_COIN_SHEET,
                    hudCoinFrame * fw, 0, fw, fh,
                    coinX, coinY, coinSz, coinSz);
        }
        Font prevFont = gc.getFont();
        gc.setFont(PIXEL_FONT_HUD != null ? PIXEL_FONT_HUD : prevFont);
        gc.setFill(Color.GOLD);
        gc.fillText(String.valueOf(GameState.score), coinX + coinSz + 6, coinY + coinSz / 2.0 + 6);
        gc.setFont(prevFont);

        // Dev / OP tags
        if (GameState.devMode) {
            gc.setFill(Color.YELLOW);
            gc.fillText("[ DEV  F1=off  N=skip  F2=OP ]", 10, 50);
        }
        if (GameState.opMode) {
            gc.setFill(Color.rgb(255, 80, 255));
            gc.fillText("[ OP  ∞HP  9999DMG  免疫 ]", 10, 68);
        }

        // ── Book effects panel ─────────────────────────────────────────────
        if (!GameState.bookEffects.isEmpty()) {
            double bx = 10, by = GameApp.HEIGHT - 14;

            // Row for each active effect (drawn bottom-to-top)
            for (GameState.BookEffect fx : GameState.bookEffects) {
                switch (fx) {
                    case EXAM_QUESTIONS -> {
                        gc.setFill(Color.color(1.0, 0.92, 0.2, 0.9));
                        gc.fillText("📖 明年考古題  ATK ×" +
                                String.format("%.1f", GameState.damageMultiplier), bx, by);
                    }
                    case SLOW_SHOT -> {
                        gc.setFill(Color.color(0.4, 0.85, 1.0, 0.9));
                        gc.fillText("📖 緩速射擊  命中使敵人緩速", bx, by);
                    }
                    case BURN_SHOT -> {
                        gc.setFill(Color.color(1.0, 0.55, 0.1, 0.9));
                        gc.fillText("📖 燃燒射擊  命中使敵人燃燒", bx, by);
                    }
                }
                by -= 18;
            }

            // Small "書本強化" header above the effect rows
            gc.setFill(Color.color(1.0, 1.0, 1.0, 0.55));
            gc.fillText("── 書本強化 ──", bx, by);
        }
        if (GameState.selectedTalent == GameState.Talent.LEBRON) {
            boolean used = GameState.talentUsedThisStage;
            gc.setFont(PIXEL_FONT_14 != null ? PIXEL_FONT_14 : Font.font(14));
            gc.setFill(used ? Color.GRAY : Color.web("#FDB927"));
            gc.fillText(used ? "LeBron [Q] ——" : "LeBron [Q] 就緒", 10, GameApp.HEIGHT - 15);
        }
        // ▼▼▼▼▼ 新增：右下角衝刺體力條 ▼▼▼▼▼
        double dashCd = player.getDashCooldownTimer();
        double dashMaxCd = player.getDashCooldownMax();

        // 計算體力比例：冷卻時間為 0 時比例是 1.0 (滿檔)；剛衝刺完是 0.0。
        double staminaRatio = (dashMaxCd > 0) ? Math.max(0, 1.0 - (dashCd / dashMaxCd)) : 1.0;

        double staminaBarW = 150; // 體力條寬度
        double staminaBarH = 16;  // 體力條高度
        double staminaBarX = GameApp.WIDTH - staminaBarW - 20;  // 靠右 (留 20px 邊距)
        double staminaBarY = GameApp.HEIGHT - staminaBarH - 20; // 靠下 (留 20px 邊距)

        // 1. 畫底色 (空體力時的暗灰色背景)
        gc.setFill(Color.color(0.2, 0.2, 0.2, 0.8));
        gc.fillRect(staminaBarX, staminaBarY, staminaBarW, staminaBarH);

        // 2. 畫目前的體力進度
        if (staminaRatio >= 1.0) {
            // 滿體力時顯示亮藍色 (代表可以衝刺)
            gc.setFill(Color.web("#00E5FF"));
        } else {
            // 還在冷卻讀秒時，顯示暗藍綠色 (或改成黃色、灰色)
            gc.setFill(Color.web("#006680"));
        }
        // 寬度會根據 staminaRatio 變動 (從 0 慢慢長大到 staminaBarW)
        gc.fillRect(staminaBarX, staminaBarY, staminaBarW * staminaRatio, staminaBarH);

        // 3. 畫外框
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1.5);
        gc.strokeRect(staminaBarX, staminaBarY, staminaBarW, staminaBarH);

        // 4. 畫文字提示
        gc.setFill(Color.WHITE);
        gc.setFont(PIXEL_FONT_14 != null ? PIXEL_FONT_14 : Font.font(14));
        String staminaText = (staminaRatio >= 1.0) ? "衝刺 READY [SPACE]" : "體力回復中...";
        // 把文字畫在體力條的上方
        gc.fillText(staminaText, staminaBarX, staminaBarY - 8);
        // ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲

        // ── Room-clear banner ─────────────────────────────────────────────
        if (clearBannerTimer > 0) {
            // Fade out during the last 0.8 s
            double alpha = Math.min(1.0, clearBannerTimer / 0.8);
            Font   font  = (PIXEL_FONT_LARGE != null) ? PIXEL_FONT_LARGE : Font.font(52);
            gc.setFont(font);

            String text  = "CLEAR!";
            double textX = GameApp.WIDTH  / 2.0 - 90;
            double textY = GameApp.HEIGHT / 4.0;       // 上方 1/4 處

            // Drop shadow
            gc.setGlobalAlpha(alpha * 0.6);
            gc.setFill(Color.BLACK);
            gc.fillText(text, textX + 3, textY + 3);

            // Main text — gold gradient effect via two layers
            gc.setGlobalAlpha(alpha);
            gc.setFill(Color.color(1.0, 0.85, 0.1));
            gc.fillText(text, textX, textY);

            gc.setGlobalAlpha(1.0);
            gc.setFont(PIXEL_FONT_14 != null ? PIXEL_FONT_14 : Font.font(14)); // restore default font
        }
    }

    // ── Mini-map (fog of war) ──────────────────────────────────────────────
    private void drawMiniMap() {
        int    G       = dungeon.getGridSize();
        double cell    = 14;
        double gap     = 2;
        double total   = G * (cell + gap);
        double originX = GameApp.WIDTH  - total - 12;
        double originY = 10;

        Room[][] grid = dungeon.getGrid();

        for (int y = 0; y < G; y++) {
            for (int x = 0; x < G; x++) {
                Room r = grid[y][x];
                if (r == null) continue;
                if (!isVisibleOnMap(r, x, y)) continue; // fog of war

                boolean visited = visitedRooms.contains(r);

                double rx = originX + x * (cell + gap);
                double ry = originY + y * (cell + gap);

                // Revealed-but-unvisited rooms show only as a neutral tile
                Color fill;
                if (!visited) {
                    fill = Color.rgb(50, 50, 55);
                } else if (r == currentRoom) {
                    fill = Color.WHITE;
                } else if (r.type == Room.Type.BOSS) {
                    fill = Color.DARKRED;
                } else if (r.type == Room.Type.EXIT) {
                    fill = Color.GOLDENROD;
                } else if (r.type == Room.Type.REWARD) {
                    fill = Color.GOLD;
                } else if (r.type == Room.Type.START) {
                    fill = Color.STEELBLUE;
                } else if (r.isCleared()) {
                    fill = Color.rgb(50, 110, 50);
                } else {
                    fill = Color.rgb(70, 70, 70);
                }
                gc.setFill(fill);
                gc.fillRect(rx, ry, cell, cell);

                // Corridor connectors between visible rooms
                gc.setFill(Color.rgb(110, 90, 55));
                if (r.getHasCorridor(2) && x + 1 < G && grid[y][x + 1] != null) // EAST
                    gc.fillRect(rx + cell, ry + cell / 2 - 1, gap, 3);
                if (r.getHasCorridor(1) && y + 1 < G && grid[y + 1][x] != null) // SOUTH
                    gc.fillRect(rx + cell / 2 - 1, ry + cell, 3, gap);
            }
        }

        gc.setStroke(Color.rgb(100, 100, 100));
        gc.setLineWidth(1);
        gc.strokeRect(originX - 3, originY - 3, total + 5, total + 5);
    }

    /**
     * A room is visible on the mini-map when it has been visited OR when it is
     * directly connected (one corridor hop) to a visited room.
     * Rooms two or more hops away remain hidden.
     */
    private boolean isVisibleOnMap(Room r, int gx, int gy) {
        if (visitedRooms.contains(r)) return true;
        Room[][] grid = dungeon.getGrid();
        int G = dungeon.getGridSize();
        for (int dir = 0; dir < 4; dir++) {
            if (!r.getHasCorridor(dir)) continue;
            int nx = gx + DIR_DX[dir], ny = gy + DIR_DY[dir];
            if (nx < 0 || nx >= G || ny < 0 || ny >= G) continue;
            Room neighbor = grid[ny][nx];
            if (neighbor != null && visitedRooms.contains(neighbor)) return true;
        }
        return false;
    }
}

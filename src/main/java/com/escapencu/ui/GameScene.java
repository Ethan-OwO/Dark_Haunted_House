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
import com.escapencu.level.RewardRoom;
import com.escapencu.level.Room;
import com.escapencu.util.CollisionUtil;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
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

    private LeBronSkill            lebronSkill = null;
    private final List<FirePatch>  firePatches = new ArrayList<>();
    private StackPane              rootPane    = null;

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

    /** Trigger a camera shake. intensity = max pixel offset. */
    private void triggerShake(double duration, double intensity) {
        shakeTimer    = duration;
        shakeDuration = duration;
        shakeIntensity = intensity;
    }

    // ── Tab-completion data (update when new commands / entities are added) ──
    /** All available slash-commands. */
    private static final List<String> CMD_LIST = List.of("/help", "/summon");

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

            // ── Normal game input ──────────────────────────────────────────
            pressedKeys.add(e.getCode());
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
        canvas.setOnMouseClicked(e -> { if (!console.isOpen()) player.shoot(); });

        gameLoop = new GameLoop(this);
        gameLoop.start();

        rootPane = new StackPane(canvas);
        Scene scene = new Scene(rootPane, GameApp.WIDTH, GameApp.HEIGHT);
        canvas.requestFocus();
        return scene;
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
        lebronSkill = null;
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
        if (currentRoom == null) return dungeon::canMoveTo;
        if (currentRoom.isCleared()) return dungeon::canMoveTo;
        if (currentRoom.type != Room.Type.NORMAL && currentRoom.type != Room.Type.BOSS)
            return dungeon::canMoveTo;

        // Only enforce lock while the player is fully inside the room
        double e = 0.5;
        double px = player.getX(), py = player.getY();
        double pw = player.getWidth(), ph = player.getHeight();
        final Room r = currentRoom;
        boolean fullyInside = r.containsPoint(px + e,      py + e)
                           && r.containsPoint(px + pw - e, py + e)
                           && r.containsPoint(px + e,      py + ph - e)
                           && r.containsPoint(px + pw - e, py + ph - e);

        if (!fullyInside) return dungeon::canMoveTo;

        return (npx, npy, npw, nph) ->
            r.containsPoint(npx + e,        npy + e)
         && r.containsPoint(npx + npw - e,  npy + e)
         && r.containsPoint(npx + e,        npy + nph - e)
         && r.containsPoint(npx + npw - e,  npy + nph - e);
    }

    // ── Update (called by GameLoop) ────────────────────────────────────────
    public void update(double deltaTime) {
        console.update(deltaTime);
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
        Room detected = dungeon.getRoomAt(player.getCenterX(), player.getCenterY());
        if (detected != null) {
            if (!detected.isActivated()) detected.activate();

            if (detected != currentRoom) {
                boolean needsFullEntry = !detected.isCleared()
                        && (detected.type == Room.Type.NORMAL
                         || detected.type == Room.Type.BOSS);
                if (!needsFullEntry) {
                    currentRoom = detected;
                    visitedRooms.add(currentRoom);
                } else {
                    double e = 0.5;
                    double px = player.getX(), py = player.getY();
                    double pw = player.getWidth(), ph = player.getHeight();
                    if (detected.containsPoint(px + e,      py + e)
                     && detected.containsPoint(px + pw - e, py + e)
                     && detected.containsPoint(px + e,      py + ph - e)
                     && detected.containsPoint(px + pw - e, py + ph - e)) {
                        currentRoom = detected;
                        visitedRooms.add(currentRoom);
                    }
                }
            }
        }

        dungeon.update(deltaTime, player);

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
        if (currentRoom instanceof RewardRoom rr) rr.onInteract(player);
    }

    private void handleShopBuy(int index) {
        if (currentRoom instanceof RewardRoom rr) rr.onShopBuy(player, index);
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
            case "help"   -> execHelp();
            case "summon" -> execSummon(parts);
            default       -> console.showOutput("❌ 未知指令：/" + parts[0] + "\n輸入 /help 查看所有指令");
        }
    }

    private void execHelp() {
        console.showOutput("""
                ── 指令清單 ──────────────────────
                /help              顯示此清單
                /summon <名稱>     在玩家旁召喚實體
                  可召喚：squirrel 松鼠、goose 鵝、termite 白蟻
                          wuxiaoguang 無小光、chenqinhan 沉沁汗
                          shiguozhen 濕幗針""", 6.0);
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

        for (Bullet b : player.getBullets()) {
            if (!b.isAlive()) continue;
            for (Entity e : enemies) {
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
            if (b.isAlive() && CollisionUtil.overlaps(b, player)) {
                player.takeDamage(b.getDamage());
                b.hit();
                if (b instanceof EffectBullet eb) eb.applyEffect(player);
            }
        }

        if (contactCooldown <= 0) {
            for (Entity e : enemies) {
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
        gc.fillText("分數: " + GameState.score, GameApp.WIDTH - 120.0, 25);

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
            gc.setFont(Font.font(14));
            gc.setFill(used ? Color.GRAY : Color.web("#FDB927"));
            gc.fillText(used ? "LeBron [Q] ——" : "LeBron [Q] 就緒", 10, GameApp.HEIGHT - 15);
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

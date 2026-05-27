package com.escapencu.level;

import com.escapencu.core.GameState;
import com.escapencu.core.GameState.BookEffect;
import com.escapencu.entity.Player;
import com.escapencu.util.FloorTileRenderer;
import com.escapencu.util.ResourceLoader;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.Random;

/**
 * Reward room — no enemies, one of four subtypes:
 *   POTION  : walk over to heal 30 HP
 *   NPC     : talk to 膏華龍, receive 明年的考古題 (damage ×1.5)
 *   CHEST   : press E for a random buff
 *   SHOP    : walk up to a table and press E to buy
 */
public class RewardRoom extends Room {

    public enum RewardType { POTION, CHEST, SHOP }

    // ── Shop item data ─────────────────────────────────────────────────────
    private static final String[] SHOP_NAMES  = {
        "回血藥水\n(+30 HP)",
        "傷害強化符\n(傷害+50%)",
        "能量飲料\n(速度+20%)"
    };
    // Prices scale with stage: Stage 1 → base, Stage 2 → ×1.75, Stage 3 → ×2.5
    private static final int[][] SHOP_PRICES_BY_STAGE = {
        {  80, 200, 120 },   // Stage 1
        { 140, 350, 210 },   // Stage 2
        { 200, 500, 300 },   // Stage 3
    };

    /** Returns the price array for the current stage (clamped to valid range). */
    private int[] shopPrices() {
        int idx = Math.max(0, Math.min(GameState.currentStage - 1, 2));
        return SHOP_PRICES_BY_STAGE[idx];
    }

    // ── Chest spritesheet (frame 0 = closed, frame 1 = open) ──────────────
    private static final Image CHEST_SHEET = ResourceLoader.getImage("/images/2D Chests & Coins/Chest.png", false);
    // Coin spritesheet for shop price icons
    private static final Image COIN_SHEET  = ResourceLoader.getImage("/images/2D Chests & Coins/Coin.png", false);

    // ── Shop sprites ───────────────────────────────────────────────────────
    private static final Image IMG_SHOPKEEPER = ResourceLoader.getImage("/images/shop/shopkeeper.png",      false);
    private static final Image IMG_SHOP_TABLE = ResourceLoader.getImage("/images/shop/shop_table.png",      false);
    private static final Image IMG_POTION     = ResourceLoader.getImage("/images/shop/potion_health.png",   false);
    private static final Image IMG_TALISMAN   = ResourceLoader.getImage("/images/shop/talisman_damage.png", false);
    private static final Image IMG_DRINK      = ResourceLoader.getImage("/images/shop/energy_drink.png",    false);

    // ── Shop font ──────────────────────────────────────────────────────────
    private static final Font SHOP_FONT;
    static {
        Font f = Font.loadFont(
                RewardRoom.class.getResourceAsStream("/fonts/Cubic_11.ttf"), 14);
        SHOP_FONT = (f != null) ? f : Font.font("SansSerif", 14);
    }

    private final RewardType rewardType;
    private final int stage;
    private boolean  collected  = false; // POTION / CHEST picked up
    private final boolean[] shopSold = new boolean[3];

    private String dialogText  = null;
    private double dialogTimer = 0;

    // ── Shop coin animation ────────────────────────────────────────────────
    private double coinAnimTimer = 0;
    private int    coinAnimFrame = 0;
    private static final int    COIN_FRAMES   = 6;
    private static final double COIN_FRAME_DUR = 0.10;

    /** Stable tile-index map generated once at construction (seeded by grid pos). */
    private final int[][] floorTilemap;

    public RewardRoom(int gridX, int gridY, RewardType type, int stage) {
        super(gridX, gridY, Room.Type.REWARD);
        this.rewardType = type;
        this.stage      = stage;
        this.floorTilemap = FloorTileRenderer.generateTilemap(
                gridX, gridY, worldW - WALL * 2, worldH - WALL * 2, stage);
    }

    @Override
    protected void spawnEnemies() { /* no enemies in reward rooms */ }

    // ── Update ─────────────────────────────────────────────────────────────
    @Override
    public void update(double deltaTime, Player player) {
        if (dialogTimer > 0) {
            dialogTimer -= deltaTime;
            if (dialogTimer <= 0) dialogText = null;
        }
        // Advance coin animation (used in SHOP display)
        coinAnimTimer += deltaTime;
        if (coinAnimTimer >= COIN_FRAME_DUR) {
            coinAnimTimer -= COIN_FRAME_DUR;
            coinAnimFrame = (coinAnimFrame + 1) % COIN_FRAMES;
        }

        // POTION: auto-pickup when player walks close enough
        if (rewardType == RewardType.POTION && !collected
                && nearCenter(player, 50)) {
            player.heal(30);
            collected = true;
            showDialog("回血 +30！");
        }
    }

    // ── Called by GameScene on E key (POTION / CHEST only) ────────────────
    public void onInteract(Player player) {
        switch (rewardType) {
            case CHEST  -> interactChest(player);
            case POTION -> {}
            case SHOP   -> {}   // shop uses buy(player, 0/1/2) via number keys
        }
    }

    // ── Called by GameScene on keys 1 / 2 / 3 ─────────────────────────────
    public void buy(Player player, int index) {
        if (rewardType != RewardType.SHOP) return;
        if (index < 0 || index > 2)        return;
        if (!nearCenter(player, 200))      { showDialog("請靠近商人！");                               return; }
        if (shopSold[index])               { showDialog("已售出！");                                   return; }
        if (GameState.score < shopPrices()[index]) {
            showDialog("分數不足！需要 " + shopPrices()[index] + " 分");                                return;
        }
        GameState.score -= shopPrices()[index];
        shopSold[index] = true;
        applyShopItem(player, index);
    }

    // ── Internal interactions ──────────────────────────────────────────────
    private void interactChest(Player player) {
        if (!nearCenter(player, 130)) return;
        if (collected) { showDialog("寶箱已開啟"); return; }
        collected = true;
        switch (new Random().nextInt(5)) {
            case 0 -> { player.heal(50);
                        showDialog("獲得：急救包 (+50 HP)"); }
            case 1 -> { GameState.damageMultiplier *= 1.2;
                        showDialog("獲得：傷害強化符 (傷害+20%)"); }
            case 2 -> { GameState.speedMultiplier *= 1.05;
                        showDialog("獲得：快跑鞋 (移動速度永久 +5%)"); }
            case 3 -> {
                GameState.bookEffects.add(BookEffect.SLOW_SHOT);
                showDialog("書本強化：緩速射擊\n子彈命中使敵人緩速 2 秒！", 3.5);
            }
            case 4 -> {
                GameState.bookEffects.add(BookEffect.BURN_SHOT);
                showDialog("書本強化：燃燒射擊\n子彈命中使敵人燃燒 2 秒！", 3.5);
            }
        }
    }

    private void applyShopItem(Player player, int i) {
        switch (i) {
            case 0 -> { player.heal(30);
                        showDialog("購買成功！回血 +30"); }
            case 1 -> { GameState.bookEffects.add(BookEffect.EXAM_QUESTIONS);
                        GameState.damageMultiplier = Math.max(GameState.damageMultiplier, 1.5);
                        showDialog("購買成功！書本強化：傷害提升 50%"); }
            case 2 -> { player.applySpeedBoost(1.2, 30.0);
                        showDialog("購買成功！速度提升 20%，30秒"); }
        }
    }

    // ── Draw ───────────────────────────────────────────────────────────────
    @Override
    protected void drawFloor(GraphicsContext gc) {
        // ── Wall area: stage-matched solid colour (same palette as NormalRoom) ──
        gc.setFill(wallColor());
        gc.fillRect(worldX, worldY, worldW, worldH);

        // ── Tiled floor (inner walkable area) ─────────────────────────────────
        FloorTileRenderer.draw(gc, floorTilemap, stage, worldX + WALL, worldY + WALL);

        // ── Stage 2: dampen bright blue tiles ─────────────────────────────────
        if (stage == 2) {
            gc.setFill(Color.color(0.0, 0.02, 0.15, 0.32));
            gc.fillRect(worldX + WALL, worldY + WALL, worldW - WALL * 2, worldH - WALL * 2);
        }

        // ── Subtle golden tint over inner floor — marks reward room as special ──
        gc.setFill(Color.color(0.55, 0.45, 0.05, 0.18));
        gc.fillRect(worldX + WALL, worldY + WALL, worldW - WALL * 2, worldH - WALL * 2);

        // ── Inner edge line ────────────────────────────────────────────────────
        gc.setFill(wallEdgeColor());
        gc.fillRect(worldX + WALL,                    worldY + WALL,              worldW - WALL * 2, 3);
        gc.fillRect(worldX + WALL,                    worldY + worldH - WALL - 3, worldW - WALL * 2, 3);
        gc.fillRect(worldX + WALL,                    worldY + WALL,              3, worldH - WALL * 2);
        gc.fillRect(worldX + worldW - WALL - 3,       worldY + WALL,              3, worldH - WALL * 2);

        double cx = worldX + worldW / 2.0;
        double cy = worldY + worldH / 2.0;

        switch (rewardType) {
            case POTION -> drawPotion(gc, cx, cy);
            case CHEST  -> drawChest(gc, cx, cy);
            case SHOP   -> drawShop(gc, cx, cy);
        }

        if (dialogText != null) drawDialog(gc, cx, cy);
    }

    /** Solid wall colour — delegates to shared helper in Room. */
    private Color wallColor()     { return stageWallColor(stage); }

    /** Dark edge strip colour — delegates to shared helper in Room. */
    private Color wallEdgeColor() { return stageWallEdgeColor(stage); }

    private void drawPotion(GraphicsContext gc, double cx, double cy) {
        if (collected) return;
        gc.setFill(Color.LIMEGREEN);
        gc.fillOval(cx - 12, cy - 16, 24, 28);
        gc.setFill(Color.rgb(80, 200, 80));
        gc.fillRect(cx - 6, cy - 24, 12, 10);
        gc.setFill(Color.WHITE);
        gc.fillText("回血藥水（走過拾取）", cx - 52, cy + 28);
    }

    private void drawChest(GraphicsContext gc, double cx, double cy) {
        double chestW = 96, chestH = 80;
        double chestX = cx - chestW / 2.0;
        double chestY = cy - chestH / 2.0;

        Font prevFont = gc.getFont();
        gc.setFont(SHOP_FONT);
        gc.setTextAlign(TextAlignment.CENTER);

        if (CHEST_SHEET != null) {
            // Spritesheet: 2 frames — 0 = closed, 1 = open
            double fw = CHEST_SHEET.getWidth() / 2.0;
            double fh = CHEST_SHEET.getHeight();
            int frame = collected ? 1 : 0;
            gc.drawImage(CHEST_SHEET,
                    frame * fw, 0, fw, fh,
                    chestX, chestY, chestW, chestH);
        } else {
            // Fallback rectangles
            gc.setFill(collected ? Color.rgb(100, 70, 30) : Color.rgb(139, 90, 43));
            gc.fillRect(chestX, chestY, chestW, chestH);
            if (!collected) {
                gc.setFill(Color.GOLD);
                gc.fillRect(chestX, chestY, chestW, 8);
            }
        }

        gc.setFill(collected ? Color.GRAY : Color.GOLD);
        gc.fillText(collected ? "已開啟" : "[ E ] 開啟", cx, chestY + chestH + 20);

        gc.setFont(prevFont);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawShop(GraphicsContext gc, double cx, double cy) {
        // ── Layout anchors ─────────────────────────────────────────────────
        double tableW  = 340, tableH  = 160;
        double tableX  = cx - tableW  / 2.0;
        double tableY  = cy - 20;           // carpet / table starts here

        double keeperW = 90,  keeperH = 110;
        double keeperX = cx   - keeperW / 2.0;
        double keeperY = tableY - keeperH + 20; // keeper stands just above the table

        double iconSize = 70;               // 40 × 1.75 ≈ 70
        double spacing  = 115;             // column spacing
        double iconY    = tableY + 20;     // icons land on the carpet surface

        // ── Shopkeeper (draw first, table covers feet) ─────────────────────
        if (IMG_SHOPKEEPER != null)
            gc.drawImage(IMG_SHOPKEEPER, keeperX, keeperY, keeperW, keeperH);
        else {
            gc.setFill(Color.rgb(100, 60, 120));
            gc.fillRect(keeperX, keeperY, keeperW, keeperH);
        }

        // ── Shop table / carpet ────────────────────────────────────────────
        if (IMG_SHOP_TABLE != null)
            gc.drawImage(IMG_SHOP_TABLE, tableX, tableY, tableW, tableH);
        else {
            gc.setFill(Color.rgb(80, 50, 20));
            gc.fillRect(tableX, tableY, tableW, tableH);
        }

        // ── Font + text alignment ──────────────────────────────────────────
        Font  prevFont  = gc.getFont();
        gc.setFont(SHOP_FONT);
        gc.setTextAlign(TextAlignment.CENTER);

        // ── Three items on the carpet ──────────────────────────────────────
        Image[] icons = { IMG_POTION, IMG_TALISMAN, IMG_DRINK };
        double startCX = cx - spacing; // centre-x of first item column

        for (int i = 0; i < 3; i++) {
            boolean sold = shopSold[i];
            double icx = startCX + i * spacing;   // column centre

            // Icon (no frame)
            if (!sold && icons[i] != null)
                gc.drawImage(icons[i], icx - iconSize / 2.0, iconY, iconSize, iconSize);

            // Item name (first line, centred)
            gc.setFill(sold ? Color.GRAY : Color.WHITE);
            gc.fillText(SHOP_NAMES[i].split("\n")[0], icx, iconY + iconSize + 16);

            // Price + key hint (centred) — with animated coin icon
            if (!sold && COIN_SHEET != null) {
                double cfw  = COIN_SHEET.getWidth() / COIN_FRAMES;
                double cfh  = COIN_SHEET.getHeight();
                double cSz  = 14;
                double priceY = iconY + iconSize + 32;
                // Coin icon left of the text
                gc.drawImage(COIN_SHEET,
                        coinAnimFrame * cfw, 0, cfw, cfh,
                        icx - 38, priceY - cSz, cSz, cSz);
            }
            gc.setFill(sold ? Color.GRAY : Color.GOLD);
            gc.fillText(sold ? "已售出" : shopPrices()[i] + "  [" + (i + 1) + "]",
                        icx + 4, iconY + iconSize + 32);
        }

        // ── Restore gc state ───────────────────────────────────────────────
        gc.setFont(prevFont);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawDialog(GraphicsContext gc, double cx, double cy) {
        String[] lines = dialogText.split("\n");
        double boxW = 280;
        double boxH = 16 + lines.length * 22;
        double bx   = cx - boxW / 2;
        double by   = cy - 150;
        gc.setFill(Color.color(0, 0, 0, 0.78));
        gc.fillRoundRect(bx, by, boxW, boxH, 10, 10);
        gc.setStroke(Color.GOLD);
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(bx, by, boxW, boxH, 10, 10);
        gc.setFill(Color.WHITE);
        for (int i = 0; i < lines.length; i++)
            gc.fillText(lines[i], bx + 14, by + 18 + i * 22);
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private boolean nearCenter(Player player, double radius) {
        return Math.hypot(player.getCenterX() - (worldX + worldW / 2.0),
                          player.getCenterY() - (worldY + worldH / 2.0)) < radius;
    }

    private void showDialog(String text)                  { showDialog(text, 2.5); }
    private void showDialog(String text, double duration) { dialogText = text; dialogTimer = duration; }

    public RewardType getRewardType() { return rewardType; }
}

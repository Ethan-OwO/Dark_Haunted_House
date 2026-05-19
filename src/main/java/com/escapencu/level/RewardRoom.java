package com.escapencu.level;

import com.escapencu.core.GameState;
import com.escapencu.core.GameState.BookEffect;
import com.escapencu.entity.Player;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Random;

/**
 * Reward room — no enemies, one of four subtypes:
 *   POTION  : walk over to heal 30 HP
 *   NPC     : talk to 膏華龍, receive 明年的考古題 (damage ×1.3)
 *   CHEST   : press E for a random buff
 *   SHOP    : press 1/2/3 to buy items with score
 */
public class RewardRoom extends Room {

    public enum RewardType { POTION, NPC, CHEST, SHOP }

    private static final String[] SHOP_NAMES  = {
        "回血藥水\n(+30 HP)",
        "傷害強化符\n(傷害+30%)",
        "能量飲料\n(速度+20%)"
    };
    private static final int[] SHOP_PRICES = { 100, 200, 150 };

    private final RewardType rewardType;
    private boolean  collected  = false; // POTION / CHEST picked up
    private boolean  npcTalked  = false;
    private final boolean[] shopSold = new boolean[3];

    private String dialogText  = null;
    private double dialogTimer = 0;

    public RewardRoom(int gridX, int gridY, RewardType type) {
        super(gridX, gridY, Room.Type.REWARD);
        this.rewardType = type;
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
        // POTION: auto-pickup when player walks close enough
        if (rewardType == RewardType.POTION && !collected
                && nearCenter(player, 50)) {
            player.heal(30);
            collected = true;
            showDialog("回血 +30！");
        }
    }

    // ── Called by GameScene on E key ───────────────────────────────────────
    public void onInteract(Player player) {
        if (!nearCenter(player, 130)) return;
        switch (rewardType) {
            case NPC   -> interactNPC(player);
            case CHEST -> interactChest(player);
            case SHOP  -> showDialog("按 1 / 2 / 3 購買商品\n分數: " + GameState.score);
            case POTION -> {}
        }
    }

    // ── Called by GameScene on 1/2/3 key ──────────────────────────────────
    public void onShopBuy(Player player, int index) {
        if (rewardType != RewardType.SHOP) return;
        if (!nearCenter(player, 200)) { showDialog("請靠近商人！"); return; }
        if (shopSold[index])          { showDialog("已賣完！"); return; }
        if (GameState.score < SHOP_PRICES[index]) {
            showDialog("分數不足！需要 " + SHOP_PRICES[index] + " 分"); return;
        }
        GameState.score -= SHOP_PRICES[index];
        shopSold[index] = true;
        applyShopItem(player, index);
    }

    // ── Internal interactions ──────────────────────────────────────────────
    private void interactNPC(Player player) {
        if (npcTalked) { showDialog("好好加油，不要讓我失望。"); return; }
        // 書本強化：明年的考古題 — 攻擊力提升至 ×1.5
        GameState.bookEffects.add(BookEffect.EXAM_QUESTIONS);
        GameState.damageMultiplier = Math.max(GameState.damageMultiplier, 1.5);
        npcTalked = true;
        showDialog("拿著，明年的考古題。\n不要說給墊積系的知道。\n書本強化：攻擊力 ×1.5", 4.5);
    }

    private void interactChest(Player player) {
        if (collected) { showDialog("寶箱已開啟"); return; }
        collected = true;
        switch (new Random().nextInt(5)) {
            case 0 -> { player.heal(50);
                        showDialog("獲得：急救包 (+50 HP)"); }
            case 1 -> { GameState.damageMultiplier *= 1.2;
                        showDialog("獲得：傷害強化符 (傷害+20%)"); }
            case 2 -> { player.applySpeedBoost(1.15, 60.0);
                        showDialog("獲得：快跑鞋 (速度+15%，60秒)"); }
            // ── 書本強化 ────────────────────────────────────────────────────
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
                        GameState.damageMultiplier = Math.max(GameState.damageMultiplier, 1.3);
                        showDialog("購買成功！書本強化：傷害提升 30%"); }
            case 2 -> { player.applySpeedBoost(1.2, 30.0);
                        showDialog("購買成功！速度提升 20%，30秒"); }
        }
    }

    // ── Draw ───────────────────────────────────────────────────────────────
    @Override
    protected void drawFloor(GraphicsContext gc) {
        gc.setFill(Color.rgb(25, 45, 25));
        gc.fillRect(worldX, worldY, worldW, worldH);
        gc.setFill(Color.rgb(35, 60, 35));
        gc.fillRect(worldX + WALL, worldY + WALL, worldW - WALL * 2, worldH - WALL * 2);

        double cx = worldX + worldW / 2.0;
        double cy = worldY + worldH / 2.0;

        switch (rewardType) {
            case POTION -> drawPotion(gc, cx, cy);
            case NPC    -> drawNPC(gc, cx, cy);
            case CHEST  -> drawChest(gc, cx, cy);
            case SHOP   -> drawShop(gc, cx, cy);
        }

        if (dialogText != null) drawDialog(gc, cx, cy);
    }

    private void drawPotion(GraphicsContext gc, double cx, double cy) {
        if (collected) return;
        gc.setFill(Color.LIMEGREEN);
        gc.fillOval(cx - 12, cy - 16, 24, 28);
        gc.setFill(Color.rgb(80, 200, 80));
        gc.fillRect(cx - 6, cy - 24, 12, 10);
        gc.setFill(Color.WHITE);
        gc.fillText("回血藥水（走過拾取）", cx - 52, cy + 28);
    }

    private void drawNPC(GraphicsContext gc, double cx, double cy) {
        gc.setFill(Color.rgb(200, 160, 90));
        gc.fillRect(cx - 16, cy - 24, 32, 48);
        gc.setFill(Color.WHITE);
        gc.fillText(npcTalked ? "膏華龍" : "膏華龍  [E]", cx - 26, cy + 36);
    }

    private void drawChest(GraphicsContext gc, double cx, double cy) {
        if (collected) {
            gc.setFill(Color.rgb(100, 70, 30));
            gc.fillRect(cx - 22, cy - 14, 44, 28);
            gc.setFill(Color.GRAY);
            gc.fillText("已開啟", cx - 18, cy + 24);
        } else {
            gc.setFill(Color.rgb(139, 90, 43));
            gc.fillRect(cx - 22, cy - 18, 44, 36);
            gc.setFill(Color.GOLD);
            gc.fillRect(cx - 22, cy - 22, 44, 8);
            gc.setFill(Color.WHITE);
            gc.fillText("寶箱  [E]", cx - 22, cy + 28);
        }
    }

    private void drawShop(GraphicsContext gc, double cx, double cy) {
        gc.setFill(Color.rgb(180, 140, 80));
        gc.fillRect(cx - 16, cy + 10, 32, 48);
        gc.setFill(Color.WHITE);
        gc.fillText("匿名商人", cx - 26, cy + 72);

        for (int i = 0; i < 3; i++) {
            double ix = cx - 140 + i * 115;
            double iy = cy - 90;
            gc.setFill(shopSold[i] ? Color.rgb(50, 50, 50) : Color.rgb(35, 65, 35));
            gc.fillRoundRect(ix, iy, 100, 76, 8, 8);
            gc.setStroke(shopSold[i] ? Color.GRAY : Color.GOLD);
            gc.setLineWidth(1.5);
            gc.strokeRoundRect(ix, iy, 100, 76, 8, 8);
            gc.setFill(Color.WHITE);
            String[] parts = SHOP_NAMES[i].split("\n");
            gc.fillText(parts[0], ix + 6, iy + 22);
            if (parts.length > 1) gc.fillText(parts[1], ix + 6, iy + 42);
            gc.setFill(shopSold[i] ? Color.GRAY : Color.GOLD);
            gc.fillText(shopSold[i] ? "賣完" : SHOP_PRICES[i] + "分  [" + (i + 1) + "]", ix + 6, iy + 62);
        }
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

    private void showDialog(String text)                   { showDialog(text, 2.5); }
    private void showDialog(String text, double duration)  { dialogText = text; dialogTimer = duration; }

    public RewardType getRewardType() { return rewardType; }
}

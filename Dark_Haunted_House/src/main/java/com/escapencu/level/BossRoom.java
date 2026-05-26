package com.escapencu.level;

import com.escapencu.entity.Entity;
import com.escapencu.entity.Player;
import com.escapencu.entity.boss.Boss;
import com.escapencu.entity.boss.ChenQinHan;
import com.escapencu.entity.boss.Mine;
import com.escapencu.entity.boss.ShiGuoZhen;
import com.escapencu.entity.boss.WuXiaoGuang;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Boss room — floor 3 of every stage.
 * Spawns the stage-specific boss and handles per-boss special ticks
 * (Mine proximity checks for Stage 1, liquid-patch DOT for Stage 3).
 */
public class BossRoom extends Room {

    private final int stage;

    public BossRoom(int gridX, int gridY, int stage) {
        super(gridX, gridY, Type.BOSS);
        this.stage = stage;
    }

    @Override
    protected void spawnEnemies() {
        double cx = worldX + worldW / 2.0;
        double cy = worldY + worldH / 2.0;
        Boss boss = switch (stage) {
            case 1  -> new WuXiaoGuang(cx, cy, stage);
            case 2  -> new ChenQinHan(cx, cy, stage);
            default -> new ShiGuoZhen(cx, cy, stage);
        };
        boss.setRoomBounds(worldX, worldY, worldW, worldH);
        enemies.add(boss);
    }

    @Override
    public void update(double deltaTime, Player player) {
        // Stage 1: Mine proximity detonation
        for (Entity e : enemies) {
            if (e instanceof Mine mine && mine.isAlive()) {
                if (mine.checkProximity(player)) {
                    player.takeDamage(mine.getExplosionDamage());
                    mine.takeDamage(mine.getHp()); // kill the mine
                }
            }
        }

        // Stage 3: liquid-patch DOT
        for (Entity e : enemies)
            if (e instanceof ShiGuoZhen sgz) sgz.tickPatches(deltaTime, player);

        // Base room logic: drain spawns, remove dead, check cleared
        super.update(deltaTime, player);
    }

    /** Convenience accessor used by GameScene HUD (optional boss HP bar). */
    public Boss getBoss() {
        return enemies.stream()
            .filter(e -> e instanceof Boss && e.isAlive())
            .map(e -> (Boss) e)
            .findFirst().orElse(null);
    }

    @Override
    protected void drawFloor(GraphicsContext gc) {
        gc.setFill(Color.rgb(40, 15, 15));
        gc.fillRect(worldX, worldY, worldW, worldH);
        gc.setFill(Color.rgb(52, 22, 22));
        gc.fillRect(worldX + WALL, worldY + WALL, worldW - WALL * 2, worldH - WALL * 2);

        String bossName = switch (stage) {
            case 1  -> "無小光";
            case 2  -> "沉沁汗";
            default -> "濕幗針";
        };
        gc.setFill(Color.rgb(180, 30, 30));
        gc.fillText("★ BOSS  Stage " + stage + " — " + bossName, worldX + 30, worldY + 50);

        if (isCleared()) drawPortal(gc);
    }

    private void drawPortal(GraphicsContext gc) {
        double cx = worldX + worldW / 2, cy = worldY + worldH / 2;
        gc.setFill(Color.color(0.4, 0.8, 1.0, 0.5));
        gc.fillOval(cx - 50, cy - 50, 100, 100);
        gc.setFill(Color.color(0.7, 0.95, 1.0));
        gc.fillOval(cx - 28, cy - 28, 56, 56);
        gc.setFill(Color.WHITE);
        gc.fillText("下一關 ▶", cx - 26, cy + 5);
    }
}

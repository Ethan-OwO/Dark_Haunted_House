package com.escapencu.level;

import com.escapencu.entity.Entity;
import com.escapencu.entity.Player;
import com.escapencu.entity.boss.Boss;
import com.escapencu.entity.boss.ChenQinHan;
import com.escapencu.entity.boss.Mine;
import com.escapencu.entity.boss.ShiGuoZhen;
import com.escapencu.entity.boss.WuXiaoGuang;
import com.escapencu.util.FloorTileRenderer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Boss room — floor 3 of every stage.
 * Spawns the stage-specific boss and handles per-boss special ticks
 * (Mine proximity checks for Stage 1, liquid-patch DOT for Stage 3).
 */
public class BossRoom extends Room {

    private final int   stage;
    private final int[][] floorTilemap;

    public BossRoom(int gridX, int gridY, int stage) {
        super(gridX, gridY, Type.BOSS);
        this.stage        = stage;
        this.floorTilemap = FloorTileRenderer.generateTilemap(gridX, gridY, worldW - WALL * 2, worldH - WALL * 2, stage);
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
        // ── Wall area: stage base colour blended with blood red ───────────
        gc.setFill(bossWallColor());
        gc.fillRect(worldX, worldY, worldW, worldH);

        // ── Tiled floor texture (inner area only, aligned to wall boundary) ──
        FloorTileRenderer.draw(gc, floorTilemap, stage, worldX + WALL, worldY + WALL);

        // ── Stage 2: dampen bright blue tiles before blood tint ───────────
        if (stage == 2) {
            gc.setFill(Color.color(0.0, 0.02, 0.15, 0.32));
            gc.fillRect(worldX + WALL, worldY + WALL, worldW - WALL * 2, worldH - WALL * 2);
        }

        // ── Inner edge: dark red strip where wall meets floor ─────────────
        gc.setFill(Color.color(0.10, 0.02, 0.02, 1.0));
        gc.fillRect(worldX + WALL,                    worldY + WALL,              worldW - WALL * 2, 3);
        gc.fillRect(worldX + WALL,                    worldY + worldH - WALL - 3, worldW - WALL * 2, 3);
        gc.fillRect(worldX + WALL,                    worldY + WALL,              3, worldH - WALL * 2);
        gc.fillRect(worldX + worldW - WALL - 3,       worldY + WALL,              3, worldH - WALL * 2);

        // ── Inner floor: semi-transparent blood tint over tiles ───────────
        gc.setFill(Color.color(0.18, 0.04, 0.04, 0.52));
        gc.fillRect(worldX + WALL, worldY + WALL, worldW - WALL * 2, worldH - WALL * 2);

        // ── Boss label ─────────────────────────────────────────────────────
        String bossName = switch (stage) {
            case 1  -> "無小光";
            case 2  -> "沉沁汗";
            default -> "濕幗針";
        };
        gc.setFill(Color.rgb(200, 40, 40));
        gc.fillText("★ BOSS  Stage " + stage + " — " + bossName, worldX + 30, worldY + 50);

        if (isCleared()) drawPortal(gc);
    }

    /**
     * Wall base colour: stage tile palette darkened and mixed with blood red,
     * so each stage's boss room feels distinct yet still ominous.
     */
    private Color bossWallColor() {
        return switch (stage) {
            case 1  -> Color.rgb(40,  28,  22);   // green-brown-red  (工程五館)
            case 2  -> Color.rgb(28,  22,  48);   // blue-purple-red  (男13宿舍)
            default -> Color.rgb(48,  18,  14);   // deep brick red   (圖書館)
        };
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

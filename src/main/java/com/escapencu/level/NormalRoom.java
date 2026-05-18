package com.escapencu.level;

import com.escapencu.entity.Enemy;
import com.escapencu.entity.enemy.Goose;
import com.escapencu.entity.enemy.Squirrel;
import com.escapencu.entity.enemy.Termite;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Standard room used for START / NORMAL / REWARD / EXIT types.
 * TODO: Replace placeholder Enemy with stage-specific subclasses
 *       (Squirrel, Goose, Termite) once those are implemented.
 */
public class NormalRoom extends Room {
    private final int stage;
    private final int floorNum;

    public NormalRoom(int gridX, int gridY, Type type, int stage, int floorNum) {
        super(gridX, gridY, type);
        this.stage    = stage;
        this.floorNum = floorNum;
    }

    @Override
    protected void spawnEnemies() {
        if (type == Type.START || type == Type.EXIT || type == Type.REWARD) return;
        int count = 2 + floorNum; // floor1→3, floor2→4, floor3→5
        for (int i = 0; i < count; i++) {
            double ex = worldX + WALL + 60 + (i * 230) % (worldW - WALL * 2 - 80);
            double ey = worldY + WALL + 60 + (i * 170) % (worldH - WALL * 2 - 80);
            Enemy e = switch (stage) {
                case 1  -> new Squirrel(ex, ey, stage);
                case 2  -> new Goose(ex, ey, stage);
                default -> new Termite(ex, ey, stage);
            };
            e.setRoomBounds(worldX + WALL, worldY + WALL,
                            worldX + worldW - WALL, worldY + worldH - WALL);
            enemies.add(e);
        }
    }

    @Override
    protected void drawFloor(GraphicsContext gc) {
        gc.setFill(floorColor());
        gc.fillRect(worldX, worldY, worldW, worldH);

        // Subtle tile grid
        gc.setStroke(tileLineColor());
        gc.setLineWidth(1);
        for (double x = worldX + WALL; x < worldX + worldW - WALL; x += 64)
            gc.strokeLine(x, worldY + WALL, x, worldY + worldH - WALL);
        for (double y = worldY + WALL; y < worldY + worldH - WALL; y += 64)
            gc.strokeLine(worldX + WALL, y, worldX + worldW - WALL, y);

        // EXIT room portal
        if (type == Type.EXIT) drawPortal(gc);

        // REWARD room indicator
        if (type == Type.REWARD) drawRewardLabel(gc);
    }

    private void drawPortal(GraphicsContext gc) {
        double cx = worldX + worldW / 2, cy = worldY + worldH / 2;
        gc.setFill(Color.color(0.4, 0.8, 1.0, 0.5));
        gc.fillOval(cx - 50, cy - 50, 100, 100);
        gc.setFill(Color.color(0.7, 0.95, 1.0));
        gc.fillOval(cx - 28, cy - 28, 56, 56);
        gc.setFill(Color.WHITE);
        gc.fillText("下一層 ▶", cx - 26, cy + 5);
    }

    private void drawRewardLabel(GraphicsContext gc) {
        double cx = worldX + worldW / 2, cy = worldY + worldH / 2;
        gc.setFill(Color.GOLD);
        gc.fillText("★ 獎勵房間 (TODO)", cx - 55, cy);
    }

    private Color floorColor() {
        return switch (stage) {
            case 1  -> Color.rgb(50, 50, 68);  // 工程五館
            case 2  -> Color.rgb(40, 52, 40);  // 男13宿舍
            default -> Color.rgb(52, 40, 40);  // 圖書館
        };
    }

    private Color tileLineColor() {
        return switch (stage) {
            case 1  -> Color.rgb(40, 40, 58);
            case 2  -> Color.rgb(32, 44, 32);
            default -> Color.rgb(44, 32, 32);
        };
    }
}

package com.escapencu.level;

import com.escapencu.entity.Enemy;
import com.escapencu.entity.Entity;
import com.escapencu.entity.Player;
import com.escapencu.map.RoomNode;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * A standard combat room or the floor exit room.
 * Enemy count and stats scale with stage and floor difficulty.
 *
 * TODO: Replace the placeholder Enemy with stage-specific enemy subclasses
 *       (Squirrel for Stage 1, Goose for Stage 2, Termite for Stage 3).
 */
public class NormalRoom extends Room {
    private final int stage;      // 1, 2, 3  – affects colour theme and enemy stats
    private final int floorNum;   // 1, 2, 3  – affects enemy count

    public NormalRoom(int stage, int floorNum) {
        this.stage    = stage;
        this.floorNum = floorNum;
    }

    @Override
    public void init() {
        enemies.clear();
        // START and EXIT rooms have no enemies
        if (node.type == RoomNode.Type.START || node.type == RoomNode.Type.EXIT) return;

        int count = 2 + floorNum; // floor 1→3, floor 2→4, floor 3→5 enemies
        for (int i = 0; i < count; i++) {
            double ex = WALL + 60 + (i * 230) % (int)(WIDTH  - WALL * 2 - 80);
            double ey = WALL + 60 + (i * 170) % (int)(HEIGHT - WALL * 2 - 80);
            enemies.add(new Enemy(ex, ey, 40, 40,
                20 * stage,
                60 + stage * 15 + floorNum * 10,
                4 * stage));
        }
    }

    @Override
    public void update(double deltaTime, Player player) {
        for (Entity e : enemies) {
            if (e instanceof Enemy enemy) {
                enemy.moveToward(player.getCenterX(), player.getCenterY(), deltaTime);
                enemy.shootAt(player.getCenterX(), player.getCenterY(), 180 + stage * 30);
            }
        }
        super.update(deltaTime, player);
    }

    @Override
    protected void drawFloor(GraphicsContext gc) {
        // Floor colour changes per stage
        gc.setFill(floorColor());
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        // Subtle tile grid
        gc.setStroke(tileLineColor());
        gc.setLineWidth(1);
        for (double x = WALL; x <= WIDTH - WALL; x += 64)
            gc.strokeLine(x, WALL, x, HEIGHT - WALL);
        for (double y = WALL; y <= HEIGHT - WALL; y += 64)
            gc.strokeLine(WALL, y, WIDTH - WALL, y);

        // EXIT room: draw portal
        if (node.type == RoomNode.Type.EXIT) drawPortal(gc);
    }

    private void drawPortal(GraphicsContext gc) {
        double cx = WIDTH / 2, cy = HEIGHT / 2;
        // Outer glow ring
        gc.setFill(Color.rgb(100, 200, 255, 0.4));
        // JavaFX doesn't support alpha in Color.rgb(int,int,int), use Color.color():
        gc.setFill(Color.color(0.4, 0.8, 1.0, 0.4));
        gc.fillOval(cx - 50, cy - 50, 100, 100);
        // Inner circle
        gc.setFill(Color.color(0.6, 0.9, 1.0));
        gc.fillOval(cx - 30, cy - 30, 60, 60);
        // Label
        gc.setFill(Color.WHITE);
        gc.fillText("下一層 ▶", cx - 25, cy + 5);
    }

    private Color floorColor() {
        return switch (stage) {
            case 1 -> Color.rgb(50, 50, 68);   // 工程五館 — blue-grey concrete
            case 2 -> Color.rgb(40, 52, 40);   // 男13宿舍 — greenish
            case 3 -> Color.rgb(52, 40, 40);   // 圖書館   — warm dark
            default -> Color.rgb(50, 50, 68);
        };
    }

    private Color tileLineColor() {
        return switch (stage) {
            case 1 -> Color.rgb(40, 40, 58);
            case 2 -> Color.rgb(32, 44, 32);
            case 3 -> Color.rgb(44, 32, 32);
            default -> Color.rgb(40, 40, 58);
        };
    }
}

package com.escapencu.level;

import com.escapencu.entity.Enemy;
import com.escapencu.entity.Entity;
import com.escapencu.entity.Player;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * The boss room at the end of every 3rd floor.
 *
 * TODO: Replace the placeholder Enemy with the real Boss subclasses:
 *   stage 1 → WuXiaoGuang (無小光)
 *   stage 2 → ChenQinHan  (沉沁汗)
 *   stage 3 → ShiGuoZhen  (濕幗針)
 */
public class BossRoom extends Room {
    private final int stage;

    public BossRoom(int stage) {
        this.stage = stage;
    }

    @Override
    public void init() {
        enemies.clear();
        // Placeholder: one very tough enemy representing the boss
        enemies.add(new Enemy(WIDTH / 2 - 30, HEIGHT / 2 - 30, 60, 60,
            250 * stage,   // HP
            45,            // speed (slow but hits hard)
            15 * stage));  // contact damage
    }

    @Override
    public void update(double deltaTime, Player player) {
        for (Entity e : enemies) {
            if (e instanceof Enemy enemy) {
                enemy.moveToward(player.getCenterX(), player.getCenterY(), deltaTime);
                enemy.shootAt(player.getCenterX(), player.getCenterY(), 230 + stage * 40);
            }
        }
        super.update(deltaTime, player);
    }

    @Override
    protected void drawFloor(GraphicsContext gc) {
        // Ominous red-tinted boss arena
        gc.setFill(Color.rgb(40, 15, 15));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        // Dark inner area with subtle pattern
        gc.setFill(Color.rgb(50, 20, 20));
        gc.fillRect(WALL, WALL, WIDTH - WALL * 2, HEIGHT - WALL * 2);

        // Warning label
        gc.setFill(Color.rgb(180, 30, 30));
        gc.fillText("★ BOSS ROOM  Stage " + stage + " ★   [TODO: 換成真正的Boss]",
                    WIDTH / 2 - 180, 50);
    }
}

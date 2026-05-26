package com.escapencu.entity.boss;

import com.escapencu.entity.Enemy;
import com.escapencu.entity.Entity;
import com.escapencu.entity.Player;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Collections;
import java.util.List;

/** Fake copy of WuXiaoGuang. One hit destroys it and spawns a TAEnemy. */
public class Decoy extends Enemy {

    private final int     stage;
    private boolean       spawned = false;

    public Decoy(double x, double y, int stage) {
        super(x, y, 56, 56, 1, 0, 0);
        this.stage = stage;
        shootCooldown = 999;
        this.spawnTimer = 0;
    }

    @Override
    public void update(double deltaTime, Player player) { /* stationary */ }

    @Override
    public List<Entity> getPendingSpawns() {
        if (!isAlive() && !spawned) {
            spawned = true;
            return List.of(new TAEnemy(getCenterX() - 11, getCenterY() - 11, stage));
        }
        return Collections.emptyList();
    }

    @Override
    public void draw(GraphicsContext gc) {
        gc.setFill(Color.color(0.47, 0.20, 0.59, 0.60));
        gc.fillRect(x, y, width, height);
        gc.setFill(Color.WHITE);
        gc.fillText("?", getCenterX() - 4, getCenterY() + 5);
    }
}

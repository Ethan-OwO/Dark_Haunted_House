package com.escapencu.entity.enemy;

import com.escapencu.entity.Enemy;
import com.escapencu.entity.Entity;
import com.escapencu.entity.Player;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Collections;
import java.util.List;

/** Stage 3 normal enemy — drops a Wing poison zone when killed. */
public class Termite extends Enemy {

    private final boolean mini;
    private final int     stage;
    private boolean       spawned = false;

    public Termite(double x, double y, int stage, boolean mini) {
        super(x, y,
              mini ? 16 : 32,
              mini ? 16 : 32,
              (mini ? 10 : 20) * stage,
              75,
              4 * stage);
        this.mini  = mini;
        this.stage = stage;
        shootCooldown = 1.8;
        bulletDamage  = 3 * stage;
    }

    @Override
    public void update(double deltaTime, Player player) {
        super.update(deltaTime);
        moveToward(player.getCenterX(), player.getCenterY(), deltaTime);
        shootAt(player.getCenterX(), player.getCenterY(), 180);
    }

    @Override
    public List<Entity> getPendingSpawns() {
        if (!isAlive() && !spawned) {
            spawned = true;
            return List.of(new Wing(getCenterX(), getCenterY()));
        }
        return Collections.emptyList();
    }

    @Override
    public void draw(GraphicsContext gc) {
        gc.setFill(mini ? Color.rgb(160, 120, 60) : Color.rgb(110, 75, 30));
        gc.fillRect(x, y, width, height);
        // Antennae for the full-size version
        if (!mini) {
            gc.setStroke(Color.rgb(80, 50, 20));
            gc.setLineWidth(1);
            gc.strokeLine(getCenterX() - 4, y,     getCenterX() - 10, y - 8);
            gc.strokeLine(getCenterX() + 4, y,     getCenterX() + 10, y - 8);
        }
        // HP bar
        if (hp < maxHp) {
            gc.setFill(Color.DARKRED);
            gc.fillRect(x, y - 8, width, 4);
            gc.setFill(Color.LIMEGREEN);
            gc.fillRect(x, y - 8, width * (double) hp / maxHp, 4);
        }
        for (var b : bullets) b.draw(gc);
    }
}

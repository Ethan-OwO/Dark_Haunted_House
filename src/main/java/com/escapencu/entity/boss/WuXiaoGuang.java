package com.escapencu.entity.boss;

import com.escapencu.entity.Entity;
import com.escapencu.entity.Player;
import com.escapencu.level.Room;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Stage 1 Boss — 無小光
 * Passive: periodically turns invisible and spawns Decoys.
 * Active:  places Hanoi-Tower floor mines.
 */
public class WuXiaoGuang extends Boss {

    private final int     stage;
    private final Random  rng = new Random();

    // Invisibility cycle
    private double  invisCD    = 5.0;  // time until next invis phase
    private double  invisTimer = 0;    // remaining invis duration
    private boolean invisible  = false;

    // Mine placement
    private double  mineTimer  = 6.0;
    private int     mineCount  = 0;
    private static final int MAX_MINES = 5;

    // Entities to inject into the room next tick
    private final List<Entity> pendingRoom = new ArrayList<>();

    public WuXiaoGuang(double cx, double cy, int stage) {
        super(cx - 28, cy - 28, 56, 56, 250 * stage, 55, 15 * stage);
        this.stage    = stage;
        bulletDamage  = 8 * stage;
        shootCooldown = 2.2;
    }

    @Override
    protected void doAttack(Player player, double deltaTime) {
        // ── Invisibility phase ─────────────────────────────────────────────
        if (invisible) {
            invisTimer -= deltaTime;
            if (invisTimer <= 0) {
                invisible  = false;
                invincible = false;
            }
        } else {
            invisCD -= deltaTime;
            if (invisCD <= 0) {
                invisible  = true;
                invincible = true;
                invisTimer = 3.0;
                invisCD    = 5.0 + rng.nextDouble() * 2;
                // Spawn 2 Decoys at random room positions
                for (int i = 0; i < 2; i++) {
                    double dx = roomX + Room.WALL + 60 + rng.nextDouble() * (roomW - Room.WALL * 2 - 120);
                    double dy = roomY + Room.WALL + 60 + rng.nextDouble() * (roomH - Room.WALL * 2 - 120);
                    pendingRoom.add(new Decoy(dx, dy, stage));
                }
            }
            // Normal movement + shooting only when visible
            moveToward(player.getCenterX(), player.getCenterY(), deltaTime);
            shootAt(player.getCenterX(), player.getCenterY(), 200 + stage * 20);
        }

        // ── Mine placement ─────────────────────────────────────────────────
        mineTimer -= deltaTime;
        if (mineTimer <= 0 && mineCount < MAX_MINES) {
            double mx = roomX + Room.WALL + 50 + rng.nextDouble() * (roomW - Room.WALL * 2 - 100);
            double my = roomY + Room.WALL + 50 + rng.nextDouble() * (roomH - Room.WALL * 2 - 100);
            pendingRoom.add(new Mine(mx, my));
            mineCount++;
            mineTimer = 5.0 + rng.nextDouble() * 2;
        }
    }

    /** Drain pattern: returns entities to spawn and clears the queue. */
    @Override
    public List<Entity> getPendingSpawns() {
        if (pendingRoom.isEmpty()) return Collections.emptyList();
        List<Entity> copy = new ArrayList<>(pendingRoom);
        pendingRoom.clear();
        return copy;
    }

    @Override
    protected void updatePhase() {
        if (phase == 1 && hp <= maxHp / 2) {
            phase     = 2;
            invisCD   = Math.min(invisCD, 2.0);   // invis more frequently in phase 2
            mineTimer = Math.min(mineTimer, 3.0);
        }
    }

    @Override
    public void draw(GraphicsContext gc) {
        if (invisible) gc.setGlobalAlpha(0.25);

        gc.setFill(phase == 2 ? Color.rgb(140, 20, 160) : Color.rgb(100, 30, 120));
        gc.fillRect(x, y, width, height);
        gc.setFill(Color.WHITE);
        gc.fillText("無小光", x + 6, y + height / 2 + 5);

        if (hp < maxHp) {
            gc.setFill(Color.DARKRED);
            gc.fillRect(x, y - 10, width, 5);
            gc.setFill(Color.LIMEGREEN);
            gc.fillRect(x, y - 10, width * (double) hp / maxHp, 5);
        }

        if (invisible) gc.setGlobalAlpha(1.0);

        for (var b : bullets) b.draw(gc);
    }
}

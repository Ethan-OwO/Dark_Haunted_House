package com.escapencu.entity;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Player extends Entity {
    private static final double SPEED         = 200.0;
    private static final double BULLET_SPEED  = 420.0;
    private static final int    BULLET_DAMAGE = 10;
    private static final double SHOOT_COOLDOWN = 0.28;
    private static final double SLOW_FACTOR    = 0.5;

    private double shootTimer   = 0;
    private double mouseX, mouseY;
    private final List<Bullet> bullets = new ArrayList<>();

    // ── Status effect timers ───────────────────────────────────────────────
    private double stunTimer   = 0;  // can't move / act
    private double slowTimer   = 0;  // speed halved
    private double poisonTimer = 0;  // periodic damage
    private double burnTimer   = 0;  // periodic damage (no slow)
    private double poisonTickTimer = 0;
    private double burnTickTimer   = 0;

    public Player(double x, double y) {
        super(x, y, 32, 32, 100);
    }

    // ── Update ─────────────────────────────────────────────────────────────
    @Override
    public void update(double deltaTime) {
        if (stunTimer   > 0) { stunTimer   -= deltaTime; return; }
        if (slowTimer   > 0)   slowTimer   -= deltaTime;
        if (shootTimer  > 0)   shootTimer  -= deltaTime;

        applyDotDamage(deltaTime);

        bullets.removeIf(b -> !b.isAlive());
        for (Bullet b : bullets) b.update(deltaTime);
    }

    private void applyDotDamage(double deltaTime) {
        if (poisonTimer > 0) {
            poisonTimer    -= deltaTime;
            poisonTickTimer -= deltaTime;
            if (poisonTickTimer <= 0) {
                takeDamage(2);          // 2 damage per tick
                poisonTickTimer = 0.5;
            }
        }
        if (burnTimer > 0) {
            burnTimer    -= deltaTime;
            burnTickTimer -= deltaTime;
            if (burnTickTimer <= 0) {
                takeDamage(3);          // 3 damage per tick
                burnTickTimer = 0.4;
            }
        }
    }

    // ── Movement ───────────────────────────────────────────────────────────
    public void handleMovement(Set<KeyCode> keys, double deltaTime,
                               double roomW, double roomH) {
        if (stunTimer > 0) return;

        double speed = (slowTimer > 0) ? SPEED * SLOW_FACTOR : SPEED;
        double dx = 0, dy = 0;

        if (keys.contains(KeyCode.W) || keys.contains(KeyCode.UP))    dy -= 1;
        if (keys.contains(KeyCode.S) || keys.contains(KeyCode.DOWN))  dy += 1;
        if (keys.contains(KeyCode.A) || keys.contains(KeyCode.LEFT))  dx -= 1;
        if (keys.contains(KeyCode.D) || keys.contains(KeyCode.RIGHT)) dx += 1;

        if (dx != 0 && dy != 0) { dx *= 0.7071; dy *= 0.7071; }  // normalise diagonal

        // Wall boundaries (20px wall thickness)
        x = Math.max(20, Math.min(roomW - width  - 20, x + dx * speed * deltaTime));
        y = Math.max(20, Math.min(roomH - height - 20, y + dy * speed * deltaTime));
    }

    // ── Shooting ───────────────────────────────────────────────────────────
    public void updateMousePosition(double mx, double my) {
        mouseX = mx;
        mouseY = my;
    }

    public void shoot() {
        if (shootTimer > 0 || stunTimer > 0) return;
        double angle = Math.atan2(mouseY - getCenterY(), mouseX - getCenterX());
        bullets.add(new Bullet(
            getCenterX(), getCenterY(),
            Math.cos(angle) * BULLET_SPEED,
            Math.sin(angle) * BULLET_SPEED,
            BULLET_DAMAGE, true
        ));
        shootTimer = SHOOT_COOLDOWN;
    }

    // ── Draw ───────────────────────────────────────────────────────────────
    @Override
    public void draw(GraphicsContext gc) {
        // Body
        gc.setFill(stunTimer > 0 ? Color.YELLOW : Color.CYAN);
        gc.fillRect(x, y, width, height);

        // Aim dot
        double angle = Math.atan2(mouseY - getCenterY(), mouseX - getCenterX());
        gc.setFill(Color.DARKBLUE);
        gc.fillOval(getCenterX() + Math.cos(angle) * 14 - 4,
                    getCenterY() + Math.sin(angle) * 14 - 4, 8, 8);

        // Status labels
        double labelY = y - 5;
        if (poisonTimer > 0) { gc.setFill(Color.LIMEGREEN); gc.fillText("毒", x,      labelY); }
        if (burnTimer   > 0) { gc.setFill(Color.ORANGE);    gc.fillText("燃", x + 16, labelY); }
        if (slowTimer   > 0) { gc.setFill(Color.LIGHTBLUE); gc.fillText("緩", x + 32, labelY); }
    }

    // ── Status effect appliers ─────────────────────────────────────────────
    public void applyStun  (double d) { stunTimer   = Math.max(stunTimer,   d); }
    public void applySlow  (double d) { slowTimer   = Math.max(slowTimer,   d); }
    public void applyPoison(double d) { poisonTimer = Math.max(poisonTimer, d); poisonTickTimer = 0; }
    public void applyBurn  (double d) { burnTimer   = Math.max(burnTimer,   d); burnTickTimer   = 0; }

    public List<Bullet> getBullets() { return bullets; }
}

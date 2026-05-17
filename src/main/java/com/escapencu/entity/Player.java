package com.escapencu.entity;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Player extends Entity {
    private static final double SPEED          = 200.0;
    private static final double BULLET_SPEED   = 420.0;
    private static final int    BULLET_DAMAGE  = 10;
    private static final double SHOOT_COOLDOWN = 0.28;
    private static final double SLOW_FACTOR    = 0.5;

    private double shootTimer = 0;
    private double mouseWorldX, mouseWorldY; // mouse position in world coords
    private final List<Bullet> bullets = new ArrayList<>();

    // ── Status effect timers ───────────────────────────────────────────────
    private double stunTimer       = 0;
    private double slowTimer       = 0;
    private double poisonTimer     = 0;
    private double burnTimer       = 0;
    private double poisonTickTimer = 0;
    private double burnTickTimer   = 0;

    public Player(double worldX, double worldY) {
        super(worldX, worldY, 32, 32, 100);
    }

    // ── Update ─────────────────────────────────────────────────────────────
    @Override
    public void update(double deltaTime) {
        if (stunTimer > 0) { stunTimer -= deltaTime; return; }
        if (slowTimer  > 0) slowTimer  -= deltaTime;
        if (shootTimer > 0) shootTimer -= deltaTime;
        applyDots(deltaTime);
        bullets.removeIf(b -> !b.isAlive());
        for (Bullet b : bullets) b.update(deltaTime);
    }

    private void applyDots(double dt) {
        if (poisonTimer > 0) {
            poisonTimer     -= dt;
            poisonTickTimer -= dt;
            if (poisonTickTimer <= 0) { takeDamage(2); poisonTickTimer = 0.5; }
        }
        if (burnTimer > 0) {
            burnTimer     -= dt;
            burnTickTimer -= dt;
            if (burnTickTimer <= 0) { takeDamage(3); burnTickTimer = 0.4; }
        }
    }

    // ── Movement (collision delegated to AreaChecker) ─────────────────────
    public void handleMovement(Set<KeyCode> keys, double dt, AreaChecker area) {
        if (stunTimer > 0) return;
        double speed = (slowTimer > 0) ? SPEED * SLOW_FACTOR : SPEED;
        double dx = 0, dy = 0;
        if (keys.contains(KeyCode.W) || keys.contains(KeyCode.UP))    dy -= 1;
        if (keys.contains(KeyCode.S) || keys.contains(KeyCode.DOWN))  dy += 1;
        if (keys.contains(KeyCode.A) || keys.contains(KeyCode.LEFT))  dx -= 1;
        if (keys.contains(KeyCode.D) || keys.contains(KeyCode.RIGHT)) dx += 1;
        if (dx != 0 && dy != 0) { dx *= 0.7071; dy *= 0.7071; }

        double newX = x + dx * speed * dt;
        double newY = y + dy * speed * dt;
        if (area.canMoveTo(newX, y, width, height)) x = newX;
        if (area.canMoveTo(x, newY, width, height)) y = newY;
    }

    // ── Shooting ───────────────────────────────────────────────────────────
    /** Call with the mouse position already converted to world coordinates. */
    public void updateMouseWorldPos(double wx, double wy) {
        mouseWorldX = wx;
        mouseWorldY = wy;
    }

    public void shoot() {
        if (shootTimer > 0 || stunTimer > 0) return;
        double angle = Math.atan2(mouseWorldY - getCenterY(), mouseWorldX - getCenterX());
        bullets.add(new Bullet(getCenterX(), getCenterY(),
                Math.cos(angle) * BULLET_SPEED,
                Math.sin(angle) * BULLET_SPEED,
                BULLET_DAMAGE, true));
        shootTimer = SHOOT_COOLDOWN;
    }

    // ── Draw (in world coordinates) ────────────────────────────────────────
    @Override
    public void draw(GraphicsContext gc) {
        gc.setFill(stunTimer > 0 ? Color.YELLOW : Color.CYAN);
        gc.fillRect(x, y, width, height);

        // Aim indicator dot
        double angle = Math.atan2(mouseWorldY - getCenterY(), mouseWorldX - getCenterX());
        gc.setFill(Color.DARKBLUE);
        gc.fillOval(getCenterX() + Math.cos(angle) * 14 - 4,
                    getCenterY() + Math.sin(angle) * 14 - 4, 8, 8);

        // Status labels
        if (poisonTimer > 0) { gc.setFill(Color.LIMEGREEN); gc.fillText("毒", x,      y - 5); }
        if (burnTimer   > 0) { gc.setFill(Color.ORANGE);    gc.fillText("燃", x + 16, y - 5); }
        if (slowTimer   > 0) { gc.setFill(Color.LIGHTBLUE); gc.fillText("緩", x + 32, y - 5); }
    }

    // ── OP mode helper ─────────────────────────────────────────────────────
    /** Restores full HP and clears all status effects. Called every frame in OP mode. */
    public void fullHeal() {
        hp              = maxHp;
        stunTimer       = 0;
        slowTimer       = 0;
        poisonTimer     = 0;
        burnTimer       = 0;
        poisonTickTimer = 0;
        burnTickTimer   = 0;
    }

    // ── Status appliers ────────────────────────────────────────────────────
    public void applySlow(double factor, double duration) { applySlow(duration); }
    public void applyStun  (double d) { stunTimer   = Math.max(stunTimer,   d); }
    public void applySlow  (double d) { slowTimer   = Math.max(slowTimer,   d); }
    public void applyPoison(double d) { poisonTimer = Math.max(poisonTimer, d); poisonTickTimer = 0; }
    public void applyBurn  (double d) { burnTimer   = Math.max(burnTimer,   d); burnTickTimer   = 0; }
    public boolean isSlowed() { return slowTimer > 0; }

    public List<Bullet> getBullets() { return bullets; }
}

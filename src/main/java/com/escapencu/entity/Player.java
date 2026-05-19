package com.escapencu.entity;

import com.escapencu.core.GameState;
import com.escapencu.util.ResourceLoader;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
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
    private static final double DASH_SPEED_MULTIPLIER = 4.0; // 衝刺速度倍率
    private static final double DASH_DURATION         = 0.15; // 衝刺持續時間
    private static final double DASH_COOLDOWN         = 1.5;  // 衝刺冷卻時間

    private double dashTimer     = 0; // 記錄衝刺狀態的倒數
    private double dashCooldownTimer = 0; // 記錄冷卻時間的倒數
    private double dashDx = 0, dashDy = 0; // 鎖定衝刺方向

    private double shootTimer  = 0;
    private double mouseWorldX, mouseWorldY;
    private final List<Bullet> bullets = new ArrayList<>();

    // ── Direction ──────────────────────────────────────────────────────────
    private enum Dir { N, S, E, W }
    private Dir    currentDir  = Dir.S;
    private double lastDx = 0, lastDy = 0;

    private boolean mouseMovedThisFrame = false;
    private double  mouseIdleTime       = 0;
    private static final double MOUSE_LOCK_SECONDS = 0.8;

    public void setMouseMoved(boolean moved) {
        mouseMovedThisFrame = moved;
        if (moved) mouseIdleTime = 0;
    }

    // ── Sprites (white background removal enabled) ────────────────────────
    private static final Image IMG_IDLE_N  = ResourceLoader.getImage("/images/player/player_idle_n.png",  true);
    private static final Image IMG_IDLE_S  = ResourceLoader.getImage("/images/player/player_idle_s.png",  true);
    private static final Image IMG_IDLE_E  = ResourceLoader.getImage("/images/player/player_idle_e.png",  true);
    private static final Image IMG_WALK1_N = ResourceLoader.getImage("/images/player/player_walk1_n.png", true);
    private static final Image IMG_WALK1_S = ResourceLoader.getImage("/images/player/player_walk1_s.png", true);
    private static final Image IMG_WALK1_E = ResourceLoader.getImage("/images/player/player_walk1_e.png", true);
    private static final Image IMG_WALK2_N = ResourceLoader.getImage("/images/player/player_walk2_n.png", true);
    private static final Image IMG_WALK2_S = ResourceLoader.getImage("/images/player/player_walk2_s.png", true);
    private static final Image IMG_WALK2_E = ResourceLoader.getImage("/images/player/player_walk2_e.png", true);
    private static final Image IMG_DEATH1  = ResourceLoader.getImage("/images/player/death1.jpg",         true);
    private static final Image IMG_DEATH2  = ResourceLoader.getImage("/images/player/death2.jpg",         true);

    // ── Animation state ────────────────────────────────────────────────────
    private boolean moving    = false;
    private double  bobTime   = 0;
    private double  bobOffset = 0;
    private static final double BOB_SPEED = 10.0; // rad/s
    private static final double BOB_AMP   = 2.0;  // pixels

    // Walk cycle (alternates walk1 ↔ walk2 while moving)
    private int    walkFrame    = 0;   // 0 = walk1, 1 = walk2
    private double walkTimer    = 0;
    private static final double WALK_FRAME_DUR = 0.15; // seconds per frame

    private boolean dying      = false;
    private double  deathTimer = 0;
    private static final double DEATH_FRAME1_DURATION = 0.4;

    // ── Temporary speed boost ──────────────────────────────────────────────
    private double speedBoostTimer  = 0;
    private double speedBoostFactor = 1.0;

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
        if (dying) { deathTimer += deltaTime; return; }

        if (dashTimer > 0) dashTimer -= deltaTime;
        if (dashCooldownTimer > 0) dashCooldownTimer -= deltaTime;


        if (stunTimer > 0) { stunTimer -= deltaTime; moving = false; return; }
        if (slowTimer  > 0) slowTimer  -= deltaTime;
        if (shootTimer > 0) shootTimer -= deltaTime;
        applyDots(deltaTime);
        bullets.removeIf(b -> !b.isAlive());
        for (Bullet b : bullets) b.update(deltaTime);

        if (!mouseMovedThisFrame) mouseIdleTime += deltaTime;
        if (speedBoostTimer > 0) speedBoostTimer -= deltaTime;
        updateDirection();

        if (moving) {
            bobTime   += deltaTime;
            bobOffset  = Math.sin(bobTime * BOB_SPEED) * BOB_AMP;
            // Advance walk frame
            walkTimer += deltaTime;
            if (walkTimer >= WALK_FRAME_DUR) {
                walkFrame = 1 - walkFrame; // toggle 0 ↔ 1
                walkTimer = 0;
            }
        } else {
            bobTime   = 0;
            bobOffset = 0;
            walkFrame = 0; // reset to walk1 when stopped
            walkTimer = 0;
        }
    }

    @Override
    public void takeDamage(int damage) {
        if (dashTimer > 0) return;

        super.takeDamage(damage);
        if (!isAlive() && !dying) dying = true;
    }

    private void updateDirection() {
        double deg;
        if (mouseIdleTime < MOUSE_LOCK_SECONDS) {
            // 滑鼠最近有移動：跟著滑鼠
            deg = Math.toDegrees(
                    Math.atan2(mouseWorldY - getCenterY(), mouseWorldX - getCenterX()));
        } else if (moving) {
            // 滑鼠靜止超過閾值且有移動：跟著 WASD
            deg = Math.toDegrees(Math.atan2(lastDy, lastDx));
        } else {
            return; // 都沒動：維持上一個方向
        }
        if      (deg >= -45  && deg <  45)  currentDir = Dir.E;
        else if (deg >=  45  && deg < 135)  currentDir = Dir.S;
        else if (deg >= -135 && deg < -45)  currentDir = Dir.N;
        else                                currentDir = Dir.W;
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

    // ── Movement ───────────────────────────────────────────────────────────
    public void handleMovement(Set<KeyCode> keys, double dt, AreaChecker area) {
        if (stunTimer > 0) { moving = false; return; }
        double speed = SPEED;
        double dx = 0, dy = 0;

        // ▼ 如果正在衝刺，鎖定方向並大幅提升速度
        if (dashTimer > 0) {
            dx = dashDx;
            dy = dashDy;
            speed *= DASH_SPEED_MULTIPLIER;
            moving = true;
        } else {
            // ▼ 原本的正常移動邏輯
            if (GameState.opMode)        speed *= 3.0;
            else if (slowTimer      > 0) speed *= SLOW_FACTOR;
            else if (speedBoostTimer > 0) speed *= speedBoostFactor;

            if (keys.contains(KeyCode.W) || keys.contains(KeyCode.UP))    dy -= 1;
            if (keys.contains(KeyCode.S) || keys.contains(KeyCode.DOWN))  dy += 1;
            if (keys.contains(KeyCode.A) || keys.contains(KeyCode.LEFT))  dx -= 1;
            if (keys.contains(KeyCode.D) || keys.contains(KeyCode.RIGHT)) dx += 1;
            if (dx != 0 && dy != 0) { dx *= 0.7071; dy *= 0.7071; }

            moving = (dx != 0 || dy != 0);
            if (moving) { lastDx = dx; lastDy = dy; }
        }

        double newX = x + dx * speed * dt;
        double newY = y + dy * speed * dt;

        // 衝刺時依然會受到牆壁阻擋，不會穿牆
        if (area.canMoveTo(newX, y, width, height)) x = newX;
        if (area.canMoveTo(x, newY, width, height)) y = newY;
    }

    // ── Shooting ───────────────────────────────────────────────────────────
    public void updateMouseWorldPos(double wx, double wy) {
        mouseWorldX = wx;
        mouseWorldY = wy;
    }

    public void shoot() {
        if (shootTimer > 0 || stunTimer > 0) return;
        double angle = Math.atan2(mouseWorldY - getCenterY(), mouseWorldX - getCenterX());
        int dmg = (int)(BULLET_DAMAGE * GameState.damageMultiplier);
        bullets.add(new Bullet(getCenterX(), getCenterY(),
                Math.cos(angle) * BULLET_SPEED,
                Math.sin(angle) * BULLET_SPEED,
                dmg, true));
        shootTimer = SHOOT_COOLDOWN;
    }

    private static final double DRAW_SIZE = 100; // visual size (collision box stays 32x32)

    // ── Draw ───────────────────────────────────────────────────────────────
    @Override
    public void draw(GraphicsContext gc) {
        Image   frame   = pickFrame();
        boolean flip    = (currentDir == Dir.W) && !dying;
        double  drawX   = getCenterX() - DRAW_SIZE / 2.0;
        double  drawY   = getCenterY() - DRAW_SIZE / 2.0 + bobOffset;

        if (stunTimer > 0) gc.setGlobalAlpha(0.55);

        if (frame != null) {
            if (flip) {
                gc.save();
                gc.translate(getCenterX(), 0);
                gc.scale(-1, 1);
                gc.drawImage(frame, -DRAW_SIZE / 2.0, drawY, DRAW_SIZE, DRAW_SIZE);
                gc.restore();
            } else {
                gc.drawImage(frame, drawX, drawY, DRAW_SIZE, DRAW_SIZE);
            }
        } else {
            gc.setFill(stunTimer > 0 ? Color.YELLOW : Color.CYAN);
            gc.fillRect(x, y, width, height);
        }

        gc.setGlobalAlpha(1.0);

        if (poisonTimer > 0) { gc.setFill(Color.LIMEGREEN); gc.fillText("毒", x,      y - 5); }
        if (burnTimer   > 0) { gc.setFill(Color.ORANGE);    gc.fillText("燃", x + 16, y - 5); }
        if (slowTimer   > 0) { gc.setFill(Color.LIGHTBLUE); gc.fillText("緩", x + 32, y - 5); }

        if (dashTimer > 0) {
            // 衝刺時畫一個半透明的淺藍色光圈或是調整整體透明度
            gc.setGlobalAlpha(0.6);
        } else if (stunTimer > 0) {
            gc.setGlobalAlpha(0.55);
        }
    }

    private Image pickFrame() {
        if (dying) return deathTimer < DEATH_FRAME1_DURATION ? IMG_DEATH1 : IMG_DEATH2;
        Dir dir = (currentDir == Dir.W) ? Dir.E : currentDir;
        if (moving) {
            if (walkFrame == 0) return switch (dir) {
                case N -> IMG_WALK1_N;
                case S -> IMG_WALK1_S;
                default -> IMG_WALK1_E;
            };
            return switch (dir) {
                case N -> IMG_WALK2_N;
                case S -> IMG_WALK2_S;
                default -> IMG_WALK2_E;
            };
        }
        return switch (dir) {
            case N -> IMG_IDLE_N;
            case S -> IMG_IDLE_S;
            default -> IMG_IDLE_E;
        };
    }

    // ── OP mode helper ─────────────────────────────────────────────────────
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
    public void heal(int amount)   { hp = Math.min(hp + amount, maxHp); }
    public void applySpeedBoost(double factor, double duration) {
        speedBoostTimer  = duration;
        speedBoostFactor = factor;
    }

    public void applySlow(double factor, double duration) { applySlow(duration); }
    public void applyStun  (double d) { stunTimer   = Math.max(stunTimer,   d); }
    public void applySlow  (double d) { slowTimer   = Math.max(slowTimer,   d); }
    public void applyPoison(double d) { poisonTimer = Math.max(poisonTimer, d); poisonTickTimer = 0; }
    public void applyBurn  (double d) { burnTimer   = Math.max(burnTimer,   d); burnTickTimer   = 0; }
    public boolean isSlowed() { return slowTimer > 0; }

    public List<Bullet> getBullets() { return bullets; }

    public void dash() {
        // 如果還在冷卻、暈眩中，或是已經在衝刺了，就不執行
        if (dashCooldownTimer > 0 || stunTimer > 0 || dashTimer > 0) return;

        // 決定衝刺方向
        if (moving) {
            // 如果玩家正在走動，往走動的方向衝刺
            dashDx = lastDx;
            dashDy = lastDy;
        } else {
            // 如果玩家站著不動，預設往他目前面向的地方衝刺
            switch (currentDir) {
                case N -> { dashDx = 0; dashDy = -1; }
                case S -> { dashDx = 0; dashDy = 1; }
                case E -> { dashDx = 1; dashDy = 0; }
                case W -> { dashDx = -1; dashDy = 0; }
            }
        }

        dashTimer = DASH_DURATION;
        dashCooldownTimer = DASH_COOLDOWN;}

    public double getDashCooldownTimer() {
        return dashCooldownTimer;
    }

    public double getDashCooldownMax() {
        return DASH_COOLDOWN; // 回傳你設定的 DASH_COOLDOWN (例如 1.5)
    }

}

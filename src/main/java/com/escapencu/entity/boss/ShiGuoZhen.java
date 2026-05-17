package com.escapencu.entity.boss;

import com.escapencu.entity.EffectBullet;
import com.escapencu.entity.Player;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Stage 3 Boss — 濕幗針 (Final Boss)
 * Passive: leaves liquid trail on the floor (DOT zone).
 * Active:  throws Python / C++ / Java projectiles with status effects.
 */
public class ShiGuoZhen extends Boss {

    // ── Liquid patch ──────────────────────────────────────────────────────
    private static class LiquidPatch {
        double wx, wy, timeLeft;
        double dotTimer = 0;
        LiquidPatch(double wx, double wy) { this.wx = wx; this.wy = wy; this.timeLeft = 5.0; }
    }

    private final List<LiquidPatch> patches   = new ArrayList<>();
    private double trailTimer  = 0;

    // ── Attack ────────────────────────────────────────────────────────────
    private double attackTimer = 2.0;
    private int    bulletCycle = 0;
    private final Random rng   = new Random();

    public ShiGuoZhen(double cx, double cy, int stage) {
        super(cx - 30, cy - 30, 60, 60, 400 * stage, 70, 16 * stage);
        bulletDamage  = 12 * stage;
        shootCooldown = 999; // managed manually
    }

    @Override
    protected void updatePhase() {
        if (phase == 1 && hp <= maxHp / 2) {
            phase = 2;
            speed = 95;
        }
    }

    @Override
    protected void doAttack(Player player, double deltaTime) {
        moveToward(player.getCenterX(), player.getCenterY(), deltaTime);

        // Leave liquid trail
        trailTimer += deltaTime;
        if (trailTimer >= 0.3) {
            trailTimer = 0;
            patches.add(new LiquidPatch(getCenterX(), getCenterY()));
        }

        // Tick patches (age them — DOT handled by tickPatches called from BossRoom)
        Iterator<LiquidPatch> it = patches.iterator();
        while (it.hasNext()) {
            LiquidPatch p = it.next();
            p.timeLeft -= deltaTime;
            if (p.timeLeft <= 0) it.remove();
        }

        // Language bullet attack
        attackTimer -= deltaTime;
        if (attackTimer <= 0) {
            fireLanguageBullet(player);
            bulletCycle = (bulletCycle + 1) % 3;
            attackTimer = (phase == 2 ? 1.3 : 2.0) + rng.nextDouble() * 0.8;
        }
    }

    private void fireLanguageBullet(Player player) {
        double dx   = player.getCenterX() - getCenterX();
        double dy   = player.getCenterY() - getCenterY();
        double dist = Math.hypot(dx, dy);
        if (dist < 1) return;
        double nx = dx / dist, ny = dy / dist;

        switch (bulletCycle) {
            case 0 -> // Python — stun
                bullets.add(new EffectBullet(getCenterX(), getCenterY(),
                    nx * 180, ny * 180, 8,
                    Color.color(0.7, 0.85, 0.1), "Py",
                    p -> p.applyStun(0.3)));
            case 1 -> // C++ — poison + slow
                bullets.add(new EffectBullet(getCenterX(), getCenterY(),
                    nx * 200, ny * 200, 12,
                    Color.CORNFLOWERBLUE, "C++",
                    p -> { p.applyPoison(3.0); p.applySlow(0.5, 3.0); }));
            case 2 -> // Java — burn
                bullets.add(new EffectBullet(getCenterX(), getCenterY(),
                    nx * 160, ny * 160, 10,
                    Color.ORANGERED, "Java",
                    p -> p.applyBurn(2.0)));
        }
    }

    /**
     * Called by BossRoom each tick.
     * Applies liquid patch DOT to player when they stand in a patch.
     */
    public void tickPatches(double deltaTime, Player player) {
        for (LiquidPatch p : patches) {
            double dist = Math.hypot(player.getCenterX() - p.wx,
                                     player.getCenterY() - p.wy);
            if (dist < 40) {
                p.dotTimer += deltaTime;
                if (p.dotTimer >= 0.3) {
                    player.takeDamage(2);
                    p.dotTimer = 0;
                }
            }
        }
    }

    @Override
    public void draw(GraphicsContext gc) {
        // Draw liquid patches on the floor first
        for (LiquidPatch p : patches) {
            double alpha = Math.min(0.45, p.timeLeft / 5.0 * 0.45);
            gc.setFill(Color.color(0.15, 0.75, 0.30, alpha));
            gc.fillOval(p.wx - 40, p.wy - 40, 80, 80);
        }

        // Boss body
        gc.setFill(phase == 2 ? Color.rgb(20, 130, 60) : Color.rgb(30, 160, 75));
        gc.fillRect(x, y, width, height);
        gc.setFill(Color.WHITE);
        gc.fillText("濕幗針", x + 6, y + height / 2 + 5);

        if (hp < maxHp) {
            gc.setFill(Color.DARKRED);
            gc.fillRect(x, y - 10, width, 5);
            gc.setFill(Color.LIMEGREEN);
            gc.fillRect(x, y - 10, width * (double) hp / maxHp, 5);
        }

        for (var b : bullets) b.draw(gc);
    }
}

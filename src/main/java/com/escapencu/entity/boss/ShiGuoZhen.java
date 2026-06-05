package com.escapencu.entity.boss;

import com.escapencu.entity.EffectBullet;
import com.escapencu.entity.HomingEffectBullet;
import com.escapencu.entity.Player;
import com.escapencu.util.ResourceLoader;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Stage 3 Final Boss — 濕幗針
 * Passive: leaves blue liquid trail (DOT zone).
 * Active:  throws Python / C++ / Java projectiles with status effects.
 */
public class ShiGuoZhen extends Boss {

    // ── Sprites ────────────────────────────────────────────────────────────
    private static final String BASE = "/images/boss/sgz/";
    private static final Image IMG_S      = ResourceLoader.getImage(BASE + "needle_idle_s.png", false);
    private static final Image IMG_PUDDLE = ResourceLoader.getImage(BASE + "puddle_water.png",  false);
    private static final Image IMG_PY     = ResourceLoader.getImage(BASE + "Python.png",         false);
    private static final Image IMG_CPP    = ResourceLoader.getImage(BASE + "Cpp.png",            false);
    private static final Image IMG_JAVA   = ResourceLoader.getImage(BASE + "Java.png",           false);

    // Bullet display size — normalises all three icons to the same size
    private static final double BULLET_SZ = 36.0;

    // ── Bob animation (vertical oscillation when moving) ─────────────────
    private double bobTimer = 0;

    // ── Liquid patch ──────────────────────────────────────────────────────
    private static class LiquidPatch {
        double wx, wy, timeLeft;
        double dotTimer = 0;
        LiquidPatch(double wx, double wy) { this.wx = wx; this.wy = wy; this.timeLeft = 5.0; }
    }

    private final List<LiquidPatch> patches  = new ArrayList<>();
    private double trailTimer = 0;

    // ── Attack ────────────────────────────────────────────────────────────
    private double attackTimer = 2.0;
    private int    bulletCycle = 0;
    private final Random rng   = new Random();

    public ShiGuoZhen(double cx, double cy, int stage) {
        super(cx - 68, cy - 68, 135, 135, 600 * stage, 80, 22 * stage);
        bulletDamage  = 16 * stage;
        shootCooldown = 999;
    }

    @Override
    protected void updatePhase() {
        if (phase == 1 && hp <= maxHp / 2) {
            phase = 2;
            speed = 120;
        }
    }

    @Override
    protected void doAttack(Player player, double deltaTime) {
        bobTimer += deltaTime;
        moveToward(player.getCenterX(), player.getCenterY(), deltaTime);

        // Leave liquid trail
        trailTimer += deltaTime;
        if (trailTimer >= 0.3) {
            trailTimer = 0;
            patches.add(new LiquidPatch(getCenterX(), getCenterY()));
        }

        // Age patches
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

    /** Fires 8 homing bullets in all directions; each bullet's language is random. */
    private void fireLanguageBullet(Player player) {
        double spd = 200;
        for (int i = 0; i < 8; i++) {
            double angle = i * (Math.PI / 4);
            int type = rng.nextInt(3); // 0=Python 1=C++ 2=Java

            Color  col  = switch (type) { case 0 -> Color.color(0.7, 0.85, 0.1); case 1 -> Color.CORNFLOWERBLUE; default -> Color.ORANGERED; };
            String lbl  = switch (type) { case 0 -> "Py"; case 1 -> "C++"; default -> "Java"; };
            Image  icon = switch (type) { case 0 -> IMG_PY; case 1 -> IMG_CPP; default -> IMG_JAVA; };
            int    dmg  = switch (type) { case 0 -> 8; case 1 -> 12; default -> 10; };
            java.util.function.Consumer<Player> effect = switch (type) {
                case 0  -> p -> p.applyStun(0.3);
                case 1  -> p -> { p.applyPoison(3.0); p.applySlow(0.5, 3.0); };
                default -> p -> p.applyBurn(2.0);
            };

            bullets.add(new HomingEffectBullet(
                    getCenterX(), getCenterY(),
                    Math.cos(angle) * spd, Math.sin(angle) * spd,
                    dmg, col, lbl, effect, BULLET_SZ, player, icon));
        }
    }

    /** Called by BossRoom each tick — applies DOT while player stands on a patch. */
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
        // ── Liquid patches (drawn under boss) ─────────────────────────────
        for (LiquidPatch p : patches) {
            double alpha = Math.min(0.6, p.timeLeft / 5.0 * 0.6);
            if (IMG_PUDDLE != null) {
                gc.setGlobalAlpha(alpha);
                double sz = 120;
                gc.drawImage(IMG_PUDDLE, p.wx - sz / 2, p.wy - sz / 2, sz, sz);
                gc.setGlobalAlpha(1.0);
            } else {
                gc.setFill(Color.color(0.2, 0.55, 0.9, alpha));
                gc.fillOval(p.wx - 40, p.wy - 40, 80, 80);
            }
        }

        // ── Boss body (vertical bob when moving) ──────────────────────────
        if (IMG_S != null) {
            double bobOffset = Math.sin(bobTimer * 9.0) * 3.5;
            if (phase == 2) {
                ColorAdjust p2 = new ColorAdjust();
                p2.setHue(-0.1);
                p2.setBrightness(-0.3);
                p2.setSaturation(0.5);
                gc.setEffect(p2);
            }
            gc.drawImage(IMG_S, x, y + bobOffset, width, height);
            gc.setEffect(null);
        } else {
            gc.setFill(phase == 2 ? Color.rgb(20, 130, 60) : Color.rgb(30, 160, 75));
            gc.fillRect(x, y, width, height);
            gc.setFill(Color.WHITE);
            gc.fillText("濕幗針", x + 6, y + height / 2 + 5);
        }

        // HP bar
        if (hp < maxHp) {
            gc.setFill(Color.DARKRED);
            gc.fillRect(x, y - 10, width, 5);
            gc.setFill(Color.LIMEGREEN);
            gc.fillRect(x, y - 10, width * (double) hp / maxHp, 5);
        }

        for (var b : bullets) b.draw(gc);
    }
}

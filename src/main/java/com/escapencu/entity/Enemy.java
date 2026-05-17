package com.escapencu.entity;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Base enemy class.
 * Subclasses (Squirrel, Goose, Termite, bosses) override update() for custom AI.
 * Call super.update(deltaTime) to handle bullet movement and shoot timer.
 */
public class Enemy extends Entity {
    protected double speed;
    protected int    contactDamage;
    protected double shootCooldown;
    protected double shootTimer = 0;
    protected int    bulletDamage;

    protected final List<Bullet> bullets = new ArrayList<>();

    public Enemy(double x, double y, double width, double height,
                 int hp, double speed, int contactDamage) {
        super(x, y, width, height, hp);
        this.speed         = speed;
        this.contactDamage = contactDamage;
        this.bulletDamage  = contactDamage;
        this.shootCooldown = 2.0;
    }

    @Override
    public void update(double deltaTime) {
        if (shootTimer > 0) shootTimer -= deltaTime;
        bullets.removeIf(b -> !b.isAlive());
        for (Bullet b : bullets) b.update(deltaTime);
    }

    // ── AI helpers (call from subclass or TestRoom) ───────────────────────

    /** Move toward a world position. */
    public void moveToward(double tx, double ty, double deltaTime) {
        double dx   = tx - getCenterX();
        double dy   = ty - getCenterY();
        double dist = Math.hypot(dx, dy);
        if (dist < 1) return;
        x += (dx / dist) * speed * deltaTime;
        y += (dy / dist) * speed * deltaTime;
    }

    /** Fire a bullet toward a world position (respects cooldown). */
    public void shootAt(double tx, double ty, double bulletSpeed) {
        if (shootTimer > 0) return;
        double dx   = tx - getCenterX();
        double dy   = ty - getCenterY();
        double dist = Math.hypot(dx, dy);
        if (dist < 1) return;
        bullets.add(new Bullet(getCenterX(), getCenterY(),
                               (dx / dist) * bulletSpeed,
                               (dy / dist) * bulletSpeed,
                               bulletDamage, false));
        shootTimer = shootCooldown;
    }

    @Override
    public void draw(GraphicsContext gc) {
        gc.setFill(Color.SALMON);
        gc.fillRect(x, y, width, height);

        // HP bar
        if (hp < maxHp) {
            gc.setFill(Color.DARKRED);
            gc.fillRect(x, y - 8, width, 5);
            gc.setFill(Color.LIMEGREEN);
            gc.fillRect(x, y - 8, width * (double) hp / maxHp, 5);
        }

        for (Bullet b : bullets) b.draw(gc);
    }

    public List<Bullet> getBullets()  { return bullets; }
    public int          getContactDamage() { return contactDamage; }
}

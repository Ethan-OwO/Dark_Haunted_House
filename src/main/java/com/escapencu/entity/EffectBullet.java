package com.escapencu.entity;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.function.Consumer;

/** Enemy bullet that applies a status effect to the player on hit. */
public class EffectBullet extends Bullet {

    private final Consumer<Player> onHit;
    private final Color            color;
    private final String           label;

    public EffectBullet(double cx, double cy, double vx, double vy,
                        int damage, Color color, String label,
                        Consumer<Player> onHit) {
        super(cx, cy, vx, vy, damage, false);
        this.onHit = onHit;
        this.color = color;
        this.label = label;
    }

    /** Constructor with custom hitbox size (for image-based logic-gate bullets). */
    public EffectBullet(double cx, double cy, double vx, double vy,
                        int damage, Color color, String label,
                        Consumer<Player> onHit, double size) {
        super(cx, cy, vx, vy, damage, false, size);
        this.onHit = onHit;
        this.color = color;
        this.label = label;
    }

    public void applyEffect(Player p) {
        if (onHit != null) onHit.accept(p);
    }

    @Override
    public void draw(GraphicsContext gc) {
        // If a custom image was set, delegate to Bullet's image+rotation logic
        if (hasImage()) {
            super.draw(gc);
            return;
        }
        // Fallback: colour oval + label
        gc.setFill(color);
        gc.fillOval(x, y, width, height);
        if (label != null && !label.isEmpty()) {
            gc.setFont(Font.font(8));
            gc.setFill(Color.WHITE);
            gc.fillText(label, x - 2, y - 2);
        }
    }
}

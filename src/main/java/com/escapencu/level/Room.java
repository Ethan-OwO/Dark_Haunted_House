package com.escapencu.level;

import com.escapencu.application.GameApp;
import com.escapencu.entity.Bullet;
import com.escapencu.entity.Enemy;
import com.escapencu.entity.Entity;
import com.escapencu.entity.Player;
import com.escapencu.map.Direction;
import com.escapencu.map.RoomNode;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * One room inside a dungeon floor.
 * Subclasses define the floor appearance and enemy spawning via init() and drawFloor().
 *
 * Door behaviour:
 *  - When room is NOT cleared: door areas are drawn as a locked wall (dark red bar).
 *  - When room IS cleared:     door areas are drawn as an open passage (floor colour gap).
 * Transition is triggered in GameScene when the player walks into a door trigger zone.
 */
public abstract class Room {
    public static final double WIDTH     = GameApp.WIDTH;
    public static final double HEIGHT    = GameApp.HEIGHT;
    public static final double WALL      = 24;   // wall thickness (px)
    public static final double DOOR_SIZE = 80;   // door opening width/height (px)
    public static final double DOOR_TRIGGER = 32; // how close to wall triggers transition

    protected final List<Entity> enemies = new ArrayList<>();
    protected RoomNode node;

    /** Called by LevelManager after creating the room instance. */
    public void setNode(RoomNode node) { this.node = node; }

    /** Spawn enemies and set up room contents. Called once when first entered. */
    public abstract void init();

    /** Draw only the floor (background). Called before walls/enemies. */
    protected abstract void drawFloor(GraphicsContext gc);

    // ── Update ─────────────────────────────────────────────────────────────
    public void update(double deltaTime, Player player) {
        enemies.removeIf(e -> !e.isAlive());
        for (Entity e : enemies) e.update(deltaTime);
        // Mark cleared when all enemies gone
        if (!node.isCleared() && enemies.stream().noneMatch(Entity::isAlive)) {
            node.setCleared(true);
        }
    }

    // ── Draw ───────────────────────────────────────────────────────────────
    public final void draw(GraphicsContext gc) {
        drawFloor(gc);
        drawWalls(gc);
        for (Entity e : enemies) e.draw(gc);
    }

    private void drawWalls(GraphicsContext gc) {
        boolean cleared = node.isCleared();
        Color wallColor   = Color.rgb(35, 35, 55);
        Color doorOpen    = Color.rgb(70, 50, 25);   // warm brown = open passage
        Color doorLocked  = Color.rgb(80, 20, 20);   // dark red   = locked

        // Top wall
        gc.setFill(wallColor);
        gc.fillRect(0, 0, WIDTH, WALL);
        if (node.hasDoor(Direction.NORTH)) {
            gc.setFill(cleared ? doorOpen : doorLocked);
            gc.fillRect(WIDTH/2 - DOOR_SIZE/2, 0, DOOR_SIZE, WALL);
        }

        // Bottom wall
        gc.setFill(wallColor);
        gc.fillRect(0, HEIGHT - WALL, WIDTH, WALL);
        if (node.hasDoor(Direction.SOUTH)) {
            gc.setFill(cleared ? doorOpen : doorLocked);
            gc.fillRect(WIDTH/2 - DOOR_SIZE/2, HEIGHT - WALL, DOOR_SIZE, WALL);
        }

        // Left wall
        gc.setFill(wallColor);
        gc.fillRect(0, 0, WALL, HEIGHT);
        if (node.hasDoor(Direction.WEST)) {
            gc.setFill(cleared ? doorOpen : doorLocked);
            gc.fillRect(0, HEIGHT/2 - DOOR_SIZE/2, WALL, DOOR_SIZE);
        }

        // Right wall
        gc.setFill(wallColor);
        gc.fillRect(WIDTH - WALL, 0, WALL, HEIGHT);
        if (node.hasDoor(Direction.EAST)) {
            gc.setFill(cleared ? doorOpen : doorLocked);
            gc.fillRect(WIDTH - WALL, HEIGHT/2 - DOOR_SIZE/2, WALL, DOOR_SIZE);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    public boolean     isCleared()        { return node.isCleared(); }
    public List<Entity> getEnemies()      { return enemies; }
    public RoomNode    getNode()          { return node; }

    public List<Bullet> getEnemyBullets() {
        List<Bullet> all = new ArrayList<>();
        for (Entity e : enemies)
            if (e instanceof Enemy en) all.addAll(en.getBullets());
        return all;
    }
}

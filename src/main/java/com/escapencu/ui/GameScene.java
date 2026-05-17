package com.escapencu.ui;

import com.escapencu.application.GameApp;
import com.escapencu.core.GameLoop;
import com.escapencu.core.GameState;
import com.escapencu.core.SceneManager;
import com.escapencu.entity.Bullet;
import com.escapencu.entity.Enemy;
import com.escapencu.entity.Entity;
import com.escapencu.entity.Player;
import com.escapencu.level.LevelManager;
import com.escapencu.level.Room;
import com.escapencu.map.Direction;
import com.escapencu.map.FloorMap;
import com.escapencu.map.RoomNode;
import com.escapencu.util.CollisionUtil;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GameScene {
    private Canvas          canvas;
    private GraphicsContext gc;
    private Player          player;
    private Room            currentRoom;
    private GameLoop        gameLoop;
    private LevelManager    levelManager;
    private FloorMap        floorMap;

    /** Room cache: rooms are created once and reused so enemies don't respawn on revisit. */
    private final Map<RoomNode, Room> roomCache   = new HashMap<>();
    private final Set<KeyCode>        pressedKeys = new HashSet<>();
    private double contactCooldown = 0;

    // ── Build ──────────────────────────────────────────────────────────────
    public Scene build() {
        canvas = new Canvas(GameApp.WIDTH, GameApp.HEIGHT);
        gc     = canvas.getGraphicsContext2D();

        levelManager = new LevelManager();
        floorMap     = levelManager.getFloorMap();
        enterStartRoom();

        canvas.setFocusTraversable(true);
        canvas.setOnKeyPressed(e  -> pressedKeys.add(e.getCode()));
        canvas.setOnKeyReleased(e -> pressedKeys.remove(e.getCode()));
        canvas.setOnMouseMoved(e  -> player.updateMousePosition(e.getX(), e.getY()));
        canvas.setOnMouseClicked(e -> player.shoot());

        gameLoop = new GameLoop(this);
        gameLoop.start();

        Scene scene = new Scene(new StackPane(canvas), GameApp.WIDTH, GameApp.HEIGHT);
        canvas.requestFocus();
        return scene;
    }

    // ── Frame callbacks (called by GameLoop) ───────────────────────────────
    public void update(double deltaTime) {
        player.handleMovement(pressedKeys, deltaTime, Room.WIDTH, Room.HEIGHT);
        player.update(deltaTime);
        currentRoom.update(deltaTime, player);
        if (contactCooldown > 0) contactCooldown -= deltaTime;

        resolveCollisions();
        GameState.playerHp = player.getHp();

        if (!player.isAlive()) {
            gameLoop.stop();
            SceneManager.showGameOver();
            return;
        }

        // ── EXIT room portal detection ─────────────────────────────────────
        if (floorMap.getCurrentNode().type == RoomNode.Type.EXIT) {
            double pcx = player.getCenterX(), pcy = player.getCenterY();
            if (Math.hypot(pcx - Room.WIDTH / 2, pcy - Room.HEIGHT / 2) < 45) {
                advanceToNextFloor();
                return;
            }
        }

        // ── BOSS room cleared → advance ────────────────────────────────────
        if (floorMap.getCurrentNode().type == RoomNode.Type.BOSS && currentRoom.isCleared()) {
            advanceToNextFloor();
            return;
        }

        // ── Door transition ────────────────────────────────────────────────
        Direction exitDir = checkDoorExit();
        if (exitDir != null) {
            floorMap.moveTo(exitDir);
            currentRoom = getOrCreateRoom(floorMap.getCurrentNode());
            spawnPlayerAtDoor(exitDir);
        }
    }

    public void render() {
        currentRoom.draw(gc);
        player.draw(gc);
        for (Bullet b : player.getBullets()) b.draw(gc);
        drawHUD();
        drawMiniMap();
    }

    // ── Room management ────────────────────────────────────────────────────
    private void enterStartRoom() {
        RoomNode startNode = floorMap.getNode(floorMap.startX, floorMap.startY);
        currentRoom = getOrCreateRoom(startNode);
        spawnPlayerCenter();
    }

    private Room getOrCreateRoom(RoomNode node) {
        return roomCache.computeIfAbsent(node, levelManager::createRoomForNode);
    }

    private void advanceToNextFloor() {
        if (levelManager.hasNextFloor()) {
            levelManager.advanceFloor();
            floorMap = levelManager.getFloorMap();
            roomCache.clear();
            enterStartRoom();
        } else {
            gameLoop.stop();
            SceneManager.showVictory();
        }
    }

    // ── Player spawn helpers ───────────────────────────────────────────────
    private void spawnPlayerCenter() {
        player = new Player(GameApp.WIDTH / 2.0 - 16, GameApp.HEIGHT / 2.0 - 16);
    }

    /**
     * Place the player just inside the door they entered from.
     * e.g. player moved NORTH → they appear near the SOUTH door of the new room.
     */
    private void spawnPlayerAtDoor(Direction movedDir) {
        double wall = Room.WALL + 8;
        double cx = GameApp.WIDTH  / 2.0 - 16;
        double cy = GameApp.HEIGHT / 2.0 - 16;
        double px, py;
        switch (movedDir) {
            case NORTH -> { px = cx; py = GameApp.HEIGHT - wall - 40; }
            case SOUTH -> { px = cx; py = wall + 8; }
            case EAST  -> { px = GameApp.WIDTH - wall - 40; py = cy; }
            case WEST  -> { px = wall + 8; py = cy; }
            default    -> { px = cx; py = cy; }
        }
        player = new Player(px, py);
    }

    // ── Door exit detection ────────────────────────────────────────────────
    /**
     * Returns which door the player is standing in, or null.
     * Transition only fires when the room is cleared.
     */
    private Direction checkDoorExit() {
        if (!currentRoom.isCleared()) return null;
        RoomNode node = floorMap.getCurrentNode();
        double   wall = Room.WALL;
        double   ds   = Room.DOOR_SIZE / 2;
        double   trig = Room.DOOR_TRIGGER;
        double   cx   = player.getCenterX();
        double   cy   = player.getCenterY();

        if (node.hasDoor(Direction.NORTH) && cy < wall + trig && Math.abs(cx - GameApp.WIDTH  / 2.0) < ds) return Direction.NORTH;
        if (node.hasDoor(Direction.SOUTH) && cy > GameApp.HEIGHT - wall - trig && Math.abs(cx - GameApp.WIDTH  / 2.0) < ds) return Direction.SOUTH;
        if (node.hasDoor(Direction.WEST)  && cx < wall + trig && Math.abs(cy - GameApp.HEIGHT / 2.0) < ds) return Direction.WEST;
        if (node.hasDoor(Direction.EAST)  && cx > GameApp.WIDTH - wall - trig && Math.abs(cy - GameApp.HEIGHT / 2.0) < ds) return Direction.EAST;

        return null;
    }

    // ── Collision resolution ───────────────────────────────────────────────
    private void resolveCollisions() {
        // Player bullets → enemies
        for (Bullet b : player.getBullets()) {
            if (!b.isAlive()) continue;
            for (Entity e : currentRoom.getEnemies()) {
                if (e.isAlive() && CollisionUtil.overlaps(b, e)) {
                    e.takeDamage(b.getDamage());
                    b.hit();
                    GameState.score += 10;
                    break;
                }
            }
        }
        // Enemy bullets → player
        for (Bullet b : currentRoom.getEnemyBullets()) {
            if (b.isAlive() && CollisionUtil.overlaps(b, player)) {
                player.takeDamage(b.getDamage());
                b.hit();
            }
        }
        // Enemy body contact → player
        if (contactCooldown <= 0) {
            for (Entity e : currentRoom.getEnemies()) {
                if (e.isAlive() && CollisionUtil.overlaps(e, player)) {
                    int dmg = (e instanceof Enemy en) ? en.getContactDamage() : 5;
                    player.takeDamage(dmg);
                    contactCooldown = 0.5;
                    break;
                }
            }
        }
    }

    // ── HUD ────────────────────────────────────────────────────────────────
    private void drawHUD() {
        double barW = 200, barH = 20;
        gc.setFill(Color.DARKRED);    gc.fillRect(10, 10, barW, barH);
        gc.setFill(Color.LIMEGREEN);  gc.fillRect(10, 10, barW * (double) GameState.playerHp / GameState.playerMaxHp, barH);
        gc.setStroke(Color.WHITE);    gc.strokeRect(10, 10, barW, barH);
        gc.setFill(Color.WHITE);      gc.fillText("HP: " + GameState.playerHp + " / " + GameState.playerMaxHp, 15, 25);

        gc.setFill(Color.WHITE);
        String floorLabel = levelManager.getFloorNum() == 3 ? "Boss Floor" : "Floor " + levelManager.getFloorNum();
        gc.fillText("Stage " + levelManager.getStage() + "  " + floorLabel, GameApp.WIDTH / 2.0 - 60, 25);
        gc.fillText("分數: " + GameState.score, GameApp.WIDTH - 120, 25);
    }

    // ── Mini-map ────────────────────────────────────────────────────────────
    private void drawMiniMap() {
        int    G        = FloorMap.GRID;
        double cell     = 14;
        double gap      = 2;
        double total    = G * (cell + gap);
        double originX  = GameApp.WIDTH  - total - 12;
        double originY  = 10;

        RoomNode[][] grid = floorMap.getGrid();
        int curX = floorMap.getCurrentX(), curY = floorMap.getCurrentY();

        for (int y = 0; y < G; y++) {
            for (int x = 0; x < G; x++) {
                RoomNode n = grid[y][x];
                if (n == null) continue;

                double rx = originX + x * (cell + gap);
                double ry = originY + y * (cell + gap);

                // Room background
                Color fill;
                if (x == curX && y == curY)        fill = Color.WHITE;
                else if (n.type == RoomNode.Type.BOSS)  fill = Color.DARKRED;
                else if (n.type == RoomNode.Type.EXIT)  fill = Color.GOLDENROD;
                else if (n.isCleared())                 fill = Color.rgb(50, 110, 50);
                else                                    fill = Color.rgb(70, 70, 70);
                gc.setFill(fill);
                gc.fillRect(rx, ry, cell, cell);

                // Door connectors between adjacent rooms
                gc.setFill(Color.rgb(110, 90, 55));
                if (n.hasDoor(Direction.EAST)  && x < G-1 && grid[y][x+1] != null)
                    gc.fillRect(rx + cell, ry + cell/2 - 1, gap, 3);
                if (n.hasDoor(Direction.SOUTH) && y < G-1 && grid[y+1][x] != null)
                    gc.fillRect(rx + cell/2 - 1, ry + cell, 3, gap);
            }
        }

        // Border
        gc.setStroke(Color.rgb(100, 100, 100));
        gc.setLineWidth(1);
        gc.strokeRect(originX - 3, originY - 3, total + 5, total + 5);
    }
}

package com.escapencu.entity;

import javafx.scene.canvas.GraphicsContext;

/**
 * Base class for every object in the game world (player, enemies, bullets).
 * Subclasses must implement update() and draw().
 */
public abstract class Entity {
    protected double x, y;
    protected double width, height;
    protected int hp, maxHp;
    protected boolean alive = true;

    public Entity(double x, double y, double width, double height, int hp) {
        this.x = x;
        this.y = y;
        this.width  = width;
        this.height = height;
        this.hp     = hp;
        this.maxHp  = hp;
    }

    public abstract void update(double deltaTime);
    public abstract void draw(GraphicsContext gc);

    public void takeDamage(int damage) {
        hp -= damage;
        if (hp <= 0) {
            hp    = 0;
            alive = false;
        }
    }

    // ── Getters ────────────────────────────────────────────────────────────
    public boolean isAlive()   { return alive; }
    public double  getX()      { return x; }
    public double  getY()      { return y; }
    public double  getWidth()  { return width; }
    public double  getHeight() { return height; }
    public int     getHp()     { return hp; }
    public int     getMaxHp()  { return maxHp; }
    public double  getCenterX(){ return x + width  / 2; }
    public double  getCenterY(){ return y + height / 2; }
}

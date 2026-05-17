package com.escapencu.entity.boss;

import com.escapencu.entity.Enemy;
import com.escapencu.entity.Player;

/** Abstract base for all boss enemies. */
public abstract class Boss extends Enemy {

    protected int     phase       = 1;
    protected boolean invincible  = false;

    protected double roomX, roomY, roomW, roomH;

    protected Boss(double x, double y, double w, double h,
                   int hp, double speed, int contactDamage) {
        super(x, y, w, h, hp, speed, contactDamage);
    }

    @Override
    public void takeDamage(int damage) {
        if (!invincible) super.takeDamage(damage);
    }

    protected void updatePhase() {
        if (phase == 1 && hp <= maxHp / 2) phase = 2;
    }

    @Override
    public void update(double deltaTime, Player player) {
        super.update(deltaTime); // bullet tick + shoot timer
        updatePhase();
        doAttack(player, deltaTime);
    }

    protected abstract void doAttack(Player player, double deltaTime);

    public void setRoomBounds(double x, double y, double w, double h) {
        this.roomX = x;
        this.roomY = y;
        this.roomW = w;
        this.roomH = h;
    }
}

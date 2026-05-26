package com.escapencu.util;

import com.escapencu.entity.Entity;

public class CollisionUtil {
    /** AABB rectangle overlap check. */
    public static boolean overlaps(Entity a, Entity b) {
        return a.getX() < b.getX() + b.getWidth()
            && a.getX() + a.getWidth()  > b.getX()
            && a.getY() < b.getY() + b.getHeight()
            && a.getY() + a.getHeight() > b.getY();
    }
}

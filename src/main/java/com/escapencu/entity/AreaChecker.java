package com.escapencu.entity;

/**
 * Functional interface used by Player to query whether a given rectangle
 * is within passable dungeon area (rooms + corridors).
 * Keeps Player independent from the level package.
 */
@FunctionalInterface
public interface AreaChecker {
    boolean canMoveTo(double x, double y, double w, double h);
}

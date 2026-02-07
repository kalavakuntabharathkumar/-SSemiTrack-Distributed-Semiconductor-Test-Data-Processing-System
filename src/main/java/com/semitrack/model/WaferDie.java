package com.semitrack.model;

import java.util.Objects;

/**
 * Represents a single die on a wafer identified by its (x, y) coordinates.
 *
 * Used as a key in TreeMap-based wafer maps so dies are always iterated in
 * coordinate order (row-major: sort by x first, then y).
 */
public final class WaferDie implements Comparable<WaferDie> {

    private final int x;
    private final int y;

    public WaferDie(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }
    public int getY() { return y; }

    /** Row-major ordering: compare x first, then y. */
    @Override
    public int compareTo(WaferDie other) {
        if (this.x != other.x) return Integer.compare(this.x, other.x);
        return Integer.compare(this.y, other.y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WaferDie d)) return false;
        return x == d.x && y == d.y;
    }

    @Override
    public int hashCode() { return Objects.hash(x, y); }

    @Override
    public String toString() { return "(" + x + "," + y + ")"; }
}

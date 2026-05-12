package com.example.snapshot;

import com.example.model.Cell;

import java.io.Serializable;

public class CellSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private int x;
    private int y;
    private String terrain;
    private String ownerId;
    private int troopsCount;
    private int level;
    private boolean water;

    public CellSnapshot() {}

    public CellSnapshot(Cell cell) {
        this.id = cell.getId();
        this.x = cell.getX();
        this.y = cell.getY();
        this.terrain = cell.getTerrain().name();
        this.ownerId = cell.getOwnerId();
        this.troopsCount = cell.getTroopsCount();
        this.level = cell.getLevel();
        this.water = cell.isWater();
    }

    // Геттеры и сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }

    public int getY() { return y; }
    public void setY(int y) { this.y = y; }

    public String getTerrain() { return terrain; }
    public void setTerrain(String terrain) { this.terrain = terrain; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public int getTroopsCount() { return troopsCount; }
    public void setTroopsCount(int troopsCount) { this.troopsCount = troopsCount; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public boolean isWater() { return water; }
    public void setWater(boolean water) { this.water = water; }
}
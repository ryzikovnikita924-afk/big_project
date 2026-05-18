package com.example.model;

import java.util.Objects;
import java.util.UUID;

public class Cell {
    private final String id;
    private final int x;
    private final int y;
    private final TerrainType terrain;
    private String ownerId;
    private int troopsCount;
    private int level;
    private int productionRate;
    private long lastUpdatedAt;

    public Cell(int x, int y, TerrainType terrain) {
        this.id = generateId(x, y);
        this.x = x;
        this.y = y;
        this.terrain = terrain;
        this.troopsCount = terrain == TerrainType.WATER ? 0 : 10;
        this.level = 1;
        this.productionRate = calculateBaseProduction();
        this.lastUpdatedAt = System.currentTimeMillis();
    }

    private static String generateId(int x, int y) {
        return x + ":" + y;
    }

    private int calculateBaseProduction() {
        if (isWater()) return 0;
        switch (terrain) {
            case CITY: return 30;
            case FOREST: return 15;
            case MOUNTAIN: return 10;
            case PLAIN: return 12;
            default: return 0;
        }
    }

    // Геттеры и сеттеры
    public String getId() { return id; }
    public int getX() { return x; }
    public int getY() { return y; }
    public TerrainType getTerrain() { return terrain; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public int getTroopsCount() { return troopsCount; }
    public void setTroopsCount(int troopsCount) { this.troopsCount = troopsCount; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getProductionRate() { return productionRate; }
    public long getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(long lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }

    public boolean isNeutral() { return ownerId == null; }
    public boolean isWater() { return terrain == TerrainType.WATER; }

    // Текущее производство ресурсов (уровень влияет на производство)
    public int getCurrentProduction() {
        return productionRate * level;
    }


    public boolean upgrade() {
        if (level >= 5) return false;
        level++;
        return true;
    }


    public boolean canUpgrade() {
        return level < 5;
    }

    // Стоимость следующего улучшения
    public int getUpgradeCostGold() {
        return 100 * level;
    }

    public int getUpgradeCostWood() {
        return 50 * level;
    }

    public int getDefenseBonus() {
        // Бонус защиты увеличивается с уровнем клетки
        return (int)(troopsCount * terrain.getDefenseBonus() * (1 + level * 0.1));
    }


    public void addTroops(int amount) {
        this.troopsCount += amount;
    }


    public void takeDamage(int damage) {
        this.troopsCount = Math.max(0, this.troopsCount - damage);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cell cell = (Cell) o;
        return Objects.equals(id, cell.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return String.format("Cell[%d,%d] owner=%s troops=%d level=%d terrain=%s",
                x, y, ownerId != null ? ownerId : "neutral", troopsCount, level, terrain);
    }
}
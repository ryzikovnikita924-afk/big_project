package com.example.model;

import java.util.Objects;

public class Cell {
    private final String id;
    private final int x;
    private final int y;
    private final TerrainType terrain;
    private String ownerId;
    private int level;
    private int productionRate;
    private BuildingType building;  // Добавлено: здание на клетке

    public Cell(int x, int y, TerrainType terrain) {
        this.id = generateId(x, y);
        this.x = x;
        this.y = y;
        this.terrain = terrain;
        this.level = 1;
        this.productionRate = calculateBaseProduction();
        this.building = BuildingType.NONE;
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

    public String getId() { return id; }
    public int getX() { return x; }
    public int getY() { return y; }
    public TerrainType getTerrain() { return terrain; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getProductionRate() { return productionRate; }
    public BuildingType getBuilding() { return building; }
    public void setBuilding(BuildingType building) { this.building = building; }

    public boolean isNeutral() { return ownerId == null; }
    public boolean isWater() { return terrain == TerrainType.WATER; }
    public boolean hasBuilding() { return building != BuildingType.NONE; }

    public int getCurrentProduction() {
        int base = productionRate * level;
        // Добавляем бонус от здания
        if (building != null) {
            base += building.getGoldBonus() + building.getWoodBonus() + building.getFoodBonus();
        }
        return base;
    }

    public int getGoldBonus() {
        return building != null ? building.getGoldBonus() : 0;
    }

    public int getWoodBonus() {
        return building != null ? building.getWoodBonus() : 0;
    }

    public int getFoodBonus() {
        return building != null ? building.getFoodBonus() : 0;
    }

    public int getTroopBonus() {
        // Казармы дают +5 войск в ход
        return building == BuildingType.BARRACKS ? 5 : 0;
    }

    public boolean upgrade() {
        if (level >= 5) return false;
        level++;
        return true;
    }

    public boolean canUpgrade() {
        return level < 5;
    }

    public int getUpgradeCostGold() {
        return 100 * level;
    }

    public int getUpgradeCostWood() {
        return 50 * level;
    }

    public int getConquestCost() {
        if (isWater()) return Integer.MAX_VALUE;

        if (isNeutral()) {
            return 0;
        }

        int baseCost = 10;
        switch (terrain) {
            case CITY: baseCost = 15; break;
            case FOREST: baseCost = 12; break;
            case MOUNTAIN: baseCost = 20; break;
            case PLAIN: baseCost = 8; break;
        }
        return baseCost * level;
    }

    public boolean canBuild(BuildingType buildingType) {
        if (building != BuildingType.NONE) return false; // Уже есть здание
        if (ownerId == null) return false; // Нейтральную клетку нельзя улучшать
        return buildingType.canBuildOn(terrain);
    }

    public int getBuildCostGold(BuildingType buildingType) {
        return buildingType.getBuildCostGold();
    }

    public int getBuildCostWood(BuildingType buildingType) {
        return buildingType.getBuildCostWood();
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
        return String.format("Cell[%d,%d] owner=%s level=%d terrain=%s building=%s",
                x, y, ownerId != null ? ownerId : "neutral", level, terrain, building.getDisplayName());
    }
}
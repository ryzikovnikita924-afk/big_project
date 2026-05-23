package com.example.model;

public enum BuildingType {
    NONE("Нет", 0, 0, 0),
    BARRACKS("Казармы", 20, 10, 0),      // +войска в ход
    PORT("Порт", 30, 15, 0),              // +золото
    SAWMILL("Лесопилка", 0, 20, 0),       // +дерево
    FARM("Ферма", 0, 0, 15);              // +еда

    private final String displayName;
    private final int goldBonus;
    private final int woodBonus;
    private final int foodBonus;

    BuildingType(String displayName, int goldBonus, int woodBonus, int foodBonus) {
        this.displayName = displayName;
        this.goldBonus = goldBonus;
        this.woodBonus = woodBonus;
        this.foodBonus = foodBonus;
    }

    public String getDisplayName() { return displayName; }
    public int getGoldBonus() { return goldBonus; }
    public int getWoodBonus() { return woodBonus; }
    public int getFoodBonus() { return foodBonus; }

    public boolean canBuildOn(TerrainType terrain) {
        switch (this) {
            case BARRACKS: return terrain == TerrainType.PLAIN;
            case PORT: return terrain == TerrainType.WATER;
            case SAWMILL: return terrain == TerrainType.FOREST;
            case FARM: return terrain == TerrainType.PLAIN;
            default: return false;
        }
    }

    public int getBuildCostGold() {
        switch (this) {
            case BARRACKS: return 100;
            case PORT: return 150;
            case SAWMILL: return 80;
            case FARM: return 60;
            default: return 0;
        }
    }

    public int getBuildCostWood() {
        switch (this) {
            case BARRACKS: return 50;
            case PORT: return 30;
            case SAWMILL: return 40;
            case FARM: return 30;
            default: return 0;
        }
    }
}
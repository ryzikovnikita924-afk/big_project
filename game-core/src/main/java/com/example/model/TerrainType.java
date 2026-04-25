package com.example.model;

public enum TerrainType {
    PLAIN(1.0, "Равнина"),
    FOREST(1.2, "Лес"),
    MOUNTAIN(1.5, "Горы"),
    WATER(0.0, "Вода"),
    CITY(0.8, "Город");

    private final double defenseBonus;
    private final String displayName;

    TerrainType(double defenseBonus, String displayName) {
        this.defenseBonus = defenseBonus;
        this.displayName = displayName;
    }

    public double getDefenseBonus(){
        return defenseBonus;
    }
    private String getDisplayName(){
        return displayName;
    }

}

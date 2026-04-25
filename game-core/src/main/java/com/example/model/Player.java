package com.example.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Player {
    private final String id;
    private final String name;
    private final Map<ResourceType, Integer> resources;
    private final Set<String> capturedCellIds;
    private int totalTroops;
    private int victories;

    public Player(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.resources = new HashMap<>();
        this.capturedCellIds = new HashSet<>();

        // Стартовые ресурсы
        resources.put(ResourceType.GOLD, 100);
        resources.put(ResourceType.WOOD, 50);
        resources.put(ResourceType.FOOD, 50);

        this.totalTroops = 0;
        this.victories = 0;
    }

    // Геттеры
    public String getId() { return id; }
    public String getName() { return name; }
    public Map<ResourceType, Integer> getResources() { return resources; }
    public Set<String> getCapturedCellIds() { return capturedCellIds; }
    public int getTotalTroops() { return totalTroops; }
    public int getVictories() { return victories; }

    public void addResource(ResourceType type, int amount) {
        resources.put(type, resources.getOrDefault(type, 0) + amount);
    }

    public boolean spendResource(ResourceType type, int amount) {
        int current = resources.getOrDefault(type, 0);
        if (current >= amount) {
            resources.put(type, current - amount);
            return true;
        }
        return false;
    }

    public void addCell(String cellId) {
        capturedCellIds.add(cellId);
    }

    public void removeCell(String cellId) {
        capturedCellIds.remove(cellId);
    }

    public void addTroops(int amount) {
        totalTroops += amount;
    }

    public void addVictory() {
        victories++;
    }

    @Override
    public String toString() {
        return String.format("Player{name='%s', cells=%d, gold=%d}",
                name, capturedCellIds.size(), resources.getOrDefault(ResourceType.GOLD, 0));
    }
}

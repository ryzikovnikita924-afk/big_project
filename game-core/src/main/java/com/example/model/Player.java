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
    private int population;

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
        this.population = 50;    // Стартовое население
    }

    // Геттеры
    public String getId() { return id; }
    public String getName() { return name; }
    public Map<ResourceType, Integer> getResources() { return resources; }
    public Set<String> getCapturedCellIds() { return capturedCellIds; }
    public int getTotalTroops() { return totalTroops; }
    public int getVictories() { return victories; }
    public int getPopulation() { return population; }

    public int getResource(ResourceType type) {
        return resources.getOrDefault(type, 0);
    }

    public void setPopulation(int population) {
        this.population = population;
    }

    public void addPopulation(int amount) {
        this.population += amount;
    }

    public void subtractPopulation(int amount) {
        this.population = Math.max(0, this.population - amount);
    }

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
    public void setId(String id) {

    }


    public void setTotalTroops(int totalTroops) {
        this.totalTroops = totalTroops;
    }


    public void setVictories(int victories) {
        this.victories = victories;
    }

    public boolean hasEnoughResources(Map<ResourceType, Integer> required) {
        for (Map.Entry<ResourceType, Integer> entry : required.entrySet()) {
            if (getResource(entry.getKey()) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    public void spendResources(Map<ResourceType, Integer> required) {
        for (Map.Entry<ResourceType, Integer> entry : required.entrySet()) {
            spendResource(entry.getKey(), entry.getValue());
        }
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

    public void removeTroops(int amount) {
        totalTroops = Math.max(0, totalTroops - amount);
    }

    public void addVictory() {
        victories++;
    }

    public int getTotalCells() {
        return capturedCellIds.size();
    }

    public boolean isAlive() {
        return !capturedCellIds.isEmpty();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("cellsCount", capturedCellIds.size());
        map.put("victories", victories);
        map.put("totalTroops", totalTroops);
        map.put("population", population);
        map.put("gold", getResource(ResourceType.GOLD));
        map.put("wood", getResource(ResourceType.WOOD));
        map.put("food", getResource(ResourceType.FOOD));
        return map;
    }

    @Override
    public String toString() {
        return String.format("Player{name='%s', cells=%d, gold=%d, population=%d}",
                name, capturedCellIds.size(), getResource(ResourceType.GOLD), population);
    }
}
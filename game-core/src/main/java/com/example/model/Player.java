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
    private final Set<Cell> capturedCells;
    private int totalTroops;
    private int victories;
    private int population;

    public Player(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.resources = new HashMap<>();
        this.capturedCells = new HashSet<>();


        resources.put(ResourceType.GOLD, 100);
        resources.put(ResourceType.WOOD, 50);
        resources.put(ResourceType.FOOD, 50);

        this.totalTroops = 0;
        this.victories = 0;
        this.population = 50;
    }

    // Геттеры
    public String getId() { return id; }
    public String getName() { return name; }
    public Set<Cell> getcapturedCells() {return capturedCells;}
    public Map<ResourceType, Integer> getResources() { return resources; }
    public Set<String> getCapturedCellIds(Set<Cell> capturedCells) {
        Set<String> ids = new HashSet<>();
        for (Cell cell : capturedCells) {
            String id = cell.getId();
            ids.add(id);
        }
        return ids;
    }
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
    public int setTroopsCount(int number){
        return totalTroops = number;
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

    public void addCell(Cell cell) {
        capturedCells.add(cell);
    }

    public void removeCell(Cell cell) {
        capturedCells.remove(cell);
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
        return capturedCells.size();
    }

    public boolean isAlive() {
        return !capturedCells.isEmpty();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("cellsCount", capturedCells.size());
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
                name, capturedCells.size(), getResource(ResourceType.GOLD), population);
    }
}
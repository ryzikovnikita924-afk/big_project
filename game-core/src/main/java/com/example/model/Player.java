package com.example.model;

import java.util.*;

public class Player {
    private String id;
    private final String name;
    private final Map<ResourceType, Integer> resources;
    private final Set<Cell> capturedCells;
    private int totalTroops;
    private int victories;
    private int population;
    private int totalGames;
    private int totalWins;


    public Player(String name) {
        this(UUID.randomUUID().toString(), name);
    }


    public Player(String id, String name) {
        this.id = id;
        this.name = name;
        this.resources = new HashMap<>();
        this.capturedCells = new HashSet<>();
        this.totalGames = 0;
        this.totalWins = 0;

        resources.put(ResourceType.GOLD, 100);
        resources.put(ResourceType.WOOD, 50);
        resources.put(ResourceType.FOOD, 50);

        this.totalTroops = 0;
        this.victories = 0;
        this.population = 50;
    }


    public String getId() { return id; }
    public String getName() { return name; }
    public Set<Cell> getCapturedCells() { return capturedCells; }  // Переименовано
    public Map<ResourceType, Integer> getResources() { return resources; }
    public int getTotalTroops() { return totalTroops; }
    public int getVictories() { return victories; }
    public int getPopulation() { return population; }
    public int getTotalWins() { return totalWins; }
    public int getTotalGames() { return totalGames; }


    public void setId(String id) { this.id = id; }
    public void setTotalWins(int wins) { this.totalWins = wins; }
    public void setTotalGames(int games) { this.totalGames = games; }
    public void setTotalTroops(int totalTroops) { this.totalTroops = totalTroops; }
    public void setVictories(int victories) { this.victories = victories; }
    public void setPopulation(int population) { this.population = population; }


    public int getResource(ResourceType type) {
        return resources.getOrDefault(type, 0);
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


    public void addCell(Cell cell) {
        capturedCells.add(cell);
        cell.setOwnerId(this.id);
    }

    public void removeCell(Cell cell) {
        capturedCells.remove(cell);
    }


    public Set<String> getCapturedCellIds() {
        Set<String> ids = new HashSet<>();
        for (Cell cell : capturedCells) {
            ids.add(cell.getId());
        }
        return ids;
    }

    public int getTotalCells() {
        return capturedCells.size();
    }

    // Методы войск
    public void addTroops(int amount) {
        totalTroops += amount;
    }

    public void removeTroops(int amount) {
        totalTroops = Math.max(0, totalTroops - amount);
    }

    public void addVictory() {
        victories++;
        totalWins++;
    }

    public boolean isAlive() {
        return !capturedCells.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("Player{id='%s', name='%s', cells=%d, gold=%d}",
                id, name, capturedCells.size(), getResource(ResourceType.GOLD));
    }
}
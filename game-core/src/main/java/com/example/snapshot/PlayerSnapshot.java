package com.example.snapshot;

import com.example.model.Player;
import com.example.model.ResourceType;

import java.io.Serializable;
import java.util.*;

public class PlayerSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private Map<String, Integer> resources;
    private Set<String> capturedCellIds;
    private int totalTroops;
    private int victories;
    private int population;

    public PlayerSnapshot() {}

    public PlayerSnapshot(Player player) {
        this.id = player.getId();
        this.name = player.getName();
        this.resources = new HashMap<>();
        for (Map.Entry<ResourceType, Integer> entry : player.getResources().entrySet()) {
            this.resources.put(entry.getKey().name(), entry.getValue());
        }
        this.capturedCellIds = new HashSet<>(player.getCapturedCellIds(player.getcapturedCells()));
        this.totalTroops = player.getTotalTroops();
        this.victories = player.getVictories();
        this.population = player.getPopulation();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Map<String, Integer> getResources() { return resources; }
    public void setResources(Map<String, Integer> resources) { this.resources = resources; }

    public Set<String> getCapturedCellIds() { return capturedCellIds; }
    public void setCapturedCellIds(Set<String> capturedCellIds) { this.capturedCellIds = capturedCellIds; }

    public int getTotalTroops() { return totalTroops; }
    public void setTotalTroops(int totalTroops) { this.totalTroops = totalTroops; }

    public int getVictories() { return victories; }
    public void setVictories(int victories) { this.victories = victories; }

    public int getPopulation() { return population; }
    public void setPopulation(int population) { this.population = population; }
}
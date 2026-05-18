package com.example.world;

import com.example.service.TurnService;
import com.example.model.*;
import com.example.engine.GameEngine;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;

@Component
public class GameWorld {
    private final Map<String, Cell> cells = new ConcurrentHashMap<>();
    private final Map<String, Player> players = new ConcurrentHashMap<>();
    private final GameEngine engine = new GameEngine();
    private volatile boolean running = false;
    private TurnService turnService;

    public void setTurnService(TurnService turnService) {
        this.turnService = turnService;
    }

    public void createWorld(int width, int height) {
        Random random = new Random();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                TerrainType terrain;
                double rand = random.nextDouble();
                if (rand < 0.1) terrain = TerrainType.WATER;
                else if (rand < 0.3) terrain = TerrainType.FOREST;
                else if (rand < 0.4) terrain = TerrainType.MOUNTAIN;
                else if (rand < 0.5) terrain = TerrainType.CITY;
                else terrain = TerrainType.PLAIN;
                Cell cell = new Cell(x, y, terrain);
                cells.put(cell.getId(), cell);
            }
        }
        System.out.printf("Создан мир %dx%d, всего клеток: %d%n", width, height, cells.size());
    }

    public void addPlayer(Player player, int startX, int startY) {
        String cellId = startX + ":" + startY;
        Cell startCell = cells.get(cellId);
        if (startCell == null || startCell.isWater()) {
            throw new IllegalArgumentException("Неверная стартовая позиция!");
        }
        players.put(player.getId(), player);
        startCell.setOwnerId(player.getId());
        startCell.setTroopsCount(20);
        player.addCell(startCell);
        player.addTroops(20);
        System.out.printf("Игрок %s начал игру на клетке [%d,%d]%n", player.getName(), startX, startY);
    }

    public void start() {
        if (running) return;
        running = true;
        System.out.println("🚀 Игровой мир запущен!");
    }

    public void stop() {
        running = false;
        System.out.println("🛑 Игровой мир остановлен");
    }

    public Map<String, Cell> getCells() { return Collections.unmodifiableMap(cells); }
    public Map<String, Player> getPlayers() { return Collections.unmodifiableMap(players); }
    public Cell getCell(String id) { return cells.get(id); }
    public Player getPlayer(String id) { return players.get(id); }

    public List<Cell> getNeighbors(Cell cell) {
        List<Cell> neighbors = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                String neighborId = (cell.getX() + dx) + ":" + (cell.getY() + dy);
                Cell neighbor = cells.get(neighborId);
                if (neighbor != null && engine.areNeighbors(cell, neighbor)) {
                    neighbors.add(neighbor);
                }
            }
        }
        return neighbors;
    }

    public boolean startTurn(String playerId) {
        return turnService != null && turnService.startTurn(playerId);
    }

    public boolean endTurn(String playerId) {
        return turnService != null && turnService.endTurn(playerId);
    }

    public boolean isMyTurn(String playerId) {
        return turnService != null && turnService.isMyTurn(playerId);
    }

    public boolean canAttack(String playerId) {
        return turnService != null && turnService.canAttack(playerId);
    }

    public Player getCurrentPlayer() {
        return turnService != null ? turnService.getCurrentPlayer() : null;
    }

    public int getCurrentTurn() {
        return turnService != null ? turnService.getTurnNumber() : 0;
    }

    public Player getWinner() {
        return turnService != null ? turnService.getWinner() : null;
    }
    public void clear() {
        cells.clear();
        players.clear();
    }

    public void addCell(Cell cell) {
        cells.put(cell.getId(), cell);
    }


    public void addPlayerDirect(Player player) {
        players.put(player.getId(), player);
    }
    public void executeInstantAttack(List<Cell> playerterritory, Cell attackcell, String playerId) {

        System.out.println("⚔️ Атака: " + playerId + " -> " +  "Клетки" +  attackcell);

        if (playerterritory == null || attackcell == null) {
            throw new IllegalArgumentException("Клетка не найдена");
        }

        if (!engine.canAttack(playerterritory, attackcell)) {
            throw new IllegalStateException("Атака невозможна!");
        }

        if (!canAttack(playerId)) {
            throw new IllegalStateException("Сейчас не ваш ход! Начните ход кнопкой 'Начать ход'.");
        }
        Player attacker = players.get(playerId);
        int attackerPower = attacker.getTotalTroops();
        int defenderPower = attackcell.getDefenseBonus();

        if (attackerPower > defenderPower) {
            int remainingTroops = attackerPower - defenderPower;
            String oldOwnerId = attackcell.getOwnerId();

            attackcell.setOwnerId(playerId);
            attackcell.setTroopsCount(remainingTroops);
            attacker.setTroopsCount(attacker.getTotalTroops() - attackerPower);

            if (attacker != null) {
                attacker.addCell(attackcell);
                attacker.addVictory();
            }
            if (oldOwnerId != null) {
                Player oldOwner = players.get(oldOwnerId);
                if (oldOwner != null) {
                    oldOwner.removeCell(attackcell);
                }
            }
            System.out.println("✅ Атака успешна! Клетка захвачена!");
        } else {
            attacker.setTroopsCount(attacker.getTotalTroops() - defenderPower);
            System.out.println("❌ Атака отбита!");
        }
    }
}
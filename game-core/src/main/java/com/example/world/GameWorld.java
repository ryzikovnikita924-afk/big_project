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
    private int worldWidth = 10;
    private int worldHeight = 10;
    private boolean worldCreated = false;

    public void setTurnService(TurnService turnService) {
        this.turnService = turnService;
    }

    public void createWorld(int width, int height) {
        this.worldWidth = width;
        this.worldHeight = height;
        Random random = new Random();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                boolean isPlayerStart = (x == 2 && y == 2);
                boolean isAIStart = (x == 7 && y == 7);

                TerrainType terrain;
                double rand = random.nextDouble();

                if (isPlayerStart || isAIStart) {
                    terrain = TerrainType.PLAIN;
                } else {
                    if (rand < 0.1) terrain = TerrainType.WATER;
                    else if (rand < 0.3) terrain = TerrainType.FOREST;
                    else if (rand < 0.4) terrain = TerrainType.MOUNTAIN;
                    else if (rand < 0.5) terrain = TerrainType.CITY;
                    else terrain = TerrainType.PLAIN;
                }

                Cell cell = new Cell(x, y, terrain);
                cells.put(cell.getId(), cell);
            }
        }

        worldCreated = true;
        System.out.printf("Создан мир %dx%d, всего клеток: %d%n", width, height, cells.size());
    }

    public void reset() {
        cells.clear();
        players.clear();
        running = false;
        worldCreated = false;
        createWorld(worldWidth, worldHeight);
        System.out.println("🔄 Игровой мир сброшен!");
    }

    public void refreshWorld() {
        if (!worldCreated) {
            createWorld(worldWidth, worldHeight);
        }
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
                int nx = cell.getX() + dx;
                int ny = cell.getY() + dy;
                if (nx >= 0 && nx < worldWidth && ny >= 0 && ny < worldHeight) {
                    String neighborId = nx + ":" + ny;
                    Cell neighbor = cells.get(neighborId);
                    if (neighbor != null) {
                        neighbors.add(neighbor);
                    }
                }
            }
        }
        return neighbors;
    }


    public boolean canAttack(String playerId) {
        return turnService != null && turnService.canAttack(playerId);
    }



    public Player getWinner() {
        return turnService != null ? turnService.getWinner() : null;
    }

    public void clear() {
        cells.clear();
        players.clear();
        worldCreated = false;
    }

    public void addCell(Cell cell) {
        cells.put(cell.getId(), cell);
    }

    public void addPlayerDirect(Player player) {
        players.put(player.getId(), player);
    }

    public void addPlayer(Player player, int startX, int startY) {
        String cellId = startX + ":" + startY;
        Cell startCell = cells.get(cellId);

        if (startCell == null || startCell.isWater()) {
            startCell = findNearestLandCell(startX, startY);
            if (startCell == null) {
                throw new IllegalArgumentException("Неверная стартовая позиция!");
            }
            System.out.println("Стартовая позиция изменена с [" + startX + "," + startY +
                    "] на [" + startCell.getX() + "," + startCell.getY() + "]");
        }

        players.put(player.getId(), player);
        startCell.setOwnerId(player.getId());
        player.addCell(startCell);
        System.out.printf("Игрок %s начал игру на клетке [%d,%d] с %d войсками%n",
                player.getName(), startCell.getX(), startCell.getY(), player.getTotalTroops());
    }

    private Cell findNearestLandCell(int startX, int startY) {
        for (int radius = 1; radius <= 5; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    int x = startX + dx;
                    int y = startY + dy;
                    if (x >= 0 && x < worldWidth && y >= 0 && y < worldHeight) {
                        String cellId = x + ":" + y;
                        Cell cell = cells.get(cellId);
                        if (cell != null && !cell.isWater()) {
                            return cell;
                        }
                    }
                }
            }
        }
        return null;
    }

    public void executeInstantAttack(List<Cell> playerTerritory, Cell attackCell, String playerId) {
        System.out.println("⚔️ Атака: " + playerId + " -> клетка [" + attackCell.getX() + "," + attackCell.getY() + "]");

        if (playerTerritory == null || playerTerritory.isEmpty() || attackCell == null) {
            throw new IllegalArgumentException("Клетка не найдена");
        }

        if (!engine.canAttack(playerTerritory, attackCell)) {
            throw new IllegalStateException("Атака невозможна! Можно атаковать только соседние клетки.");
        }

        if (!canAttack(playerId)) {
            throw new IllegalStateException("Сейчас не ваш ход! Начните ход кнопкой 'Начать ход'.");
        }

        Player attacker = players.get(playerId);
        if (attacker == null) {
            throw new IllegalStateException("Атакующий игрок не найден!");
        }

        boolean isNeutral = attackCell.getOwnerId() == null;
        int conquestCost = attackCell.getConquestCost();


        if (!isNeutral && attacker.getTotalTroops() < conquestCost) {
            throw new IllegalStateException("Недостаточно войск для атаки! Нужно " + conquestCost + ", есть " + attacker.getTotalTroops());
        }


        String oldOwnerId = attackCell.getOwnerId();
        attackCell.setOwnerId(playerId);


        if (!isNeutral) {
            attacker.removeTroops(conquestCost);
            System.out.println("Потрачено войск: " + conquestCost);
        } else {
            System.out.println("🌾 Нейтральная клетка захвачена бесплатно!");
        }

        System.out.println("Осталось войск у атакующего: " + attacker.getTotalTroops());

        if (oldOwnerId != null && !oldOwnerId.equals(playerId)) {
            Player oldOwner = players.get(oldOwnerId);
            if (oldOwner != null) {
                oldOwner.removeCell(attackCell);
            }
            attacker.addVictory();
        }

        attacker.addCell(attackCell);

        System.out.println("✅ Атака успешна! Клетка захвачена!");
    }

    public void buildBuilding(Cell cell, BuildingType buildingType, String playerId) {
        Player player = players.get(playerId);
        if (player == null) {
            System.out.println("❌ Игрок не найден для строительства!");
            return;
        }

        if (!player.getId().equals(cell.getOwnerId())) {
            System.out.println("❌ Нельзя строить на чужой клетке!");
            return;
        }

        if (cell.hasBuilding()) {
            System.out.println("❌ На этой клетке уже есть здание!");
            return;
        }

        if (!cell.canBuild(buildingType)) {
            System.out.println("❌ Нельзя построить " + buildingType.getDisplayName() + " на " + cell.getTerrain());
            return;
        }

        int costGold = buildingType.getBuildCostGold();
        int costWood = buildingType.getBuildCostWood();

        if (player.getResource(ResourceType.GOLD) < costGold) {
            System.out.println("❌ Недостаточно золота! Нужно: " + costGold + ", есть: " + player.getResource(ResourceType.GOLD));
            return;
        }

        if (player.getResource(ResourceType.WOOD) < costWood) {
            System.out.println("❌ Недостаточно дерева! Нужно: " + costWood + ", есть: " + player.getResource(ResourceType.WOOD));
            return;
        }

        player.spendResource(ResourceType.GOLD, costGold);
        player.spendResource(ResourceType.WOOD, costWood);
        cell.setBuilding(buildingType);

        System.out.println("🏗️ Построено " + buildingType.getDisplayName() +
                " на клетке [" + cell.getX() + "," + cell.getY() + "]!");
        System.out.println("   Осталось золота: " + player.getResource(ResourceType.GOLD));
        System.out.println("   Осталось дерева: " + player.getResource(ResourceType.WOOD));
    }
}
package com.example.world;

import com.example.engine.GameEngine;
import com.example.model.*;

import java.util.*;
import java.util.concurrent.*;

public class GameWorld {
    private final Map<String, Cell> cells = new ConcurrentHashMap<>();
    private final Map<String, Player> players = new ConcurrentHashMap<>();
    private final PriorityBlockingQueue<AttackOrder> actionQueue = new PriorityBlockingQueue<>();
    private final GameEngine engine = new GameEngine();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private volatile boolean running = false;

    // Создание мира с сеткой N x M
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

        System.out.printf("Создан мир %dx%d, всего клеток: %d%n",
                width, height, cells.size());
    }

    // Добавление игрока и выделение стартовой клетки
    public void addPlayer(Player player, int startX, int startY) {
        String cellId = startX + ":" + startY;
        Cell startCell = cells.get(cellId);

        if (startCell == null || startCell.isWater()) {
            throw new IllegalArgumentException("Неверная стартовая позиция!");
        }

        players.put(player.getId(), player);
        startCell.setOwnerId(player.getId());
        startCell.setTroopsCount(20);
        player.addCell(cellId);
        player.addTroops(20);

        System.out.printf("Игрок %s начал игру на клетке [%d,%d]%n",
                player.getName(), startX, startY);
    }

    // Создание приказа на атаку
    public void scheduleAttack(String fromCellId, String toCellId,
                               int troopsCount, String playerId) {
        Cell fromCell = cells.get(fromCellId);
        Cell toCell = cells.get(toCellId);

        if (fromCell == null || toCell == null) {
            throw new IllegalArgumentException("Клетка не найдена");
        }

        if (!fromCell.getOwnerId().equals(playerId)) {
            throw new IllegalStateException("Это не ваша клетка!");
        }

        if (troopsCount > fromCell.getTroopsCount()) {
            throw new IllegalStateException("Недостаточно войск!");
        }

        if (!engine.canAttack(fromCell, toCell)) {
            throw new IllegalStateException("Атака невозможна!");
        }

        AttackOrder order = new AttackOrder(fromCellId, toCellId, troopsCount, playerId);
        long now = System.currentTimeMillis();
        order.setStartedAt(now);
        order.setCompletesAt(now + engine.getAttackDuration());

        actionQueue.add(order);
        System.out.printf("⚔️ Приказ на атаку от %s с клетки %s на %s (%d войск). Завершится через %d мс%n",
                playerId, fromCellId, toCellId, troopsCount, engine.getAttackDuration());
    }

    // Обработка тика игры
    public void processTick() {
        long now = System.currentTimeMillis();
        List<AttackOrder> completedOrders = new ArrayList<>();

        // Забираем все завершенные атаки
        while (!actionQueue.isEmpty()) {
            AttackOrder next = actionQueue.peek();
            if (next.getCompletesAt() <= now) {
                completedOrders.add(actionQueue.poll());
            } else {
                break;
            }
        }

        // Выполняем завершенные атаки
        for (AttackOrder order : completedOrders) {
            try {
                Cell fromCell = cells.get(order.getFromCellId());
                Cell toCell = cells.get(order.getToCellId());

                // Проверяем, что условия атаки все еще выполняются
                if (engine.canAttack(fromCell, toCell) &&
                        fromCell.getTroopsCount() >= order.getTroopsCount()) {

                    // Временно уменьшаем войска на атакующей клетке
                    fromCell.setTroopsCount(fromCell.getTroopsCount() - order.getTroopsCount());

                    // Создаем копию для битвы с указанным количеством войск
                    Cell attackingForce = new Cell(fromCell.getX(), fromCell.getY(), fromCell.getTerrain());
                    attackingForce.setTroopsCount(order.getTroopsCount());
                    attackingForce.setOwnerId(fromCell.getOwnerId());

                    engine.executeAttack(order, attackingForce, toCell);

                    // Обновляем победителя
                    if (toCell.getOwnerId() != null &&
                            toCell.getOwnerId().equals(order.getAttackerId())) {
                        Player player = players.get(order.getAttackerId());
                        player.addCell(toCell.getId());
                        player.addVictory();

                        Player oldOwner = players.get(order.getFromCellId());
                        if (oldOwner != null) {
                            oldOwner.removeCell(toCell.getId());
                        }
                    }
                } else {
                    System.out.printf("⚠️ Атака отменена: условия больше не выполняются%n");
                }
            } catch (Exception e) {
                System.err.println("Ошибка при выполнении атаки: " + e.getMessage());
            }
        }
    }

    // Производство ресурсов
    public void produceResources() {
        for (Player player : players.values()) {
            int goldIncome = 0;
            int woodIncome = 0;
            int foodIncome = 0;

            for (String cellId : player.getCapturedCellIds()) {
                Cell cell = cells.get(cellId);
                if (cell != null && !cell.isWater()) {
                    int production = engine.calculateResourceProduction(cell);
                    switch (cell.getTerrain()) {
                        case CITY: goldIncome += production; break;
                        case FOREST: woodIncome += production; break;
                        case PLAIN: foodIncome += production; break;
                        default: goldIncome += production / 2;
                    }
                }
            }

            if (goldIncome > 0) player.addResource(ResourceType.GOLD, goldIncome);
            if (woodIncome > 0) player.addResource(ResourceType.WOOD, woodIncome);
            if (foodIncome > 0) player.addResource(ResourceType.FOOD, foodIncome);

            System.out.printf("💰 %s получил ресурсы: золото +%d, дерево +%d, еда +%d%n",
                    player.getName(), goldIncome, woodIncome, foodIncome);
        }
    }

    // Запуск игрового цикла
    public void start() {
        if (running) return;
        running = true;

        // Тик обработки атак (каждую секунду)
        scheduler.scheduleAtFixedRate(() -> {
            try {
                processTick();
            } catch (Exception e) {
                System.err.println("Ошибка в processTick: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.SECONDS);

        // Производство ресурсов (каждые 10 секунд)
        scheduler.scheduleAtFixedRate(() -> {
            try {
                produceResources();
            } catch (Exception e) {
                System.err.println("Ошибка в produceResources: " + e.getMessage());
            }
        }, engine.getProductionInterval(), engine.getProductionInterval(), TimeUnit.MILLISECONDS);

        System.out.println("🚀 Игровой мир запущен!");
    }

    public void stop() {
        running = false;
        scheduler.shutdown();
        System.out.println("🛑 Игровой мир остановлен");
    }

    // Геттеры для внешнего доступа
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
}
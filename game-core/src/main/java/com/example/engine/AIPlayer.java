package com.example.engine;

import com.example.model.*;
import com.example.service.TurnService;
import com.example.world.GameWorld;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class AIPlayer {

    public enum AITactic {
        AGGRESSIVE, DEFENSIVE, ECONOMIC, BALANCED, RANDOM
    }

    private final GameWorld gameWorld;
    private final TurnService turnService;
    private AITactic tactic = AITactic.BALANCED;
    private Random random = new Random();

    public AIPlayer(GameWorld gameWorld, TurnService turnService) {
        this.gameWorld = gameWorld;
        this.turnService = turnService;
    }

    public void setTactic(AITactic tactic) {
        this.tactic = tactic;
        System.out.println("🤖 AI тактика изменена на: " + tactic);
    }

    public AITactic getCurrentTactic() {
        return this.tactic;
    }

    public void makeTurn(String aiPlayerId) {
        System.out.println("\n🤖 AI (" + getPlayerName(aiPlayerId) + ") начинает ход. Тактика: " + tactic);

        try {
            Player aiPlayer = gameWorld.getPlayer(aiPlayerId);
            if (aiPlayer == null) {
                System.out.println("❌ AI игрок не найден!");
                return;
            }

            System.out.println("📊 AI состояние до хода:");
            System.out.println("  - Клеток: " + aiPlayer.getCapturedCells().size());
            System.out.println("  - Войск: " + aiPlayer.getTotalTroops());
            System.out.println("  - Золото: " + aiPlayer.getResource(ResourceType.GOLD));
            System.out.println("  - Дерево: " + aiPlayer.getResource(ResourceType.WOOD));
            System.out.println("  - Еда: " + aiPlayer.getResource(ResourceType.FOOD));

            if (turnService.getState() != TurnService.GameState.WAITING) {
                System.out.println("❌ AI не может начать ход. Состояние игры: " + turnService.getState());
                return;
            }

            if (!turnService.startTurn(aiPlayerId)) {
                System.out.println("❌ AI не может начать ход!");
                return;
            }

            Thread.sleep(500);

            aiPlayer = gameWorld.getPlayer(aiPlayerId);
            if (aiPlayer == null) {
                System.out.println("❌ AI игрок не найден после начала хода!");
                turnService.endTurn(aiPlayerId);
                return;
            }

            System.out.println("📊 AI состояние после начала хода:");
            System.out.println("  - Клеток: " + aiPlayer.getCapturedCells().size());
            System.out.println("  - Войск: " + aiPlayer.getTotalTroops());
            System.out.println("  - Золото: " + aiPlayer.getResource(ResourceType.GOLD));
            System.out.println("  - Дерево: " + aiPlayer.getResource(ResourceType.WOOD));
            System.out.println("  - Еда: " + aiPlayer.getResource(ResourceType.FOOD));

            List<Cell> myCells = getAICells(aiPlayerId);
            System.out.println("📊 Клетки AI: " + myCells.size());

            for (Cell cell : myCells) {
                System.out.println("  - Клетка AI [" + cell.getX() + "," + cell.getY() + "] " + cell.getTerrain());
            }

            if (myCells.isEmpty()) {
                System.out.println("⚠️ У AI нет клеток!");
                Thread.sleep(500);
                turnService.endTurn(aiPlayerId);
                return;
            }

            int availableTroops = aiPlayer.getTotalTroops();
            int availableGold = aiPlayer.getResource(ResourceType.GOLD);
            int availableWood = aiPlayer.getResource(ResourceType.WOOD);

            System.out.println("📊 Доступно: войск=" + availableTroops + ", золота=" + availableGold + ", дерева=" + availableWood);

            // Находим все возможные цели
            List<Cell> allNeighbors = findAllNeighbors(myCells);
            List<Cell> enemyTargets = allNeighbors.stream()
                    .filter(c -> c.getOwnerId() != null && !c.getOwnerId().equals(aiPlayerId) && !c.isWater())
                    .collect(Collectors.toList());
            List<Cell> neutralTargets = allNeighbors.stream()
                    .filter(c -> c.getOwnerId() == null && !c.isWater())
                    .collect(Collectors.toList());

            System.out.println("🎯 Доступные цели: вражеских=" + enemyTargets.size() + ", нейтральных=" + neutralTargets.size());

            for (Cell target : enemyTargets) {
                System.out.println("  - Вражеская клетка [" + target.getX() + "," + target.getY() + "] " + target.getTerrain());
            }
            for (Cell target : neutralTargets) {
                System.out.println("  - Нейтральная клетка [" + target.getX() + "," + target.getY() + "] " + target.getTerrain());
            }

            // Сначала пробуем атаковать (приоритет)
            boolean attacked = false;

            if (!enemyTargets.isEmpty()) {
                Cell target = selectBestTarget(enemyTargets);
                int conquestCost = target.getConquestCost() + 5;
                if (availableTroops >= conquestCost) {
                    System.out.println("🤖 AI атакует вражескую клетку [" + target.getX() + "," + target.getY() +
                            "] стоимостью " + conquestCost + " войск");
                    gameWorld.executeInstantAttack(myCells, target, aiPlayerId);
                    attacked = true;
                } else {
                    System.out.println("🤖 AI недостаточно войск для атаки врага! Нужно: " + conquestCost);
                }
            }

            // Если не атаковали врага, пробуем захватить нейтральную клетку
            if (!attacked && !neutralTargets.isEmpty()) {
                Cell target = selectBestTarget(neutralTargets);
                int conquestCost = target.getConquestCost();
                if (availableTroops >= conquestCost) {
                    System.out.println("🤖 AI атакует нейтральную клетку [" + target.getX() + "," + target.getY() +
                            "] стоимостью " + conquestCost + " войск");
                    gameWorld.executeInstantAttack(myCells, target, aiPlayerId);
                    attacked = true;
                } else {
                    System.out.println("🤖 AI недостаточно войск для атаки нейтрала! Нужно: " + conquestCost);
                }
            }

            // Если не атаковали, пробуем построить здание
            if (!attacked) {
                tryBuildBuilding(aiPlayerId, myCells, availableGold, availableWood);
            }

            Thread.sleep(500);
            turnService.endTurn(aiPlayerId);
            System.out.println("✅ AI завершил ход");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("AI ход прерван: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Ошибка в AI ходе: " + e.getMessage());
            e.printStackTrace();
            try {
                turnService.endTurn(aiPlayerId);
            } catch (Exception ex) {
                System.err.println("Не удалось завершить ход AI: " + ex.getMessage());
            }
        }
    }

    private List<Cell> findAllNeighbors(List<Cell> myCells) {
        Set<Cell> allNeighbors = new HashSet<>();
        for (Cell cell : myCells) {
            List<Cell> neighbors = gameWorld.getNeighbors(cell);
            allNeighbors.addAll(neighbors);
        }
        // Убираем свои клетки из списка соседей
        allNeighbors.removeAll(myCells);
        return new ArrayList<>(allNeighbors);
    }

    private Cell selectBestTarget(List<Cell> targets) {
        // Выбираем цель с наименьшей стоимостью захвата
        return targets.stream()
                .min(Comparator.comparingInt(Cell::getConquestCost))
                .orElse(null);
    }

    private boolean tryBuildBuilding(String aiPlayerId, List<Cell> myCells, int gold, int wood) {
        List<Cell> buildableCells = myCells.stream()
                .filter(cell -> !cell.hasBuilding())
                .collect(Collectors.toList());

        if (buildableCells.isEmpty()) {
            System.out.println("🏗️ Нет свободных клеток для строительства");
            return false;
        }

        // Приоритеты строительства
        BuildingType[] priorities = {BuildingType.SAWMILL, BuildingType.FARM, BuildingType.BARRACKS, BuildingType.PORT};

        for (BuildingType building : priorities) {
            Cell targetCell = buildableCells.stream()
                    .filter(cell -> cell.canBuild(building))
                    .findFirst()
                    .orElse(null);

            if (targetCell != null) {
                int costGold = building.getBuildCostGold();
                int costWood = building.getBuildCostWood();

                if (gold >= costGold && wood >= costWood) {
                    System.out.println("🏗️ AI строит " + building.getDisplayName() +
                            " на клетке [" + targetCell.getX() + "," + targetCell.getY() + "]");
                    gameWorld.buildBuilding(targetCell, building, aiPlayerId);
                    return true;
                }
            }
        }

        return false;
    }

    private List<Cell> getAICells(String aiPlayerId) {
        Player player = gameWorld.getPlayer(aiPlayerId);
        if (player == null) return new ArrayList<>();

        return player.getCapturedCells().stream()
                .filter(cell -> !cell.isWater())
                .collect(Collectors.toList());
    }

    private String getPlayerName(String playerId) {
        Player player = gameWorld.getPlayer(playerId);
        return player != null ? player.getName() : "Unknown";
    }
}
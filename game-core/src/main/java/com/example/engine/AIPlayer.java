package com.example.engine;

import com.example.model.Cell;
import com.example.model.Player;
import com.example.model.ResourceType;
import com.example.world.GameWorld;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class AIPlayer {

    public enum AITactic {
        AGGRESSIVE,   // Агрессивный - атакует как можно чаще
        DEFENSIVE,    // Защитный - укрепляет свои клетки
        ECONOMIC,     // Экономический - копит ресурсы
        BALANCED,     // Сбалансированный
        RANDOM        // Случайный
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

    public void makeTurn(String aiPlayerId) {
        System.out.println("\n🤖 AI (" + getPlayerName(aiPlayerId) + ") начинает ход. Тактика: " + tactic);

        try {
            // Начинаем ход
            if (!turnService.startTurn(aiPlayerId)) {
                System.out.println("❌ AI не может начать ход!");
                return;
            }

            // Небольшая задержка для имитации "обдумывания"
            Thread.sleep(500);

            // Выполняем действия в зависимости от тактики
            switch (tactic) {
                case AGGRESSIVE:
                    aggressiveMove(aiPlayerId);
                    break;
                case DEFENSIVE:
                    defensiveMove(aiPlayerId);
                    break;
                case ECONOMIC:
                    economicMove(aiPlayerId);
                    break;
                case RANDOM:
                    randomMove(aiPlayerId);
                    break;
                default:
                    balancedMove(aiPlayerId);
                    break;
            }

            // Небольшая задержка перед завершением хода
            Thread.sleep(500);

            // Завершаем ход
            turnService.endTurn(aiPlayerId);
            System.out.println("✅ AI завершил ход");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Агрессивная тактика - атакует соседние клетки
    private void aggressiveMove(String aiPlayerId) {
        List<Cell> myCells = getAICells(aiPlayerId);
        if (myCells.isEmpty()) return;

        // Находим все возможные цели для атаки
        List<Cell> targets = findAttackTargets(aiPlayerId, myCells);

        if (!targets.isEmpty()) {
            // Атакуем наиболее слабую цель
            targets.sort(Comparator.comparingInt(Cell::getTroopsCount));
            Cell target = targets.get(0);
            Cell fromCell = findBestAttackingCell(aiPlayerId, target);

            if (fromCell != null) {
                int troopsToUse = Math.min(fromCell.getTroopsCount() / 2, 30);
                troopsToUse = Math.max(troopsToUse, 10);

                System.out.println("🤖 AI атакует клетку [" + target.getX() + "," + target.getY() +
                        "] с " + troopsToUse + " войсками");

                gameWorld.executeInstantAttack(fromCell.getId(), target.getId(), troopsToUse, aiPlayerId);
            }
        } else {
            // Нет целей - усиливаем войска
            reinforceWeakCells(aiPlayerId, myCells);
        }
    }

    // Защитная тактика - укрепляет свои клетки
    private void defensiveMove(String aiPlayerId) {
        List<Cell> myCells = getAICells(aiPlayerId);
        if (myCells.isEmpty()) return;

        // Укрепляем граничные клетки
        List<Cell> borderCells = findBorderCells(aiPlayerId, myCells);

        if (!borderCells.isEmpty()) {
            // Распределяем войска на граничные клетки
            for (Cell cell : borderCells) {
                int currentTroops = cell.getTroopsCount();
                int targetTroops = Math.min(currentTroops + 15, 100);
                // В реальности у нас нет прямого метода добавления войск,
                // но мы можем оставить больше войск в клетке при атаке
                System.out.println("🤖 AI укрепляет клетку [" + cell.getX() + "," + cell.getY() + "]");
            }
        }

        // Атакуем только если есть явное преимущество
        List<Cell> targets = findAttackTargets(aiPlayerId, myCells);
        for (Cell target : targets) {
            Cell fromCell = findBestAttackingCell(aiPlayerId, target);
            if (fromCell != null && fromCell.getTroopsCount() > target.getTroopsCount() * 1.5) {
                int troopsToUse = Math.min(fromCell.getTroopsCount() / 2, target.getTroopsCount() + 5);
                System.out.println("🤖 AI (защитная) атакует с преимуществом");
                gameWorld.executeInstantAttack(fromCell.getId(), target.getId(), troopsToUse, aiPlayerId);
                break;
            }
        }
    }

    // Экономическая тактика - копит ресурсы
    private void economicMove(String aiPlayerId) {
        Player ai = gameWorld.getPlayer(aiPlayerId);
        if (ai == null) return;

        System.out.println("🤖 AI копит ресурсы: Золото=" + ai.getResource(ResourceType.GOLD) +
                ", Дерево=" + ai.getResource(ResourceType.WOOD) +
                ", Еда=" + ai.getResource(ResourceType.FOOD));

        // Атакуем только соседние нейтральные или слабые клетки
        List<Cell> myCells = getAICells(aiPlayerId);
        List<Cell> neutralTargets = findNeutralTargets(aiPlayerId, myCells);

        if (!neutralTargets.isEmpty()) {
            Cell target = neutralTargets.get(0);
            Cell fromCell = findBestAttackingCell(aiPlayerId, target);
            if (fromCell != null && fromCell.getTroopsCount() > target.getTroopsCount() + 10) {
                int troopsToUse = Math.min(15, fromCell.getTroopsCount() / 3);
                gameWorld.executeInstantAttack(fromCell.getId(), target.getId(), troopsToUse, aiPlayerId);
            }
        }
    }

    // Сбалансированная тактика
    private void balancedMove(String aiPlayerId) {
        List<Cell> myCells = getAICells(aiPlayerId);
        if (myCells.isEmpty()) return;

        List<Cell> targets = findAttackTargets(aiPlayerId, myCells);

        // Если есть выгодные цели - атакуем
        for (Cell target : targets) {
            Cell fromCell = findBestAttackingCell(aiPlayerId, target);
            if (fromCell != null && fromCell.getTroopsCount() > target.getTroopsCount() + 5) {
                int troopsToUse = Math.min(fromCell.getTroopsCount() / 2, 25);
                System.out.println("🤖 AI (сбалансированный) атакует");
                gameWorld.executeInstantAttack(fromCell.getId(), target.getId(), troopsToUse, aiPlayerId);
                return;
            }
        }

        // Иначе усиляемся
        reinforceWeakCells(aiPlayerId, myCells);
    }

    // Случайная тактика
    private void randomMove(String aiPlayerId) {
        List<Cell> myCells = getAICells(aiPlayerId);
        if (myCells.isEmpty()) return;

        int action = random.nextInt(3);

        switch (action) {
            case 0: // Атака
                List<Cell> targets = findAttackTargets(aiPlayerId, myCells);
                if (!targets.isEmpty()) {
                    Cell target = targets.get(random.nextInt(targets.size()));
                    Cell fromCell = findBestAttackingCell(aiPlayerId, target);
                    if (fromCell != null) {
                        int troopsToUse = random.nextInt(Math.min(fromCell.getTroopsCount(), 30)) + 5;
                        gameWorld.executeInstantAttack(fromCell.getId(), target.getId(), troopsToUse, aiPlayerId);
                    }
                }
                break;
            case 1: // Усиление
                reinforceWeakCells(aiPlayerId, myCells);
                break;
            case 2: // Ничего не делать
                System.out.println("🤖 AI (случайный) пропускает ход");
                break;
        }
    }

    // Вспомогательные методы

    private List<Cell> getAICells(String aiPlayerId) {
        return gameWorld.getCells().values().stream()
                .filter(cell -> aiPlayerId.equals(cell.getOwnerId()) && !cell.isWater())
                .collect(Collectors.toList());
    }

    private List<Cell> findAttackTargets(String aiPlayerId, List<Cell> myCells) {
        Set<Cell> targets = new HashSet<>();

        for (Cell myCell : myCells) {
            List<Cell> neighbors = gameWorld.getNeighbors(myCell);
            for (Cell neighbor : neighbors) {
                if (neighbor.getOwnerId() != null &&
                        !neighbor.getOwnerId().equals(aiPlayerId) &&
                        !neighbor.isWater()) {
                    targets.add(neighbor);
                }
            }
        }

        return new ArrayList<>(targets);
    }

    private List<Cell> findNeutralTargets(String aiPlayerId, List<Cell> myCells) {
        Set<Cell> targets = new HashSet<>();

        for (Cell myCell : myCells) {
            List<Cell> neighbors = gameWorld.getNeighbors(myCell);
            for (Cell neighbor : neighbors) {
                if (neighbor.getOwnerId() == null && !neighbor.isWater()) {
                    targets.add(neighbor);
                }
            }
        }

        return new ArrayList<>(targets);
    }

    private List<Cell> findBorderCells(String aiPlayerId, List<Cell> myCells) {
        Set<Cell> borderCells = new HashSet<>();

        for (Cell myCell : myCells) {
            List<Cell> neighbors = gameWorld.getNeighbors(myCell);
            for (Cell neighbor : neighbors) {
                if (neighbor.getOwnerId() == null || !neighbor.getOwnerId().equals(aiPlayerId)) {
                    borderCells.add(myCell);
                    break;
                }
            }
        }

        return new ArrayList<>(borderCells);
    }

    private Cell findBestAttackingCell(String aiPlayerId, Cell target) {
        List<Cell> myCells = getAICells(aiPlayerId);

        for (Cell myCell : myCells) {
            List<Cell> neighbors = gameWorld.getNeighbors(myCell);
            if (neighbors.contains(target)) {
                return myCell;
            }
        }

        return myCells.isEmpty() ? null : myCells.get(0);
    }

    private void reinforceWeakCells(String aiPlayerId, List<Cell> myCells) {
        // Находим самые слабые клетки
        myCells.sort(Comparator.comparingInt(Cell::getTroopsCount));

        for (Cell weakCell : myCells.stream().limit(3).collect(Collectors.toList())) {
            System.out.println("🤖 AI усиливает слабую клетку [" + weakCell.getX() + "," + weakCell.getY() +
                    "] (войск: " + weakCell.getTroopsCount() + ")");
        }
    }

    private String getPlayerName(String playerId) {
        Player player = gameWorld.getPlayer(playerId);
        return player != null ? player.getName() : "Unknown";
    }
}
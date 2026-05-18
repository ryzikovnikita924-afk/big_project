package com.example.engine;

import com.example.model.Cell;
import com.example.model.Player;
import com.example.model.ResourceType;
import com.example.service.TurnService;
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

            // Получаем текущего игрока
            Player aiPlayer = gameWorld.getPlayer(aiPlayerId);
            if (aiPlayer == null) {
                System.out.println("❌ AI игрок не найден!");
                turnService.endTurn(aiPlayerId);
                return;
            }

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

    // Агрессивная тактика - атакует как можно чаще
    private void aggressiveMove(String aiPlayerId) {
        List<Cell> myCells = getAICells(aiPlayerId);
        if (myCells.isEmpty()) return;

        // Находим все возможные цели для атаки
        List<Cell> targets = findAttackTargets(aiPlayerId, myCells);

        if (!targets.isEmpty()) {
            // Атакуем наиболее слабую цель
            targets.sort(Comparator.comparingInt(Cell::getTroopsCount));
            Cell target = targets.get(0);

            System.out.println("🤖 AI атакует клетку [" + target.getX() + "," + target.getY() +
                    "] с защитой " + target.getDefenseBonus());

            // Вызываем executeInstantAttack с моими клетками и целевой клеткой
            gameWorld.executeInstantAttack(myCells, target, aiPlayerId);
        } else {
            System.out.println("🤖 AI (агрессивный) нет целей для атаки");
        }
    }

    // Защитная тактика - редко атакует, только при явном преимуществе
    private void defensiveMove(String aiPlayerId) {
        List<Cell> myCells = getAICells(aiPlayerId);
        if (myCells.isEmpty()) return;

        // Атакуем только если у нас больше войск, чем у защитника
        List<Cell> targets = findAttackTargets(aiPlayerId, myCells);

        Player attacker = gameWorld.getPlayer(aiPlayerId);
        if (attacker == null) return;

        for (Cell target : targets) {
            // Проверяем, что у нас больше войск, чем защита клетки
            if (attacker.getTotalTroops() > target.getDefenseBonus() * 2) {
                System.out.println("🤖 AI (защитный) атакует с большим преимуществом");
                gameWorld.executeInstantAttack(myCells, target, aiPlayerId);
                return;
            }
        }

        System.out.println("🤖 AI (защитный) не нашел выгодных целей для атаки");
    }

    // Экономическая тактика - атакует только нейтральные клетки
    private void economicMove(String aiPlayerId) {
        Player aiPlayer = gameWorld.getPlayer(aiPlayerId);
        if (aiPlayer == null) return;

        System.out.println("🤖 AI копит ресурсы: Золото=" + aiPlayer.getResource(ResourceType.GOLD) +
                ", Дерево=" + aiPlayer.getResource(ResourceType.WOOD) +
                ", Еда=" + aiPlayer.getResource(ResourceType.FOOD));

        // Атакуем только соседние нейтральные клетки
        List<Cell> myCells = getAICells(aiPlayerId);
        List<Cell> neutralTargets = findNeutralTargets(aiPlayerId, myCells);

        if (!neutralTargets.isEmpty()) {
            Cell target = neutralTargets.get(0);
            System.out.println("🤖 AI (экономический) атакует нейтральную клетку");
            gameWorld.executeInstantAttack(myCells, target, aiPlayerId);
        } else {
            System.out.println("🤖 AI (экономический) нет нейтральных клеток для атаки");
        }
    }

    // Сбалансированная тактика
    private void balancedMove(String aiPlayerId) {
        List<Cell> myCells = getAICells(aiPlayerId);
        if (myCells.isEmpty()) return;

        Player attacker = gameWorld.getPlayer(aiPlayerId);
        if (attacker == null) return;

        // Если есть выгодные цели - атакуем
        List<Cell> targets = findAttackTargets(aiPlayerId, myCells);

        for (Cell target : targets) {
            // Проверяем, что у нас больше войск, чем у защитника
            if (attacker.getTotalTroops() > target.getDefenseBonus() + 10) {
                System.out.println("🤖 AI (сбалансированный) атакует выгодную цель");
                gameWorld.executeInstantAttack(myCells, target, aiPlayerId);
                return;
            }
        }

        // Иначе атакуем нейтральные клетки
        List<Cell> neutralTargets = findNeutralTargets(aiPlayerId, myCells);
        if (!neutralTargets.isEmpty()) {
            Cell target = neutralTargets.get(0);
            System.out.println("🤖 AI (сбалансированный) атакует нейтральную клетку");
            gameWorld.executeInstantAttack(myCells, target, aiPlayerId);
        } else {
            System.out.println("🤖 AI (сбалансированный) нет целей для атаки");
        }
    }

    // Случайная тактика
    private void randomMove(String aiPlayerId) {
        List<Cell> myCells = getAICells(aiPlayerId);
        if (myCells.isEmpty()) return;

        int action = random.nextInt(3);

        switch (action) {
            case 0: // Атака врага
                List<Cell> targets = findAttackTargets(aiPlayerId, myCells);
                if (!targets.isEmpty()) {
                    Cell target = targets.get(random.nextInt(targets.size()));
                    System.out.println("🤖 AI (случайный) атакует вражескую клетку");
                    gameWorld.executeInstantAttack(myCells, target, aiPlayerId);
                } else {
                    System.out.println("🤖 AI (случайный) нет вражеских целей для атаки");
                }
                break;
            case 1: // Атака нейтральной клетки
                List<Cell> neutralTargets = findNeutralTargets(aiPlayerId, myCells);
                if (!neutralTargets.isEmpty()) {
                    Cell target = neutralTargets.get(random.nextInt(neutralTargets.size()));
                    System.out.println("🤖 AI (случайный) атакует нейтральную клетку");
                    gameWorld.executeInstantAttack(myCells, target, aiPlayerId);
                }
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

    private String getPlayerName(String playerId) {
        Player player = gameWorld.getPlayer(playerId);
        return player != null ? player.getName() : "Unknown";
    }
}
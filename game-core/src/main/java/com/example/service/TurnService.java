package com.example.service;

import com.example.model.Player;
import com.example.model.Cell;
import com.example.model.ResourceType;
import com.example.world.GameWorld;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TurnService {
    private final GameWorld gameWorld;
    private List<Player> players;
    private int currentPlayerIndex = 0;
    private int turnNumber = 1;
    private GameState state = GameState.WAITING;
    private Player winner = null;
    private boolean capturedThisTurn = false;

    public enum GameState {
        WAITING, PROCESSING, FINISHED
    }

    public TurnService(GameWorld gameWorld) {
        this.gameWorld = gameWorld;
        this.gameWorld.setTurnService(this);
    }

    public void initialize(List<Player> players) {
        this.players = new ArrayList<>(players);
        this.currentPlayerIndex = 0;
        this.turnNumber = 1;
        this.state = GameState.WAITING;
        this.capturedThisTurn = false;

        System.out.println("\n🎮 Игра началась! Первый ход: " + getCurrentPlayer().getName());
        System.out.println("Начальный статус: " + state);
        System.out.println("Начальный игрок ID: " + getCurrentPlayer().getId());
        System.out.println("Начальный игрок клетки: " + getCurrentPlayer().getCapturedCells().size());
        System.out.println("Начальное количество войск: " + getCurrentPlayer().getTotalTroops());
    }

    public void initializeFromSnapshot(List<Player> players, String currentPlayerId, int turnNumber, GameState gameState) {
        this.players = new ArrayList<>(players);
        this.turnNumber = turnNumber;
        this.state = gameState;
        this.capturedThisTurn = false;

        for (int i = 0; i < this.players.size(); i++) {
            if (this.players.get(i).getId().equals(currentPlayerId)) {
                this.currentPlayerIndex = i;
                break;
            }
        }

        System.out.println("\n🎮 Игра загружена! Ход " + turnNumber + " - " + getCurrentPlayer().getName());
    }

    public synchronized boolean startTurn(String playerId) {
        System.out.println("\n========== START TURN DEBUG ==========");
        System.out.println("Вызван startTurn для playerId: " + playerId);
        System.out.println("Текущий статус state: " + state);
        System.out.println("Текущий игрок: " + (getCurrentPlayer() != null ? getCurrentPlayer().getName() : "null"));
        System.out.println("Текущий игрок ID: " + (getCurrentPlayer() != null ? getCurrentPlayer().getId() : "null"));

        if (state != GameState.WAITING) {
            System.out.println("❌ Ошибка: state != WAITING (текущий: " + state + ")");
            return false;
        }

        Player current = getCurrentPlayer();
        if (current == null) {
            System.out.println("❌ Ошибка: current player is null");
            return false;
        }

        if (!current.getId().equals(playerId)) {
            System.out.println("❌ Ошибка: ID не совпадают. Ожидаемый: " + current.getId() + ", Получен: " + playerId);
            return false;
        }

        System.out.println("✅ Все проверки пройдены, меняем статус с WAITING на PROCESSING");
        state = GameState.PROCESSING;
        capturedThisTurn = false;
        System.out.println("Новый статус: " + state);

        collectResourcesForPlayer(current);
        recruitTroopsForPlayer(current);

        System.out.println("\n📢 ===== ХОД " + turnNumber + " - " + current.getName() + " =====");
        System.out.println("Клеток у игрока: " + current.getCapturedCells().size());
        System.out.println("Золото: " + current.getResource(ResourceType.GOLD));
        System.out.println("Войска: " + current.getTotalTroops());
        System.out.println("========== START TURN END ==========\n");
        return true;
    }

    private void collectResourcesForPlayer(Player player) {
        int goldIncome = 0, woodIncome = 0, foodIncome = 0;
        int troopBonus = 0;

        System.out.println("Сбор ресурсов для игрока: " + player.getName());
        System.out.println("Клеток для сбора: " + player.getCapturedCells().size());

        for (String cellId : player.getCapturedCellIds()) {
            Cell cell = gameWorld.getCell(cellId);
            if (cell != null && !cell.isWater()) {
                int production = cell.getCurrentProduction();

                // Базовое производство от местности
                switch (cell.getTerrain()) {
                    case CITY:
                        goldIncome += production;
                        System.out.println("  Город [" + cell.getX() + "," + cell.getY() + "]: +" + production + " золота");
                        break;
                    case FOREST:
                        woodIncome += production;
                        System.out.println("  Лес [" + cell.getX() + "," + cell.getY() + "]: +" + production + " дерева");
                        break;
                    case PLAIN:
                        foodIncome += production;
                        System.out.println("  Равнина [" + cell.getX() + "," + cell.getY() + "]: +" + production + " еды");
                        break;
                    default:
                        goldIncome += production / 2;
                        System.out.println("  " + cell.getTerrain() + " [" + cell.getX() + "," + cell.getY() + "]: +" + (production/2) + " золота");
                }

                // Бонус от зданий
                if (cell.hasBuilding()) {
                    goldIncome += cell.getGoldBonus();
                    woodIncome += cell.getWoodBonus();
                    foodIncome += cell.getFoodBonus();
                    troopBonus += cell.getTroopBonus();
                    System.out.println("    🏗️ " + cell.getBuilding().getDisplayName() +
                            " дает: золото +" + cell.getGoldBonus() +
                            ", дерево +" + cell.getWoodBonus() +
                            ", еда +" + cell.getFoodBonus());
                }
            }
        }

        goldIncome += player.getTotalCells() * 10;
        System.out.println("Бонус за клетки: +" + (player.getTotalCells() * 10) + " золота");

        if (goldIncome > 0) player.addResource(ResourceType.GOLD, goldIncome);
        if (woodIncome > 0) player.addResource(ResourceType.WOOD, woodIncome);
        if (foodIncome > 0) player.addResource(ResourceType.FOOD, foodIncome);

        // Бонусные войска от казарм
        if (troopBonus > 0) {
            player.addTroops(troopBonus);
            System.out.printf("⚔️ Бонус от казарм: +%d войск!%n", troopBonus);
        }

        System.out.printf("💰 %s получил ресурсы: золото +%d, дерево +%d, еда +%d%n",
                player.getName(), goldIncome, woodIncome, foodIncome);
    }

    private void recruitTroopsForPlayer(Player player) {
        int gold = player.getResource(ResourceType.GOLD);
        int food = player.getResource(ResourceType.FOOD);

        int maxRecruits = Math.min(gold / 20, food / 10);
        maxRecruits = Math.min(maxRecruits, 20);

        System.out.println("Найм войск для " + player.getName() + ":");
        System.out.println("  Золото: " + gold + ", Еда: " + food);
        System.out.println("  Макс. найм: " + maxRecruits);

        if (maxRecruits > 0) {
            player.spendResource(ResourceType.GOLD, maxRecruits * 20);
            player.spendResource(ResourceType.FOOD, maxRecruits * 10);
            player.addTroops(maxRecruits);
            System.out.printf("⚔️ %s нанял %d новых войск! Всего войск: %d%n",
                    player.getName(), maxRecruits, player.getTotalTroops());
        } else {
            System.out.println("  Недостаточно ресурсов для найма");
        }
    }

    public synchronized boolean endTurn(String playerId) {
        System.out.println("\n========== END TURN DEBUG ==========");
        System.out.println("Вызван endTurn для playerId: " + playerId);
        System.out.println("Текущий статус state: " + state);

        if (state == GameState.FINISHED) {
            System.out.println("✅ Игра уже завершена");
            return true;
        }

        if (state != GameState.PROCESSING) {
            System.out.println("❌ Ошибка: state != PROCESSING (текущий: " + state + ")");
            return false;
        }

        Player current = getCurrentPlayer();
        if (current == null) {
            System.out.println("❌ Ошибка: current player is null");
            return false;
        }

        if (!current.getId().equals(playerId)) {
            System.out.println("❌ Ошибка: ID не совпадают. Ожидаемый: " + current.getId() + ", Получен: " + playerId);
            return false;
        }

        System.out.println("✅ Завершаем ход игрока: " + current.getName());

        // Переход хода к следующему игроку
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        turnNumber++;

        state = GameState.WAITING;
        capturedThisTurn = false;

        // Проверяем, есть ли у кого-то 0 клеток
        checkAndEliminatePlayers();

        System.out.println("✅ Ход завершен. Новый статус: " + state);
        System.out.println("Следующий игрок: " + getCurrentPlayer().getName());
        System.out.println("Номер хода: " + turnNumber);
        System.out.println("========== END TURN END ==========\n");
        return true;
    }

    private void checkAndEliminatePlayers() {
        List<Player> playersToRemove = new ArrayList<>();

        // Находим игроков без клеток
        for (Player player : players) {
            if (player.getCapturedCells().isEmpty()) {
                playersToRemove.add(player);
                System.out.println("💀 " + player.getName() + " уничтожен! (нет клеток)");
            }
        }

        // Удаляем уничтоженных игроков
        players.removeAll(playersToRemove);

        // Если остался только один игрок - объявляем победителя
        if (players.size() == 1 && winner == null) {
            winner = players.get(0);
            state = GameState.FINISHED;
            System.out.println("\n🏆🏆🏆 ПОБЕДИТЕЛЬ: " + winner.getName() + "! 🏆🏆🏆");
        } else if (players.isEmpty()) {
            state = GameState.FINISHED;
            System.out.println("\n🏆 ИГРА ЗАВЕРШЕНА! Нет живых игроков.");
        }

        // Корректируем индекс текущего игрока если нужно
        if (currentPlayerIndex >= players.size() && players.size() > 0) {
            currentPlayerIndex = 0;
        }
    }

    public void checkAndSetWinner() {
        if (state == GameState.FINISHED) return;

        List<Player> alivePlayers = new ArrayList<>();
        for (Player player : players) {
            if (!player.getCapturedCells().isEmpty()) {
                alivePlayers.add(player);
            }
        }

        if (alivePlayers.size() == 1) {
            winner = alivePlayers.get(0);
            state = GameState.FINISHED;
            System.out.println("\n🏆 ПОБЕДИТЕЛЬ: " + winner.getName() + "! 🏆");
        } else if (alivePlayers.isEmpty()) {
            state = GameState.FINISHED;
            System.out.println("\n🏆 ИГРА ЗАВЕРШЕНА! Нет живых игроков.");
        }
    }

    public void removeDefeatedPlayer(Player defeatedPlayer) {
        if (players == null) return;
        players.remove(defeatedPlayer);
        if (currentPlayerIndex >= players.size()) {
            currentPlayerIndex = 0;
        }
        checkAndSetWinner();
    }

    public boolean canCaptureThisTurn() {
        return state == GameState.PROCESSING && !capturedThisTurn;
    }

    public void registerCapture() {
        this.capturedThisTurn = true;
        System.out.println("📝 Захват клетки зарегистрирован. В этом ходу больше нельзя атаковать.");
    }

    public boolean hasCapturedThisTurn() {
        return capturedThisTurn;
    }

    public boolean canEndTurn(String playerId) {
        Player current = getCurrentPlayer();
        boolean result = current != null &&
                current.getId().equals(playerId) &&
                state == GameState.PROCESSING;
        System.out.println("canEndTurn check: playerId=" + playerId +
                ", currentPlayer=" + (current != null ? current.getName() : "null") +
                ", state=" + state +
                ", result=" + result);
        return result;
    }

    public Player getCurrentPlayer() {
        if (players == null || players.isEmpty()) return null;
        return players.get(currentPlayerIndex);
    }

    public int getTurnNumber() { return turnNumber; }
    public GameState getState() { return state; }

    public boolean isMyTurn(String playerId) {
        Player current = getCurrentPlayer();
        boolean result = current != null && current.getId().equals(playerId) && state == GameState.WAITING;
        System.out.println("isMyTurn check: playerId=" + playerId +
                ", currentPlayer=" + (current != null ? current.getName() : "null") +
                ", state=" + state + ", result=" + result);
        return result;
    }

    public boolean canAttack(String playerId) {
        Player current = getCurrentPlayer();
        Player attacker = gameWorld.getPlayer(playerId);

        boolean result = current != null &&
                current.getId().equals(playerId) &&
                state == GameState.PROCESSING &&
                !capturedThisTurn;

        System.out.println("canAttack check: playerId=" + playerId +
                ", currentPlayer=" + (current != null ? current.getName() : "null") +
                ", state=" + state +
                ", capturedThisTurn=" + capturedThisTurn +
                ", result=" + result);
        return result;
    }

    public Player getWinner() { return winner; }

    public void setWinner(Player winner) {
        this.winner = winner;
        this.state = GameState.FINISHED;
        System.out.println("\n🏆 ПОБЕДИТЕЛЬ: " + winner.getName() + "! 🏆");
    }

    public List<Player> getPlayers() {
        return players != null ? Collections.unmodifiableList(players) : Collections.emptyList();
    }

    public void updatePlayers(List<Player> newPlayers) {
        this.players = new ArrayList<>(newPlayers);
        System.out.println("📋 Список игроков обновлен: " + players.size() + " игроков");
    }

    public boolean isGameFinished() {
        return state == GameState.FINISHED;
    }

    public void reset() {
        this.players = null;
        this.currentPlayerIndex = 0;
        this.turnNumber = 1;
        this.state = GameState.WAITING;
        this.winner = null;
        this.capturedThisTurn = false;
        System.out.println("🔄 TurnService сброшен");
    }
}
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
        System.out.println("\n🎮 Игра началась! Первый ход: " + getCurrentPlayer().getName());
        System.out.println("Начальный статус: " + state);
        System.out.println("Начальный игрок ID: " + getCurrentPlayer().getId());
    }

    public void initializeFromSnapshot(List<Player> players, String currentPlayerId, int turnNumber, GameState gameState) {
        this.players = new ArrayList<>(players);
        this.turnNumber = turnNumber;
        this.state = gameState;

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
        System.out.println("Новый статус: " + state);

        collectResourcesForPlayer(current);
        recruitTroopsForPlayer(current);

        System.out.println("\n📢 ===== ХОД " + turnNumber + " - " + current.getName() + " =====");
        System.out.println("========== START TURN END ==========\n");
        return true;
    }

    private void collectResourcesForPlayer(Player player) {
        int goldIncome = 0, woodIncome = 0, foodIncome = 0;
        for (String cellId : player.getCapturedCellIds(player.getcapturedCells())) {
            Cell cell = gameWorld.getCell(cellId);
            if (cell != null && !cell.isWater()) {
                int production = cell.getCurrentProduction();
                switch (cell.getTerrain()) {
                    case CITY: goldIncome += production; break;
                    case FOREST: woodIncome += production; break;
                    case PLAIN: foodIncome += production; break;
                    default: goldIncome += production / 2;
                }
            }
        }

        goldIncome += player.getTotalCells() * 10;

        if (goldIncome > 0) player.addResource(ResourceType.GOLD, goldIncome);
        if (woodIncome > 0) player.addResource(ResourceType.WOOD, woodIncome);
        if (foodIncome > 0) player.addResource(ResourceType.FOOD, foodIncome);

        System.out.printf("💰 %s получил ресурсы: золото +%d, дерево +%d, еда +%d%n",
                player.getName(), goldIncome, woodIncome, foodIncome);
    }

    private void recruitTroopsForPlayer(Player player) {
        int gold = player.getResource(ResourceType.GOLD);
        int food = player.getResource(ResourceType.FOOD);
        int maxRecruits = Math.min(gold / 20, food / 10);
        maxRecruits = Math.min(maxRecruits, 20);

        if (maxRecruits > 0) {
            player.spendResource(ResourceType.GOLD, maxRecruits * 20);
            player.spendResource(ResourceType.FOOD, maxRecruits * 10);
            player.addTroops(maxRecruits);

            List<Cell> playerCells = new ArrayList<>();
            for (String cellId : player.getCapturedCellIds(player.getcapturedCells())) {
                Cell cell = gameWorld.getCell(cellId);
                if (cell != null && !cell.isWater()) {
                    playerCells.add(cell);
                }
            }
            if (!playerCells.isEmpty()) {
                int perCell = maxRecruits / playerCells.size();
                for (Cell cell : playerCells) {
                    cell.setTroopsCount(cell.getTroopsCount() + perCell);
                }
            }
            System.out.printf("⚔️ %s нанял %d новых войск!%n", player.getName(), maxRecruits);
        }
    }

    public synchronized boolean endTurn(String playerId) {
        System.out.println("\n========== END TURN DEBUG ==========");
        System.out.println("Вызван endTurn для playerId: " + playerId);
        System.out.println("Текущий статус state: " + state);

        if (state != GameState.PROCESSING) {
            System.out.println("❌ Ошибка: state != PROCESSING (текущий: " + state + ")");
            return false;
        }

        Player current = getCurrentPlayer();
        if (!current.getId().equals(playerId)) {
            System.out.println("❌ Ошибка: ID не совпадают");
            return false;
        }

        if (current.getcapturedCells().isEmpty()) {
            System.out.println("💀 " + current.getName() + " уничтожен!");
            players.remove(currentPlayerIndex);
            if (players.size() == 1) {
                winner = players.get(0);
                state = GameState.FINISHED;
                System.out.println("\n🏆 ПОБЕДИТЕЛЬ: " + winner.getName() + "! 🏆");
                return true;
            }
            if (currentPlayerIndex >= players.size()) currentPlayerIndex = 0;
        } else {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            turnNumber++;
        }

        state = GameState.WAITING;
        System.out.println("✅ Ход завершен. Новый статус: " + state);
        System.out.println("Следующий игрок: " + getCurrentPlayer().getName());
        System.out.println("========== END TURN END ==========\n");
        return true;
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
        boolean result = current != null &&
                current.getId().equals(playerId) &&
                state == GameState.PROCESSING;
        System.out.println("canAttack check: playerId=" + playerId +
                ", currentPlayer=" + (current != null ? current.getName() : "null") +
                ", state=" + state + ", result=" + result);
        return result;
    }

    public Player getWinner() { return winner; }

    public void setWinner(Player winner) {
        this.winner = winner;
        this.state = GameState.FINISHED;
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
}
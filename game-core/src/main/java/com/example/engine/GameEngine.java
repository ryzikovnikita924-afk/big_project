package com.example.engine;

import com.example.model.*;

import java.util.List;

public class GameEngine {

    private static final int ATTACK_DURATION_MS = 5000;
    private static final int PRODUCTION_INTERVAL_MS = 10000;

    public boolean areNeighbors(Cell a, Cell b) {
        if (a == null || b == null) return false;

        int dx = Math.abs(a.getX() - b.getX());
        int dy = Math.abs(a.getY() - b.getY());

        // Соседними считаются клетки по горизонтали, вертикали и диагонали
        if (dx == 0 && dy == 1) return true;
        if (dx == 1 && dy == 0) return true;
        if (dx == 1 && dy == 1) return true;

        return false;
    }

    public boolean canAttack(List<Cell> fromCells, Cell targetCell) {
        if (fromCells == null || fromCells.isEmpty() || targetCell == null) return false;

        // Нельзя атаковать воду
        if (targetCell.isWater()) return false;

        // Нельзя атаковать свои клетки
        String ownerId = fromCells.get(0).getOwnerId();
        if (ownerId != null && ownerId.equals(targetCell.getOwnerId())) return false;

        // Проверяем, есть ли среди атакующих клеток хотя бы одна, соседняя с целью
        for (Cell fromCell : fromCells) {
            if (areNeighbors(fromCell, targetCell)) {
                return true;
            }
        }

        return false;
    }

    public int calculateResourceProduction(Cell cell) {
        if (cell.isNeutral() || cell.isWater()) return 0;

        int baseProduction = 10;
        switch (cell.getTerrain()) {
            case CITY: baseProduction = 30; break;
            case FOREST: baseProduction = 15; break;
            case PLAIN: baseProduction = 10; break;
            case MOUNTAIN: baseProduction = 5; break;
        }

        return baseProduction * cell.getLevel();
    }

    public int getAttackDuration() {
        return ATTACK_DURATION_MS;
    }

    public int getProductionInterval() {
        return PRODUCTION_INTERVAL_MS;
    }
}
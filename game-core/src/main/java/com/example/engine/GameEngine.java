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

        if (dx == 0 && dy == 1) return true;
        if (dx == 1 && dy == 0) return true;
        if (dx == 1 && dy == 1) return true;

        return false;
    }


    public boolean canAttack(List<Cell> from, Cell to) {
        if (from == null || to == null) return false;
        if (to.isWater()) return false;
        Cell cell= from.get(0);
        if (cell.getOwnerId().equals(to.getOwnerId())) return false;


        return true;
    }


    public BattleResult resolveBattle(Player attacker, Cell defender) {
        int attackerPower = attacker.getTotalTroops();
        int defenderPower = defender.getDefenseBonus();

        System.out.printf("Битва: %d vs %d (бонус защиты: %.1f)%n",
                attackerPower, defenderPower, defender.getTerrain().getDefenseBonus());

        if (attackerPower > defenderPower) {

            int remainingTroops = attackerPower - defenderPower;
            int defenderLosses = defender.getTroopsCount();

            return new BattleResult(true, remainingTroops, defender, defenderLosses);
        } else {

            int defenderLosses = defender.getTroopsCount();
            int remainingDefender = defenderPower - attackerPower;
            defender.setTroopsCount(remainingDefender);

            return new BattleResult(false, 0, defender, attackerPower);
        }
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
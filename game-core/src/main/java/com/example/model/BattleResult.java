package com.example.model;

public class BattleResult {
    private final boolean attackerWon;
    private final int remainingTroops;
    private final Cell defenderCell;
    private final int defenderLosses;

    public BattleResult(boolean attackerWon, int remainingTroops,
                        Cell defenderCell, int defenderLosses) {
        this.attackerWon = attackerWon;
        this.remainingTroops = remainingTroops;
        this.defenderCell = defenderCell;
        this.defenderLosses = defenderLosses;
    }

    public boolean isAttackerWon() { return attackerWon; }
    public int getRemainingTroops() { return remainingTroops; }
    public Cell getDefenderCell() { return defenderCell; }
    public int getDefenderLosses() { return defenderLosses; }

    @Override
    public String toString() {
        return String.format("BattleResult{winner=%s, remaining=%d}",
                attackerWon ? "attacker" : "defender", remainingTroops);
    }
}
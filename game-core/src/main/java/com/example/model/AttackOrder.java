package com.example.model;

public class AttackOrder implements Comparable<AttackOrder> {
    private final String id;
    private final String fromCellId;
    private final String toCellId;
    private final int troopsCount;
    private final String attackerId;
    private long startedAt;
    private long completesAt;

    public AttackOrder(String fromCellId, String toCellId,
                       int troopsCount, String attackerId) {
        this.id = java.util.UUID.randomUUID().toString();
        this.fromCellId = fromCellId;
        this.toCellId = toCellId;
        this.troopsCount = troopsCount;
        this.attackerId = attackerId;
    }


    public String getId() { return id; }
    public String getFromCellId() { return fromCellId; }
    public String getToCellId() { return toCellId; }
    public int getTroopsCount() { return troopsCount; }
    public String getAttackerId() { return attackerId; }
    public long getStartedAt() { return startedAt; }
    public void setStartedAt(long startedAt) { this.startedAt = startedAt; }
    public long getCompletesAt() { return completesAt; }
    public void setCompletesAt(long completesAt) { this.completesAt = completesAt; }

    @Override
    public int compareTo(AttackOrder o) {
        return Long.compare(this.completesAt, o.completesAt);
    }
}

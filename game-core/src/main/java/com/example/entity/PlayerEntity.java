package com.example.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "players")
public class PlayerEntity {

    @Id
    private String id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "auth_id", unique = true)
    private String authId;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "last_played")
    private LocalDateTime lastPlayed;

    @Column(name = "total_games")
    private int totalGames;

    @Column(name = "total_wins")
    private int totalWins;

    @Column(name = "total_cells_captured")
    private int totalCellsCaptured;

    @Column(name = "total_troops_killed")
    private int totalTroopsKilled;

    @Column(name = "total_score")
    private int totalScore;

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GameHistoryEntity> gameHistory = new ArrayList<>();

    @Transient
    private boolean isOnline;

    public PlayerEntity() {}

    public PlayerEntity(String id, String name) {
        this.id = id;
        this.name = name;
        this.createdAt = LocalDateTime.now();
        this.lastLogin = LocalDateTime.now();
        this.lastPlayed = LocalDateTime.now();
        this.totalGames = 0;
        this.totalWins = 0;
        this.totalCellsCaptured = 0;
        this.totalTroopsKilled = 0;
        this.totalScore = 0;
    }

    // Конструктор с email
    public PlayerEntity(String id, String name, String email) {
        this(id, name);
        this.email = email;
    }

    // Геттеры и сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAuthId() { return authId; }
    public void setAuthId(String authId) { this.authId = authId; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

    public LocalDateTime getLastPlayed() { return lastPlayed; }
    public void setLastPlayed(LocalDateTime lastPlayed) { this.lastPlayed = lastPlayed; }

    public int getTotalGames() { return totalGames; }
    public void setTotalGames(int totalGames) { this.totalGames = totalGames; }

    public int getTotalWins() { return totalWins; }
    public void setTotalWins(int totalWins) { this.totalWins = totalWins; }

    public int getTotalCellsCaptured() { return totalCellsCaptured; }
    public void setTotalCellsCaptured(int totalCellsCaptured) { this.totalCellsCaptured = totalCellsCaptured; }

    public int getTotalTroopsKilled() { return totalTroopsKilled; }
    public void setTotalTroopsKilled(int totalTroopsKilled) { this.totalTroopsKilled = totalTroopsKilled; }

    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int totalScore) { this.totalScore = totalScore; }

    public List<GameHistoryEntity> getGameHistory() { return gameHistory; }
    public void setGameHistory(List<GameHistoryEntity> gameHistory) { this.gameHistory = gameHistory; }

    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }

    // Вспомогательные методы
    public void incrementGames() {
        this.totalGames++;
    }

    public void incrementWins() {
        this.totalWins++;
    }

    public void addCellsCaptured(int cells) {
        this.totalCellsCaptured += cells;
    }

    public void addTroopsKilled(int troops) {
        this.totalTroopsKilled += troops;
    }

    public void addScore(int score) {
        this.totalScore += score;
    }

    public void updateLastPlayed() {
        this.lastPlayed = LocalDateTime.now();
    }

    public void updateLastLogin() {
        this.lastLogin = LocalDateTime.now();
    }

    // Для отображения в JSON
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("email", email);
        map.put("avatarUrl", avatarUrl);
        map.put("totalGames", totalGames);
        map.put("totalWins", totalWins);
        map.put("totalScore", totalScore);
        map.put("isOnline", isOnline);
        return map;
    }

    @Override
    public String toString() {
        return String.format("PlayerEntity{id='%s', name='%s', email='%s', wins=%d, games=%d}",
                id, name, email, totalWins, totalGames);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerEntity that = (PlayerEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
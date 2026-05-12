package com.example.repository;

import com.example.entity.GameHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GameHistoryRepository extends JpaRepository<GameHistoryEntity, String> {
    List<GameHistoryEntity> findByPlayerId(String playerId);
    List<GameHistoryEntity> findByGameId(String gameId);
    long countByPlayerIdAndIsWinnerTrue(String playerId);
}
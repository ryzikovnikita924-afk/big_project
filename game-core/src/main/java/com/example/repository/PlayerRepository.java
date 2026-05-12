package com.example.repository;

import com.example.entity.PlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<PlayerEntity, String> {
    Optional<PlayerEntity> findByName(String name);
    Optional<PlayerEntity> findByEmail(String email);
    Optional<PlayerEntity> findByAuthId(String authId);
    boolean existsByName(String name);
    boolean existsByEmail(String email);
}
package com.example.service;

import com.example.entity.PlayerEntity;
import com.example.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PlayerSyncService {

    private final PlayerRepository repo;
    private final CurrentUserService current;

    public PlayerSyncService(PlayerRepository repo, CurrentUserService current) {
        this.repo = repo;
        this.current = current;
    }

    @Transactional
    public PlayerEntity sync() {

        String authId = current.getAuthId();
        String email = current.getEmail();
        String username = current.getUsername();

        if (authId == null)
            throw new RuntimeException("User is not authenticated");

        PlayerEntity p = repo.findByAuthId(authId).orElse(null);

        if (p == null) {
            p = new PlayerEntity(UUID.randomUUID().toString(), username);
            p.setAuthId(authId);
            p.setEmail(email);
            p.setCreatedAt(LocalDateTime.now());
        }

        p.setLastLogin(LocalDateTime.now());
        return repo.save(p);
    }
}
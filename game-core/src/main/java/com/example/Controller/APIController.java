package com.example.Controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
public class APIController {


    @GetMapping("/api/game/map")
    public ResponseEntity<Map<String, Object>> getMap() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Game map endpoint");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
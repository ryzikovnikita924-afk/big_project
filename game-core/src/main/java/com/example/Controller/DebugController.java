package com.example.Controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DebugController {

    @GetMapping("/login/oauth2/code/keycloak")
    public ResponseEntity<String> debugCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpServletRequest request) {

        System.out.println("=== CALLBACK RECEIVED! ===");
        System.out.println("Code: " + code);
        System.out.println("State: " + state);
        System.out.println("Error: " + error);
        System.out.println("Full URL: " + request.getRequestURL() + "?" + request.getQueryString());

        if (code != null) {
            return ResponseEntity.ok("Callback received with code: " + code);
        }
        return ResponseEntity.badRequest().body("No code received");
    }
}
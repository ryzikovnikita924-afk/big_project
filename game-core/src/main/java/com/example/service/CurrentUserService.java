package com.example.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class CurrentUserService {

    private HttpServletRequest request() {
        return ((ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes()).getRequest();
    }

    public String getAuthId() {
        // Пробуем разные возможные заголовки от oauth2-proxy
        String user = request().getHeader("X-Forwarded-User");
        if (user != null && !user.isEmpty()) return user;

        String email = request().getHeader("X-Forwarded-Email");
        if (email != null && !email.isEmpty()) return email;

        String preferredUsername = request().getHeader("X-Forwarded-Preferred-Username");
        if (preferredUsername != null && !preferredUsername.isEmpty()) return preferredUsername;

        return null;
    }

    public String getEmail() {
        String email = request().getHeader("X-Forwarded-Email");
        if (email != null && !email.isEmpty()) return email;
        return null;
    }

    public String getUsername() {
        String username = request().getHeader("X-Forwarded-Preferred-Username");
        if (username != null && !username.isEmpty()) return username;

        String user = request().getHeader("X-Forwarded-User");
        if (user != null && !user.isEmpty()) return user;

        return null;
    }

    public String getFirstName() {
        return request().getHeader("X-Auth-Request-Given-Name");
    }

    public String getLastName() {
        return request().getHeader("X-Auth-Request-Family-Name");
    }
}
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
        return request().getHeader("X-Auth-Request-User");
    }

    public String getEmail() {
        return request().getHeader("X-Auth-Request-Email");
    }

    public String getUsername() {
        return request().getHeader("X-Auth-Request-Preferred-Username");
    }

    public String getFirstName() {
        return request().getHeader("X-Auth-Request-Given-Name");
    }

    public String getLastName() {
        return request().getHeader("X-Auth-Request-Family-Name");
    }
}
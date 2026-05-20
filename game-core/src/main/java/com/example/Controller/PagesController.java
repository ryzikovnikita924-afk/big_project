package com.example.Controller;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class PagesController implements WebMvcConfigurer {

    @Override
    public void addViewControllers(@NonNull ViewControllerRegistry registry) {
        // Отдаем статическую страницу логина
        registry.addViewController("/login").setViewName("login");
        // Если нужно перенаправление на HTML страницу
        registry.addRedirectViewController("/login-page", "/login.html");
    }
}
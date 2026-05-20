package com.example;

import com.example.config.properties.CorsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(CorsProperties.class)
@EnableScheduling
public class GameApplication {
    public static void main(String[] args) {
        SpringApplication.run(GameApplication.class, args);

    }
}
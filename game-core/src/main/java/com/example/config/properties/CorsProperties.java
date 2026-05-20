package com.example.config.properties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Data
@Component
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {

    // Список origins выносим в properties, чтобы не зашивать окружение в код
    private List<String> allowedOrigins = Collections.emptyList();
}
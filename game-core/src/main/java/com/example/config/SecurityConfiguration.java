package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        // Отключаем CSRF только для OAuth2 callback и статических страниц
                        .ignoringRequestMatchers(request -> {
                            String uri = request.getRequestURI();
                            return uri.equals("/login.html") ||
                                    uri.startsWith("/oauth2/authorization/") ||
                                    uri.startsWith("/login/oauth2/code/") ||
                                    request.getMethod().equals(HttpMethod.OPTIONS.name());
                        })
                )
                .authorizeHttpRequests(authorize -> authorize
                        // Статические ресурсы - публичные
                        .requestMatchers("/", "/index.html", "/login.html", "/error").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()

                        // OAuth2 эндпоинты
                        .requestMatchers("/oauth2/authorization/**", "/login/oauth2/code/**").permitAll()

                        // API эндпоинты - требуют аутентификации
                        .requestMatchers("/api/auth/me").authenticated()
                        .requestMatchers("/api/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Все остальное требует аутентификации
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login.html")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login.html?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessUrl("/login.html?logout=true")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        // Добавляем logout handler для очистки OAuth2
                        .addLogoutHandler((request, response, authentication) -> {
                            if (authentication != null) {
                                // Очистка OAuth2 токенов
                                request.getSession().removeAttribute("oauth2AuthorizationRequest");
                            }
                        })
                );

        return http.build();
    }
}
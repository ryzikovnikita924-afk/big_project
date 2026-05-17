package com.example.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;

@Configuration
@EnableRedisIndexedHttpSession(redisNamespace = "jbcd:7a:sso-jwt-to-session-auth")
public class SessionConfiguration {
}

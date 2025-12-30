package com.splitwise.sim.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtSecretChecker {

    private static final Logger log = LoggerFactory.getLogger(JwtSecretChecker.class);

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            log.warn("JWT secret is not set (empty). Please configure JWT_SECRET environment variable in your deployment.");
        } else {
            log.info("JWT secret present, length={}", jwtSecret.length());
        }
    }
}

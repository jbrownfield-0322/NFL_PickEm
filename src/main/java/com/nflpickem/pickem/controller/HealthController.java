package com.nflpickem.pickem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {
    
    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;
    
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", Instant.now().toString());
        health.put("service", "NFL Pickem API");
        health.put("version", "1.0.0");
        health.put("port", System.getenv("PORT"));
        health.put("environment", System.getenv("SPRING_PROFILES_ACTIVE"));
        return ResponseEntity.ok(health);
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", Instant.now().toString());
        health.put("service", "NFL Pickem API");
        health.put("version", "1.0.0");
        health.put("port", System.getenv("PORT"));
        health.put("environment", System.getenv("SPRING_PROFILES_ACTIVE"));
        
        // Check database connectivity
        try {
            if (jdbcTemplate != null) {
                jdbcTemplate.queryForObject("SELECT 1", Integer.class);
                health.put("database", "Connected");
            } else {
                health.put("database", "Not configured");
            }
        } catch (Exception e) {
            health.put("database", "Error: " + e.getMessage());
            health.put("status", "DOWN");
        }
        
        // Check environment variables
        health.put("database_url_set", System.getenv("DATABASE_URL") != null);
        health.put("odds_api_key_set", System.getenv("THEODDS_API_KEY") != null);
        
        return ResponseEntity.ok(health);
    }
    
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }
}

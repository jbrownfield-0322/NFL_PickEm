package com.nflpickem.pickem.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {
    
    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", Instant.now().toString());
        health.put("service", "NFL Pickem API");
        health.put("version", "1.0.0");
        health.put("port", System.getenv("PORT"));
        health.put("environment", System.getenv("SPRING_PROFILES_ACTIVE"));
        health.put("message", "Service is running");
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
        health.put("database_url_set", System.getenv("DATABASE_URL") != null);
        health.put("odds_api_key_set", System.getenv("THEODDS_API_KEY") != null);
        health.put("message", "Detailed health check passed");
        return ResponseEntity.ok(health);
    }
    
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }
    
    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> readinessCheck() {
        Map<String, Object> readiness = new HashMap<>();
        readiness.put("status", "READY");
        readiness.put("timestamp", Instant.now().toString());
        readiness.put("message", "Application is ready to receive traffic");
        return ResponseEntity.ok(readiness);
    }
}

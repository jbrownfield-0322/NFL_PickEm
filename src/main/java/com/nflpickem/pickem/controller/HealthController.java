package com.nflpickem.pickem.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "NFL Pick'em Backend");
        response.put("timestamp", System.currentTimeMillis());
        response.put("version", "1.0.0");
        response.put("environment", System.getenv("SPRING_PROFILES_ACTIVE"));
        response.put("port", System.getenv("PORT"));
        response.put("database_url_set", System.getenv("DATABASE_URL") != null);
        System.out.println("Health check accessed at: " + System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/")
    public ResponseEntity<String> root() {
        try {
            // Try to serve the React app's index.html
            ClassPathResource resource = new ClassPathResource("static/index.html");
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            
            System.out.println("Serving React app at: " + System.currentTimeMillis());
            return new ResponseEntity<>(content, headers, HttpStatus.OK);
        } catch (IOException e) {
            // Fallback to API response if React app isn't built
            Map<String, Object> response = new HashMap<>();
            response.put("message", "NFL Pick'em Backend API");
            response.put("status", "running");
            response.put("health", "/health");
            response.put("actuator", "/actuator/health");
            response.put("ping", "/ping");
            response.put("timestamp", System.currentTimeMillis());
            response.put("port", System.getenv("PORT"));
            response.put("note", "Frontend not built - build React app and copy to static/ directory");
            
            System.out.println("Serving API fallback at: " + System.currentTimeMillis());
            return ResponseEntity.ok(response.toString());
        }
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        System.out.println("Ping endpoint accessed at: " + System.currentTimeMillis());
        return ResponseEntity.ok("pong");
    }
}

package com.nflpickem.pickem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .httpBasic(basic -> basic.disable()) // Disable HTTP Basic authentication
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/ping", "/health", "/actuator/health", "/actuator/info").permitAll() // Health checks always accessible
                .requestMatchers("/static/**", "/static/js/**", "/static/css/**", "/static/media/**").permitAll() // Static assets
                .requestMatchers("/register", "/login", "/games", "/leaderboard", "/leagues/**", "/my-leagues", "/account").permitAll() // React routes
                .requestMatchers("/api/auth/**", "/api/games/**", "/api/picks/**", "/api/leagues/**", "/api/leaderboard/**").permitAll()
                .requestMatchers("/api/admin/**").permitAll() // Allow admin endpoints for testing
                .anyRequest().authenticated()
            );
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        
        // Allow multiple origins for different environments
        List<String> allowedOrigins = Arrays.asList(
            "http://localhost:3000",  // Development
            "https://*.railway.app",  // Railway domains
            "https://*.up.railway.app" // Railway alternative domains
        );
        config.setAllowedOriginPatterns(allowedOrigins);
        
        config.setAllowedHeaders(Arrays.asList("Origin", "Content-Type", "Accept", "Authorization"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
} 
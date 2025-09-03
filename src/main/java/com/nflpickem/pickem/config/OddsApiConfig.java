package com.nflpickem.pickem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class OddsApiConfig implements CommandLineRunner {
    
    @Value("${theodds.api.key}")
    private String apiKey;
    
    @Value("${theodds.api.base-url:https://api.the-odds-api.com}")
    private String baseUrl;
    
    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== The Odds API Configuration ===");
        System.out.println("Base URL: " + baseUrl);
        
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("WARNING: THEODDS_API_KEY is not set!");
            System.err.println("Odds fetching will not work. Please set the environment variable.");
        } else if ("your_api_key_here".equals(apiKey)) {
            System.err.println("WARNING: THEODDS_API_KEY is set to placeholder value!");
            System.err.println("Please set a valid API key from https://the-odds-api.com/");
        } else {
            System.out.println("API Key: " + apiKey.substring(0, Math.min(8, apiKey.length())) + "... (valid)");
            System.out.println("Odds API is properly configured.");
        }
        System.out.println("=====================================");
    }
}

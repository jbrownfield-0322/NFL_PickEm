package com.nflpickem.pickem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {ReactiveSecurityAutoConfiguration.class})
@EnableScheduling
public class PickemApplication {

	public static void main(String[] args) {
		System.out.println("=== Starting NFL Pickem Application ===");
		System.out.println("Java version: " + System.getProperty("java.version"));
		System.out.println("Port: " + System.getenv("PORT"));
		System.out.println("Environment: " + System.getenv("SPRING_PROFILES_ACTIVE"));
		System.out.println("Database URL set: " + (System.getenv("DATABASE_URL") != null));
		
		SpringApplication.run(PickemApplication.class, args);
	}
	
	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		System.out.println("=== Application is READY ===");
		System.out.println("Health check available at: /");
		System.out.println("Detailed health at: /health");
		System.out.println("Readiness check at: /ready");
		System.out.println("Port: " + System.getenv("PORT"));
	}
}

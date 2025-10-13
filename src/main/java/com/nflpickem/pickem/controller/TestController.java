package com.nflpickem.pickem.controller;

import com.nflpickem.pickem.service.DiscordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class TestController {
    
    @Autowired
    private DiscordService discordService;
    
    @PostMapping("/discord")
    public ResponseEntity<String> testDiscord() {
        try {
            discordService.sendMilestoneAlert(100, "test-endpoint", 95);
            return ResponseEntity.ok("Discord test message sent!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to send Discord message: " + e.getMessage());
        }
    }
}

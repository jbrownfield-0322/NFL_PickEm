package com.nflpickem.pickem.controller;

import com.nflpickem.pickem.model.Game;
import com.nflpickem.pickem.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/games")
public class GameController {
    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping
    public ResponseEntity<List<Game>> getAllGames() {
        return ResponseEntity.ok(gameService.getAllGames());
    }

    @GetMapping("/week/{weekNum}")
    public ResponseEntity<List<Game>> getGamesByWeek(@PathVariable Integer weekNum) {
        return ResponseEntity.ok(gameService.getGamesByWeek(weekNum));
    }

    @GetMapping("/currentWeek")
    public ResponseEntity<Integer> getCurrentWeek() {
        return ResponseEntity.ok(gameService.getCurrentWeek());
    }
} 
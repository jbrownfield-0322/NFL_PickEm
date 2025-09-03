package com.nflpickem.pickem.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Data
public class BettingOdds {
    @Id
    @GeneratedValue(strategy =GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "game_id")
    private Game game;
    
    private String sportsbook;
    private Double homeTeamOdds;
    private Double awayTeamOdds;
    private Double spread;
    private String spreadTeam;
    private Double total;
    private Instant lastUpdated;
    private String oddsType; // "american", "decimal", "fractional"
    
    // Constructor
    public BettingOdds() {}
    
    public BettingOdds(Game game, String sportsbook, Double homeTeamOdds, Double awayTeamOdds, 
                       Double spread, String spreadTeam, Double total, String oddsType) {
        this.game = game;
        this.sportsbook = sportsbook;
        this.homeTeamOdds = homeTeamOdds;
        this.awayTeamOdds = awayTeamOdds;
        this.spread = spread;
        this.spreadTeam = spreadTeam;
        this.total = total;
        this.oddsType = oddsType;
        this.lastUpdated = Instant.now();
    }
}

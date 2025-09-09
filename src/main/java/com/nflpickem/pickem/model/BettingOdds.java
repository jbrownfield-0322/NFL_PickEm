package com.nflpickem.pickem.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"game_id", "sportsbook", "oddsType"})
})
@Data
public class BettingOdds {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;
    
    private String sportsbook;
    private Double spread;
    private String spreadTeam;
    private Double total;
    private Double homeTeamOdds;
    private Double awayTeamOdds;
    private String oddsType; // "american", "decimal", "fractional"
    private Instant lastUpdated;
    
    // Constructor
    public BettingOdds() {
        this.lastUpdated = Instant.now();
    }
    
    public BettingOdds(Game game, String sportsbook) {
        this();
        this.game = game;
        this.sportsbook = sportsbook;
    }
}

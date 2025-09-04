package com.nflpickem.pickem.dto;

import com.nflpickem.pickem.model.BettingOdds;
import com.nflpickem.pickem.model.Game;
import lombok.Data;

import java.util.List;

@Data
public class GameWithOddsDto {
    private Long id;
    private Integer week;
    private String homeTeam;
    private String awayTeam;
    private String kickoffTime;
    private String winningTeam;
    private Boolean scored;
    private List<BettingOdds> odds;
    private BettingOdds primaryOdds; // The main odds to display (FanDuel)

    public GameWithOddsDto(Game game, List<BettingOdds> odds) {
        this.id = game.getId();
        this.week = game.getWeek();
        this.homeTeam = game.getHomeTeam();
        this.awayTeam = game.getAwayTeam();
        this.kickoffTime = game.getKickoffTime() != null ? game.getKickoffTime().toString() : null;
        this.winningTeam = game.getWinningTeam();
        this.scored = game.getScored();
        this.odds = odds;
        
        // Set primary odds (first available odds, which should be FanDuel)
        this.primaryOdds = odds.isEmpty() ? null : odds.get(0);
    }
}

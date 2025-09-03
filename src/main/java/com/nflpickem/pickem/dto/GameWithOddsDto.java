package com.nflpickem.pickem.dto;

import com.nflpickem.pickem.model.BettingOdds;
import com.nflpickem.pickem.model.Game;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
public class GameWithOddsDto {
    private Long id;
    private Integer week;
    private String homeTeam;
    private String awayTeam;
    private Instant kickoffTime;
    private String winningTeam;
    private Boolean scored;
    private BettingOdds odds;
    
    public GameWithOddsDto(Game game) {
        this.id = game.getId();
        this.week = game.getWeek();
        this.homeTeam = game.getHomeTeam();
        this.awayTeam = game.getAwayTeam();
        this.kickoffTime = game.getKickoffTime();
        this.winningTeam = game.getWinningTeam();
        this.scored = game.isScored(); // Use isScored() for boolean primitive
    }
    
    public void setOdds(BettingOdds odds) {
        this.odds = odds;
    }
    
    // Helper methods for formatting odds display
    public String getFormattedSpread() {
        if (odds == null || odds.getSpread() == null) {
            return "N/A";
        }
        String team = odds.getSpreadTeam() != null ? odds.getSpreadTeam() : "";
        return String.format("%.1f %s", odds.getSpread(), team);
    }
    
    public String getFormattedTotal() {
        if (odds == null || odds.getTotal() == null) {
            return "N/A";
        }
        return String.format("%.1f", odds.getTotal());
    }
    
    public String getFormattedHomeOdds() {
        if (odds == null || odds.getHomeTeamOdds() == null) {
            return "N/A";
        }
        return formatAmericanOdds(odds.getHomeTeamOdds());
    }
    
    public String getFormattedAwayOdds() {
        if (odds == null || odds.getAwayTeamOdds() == null) {
            return "N/A";
        }
        return formatAmericanOdds(odds.getAwayTeamOdds());
    }
    
    private String formatAmericanOdds(Double odds) {
        if (odds == null) return "N/A";
        if (odds > 0) {
            return "+" + String.valueOf(odds.intValue());
        } else {
            return String.valueOf(odds.intValue());
        }
    }
    
    public String getFormattedKickoffTime() {
        if (kickoffTime == null) return "TBD";
        LocalDateTime localDateTime = LocalDateTime.ofInstant(kickoffTime, ZoneId.of("America/New_York"));
        return localDateTime.format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"));
    }
}

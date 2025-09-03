package com.nflpickem.pickem.dto;

import com.nflpickem.pickem.model.BettingOdds;
import com.nflpickem.pickem.model.Game;
import lombok.Data;

import java.time.Instant;

@Data
public class GameWithOddsDto {
    private Long id;
    private Integer week;
    private String homeTeam;
    private String awayTeam;
    private Instant kickoffTime;
    private String winningTeam;
    private boolean scored;
    
    // Betting odds information
    private String sportsbook;
    private Double homeTeamOdds;
    private Double awayTeamOdds;
    private Double spread;
    private String spreadTeam;
    private Double total;
    private Instant oddsLastUpdated;
    
    public GameWithOddsDto(Game game) {
        this.id = game.getId();
        this.week = game.getWeek();
        this.homeTeam = game.getHomeTeam();
        this.awayTeam = game.getAwayTeam();
        this.kickoffTime = game.getKickoffTime();
        this.winningTeam = game.getWinningTeam();
        this.scored = game.isScored();
    }
    
    public void setOdds(BettingOdds odds) {
        if (odds != null) {
            this.sportsbook = odds.getSportsbook();
            this.homeTeamOdds = odds.getHomeTeamOdds();
            this.awayTeamOdds = odds.getAwayTeamOdds();
            this.spread = odds.getSpread();
            this.spreadTeam = odds.getSpreadTeam();
            this.total = odds.getTotal();
            this.oddsLastUpdated = odds.getLastUpdated();
        }
    }
    
    /**
     * Get formatted spread display
     */
    public String getFormattedSpread() {
        if (spread == null || spreadTeam == null) {
            return "N/A";
        }
        
        String teamName = spreadTeam.equals(homeTeam) ? "H" : "A";
        return String.format("%s %s%.1f", teamName, spread > 0 ? "+" : "-", spread);
    }
    
    /**
     * Get formatted total display
     */
    public String getFormattedTotal() {
        if (total == null) {
            return "N/A";
        }
        return String.format("%.1f", total);
    }
    
    /**
     * Get formatted odds display
     */
    public String getFormattedHomeOdds() {
        if (homeTeamOdds == null) {
            return "N/A";
        }
        return formatAmericanOdds(homeTeamOdds);
    }
    
    public String getFormattedAwayOdds() {
        if (awayTeamOdds == null) {
            return "N/A";
        }
        return formatAmericanOdds(awayTeamOdds);
    }
    
    /**
     * Format American odds (e.g., +150, -110)
     */
    private String formatAmericanOdds(Double odds) {
        if (odds == null) {
            return "N/A";
        }
        
        if (odds > 0) {
            return String.format("+%.0f", odds);
        } else {
            return String.format("%.0f", odds);
        }
    }
}

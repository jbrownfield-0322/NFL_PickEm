package com.nflpickem.pickem.repository;

import com.nflpickem.pickem.model.BettingOdds;
import com.nflpickem.pickem.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface BettingOddsRepository extends JpaRepository<BettingOdds, Long> {
    
    List<BettingOdds> findByGame(Game game);
    
    List<BettingOdds> findByGameId(Long gameId);
    
    Optional<BettingOdds> findByGameAndSportsbook(Game game, String sportsbook);
    
    @Query("SELECT bo FROM BettingOdds bo WHERE bo.game.week = :week")
    List<BettingOdds> findByWeek(@Param("week") Integer week);
    
    @Query("SELECT bo FROM BettingOdds bo WHERE bo.game.week = :week AND bo.sportsbook = :sportsbook")
    List<BettingOdds> findByWeekAndSportsbook(@Param("week") Integer week, @Param("sportsbook") String sportsbook);
    
    @Query("SELECT bo FROM BettingOdds bo WHERE bo.lastUpdated < :cutoffTime")
    List<BettingOdds> findStaleOdds(@Param("cutoffTime") java.time.Instant cutoffTime);
    
    void deleteByGame(Game game);
    
    /**
     * Upsert method to insert or update betting odds
     * Uses PostgreSQL's ON CONFLICT clause for atomic upsert operation
     */
    @Modifying
    @Query(value = """
        INSERT INTO betting_odds (game_id, sportsbook, odds_type, spread, spread_team, total, home_team_odds, away_team_odds, last_updated)
        VALUES (:gameId, :sportsbook, :oddsType, :spread, :spreadTeam, :total, :homeTeamOdds, :awayTeamOdds, :lastUpdated)
        ON CONFLICT (game_id, sportsbook, odds_type) 
        DO UPDATE SET
            spread = EXCLUDED.spread,
            spread_team = EXCLUDED.spread_team,
            total = EXCLUDED.total,
            home_team_odds = EXCLUDED.home_team_odds,
            away_team_odds = EXCLUDED.away_team_odds,
            last_updated = EXCLUDED.last_updated
        """, nativeQuery = true)
    void upsertOdds(@Param("gameId") Long gameId, 
                   @Param("sportsbook") String sportsbook,
                   @Param("oddsType") String oddsType,
                   @Param("spread") Double spread,
                   @Param("spreadTeam") String spreadTeam,
                   @Param("total") Double total,
                   @Param("homeTeamOdds") Double homeTeamOdds,
                   @Param("awayTeamOdds") Double awayTeamOdds,
                   @Param("lastUpdated") Instant lastUpdated);
}

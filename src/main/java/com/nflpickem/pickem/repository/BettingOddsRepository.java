package com.nflpickem.pickem.repository;

import com.nflpickem.pickem.model.BettingOdds;
import com.nflpickem.pickem.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}

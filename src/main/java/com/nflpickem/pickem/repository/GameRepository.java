package com.nflpickem.pickem.repository;

import com.nflpickem.pickem.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    List<Game> findByWeek(Integer week);
    List<Game> findByScoredFalseAndKickoffTimeBefore(Instant dateTime);
    
    // Check for exact duplicate
    Optional<Game> findByWeekAndHomeTeamAndAwayTeam(Integer week, String homeTeam, String awayTeam);
    
    // Find games with similar teams and time (within 2 hours)
    @Query("SELECT g FROM Game g WHERE g.week = :week AND " +
           "((g.homeTeam = :homeTeam AND g.awayTeam = :awayTeam) OR " +
           "(g.homeTeam = :awayTeam AND g.awayTeam = :homeTeam)) AND " +
           "ABS(EXTRACT(EPOCH FROM (g.kickoffTime - :kickoffTime))) <= 7200")
    List<Game> findSimilarGames(@Param("week") Integer week, 
                               @Param("homeTeam") String homeTeam, 
                               @Param("awayTeam") String awayTeam, 
                               @Param("kickoffTime") Instant kickoffTime);
} 
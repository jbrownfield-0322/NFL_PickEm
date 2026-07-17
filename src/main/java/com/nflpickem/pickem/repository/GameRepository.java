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

    List<Game> findBySeasonYear(Integer seasonYear);

    List<Game> findBySeasonYearAndWeek(Integer seasonYear, Integer week);

    List<Game> findByScoredFalseAndKickoffTimeBefore(Instant dateTime);

    List<Game> findByScoredFalseAndKickoffTimeBetween(Instant startTime, Instant endTime);

    Optional<Game> findByWeekAndHomeTeamAndAwayTeam(Integer week, String homeTeam, String awayTeam);

    Optional<Game> findBySeasonYearAndWeekAndHomeTeamAndAwayTeam(
            Integer seasonYear, Integer week, String homeTeam, String awayTeam);

    Optional<Game> findByHomeTeamAndAwayTeam(String homeTeam, String awayTeam);

    List<Game> findByKickoffTimeBetween(Instant startTime, Instant endTime);

    @Query("SELECT DISTINCT g.seasonYear FROM Game g WHERE g.seasonYear IS NOT NULL ORDER BY g.seasonYear DESC")
    List<Integer> findDistinctSeasonYears();

    @Query("SELECT g FROM Game g WHERE g.seasonYear IS NULL")
    List<Game> findBySeasonYearIsNull();

    @Query("SELECT g FROM Game g WHERE g.seasonYear = :seasonYear AND g.week = :week AND " +
           "((g.homeTeam = :homeTeam AND g.awayTeam = :awayTeam) OR " +
           "(g.homeTeam = :awayTeam AND g.awayTeam = :homeTeam)) AND " +
           "g.kickoffTime BETWEEN :startTime AND :endTime")
    List<Game> findSimilarGames(@Param("seasonYear") Integer seasonYear,
                               @Param("week") Integer week,
                               @Param("homeTeam") String homeTeam,
                               @Param("awayTeam") String awayTeam,
                               @Param("startTime") Instant startTime,
                               @Param("endTime") Instant endTime);

    /** Legacy similar-games lookup without season (prefer season-aware overload). */
    @Query("SELECT g FROM Game g WHERE g.week = :week AND " +
           "((g.homeTeam = :homeTeam AND g.awayTeam = :awayTeam) OR " +
           "(g.homeTeam = :awayTeam AND g.awayTeam = :homeTeam)) AND " +
           "g.kickoffTime BETWEEN :startTime AND :endTime")
    List<Game> findSimilarGamesByWeek(@Param("week") Integer week,
                               @Param("homeTeam") String homeTeam,
                               @Param("awayTeam") String awayTeam,
                               @Param("startTime") Instant startTime,
                               @Param("endTime") Instant endTime);
}

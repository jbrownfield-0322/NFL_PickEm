package com.nflpickem.pickem.repository;

import com.nflpickem.pickem.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    List<Game> findByWeek(Integer week);
    List<Game> findByScoredFalseAndKickoffTimeBefore(Instant dateTime);
} 
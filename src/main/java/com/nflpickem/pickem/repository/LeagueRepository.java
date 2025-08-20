package com.nflpickem.pickem.repository;

import com.nflpickem.pickem.model.League;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LeagueRepository extends JpaRepository<League, Long> {
    Optional<League> findByJoinCode(String joinCode);
} 
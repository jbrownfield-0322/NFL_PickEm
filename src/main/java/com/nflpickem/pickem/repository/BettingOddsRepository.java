package com.nflpickem.pickem.repository;

import com.nflpickem.pickem.model.BettingOdds;
import com.nflpickem.pickem.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BettingOddsRepository extends JpaRepository<BettingOdds, Long> {
    
    Optional<BettingOdds> findByGame(Game game);
    
    List<BettingOdds> findByGameWeek(Integer week);
    
    void deleteByGame(Game game);
    
    List<BettingOdds> findBySportsbook(String sportsbook);
}

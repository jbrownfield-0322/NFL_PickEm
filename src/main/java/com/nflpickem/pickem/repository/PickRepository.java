package com.nflpickem.pickem.repository;

import com.nflpickem.pickem.model.Game;
import com.nflpickem.pickem.model.Pick;
import com.nflpickem.pickem.model.User;
import com.nflpickem.pickem.model.League;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PickRepository extends JpaRepository<Pick, Long> {
    List<Pick> findByUser(User user);
    List<Pick> findByGame(Game game);
    Pick findByUserAndGame(User user, Game game);
    Pick findByUserAndGameAndLeague(User user, Game game, League league);
    List<Pick> findByUserAndLeague(User user, League league);
    List<Pick> findByGameAndLeague(Game game, League league);
    List<Pick> findByLeague(League league);
} 
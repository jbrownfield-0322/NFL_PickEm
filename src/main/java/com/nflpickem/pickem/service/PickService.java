package com.nflpickem.pickem.service;

import com.nflpickem.pickem.model.Game;
import com.nflpickem.pickem.model.Pick;
import com.nflpickem.pickem.model.User;
import com.nflpickem.pickem.model.League;
import com.nflpickem.pickem.repository.PickRepository;
import com.nflpickem.pickem.repository.UserRepository;
import com.nflpickem.pickem.repository.LeagueRepository;
import org.springframework.stereotype.Service;

import java.time.Instant; // Import Instant
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PickService {
    private final PickRepository pickRepository;
    private final GameService gameService;
    private final UserRepository userRepository;
    private final LeagueRepository leagueRepository;

    public PickService(PickRepository pickRepository, GameService gameService, UserRepository userRepository, LeagueRepository leagueRepository) {
        this.pickRepository = pickRepository;
        this.gameService = gameService;
        this.userRepository = userRepository;
        this.leagueRepository = leagueRepository;
    }

    public Pick submitPick(Long userId, Long gameId, String pickedTeam, Long leagueId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Game game = gameService.getGameById(gameId);
        if (game == null) {
            throw new RuntimeException("Game not found");
        }

        League league = null;
        if (leagueId != null) {
            league = leagueRepository.findById(leagueId)
                    .orElseThrow(() -> new RuntimeException("League not found"));
            if (!league.getMembers().contains(user)) {
                throw new RuntimeException("User is not a member of this league");
            }
        }

        if (game.getKickoffTime().isBefore(Instant.now())) { // Compare Instant with Instant.now()
            throw new RuntimeException("Cannot submit pick after kickoff time");
        }

        Pick existingPick = (leagueId != null) ? pickRepository.findByUserAndGameAndLeague(user, game, league) : pickRepository.findByUserAndGame(user, game);
        
        if (existingPick != null) {
            // Update existing pick
            if (!pickedTeam.equals(game.getHomeTeam()) && !pickedTeam.equals(game.getAwayTeam())) {
                throw new RuntimeException("Invalid team picked. Must be either home or away team.");
            }
            existingPick.setPickedTeam(pickedTeam);
            return pickRepository.save(existingPick);
        } else {
            // Create new pick
            if (!pickedTeam.equals(game.getHomeTeam()) && !pickedTeam.equals(game.getAwayTeam())) {
                throw new RuntimeException("Invalid team picked. Must be either home or away team.");
            }
            Pick pick = new Pick();
            pick.setUser(user);
            pick.setGame(game);
            pick.setLeague(league);
            pick.setPickedTeam(pickedTeam);
            pick.setCorrect(false);
            return pickRepository.save(pick);
        }
    }

    public List<Pick> getPicksByUser(Long userId, Long leagueId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (leagueId != null) {
            League league = leagueRepository.findById(leagueId)
                    .orElseThrow(() -> new RuntimeException("League not found"));
            return pickRepository.findByUserAndLeague(user, league);
        } else {
            return pickRepository.findByUser(user);
        }
    }
} 
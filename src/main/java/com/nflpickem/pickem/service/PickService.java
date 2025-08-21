package com.nflpickem.pickem.service;

import com.nflpickem.pickem.model.Game;
import com.nflpickem.pickem.model.Pick;
import com.nflpickem.pickem.model.User;
import com.nflpickem.pickem.model.League;
import com.nflpickem.pickem.repository.PickRepository;
import com.nflpickem.pickem.repository.UserRepository;
import com.nflpickem.pickem.repository.LeagueRepository;
import com.nflpickem.pickem.dto.PickComparisonDto;
import com.nflpickem.pickem.dto.BulkPickRequest;
import com.nflpickem.pickem.dto.PickRequest;
import org.springframework.stereotype.Service;

import java.time.Instant; // Import Instant
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

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

        // Find existing pick for this specific league (or no league)
        Pick existingPick = null;
        if (leagueId != null) {
            // For league picks, only look for picks in the same league
            existingPick = pickRepository.findByUserAndGameAndLeague(user, game, league);
        } else {
            // For non-league picks, only look for picks with no league
            // We need to handle this differently since findByUserAndGame might include league picks
            List<Pick> userPicksForGame = pickRepository.findByUserAndGame(user, game);
            existingPick = userPicksForGame.stream()
                .filter(pick -> pick.getLeague() == null)
                .findFirst()
                .orElse(null);
        }
        
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

    public List<PickComparisonDto> getPickComparison(Long userId, Integer week, Long leagueId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Get all games for the specified week
        List<Game> games = gameService.getGamesByWeek(week);
        List<PickComparisonDto> comparisonData = new ArrayList<>();
        
        for (Game game : games) {
            PickComparisonDto comparison = new PickComparisonDto();
            comparison.setGameId(game.getId());
            comparison.setWeek(game.getWeek());
            comparison.setAwayTeam(game.getAwayTeam());
            comparison.setHomeTeam(game.getHomeTeam());
            comparison.setWinningTeam(game.getWinningTeam());
            comparison.setScored(game.isScored());
            
            // Get user's pick for this game
            Pick userPick = null;
            if (leagueId != null) {
                League league = leagueRepository.findById(leagueId)
                        .orElseThrow(() -> new RuntimeException("League not found"));
                userPick = pickRepository.findByUserAndGameAndLeague(user, game, league);
            } else {
                userPick = pickRepository.findByUserAndGame(user, game);
            }
            
            comparison.setYourPick(userPick != null ? userPick.getPickedTeam() : null);
            
            // Get all other picks for this game in the league
            List<PickComparisonDto.UserPickDto> otherPicks = new ArrayList<>();
            List<Pick> allPicksForGame;
            
            if (leagueId != null) {
                League league = leagueRepository.findById(leagueId)
                        .orElseThrow(() -> new RuntimeException("League not found"));
                allPicksForGame = pickRepository.findByGameAndLeague(game, league);
            } else {
                allPicksForGame = pickRepository.findByGame(game);
            }
            
            for (Pick pick : allPicksForGame) {
                if (!pick.getUser().getId().equals(userId)) { // Exclude current user
                    PickComparisonDto.UserPickDto userPickDto = new PickComparisonDto.UserPickDto();
                    userPickDto.setUsername(pick.getUser().getUsername());
                    userPickDto.setPickedTeam(pick.getPickedTeam());
                    userPickDto.setCorrect(pick.isCorrect());
                    otherPicks.add(userPickDto);
                }
            }
            
            comparison.setOtherPicks(otherPicks);
            comparisonData.add(comparison);
        }
        
        return comparisonData;
    }

    public List<Pick> submitBulkPicks(BulkPickRequest bulkRequest) {
        User user = userRepository.findById(bulkRequest.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        League league = null;
        if (bulkRequest.getLeagueId() != null) {
            league = leagueRepository.findById(bulkRequest.getLeagueId())
                    .orElseThrow(() -> new RuntimeException("League not found"));
            if (!league.getMembers().contains(user)) {
                throw new RuntimeException("User is not a member of this league");
            }
        }
        
        List<Pick> submittedPicks = new ArrayList<>();
        
        for (PickRequest pickRequest : bulkRequest.getPicks()) {
            try {
                Game game = gameService.getGameById(pickRequest.getGameId());
                if (game == null) {
                    throw new RuntimeException("Game not found: " + pickRequest.getGameId());
                }
                
                if (game.getKickoffTime().isBefore(Instant.now())) {
                    throw new RuntimeException("Cannot submit pick after kickoff time for game: " + game.getId());
                }
                
                if (!pickRequest.getPickedTeam().equals(game.getHomeTeam()) && 
                    !pickRequest.getPickedTeam().equals(game.getAwayTeam())) {
                    throw new RuntimeException("Invalid team picked for game: " + game.getId());
                }
                
                // Find existing pick for this specific league (or no league)
                Pick existingPick = null;
                if (league != null) {
                    // For league picks, only look for picks in the same league
                    existingPick = pickRepository.findByUserAndGameAndLeague(user, game, league);
                } else {
                    // For non-league picks, only look for picks with no league
                    // We need to handle this differently since findByUserAndGame might include league picks
                    List<Pick> userPicksForGame = pickRepository.findByUserAndGame(user, game);
                    existingPick = userPicksForGame.stream()
                        .filter(pick -> pick.getLeague() == null)
                        .findFirst()
                        .orElse(null);
                }
                
                Pick pick;
                if (existingPick != null) {
                    // Update existing pick
                    existingPick.setPickedTeam(pickRequest.getPickedTeam());
                    pick = pickRepository.save(existingPick);
                } else {
                    // Create new pick
                    pick = new Pick();
                    pick.setUser(user);
                    pick.setGame(game);
                    pick.setLeague(league);
                    pick.setPickedTeam(pickRequest.getPickedTeam());
                    pick.setCorrect(false);
                    pick = pickRepository.save(pick);
                }
                
                submittedPicks.add(pick);
                
            } catch (Exception e) {
                throw new RuntimeException("Error processing pick for game " + pickRequest.getGameId() + ": " + e.getMessage());
            }
        }
        
        return submittedPicks;
    }
} 
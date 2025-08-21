package com.nflpickem.pickem.service;

import com.nflpickem.pickem.model.League;
import com.nflpickem.pickem.model.User;
import com.nflpickem.pickem.repository.LeagueRepository;
import com.nflpickem.pickem.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LeagueService {
    private static final Logger logger = LoggerFactory.getLogger(LeagueService.class);
    
    private final LeagueRepository leagueRepository;
    private final UserRepository userRepository;

    public LeagueService(LeagueRepository leagueRepository, UserRepository userRepository) {
        this.leagueRepository = leagueRepository;
        this.userRepository = userRepository;
    }

    public League createLeague(String leagueName, Long adminId) {
        logger.info("Creating league: {} with admin: {}", leagueName, adminId);
        
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin user not found with id: " + adminId));

        String joinCode = generateUniqueJoinCode();
        League league = new League();
        league.setName(leagueName);
        league.setJoinCode(joinCode);
        league.setAdmin(admin);
        league.addMember(admin); // Admin is automatically a member
        
        League savedLeague = leagueRepository.save(league);
        logger.info("Created league: {} with id: {}", leagueName, savedLeague.getId());
        return savedLeague;
    }

    public League joinLeague(String joinCode, Long userId) {
        logger.info("Joining league with code: {} for user: {}", joinCode, userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        League league = leagueRepository.findByJoinCode(joinCode)
                .orElseThrow(() -> new RuntimeException("Invalid join code: " + joinCode));

        if (league.getMembers().contains(user)) {
            throw new RuntimeException("User is already a member of this league");
        }

        league.addMember(user);
        League savedLeague = leagueRepository.save(league);
        logger.info("User {} joined league: {}", userId, league.getName());
        return savedLeague;
    }

    public Optional<League> getLeagueById(Long id) {
        logger.info("Getting league by id: {}", id);
        return leagueRepository.findById(id);
    }

    public List<League> getLeaguesByUserId(Long userId) {
        logger.info("Getting leagues for user: {}", userId);
        
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
            
            List<League> allLeagues = leagueRepository.findAll();
            logger.info("Found {} total leagues", allLeagues.size());
            
            List<League> userLeagues = allLeagues.stream()
                    .filter(league -> league.getMembers().contains(user))
                    .collect(Collectors.toList());
            
            logger.info("User {} is a member of {} leagues", userId, userLeagues.size());
            return userLeagues;
        } catch (Exception e) {
            logger.error("Error getting leagues for user: {}", userId, e);
            throw e;
        }
    }

    private String generateUniqueJoinCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (leagueRepository.findByJoinCode(code).isPresent());
        return code;
    }
} 
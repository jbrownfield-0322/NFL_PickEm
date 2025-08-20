package com.nflpickem.pickem.service;

import com.nflpickem.pickem.model.League;
import com.nflpickem.pickem.model.User;
import com.nflpickem.pickem.repository.LeagueRepository;
import com.nflpickem.pickem.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LeagueService {
    private final LeagueRepository leagueRepository;
    private final UserRepository userRepository;

    public LeagueService(LeagueRepository leagueRepository, UserRepository userRepository) {
        this.leagueRepository = leagueRepository;
        this.userRepository = userRepository;
    }

    public League createLeague(String leagueName, Long adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin user not found"));

        String joinCode = generateUniqueJoinCode();
        League league = new League();
        league.setName(leagueName);
        league.setJoinCode(joinCode);
        league.setAdmin(admin);
        league.addMember(admin); // Admin is automatically a member
        return leagueRepository.save(league);
    }

    public League joinLeague(String joinCode, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        League league = leagueRepository.findByJoinCode(joinCode)
                .orElseThrow(() -> new RuntimeException("Invalid join code"));

        if (league.getMembers().contains(user)) {
            throw new RuntimeException("User is already a member of this league");
        }

        league.addMember(user);
        return leagueRepository.save(league);
    }

    public Optional<League> getLeagueById(Long id) {
        return leagueRepository.findById(id);
    }

    public List<League> getLeaguesByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return leagueRepository.findAll().stream()
                .filter(league -> league.getMembers().contains(user))
                .collect(Collectors.toList());
    }

    private String generateUniqueJoinCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (leagueRepository.findByJoinCode(code).isPresent());
        return code;
    }
} 
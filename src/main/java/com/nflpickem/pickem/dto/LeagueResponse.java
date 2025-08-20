package com.nflpickem.pickem.dto;

import com.nflpickem.pickem.model.League;
import com.nflpickem.pickem.model.User;
import lombok.Data;

import java.util.Set;
import java.util.stream.Collectors;

@Data
public class LeagueResponse {
    private Long id;
    private String name;
    private String joinCode;
    private UserResponse admin;
    private Set<UserResponse> members;

    public LeagueResponse(League league) {
        this.id = league.getId();
        this.name = league.getName();
        this.joinCode = league.getJoinCode();
        
        if (league.getAdmin() != null) {
            this.admin = new UserResponse(league.getAdmin());
        }
        
        if (league.getMembers() != null) {
            this.members = league.getMembers().stream()
                .map(UserResponse::new)
                .collect(Collectors.toSet());
        }
    }
}

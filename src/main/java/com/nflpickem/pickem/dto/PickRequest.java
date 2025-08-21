package com.nflpickem.pickem.dto;

import lombok.Data;

@Data
public class PickRequest {
    private Long userId;
    private Long gameId;
    private String pickedTeam;
    private Long leagueId;
} 
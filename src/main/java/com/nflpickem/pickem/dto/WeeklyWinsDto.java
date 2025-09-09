package com.nflpickem.pickem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyWinsDto {
    private String username;
    private String name;
    private Long weeklyWins;
}

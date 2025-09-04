package com.nflpickem.pickem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerScore {
    private String username;
    private String name;
    private Long score;
} 
package com.nflpickem.pickem.dto;

import lombok.Data;
import java.util.List;

@Data
public class PickComparisonDto {
    private Long gameId;
    private Integer week;
    private String awayTeam;
    private String homeTeam;
    private String winningTeam;
    private boolean scored;
    private String yourPick;
    private List<UserPickDto> otherPicks;
    
    @Data
    public static class UserPickDto {
        private String username;
        private String name;
        private String pickedTeam;
        private boolean correct;
    }
}

package com.nflpickem.pickem.dto;

import lombok.Data;
import java.util.List;

@Data
public class BulkPickRequest {
    private Long userId;
    private Long leagueId;
    private List<PickRequest> picks;
}

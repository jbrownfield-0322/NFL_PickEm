package com.nflpickem.pickem.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

import java.time.Instant;

@Entity
@Data
@Table(uniqueConstraints = {
    @UniqueConstraint(name = "uk_game_season_week_teams", columnNames = {"seasonYear", "week", "homeTeam", "awayTeam"})
})
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** NFL season start year (e.g. 2025 for the Sep 2025 – Feb 2026 season). */
    @Column(nullable = true) // null only briefly until SeasonBackfillRunner runs
    private Integer seasonYear;

    private Integer week;
    private String homeTeam;
    private String awayTeam;
    private Instant kickoffTime;
    private String winningTeam;
    private boolean scored;
}

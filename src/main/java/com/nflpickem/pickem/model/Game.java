package com.nflpickem.pickem.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

import java.time.Instant; // Use Instant for timezone-aware timestamps
import java.time.LocalDateTime;

@Entity
@Data
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"week", "homeTeam", "awayTeam"})
})
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer week;
    private String homeTeam;
    private String awayTeam;
    private Instant kickoffTime; // Change to Instant
    private String winningTeam;
    private boolean scored;
} 
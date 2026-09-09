package com.nflpickem.pickem.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.List;
import java.util.Map;

/**
 * Multi-season support requires uniqueness on (season_year, week, home, away).
 * Older DBs still have a Hibernate-generated unique constraint on
 * (week, home_team, away_team) only — that blocks creating 2026 games that
 * share a week/matchup with 2025 (and poisons odds updates).
 */
@Component
@Order(50)
public class GameUniqueConstraintMigrator implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(GameUniqueConstraintMigrator.class);

    /** Legacy auto-named unique constraint seen in production Postgres logs. */
    private static final String LEGACY_CONSTRAINT = "ukofd1g6nsgxrul4uvbe4e5m739";
    private static final String SEASON_AWARE_CONSTRAINT = "uk_game_season_week_teams";

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public GameUniqueConstraintMigrator(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isPostgres()) {
            logger.debug("Skipping game unique-constraint migration (not PostgreSQL)");
            return;
        }
        try {
            dropConstraintIfExists(LEGACY_CONSTRAINT);
            dropLegacyWeekTeamUniques();
            ensureSeasonAwareUniqueExists();
        } catch (Exception e) {
            logger.error("Failed to migrate game unique constraints: {}", e.getMessage(), e);
        }
    }

    private boolean isPostgres() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            String product = meta.getDatabaseProductName();
            return product != null && product.toLowerCase().contains("postgresql");
        } catch (Exception e) {
            return false;
        }
    }

    private void dropConstraintIfExists(String constraintName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM pg_constraint
                WHERE conname = ? AND contype = 'u'
                """,
                Integer.class,
                constraintName);
        if (count != null && count > 0) {
            jdbcTemplate.execute("ALTER TABLE game DROP CONSTRAINT IF EXISTS " + safeIdent(constraintName));
            logger.info("Dropped legacy unique constraint {}", constraintName);
        }
    }

    /**
     * Drop unique constraints on game whose columns are week + home/away only
     * (any name Hibernate may have generated).
     */
    private void dropLegacyWeekTeamUniques() {
        List<Map<String, Object>> constraints = jdbcTemplate.queryForList(
                """
                SELECT c.conname AS name,
                       (
                         SELECT string_agg(a.attname, ',' ORDER BY u.ord)
                         FROM unnest(c.conkey) WITH ORDINALITY AS u(attnum, ord)
                         JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = u.attnum
                       ) AS cols
                FROM pg_constraint c
                JOIN pg_class t ON c.conrelid = t.oid
                WHERE t.relname = 'game'
                  AND c.contype = 'u'
                """);

        for (Map<String, Object> row : constraints) {
            String name = String.valueOf(row.get("name"));
            String cols = String.valueOf(row.get("cols"));
            if (SEASON_AWARE_CONSTRAINT.equals(name)) {
                continue;
            }
            if (!isLegacyWeekTeamOnly(cols)) {
                continue;
            }
            jdbcTemplate.execute("ALTER TABLE game DROP CONSTRAINT IF EXISTS " + safeIdent(name));
            logger.info("Dropped legacy week/team unique constraint {} ({})", name, cols);
        }
    }

    private static boolean isLegacyWeekTeamOnly(String cols) {
        if (cols == null || cols.contains("season_year")) {
            return false;
        }
        return cols.contains("week")
                && cols.contains("home_team")
                && cols.contains("away_team");
    }

    private void ensureSeasonAwareUniqueExists() {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM pg_constraint
                WHERE conname = ? AND contype = 'u'
                """,
                Integer.class,
                SEASON_AWARE_CONSTRAINT);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.execute(
                """
                ALTER TABLE game
                ADD CONSTRAINT uk_game_season_week_teams
                UNIQUE (season_year, week, home_team, away_team)
                """);
        logger.info("Added season-aware unique constraint {}", SEASON_AWARE_CONSTRAINT);
    }

    private static String safeIdent(String ident) {
        if (!ident.matches("[a-zA-Z0-9_]+")) {
            throw new IllegalArgumentException("Unsafe constraint name: " + ident);
        }
        return ident;
    }
}

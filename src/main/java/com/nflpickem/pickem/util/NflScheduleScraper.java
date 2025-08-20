package com.nflpickem.pickem.util;

import com.nflpickem.pickem.model.Game;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class NflScheduleScraper {

    private static final String BASE_NFL_SCHEDULE_URL = "https://www.pro-football-reference.com/years/";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy h:mm a", Locale.ENGLISH);
    private static final ZoneId EASTERN_TIME = ZoneId.of("America/New_York"); // Assuming ET for NFL game times

    public List<Game> scrapeGames(Integer year, Integer targetWeekNum) throws IOException {
        List<Game> games = new ArrayList<>();
        String url = BASE_NFL_SCHEDULE_URL + year + "/games.htm";
        Document doc = Jsoup.connect(url).get();

        Elements gameRows = doc.select("table#games tbody tr");

        for (Element row : gameRows) {
            try {
                String weekNumStr = row.select("th[data-stat=week_num]").text();
                if (weekNumStr.isEmpty()) {
                    weekNumStr = row.select("td[data-stat=week_num]").text();
                }

                // Skip header rows or rows that don't have a numeric week number for regular season
                if (row.hasClass("thead")) {
                    System.err.println("Skipping header row (hasClass(\"thead\")): " + row.outerHtml());
                    continue;
                }

                Integer week = null;
                try {
                    week = Integer.parseInt(weekNumStr);
                } catch (NumberFormatException e) {
                    System.err.println("Skipping row with non-numeric week number: '" + weekNumStr + "'. Full row: " + row.outerHtml());
                    continue;
                }

                if (!week.equals(targetWeekNum)) {
                    // This is intentional, as we only want games for targetWeekNum for this specific scrape call
                    // System.err.println("Skipping game not for target week " + targetWeekNum + ": Week " + week + ". Full row: " + row.outerHtml());
                    continue;
                }

                String awayTeam = row.select("td[data-stat=visitor_team]").text();
                String homeTeam = row.select("td[data-stat=home_team]").text();
                String dateStr = row.select("td[data-stat=boxscore_word]").text().trim();
                String timeStr = row.select("td[data-stat=gametime]").text().trim();

                if (dateStr.isEmpty() || timeStr.isEmpty()) {
                    System.err.println("Skipping row due to empty date or time string. Full row: " + row.outerHtml());
                    continue;
                }

                String awayScoreStr = row.select("td[data-stat=pts_vis]").text();
                String homeScoreStr = row.select("td[data-stat=pts_home]").text();

                String winningTeam = null;
                if (!awayScoreStr.isEmpty() && !homeScoreStr.isEmpty()) {
                    try {
                        int awayScore = Integer.parseInt(awayScoreStr);
                        int homeScore = Integer.parseInt(homeScoreStr);
                        if (awayScore > homeScore) {
                            winningTeam = awayTeam;
                        } else if (homeScore > awayScore) {
                            winningTeam = homeTeam;
                        } else {
                            winningTeam = "TIE";
                        }
                    } catch (NumberFormatException e) {
                        winningTeam = null;
                    }
                }

                String fullDateTimeStr = dateStr + ", " + year + " " + timeStr;
                Instant kickoffInstant = parseDateTime(fullDateTimeStr); // Now returns Instant

                if (kickoffInstant != null) {
                    Game game = new Game();
                    game.setWeek(targetWeekNum);
                    game.setHomeTeam(homeTeam);
                    game.setAwayTeam(awayTeam);
                    game.setKickoffTime(kickoffInstant); // Set Instant
                    game.setWinningTeam(winningTeam);
                    games.add(game);
                } else {
                    System.err.println("Skipping game due to parsing error: " + fullDateTimeStr);
                }
            } catch (Exception e) {
                System.err.println("Error parsing game row: " + row.outerHtml() + ", Error: " + e.getMessage());
            }
        }
        return games;
    }

    private Instant parseDateTime(String fullDateTimeStr) {
        try {
            // Parse as LocalDateTime first, then convert to ZonedDateTime with assumed timezone, then to Instant
            LocalDateTime localDateTime = LocalDateTime.parse(fullDateTimeStr, DATE_TIME_FORMATTER);
            ZonedDateTime zonedDateTime = localDateTime.atZone(EASTERN_TIME);
            return zonedDateTime.toInstant();
        } catch (Exception e) {
            System.err.println("Failed to parse date-time string: " + fullDateTimeStr + ", Error: " + e.getMessage());
            return null;
        }
    }
} 
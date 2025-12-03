-- ============================================
-- NFL Pick'em Analytics Queries
-- ============================================

-- 1. Who picks the most underdogs?
-- This query identifies users who pick underdogs most frequently.
-- An underdog is determined by the betting spread:
--   - If spread_team has positive spread, they are the underdog
--   - If spread_team has negative spread, the other team is the underdog
-- Note: Uses PostgreSQL FILTER clause. For H2/MySQL compatibility, see alternative below.
WITH user_pick_stats AS (
    SELECT 
        u.id as user_id,
        u.username,
        u.name,
        COUNT(*) FILTER (WHERE 
            bo.spread IS NOT NULL AND (
                (bo.spread > 0 AND p.picked_team = bo.spread_team)
                OR
                (bo.spread < 0 AND p.picked_team != bo.spread_team)
            )
        ) as underdog_picks,
        COUNT(*) FILTER (WHERE bo.spread IS NOT NULL) as total_picks_with_odds
    FROM pick p
    JOIN app_user u ON p.user_id = u.id
    JOIN game g ON p.game_id = g.id
    LEFT JOIN betting_odds bo ON g.id = bo.game_id 
        AND bo.odds_type = 'american'
        AND bo.id = (
            SELECT id 
            FROM betting_odds 
            WHERE game_id = g.id 
            AND odds_type = 'american'
            ORDER BY last_updated DESC 
            LIMIT 1
        )
    GROUP BY u.id, u.username, u.name
)
SELECT 
    username,
    name,
    underdog_picks,
    total_picks_with_odds,
    ROUND(underdog_picks * 100.0 / NULLIF(total_picks_with_odds, 0), 2) as underdog_pick_percentage
FROM user_pick_stats
WHERE total_picks_with_odds > 0
ORDER BY underdog_picks DESC;

-- Alternative version using CASE WHEN for broader database compatibility:
-- WITH user_pick_stats AS (
--     SELECT 
--         u.id as user_id,
--         u.username,
--         u.name,
--         SUM(CASE WHEN 
--             bo.spread IS NOT NULL AND (
--                 (bo.spread > 0 AND p.picked_team = bo.spread_team)
--                 OR
--                 (bo.spread < 0 AND p.picked_team != bo.spread_team)
--             ) THEN 1 ELSE 0 END) as underdog_picks,
--         SUM(CASE WHEN bo.spread IS NOT NULL THEN 1 ELSE 0 END) as total_picks_with_odds
--     FROM pick p
--     JOIN app_user u ON p.user_id = u.id
--     JOIN game g ON p.game_id = g.id
--     LEFT JOIN betting_odds bo ON g.id = bo.game_id 
--         AND bo.odds_type = 'american'
--         AND bo.id = (
--             SELECT id 
--             FROM betting_odds 
--             WHERE game_id = g.id 
--             AND odds_type = 'american'
--             ORDER BY last_updated DESC 
--             LIMIT 1
--         )
--     GROUP BY u.id, u.username, u.name
-- )
-- SELECT 
--     username,
--     name,
--     underdog_picks,
--     total_picks_with_odds,
--     ROUND(underdog_picks * 100.0 / NULLIF(total_picks_with_odds, 0), 2) as underdog_pick_percentage
-- FROM user_pick_stats
-- WHERE total_picks_with_odds > 0
-- ORDER BY underdog_picks DESC;


-- ============================================
-- 2. Most picked team by each user
-- ============================================
SELECT 
    u.username,
    u.name,
    p.picked_team as team,
    COUNT(*) as pick_count,
    ROUND(COUNT(*) * 100.0 / NULLIF(SUM(COUNT(*)) OVER (PARTITION BY u.id), 0), 2) as percentage_of_user_picks
FROM pick p
JOIN app_user u ON p.user_id = u.id
GROUP BY u.id, u.username, u.name, p.picked_team
HAVING COUNT(*) = (
    SELECT MAX(pick_count)
    FROM (
        SELECT picked_team, COUNT(*) as pick_count
        FROM pick
        WHERE user_id = u.id
        GROUP BY picked_team
    ) user_team_picks
)
ORDER BY u.username, pick_count DESC;


-- ============================================
-- 3. Who picks the most away teams?
-- ============================================
SELECT 
    u.username,
    u.name,
    COUNT(*) as away_team_picks,
    COUNT(DISTINCT p.game_id) as total_picks,
    ROUND(COUNT(*) * 100.0 / NULLIF(COUNT(DISTINCT p.game_id), 0), 2) as away_pick_percentage
FROM pick p
JOIN app_user u ON p.user_id = u.id
JOIN game g ON p.game_id = g.id
WHERE p.picked_team = g.away_team
GROUP BY u.id, u.username, u.name
ORDER BY away_team_picks DESC;


-- ============================================
-- 4. Least picked team overall (by all users)
-- ============================================
SELECT 
    p.picked_team as team,
    COUNT(*) as total_picks,
    COUNT(DISTINCT p.user_id) as unique_users_who_picked,
    ROUND(COUNT(*) * 100.0 / NULLIF((SELECT COUNT(*) FROM pick), 0), 2) as percentage_of_all_picks
FROM pick p
GROUP BY p.picked_team
ORDER BY total_picks ASC;


-- ============================================
-- 4b. Least picked team by each user (alternative interpretation)
-- ============================================
SELECT 
    u.username,
    u.name,
    p.picked_team as least_picked_team,
    COUNT(*) as pick_count,
    ROUND(COUNT(*) * 100.0 / NULLIF(SUM(COUNT(*)) OVER (PARTITION BY u.id), 0), 2) as percentage_of_user_picks
FROM pick p
JOIN app_user u ON p.user_id = u.id
GROUP BY u.id, u.username, u.name, p.picked_team
HAVING COUNT(*) = (
    SELECT MIN(pick_count)
    FROM (
        SELECT picked_team, COUNT(*) as pick_count
        FROM pick
        WHERE user_id = u.id
        GROUP BY picked_team
    ) user_team_picks
)
ORDER BY u.username, pick_count ASC;


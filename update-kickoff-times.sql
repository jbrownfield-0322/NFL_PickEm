-- Update script to add 3 hours to all kickoff_time values in the game table
-- This will fix the timezone conversion issue where times were off by 3 hours

-- First, let's see what the current data looks like
SELECT id, week, away_team, home_team, kickoff_time 
FROM game 
ORDER BY kickoff_time 
LIMIT 10;

-- Update all kickoff_time values by adding 3 hours
UPDATE game 
SET kickoff_time = kickoff_time + INTERVAL '3 hours';

-- Verify the changes by checking a few records
SELECT id, week, away_team, home_team, kickoff_time 
FROM game 
ORDER BY kickoff_time 
LIMIT 10;

-- Optional: If you want to see the total number of records updated
SELECT COUNT(*) as total_games_updated FROM game;

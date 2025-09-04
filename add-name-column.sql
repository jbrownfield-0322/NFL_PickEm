-- Add name column to app_user table
ALTER TABLE app_user ADD COLUMN name VARCHAR(255);

-- Update existing users to have a default name based on their username
-- Extract the part before @ symbol from email as a default name
UPDATE app_user 
SET name = SUBSTRING(username, 1, LOCATE('@', username) - 1)
WHERE name IS NULL OR name = '';

-- For users without @ symbol, use the username as name
UPDATE app_user 
SET name = username 
WHERE name IS NULL OR name = '';

-- Make name column NOT NULL after populating it
ALTER TABLE app_user MODIFY COLUMN name VARCHAR(255) NOT NULL;

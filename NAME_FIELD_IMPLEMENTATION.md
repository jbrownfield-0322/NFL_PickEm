# Name Field Implementation Guide

## Overview
This document outlines the changes made to add a `name` field to users throughout the NFL Pickem application. The name field will be displayed instead of email addresses in the user interface, while emails continue to be used for login purposes.

## Backend Changes

### 1. User Model Updates
- **File**: `src/main/java/com/nflpickem/pickem/model/User.java`
- **Change**: Added `private String name;` field

### 2. DTO Updates
- **UserResponse.java**: Added `name` field and updated constructor
- **RegisterRequest.java**: Added `name` field for registration
- **PickComparisonDto.java**: Added `name` field to `UserPickDto`
- **PlayerScore.java**: Added `name` field for leaderboards

### 3. Service Updates
- **AuthService.java**: Updated `registerUser` method to accept and set name
- **PickService.java**: Updated pick comparison to include user names
- **LeaderboardService.java**: Updated to include names in leaderboard data

### 4. Controller Updates
- **AuthController.java**: Updated registration to pass name to service
- **UserController.java**: **NEW FILE** - Added endpoints for updating user profile information

### 5. New UserController Endpoints
- `PUT /api/user/{userId}/updateName` - Update user's display name
- `PUT /api/user/{userId}/updateUsername` - Update user's email
- `PUT /api/user/{userId}/updatePassword` - Update user's password

## Frontend Changes

### 1. Registration Form
- **File**: `frontend/src/components/Register.js`
- **Change**: Added name input field to registration form

### 2. Account Management
- **File**: `frontend/src/components/Account.js`
- **Changes**: 
  - Added name display and update functionality
  - Updated labels to clarify email vs. name
  - Added name update form

### 3. Leaderboards
- **File**: `frontend/src/components/Leaderboard.js`
- **Changes**:
  - Updated pick comparison table to show names in column headers
  - Updated leaderboard tables to show names instead of usernames
  - Enhanced pick comparison with better visual organization

### 4. League Details
- **File**: `frontend/src/components/LeagueDetails.js`
- **Changes**: Updated to display member names instead of usernames

## Database Migration

### SQL Script: `add-name-column.sql`
```sql
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
```

## Deployment Steps

### 1. Database Migration
1. Connect to your database
2. Run the SQL script: `add-name-column.sql`
3. Verify the column was added and populated

### 2. Backend Deployment
1. Build the updated Java application
2. Deploy to your server
3. Restart the application

### 3. Frontend Deployment
1. Build the updated React application: `npm run build`
2. Deploy the build files to your web server

## User Experience Changes

### Before
- Users were identified by email addresses throughout the interface
- Pick comparison showed email addresses in column headers
- Leaderboards displayed email addresses

### After
- Users are identified by their display names throughout the interface
- Pick comparison shows names in column headers with "(You)" indicator for current user
- Leaderboards display names instead of emails
- Users can update their display name in the Account section
- Registration requires a name field

## Fallback Behavior
- If a user doesn't have a name set, the system falls back to displaying their email address
- This ensures backward compatibility and prevents display issues

## Testing Checklist

### Backend
- [ ] User registration with name field works
- [ ] Login returns user data including name
- [ ] Pick comparison includes user names
- [ ] Leaderboards include user names
- [ ] User profile update endpoints work

### Frontend
- [ ] Registration form includes name field
- [ ] Account page shows and allows updating name
- [ ] Leaderboards display names instead of emails
- [ ] Pick comparison shows names in headers
- [ ] League details show member names
- [ ] Fallback to email works when name is not set

## Notes
- The name field is required during registration
- Existing users will have their name automatically set to the part of their email before the @ symbol
- The system maintains backward compatibility by falling back to email when name is not available
- All user-facing interfaces now prioritize displaying names over email addresses

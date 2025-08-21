const fs = require('fs');
const https = require('https');
const { JSDOM } = require('jsdom');

// Configuration
const API_BASE = process.env.API_BASE || 'http://localhost:8080/api';
const SEASON = process.env.SEASON || '2024';
const OUTPUT_FILE = `nfl-schedule-${SEASON}.json`;

// NFL team name mappings (from Pro-Football-Reference to our format)
const TEAM_MAPPINGS = {
  'Green Bay Packers': 'Green Bay Packers',
  'Philadelphia Eagles': 'Philadelphia Eagles',
  'Pittsburgh Steelers': 'Pittsburgh Steelers',
  'Atlanta Falcons': 'Atlanta Falcons',
  'Arizona Cardinals': 'Arizona Cardinals',
  'Buffalo Bills': 'Buffalo Bills',
  'Tennessee Titans': 'Tennessee Titans',
  'Chicago Bears': 'Chicago Bears',
  'New England Patriots': 'New England Patriots',
  'Cincinnati Bengals': 'Cincinnati Bengals',
  'Houston Texans': 'Houston Texans',
  'Indianapolis Colts': 'Indianapolis Colts',
  'Jacksonville Jaguars': 'Jacksonville Jaguars',
  'Miami Dolphins': 'Miami Dolphins',
  'Minnesota Vikings': 'Minnesota Vikings',
  'New York Giants': 'New York Giants',
  'Carolina Panthers': 'Carolina Panthers',
  'New Orleans Saints': 'New Orleans Saints',
  'Tampa Bay Buccaneers': 'Tampa Bay Buccaneers',
  'Washington Commanders': 'Washington Commanders',
  'Denver Broncos': 'Denver Broncos',
  'Seattle Seahawks': 'Seattle Seahawks',
  'Las Vegas Raiders': 'Las Vegas Raiders',
  'Los Angeles Chargers': 'Los Angeles Chargers',
  'Dallas Cowboys': 'Dallas Cowboys',
  'Cleveland Browns': 'Cleveland Browns',
  'Los Angeles Rams': 'Los Angeles Rams',
  'Detroit Lions': 'Detroit Lions',
  'Baltimore Ravens': 'Baltimore Ravens',
  'Kansas City Chiefs': 'Kansas City Chiefs',
  'New York Jets': 'New York Jets',
  'San Francisco 49ers': 'San Francisco 49ers'
};

// Helper function to make HTTPS requests
function makeRequest(url) {
  return new Promise((resolve, reject) => {
    https.get(url, (res) => {
      let data = '';
      res.on('data', (chunk) => data += chunk);
      res.on('end', () => resolve(data));
    }).on('error', reject);
  });
}

// Parse date and time from Pro-Football-Reference format
function parseGameDateTime(dateStr, timeStr) {
  try {
    // Handle different date formats
    let dateMatch, timeMatch;
    
    // Try full format first: "Thu, Sep 5, 2024"
    dateMatch = dateStr.match(/(\w+), (\w+) (\d+), (\d+)/);
    if (!dateMatch) {
      // Try simple format: "July 31" (for 2025 preseason)
      dateMatch = dateStr.match(/(\w+) (\d+)/);
      if (dateMatch) {
        // For 2025 preseason, assume 2025 year
        const [, month, day] = dateMatch;
        dateMatch = [null, null, month, day, '2025'];
      }
    }
    
    // Try time format: "8:20 PM"
    timeMatch = timeStr.match(/(\d+):(\d+) (AM|PM)/);
    
    if (!dateMatch || !timeMatch) {
      console.warn(`Could not parse date/time: ${dateStr} ${timeStr}`);
      return null;
    }
    
    const [, dayName, month, day, year] = dateMatch;
    const [, hour, minute, ampm] = timeMatch;
    
    // Convert month name to number
    const monthMap = {
      'Jan': 0, 'January': 0, 'Feb': 1, 'February': 1, 'Mar': 2, 'March': 2, 
      'Apr': 3, 'April': 3, 'May': 4, 'Jun': 5, 'June': 5,
      'Jul': 6, 'July': 6, 'Aug': 7, 'August': 7, 'Sep': 8, 'September': 8, 
      'Oct': 9, 'October': 9, 'Nov': 10, 'November': 10, 'Dec': 11, 'December': 11
    };
    
    const monthNum = monthMap[month];
    if (monthNum === undefined) {
      console.warn(`Unknown month: ${month}`);
      return null;
    }
    
    // Convert 12-hour to 24-hour format
    let hour24 = parseInt(hour);
    if (ampm === 'PM' && hour24 !== 12) hour24 += 12;
    if (ampm === 'AM' && hour24 === 12) hour24 = 0;
    
    // Create date in Eastern Time (NFL games are typically in ET)
    const date = new Date(year, monthNum, parseInt(day), hour24, parseInt(minute));
    
    // Convert to UTC (assuming Eastern Time)
    const utcDate = new Date(date.getTime() - (4 * 60 * 60 * 1000)); // EST is UTC-4
    
    return utcDate.toISOString();
  } catch (error) {
    console.error(`Error parsing date/time: ${dateStr} ${timeStr}`, error);
    return null;
  }
}

// Scrape NFL schedule from Pro-Football-Reference
async function scrapeNFLSchedule() {
  console.log(`Scraping NFL schedule for ${SEASON} season...`);
  
  const games = [];
  const url = `https://www.pro-football-reference.com/years/${SEASON}/games.htm`;
  
  try {
    const html = await makeRequest(url);
    const dom = new JSDOM(html);
    const document = dom.window.document;
    
    // Find the schedule table
    const table = document.querySelector('#games');
    if (!table) {
      throw new Error('Could not find games table');
    }
    
    const rows = table.querySelectorAll('tbody tr');
    console.log(`Found ${rows.length} game rows`);
    
    for (const row of rows) {
      const cells = row.querySelectorAll('td');
      if (cells.length < 8) continue;
      
      // Skip header rows or empty rows
      if (row.classList.contains('thead') || cells[0].textContent.trim() === '') {
        continue;
      }
      
      try {
        const weekText = cells[0].textContent.trim();
        const date = cells[1].textContent.trim();
        const awayTeam = cells[2].textContent.trim();
        const homeTeam = cells[5].textContent.trim();
        
        // Skip if we don't have valid data
        if (!weekText || !date || !awayTeam || !homeTeam) {
          continue;
        }
        
        // Convert week text to number (e.g., "Thu" -> 1, "Fri" -> 1, etc.)
        let week;
        if (weekText === 'Thu' || weekText === 'Fri' || weekText === 'Sat' || weekText === 'Sun' || weekText === 'Mon') {
          week = 1; // Preseason games are typically week 1
        } else {
          week = parseInt(weekText);
        }
        
        if (!week) {
          continue;
        }
        
        // Map team names to our format
        const mappedAwayTeam = TEAM_MAPPINGS[awayTeam];
        const mappedHomeTeam = TEAM_MAPPINGS[homeTeam];
        
        if (!mappedAwayTeam || !mappedHomeTeam) {
          console.warn(`Unknown team: ${awayTeam} or ${homeTeam}`);
          continue;
        }
        
        // Parse kickoff time (no time provided, use default 8:00 PM)
        const kickoffTime = parseGameDateTime(date, "8:00 PM");
        if (!kickoffTime) {
          console.warn(`Skipping game due to date/time parsing error: ${awayTeam} @ ${homeTeam}`);
          continue;
        }
        
        const game = {
          week: week,
          awayTeam: mappedAwayTeam,
          homeTeam: mappedHomeTeam,
          kickoffTime: kickoffTime,
          winningTeam: "",
          scored: false
        };
        
        games.push(game);
        console.log(`Parsed: Week ${week} - ${awayTeam} @ ${homeTeam} (${kickoffTime})`);
        
      } catch (error) {
        console.error('Error parsing row:', error);
        continue;
      }
    }
    
    console.log(`Successfully parsed ${games.length} games`);
    return games;
    
  } catch (error) {
    console.error('Error scraping NFL schedule:', error);
    throw error;
  }
}

// Load games into database via API
async function loadGamesToDatabase(games) {
  console.log(`Loading ${games.length} games into database...`);
  
  let successCount = 0;
  let errorCount = 0;
  
  for (const game of games) {
    try {
      const response = await fetch(`${API_BASE}/games`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(game),
      });
      
      if (response.ok) {
        const savedGame = await response.json();
        console.log(`✅ Added: ${game.awayTeam} @ ${game.homeTeam} (Week ${game.week})`);
        successCount++;
      } else {
        console.error(`❌ Failed to add: ${game.awayTeam} @ ${game.homeTeam} - Status: ${response.status}`);
        errorCount++;
      }
      
      // Add a small delay to avoid overwhelming the API
      await new Promise(resolve => setTimeout(resolve, 100));
      
    } catch (error) {
      console.error(`❌ Error adding game: ${game.awayTeam} @ ${game.homeTeam} - ${error.message}`);
      errorCount++;
    }
  }
  
  console.log(`\nLoad Summary:`);
  console.log(`✅ Successfully loaded: ${successCount} games`);
  console.log(`❌ Failed to load: ${errorCount} games`);
  
  return { successCount, errorCount };
}

// Save games to JSON file
function saveGamesToFile(games) {
  try {
    fs.writeFileSync(OUTPUT_FILE, JSON.stringify(games, null, 2));
    console.log(`✅ Saved ${games.length} games to ${OUTPUT_FILE}`);
  } catch (error) {
    console.error('Error saving games to file:', error);
  }
}

// Main function
async function main() {
  try {
    console.log('🚀 NFL Schedule Scraper');
    console.log('========================');
    
    // Check if we should load from file or scrape
    const loadFromFile = process.argv.includes('--from-file');
    
    let games;
    
    if (loadFromFile && fs.existsSync(OUTPUT_FILE)) {
      console.log(`Loading games from ${OUTPUT_FILE}...`);
      const fileContent = fs.readFileSync(OUTPUT_FILE, 'utf8');
      games = JSON.parse(fileContent);
      console.log(`Loaded ${games.length} games from file`);
    } else {
      // Scrape the schedule
      games = await scrapeNFLSchedule();
      
      // Save to file for future use
      saveGamesToFile(games);
    }
    
    // Load games into database
    if (games.length > 0) {
      await loadGamesToDatabase(games);
    } else {
      console.log('No games to load');
    }
    
    console.log('\n🎉 NFL Schedule loading complete!');
    
  } catch (error) {
    console.error('❌ Error in main function:', error);
    process.exit(1);
  }
}

// Run the script
if (require.main === module) {
  main();
}

module.exports = { scrapeNFLSchedule, loadGamesToDatabase };

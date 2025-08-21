const fs = require('fs');
const https = require('https');
const { JSDOM } = require('jsdom');

// Configuration
const SEASON = process.env.SEASON || '2025';

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

// Debug function to examine the schedule
async function debugSchedule() {
  console.log(`🔍 Debugging NFL schedule for ${SEASON} season...`);
  
  const url = `https://www.pro-football-reference.com/years/${SEASON}/games.htm`;
  console.log(`📡 Fetching from: ${url}`);
  
  try {
    const html = await makeRequest(url);
    const dom = new JSDOM(html);
    const document = dom.window.document;
    
    // Find the schedule table
    const table = document.querySelector('#games');
    if (!table) {
      console.error('❌ Could not find games table');
      return;
    }
    
    const rows = table.querySelectorAll('tbody tr');
    console.log(`📊 Found ${rows.length} game rows`);
    
    // Examine first few rows to understand structure
    console.log('\n🔍 Examining first 5 rows:');
    for (let i = 0; i < Math.min(5, rows.length); i++) {
      const row = rows[i];
      const cells = row.querySelectorAll('td');
      
      console.log(`\nRow ${i + 1}:`);
      console.log(`  Classes: ${row.className}`);
      console.log(`  Cell count: ${cells.length}`);
      
      if (cells.length > 0) {
        console.log(`  Cell 0 (Week): "${cells[0].textContent.trim()}"`);
        console.log(`  Cell 1 (Date): "${cells[1].textContent.trim()}"`);
        console.log(`  Cell 2 (Time): "${cells[2].textContent.trim()}"`);
        console.log(`  Cell 3 (Away): "${cells[3].textContent.trim()}"`);
        console.log(`  Cell 4 (Away Score): "${cells[4].textContent.trim()}"`);
        console.log(`  Cell 5 (Home): "${cells[5].textContent.trim()}"`);
        console.log(`  Cell 6 (Home Score): "${cells[6].textContent.trim()}"`);
      }
    }
    
    // Check for any games that might be parseable
    console.log('\n🔍 Looking for parseable games:');
    let parseableCount = 0;
    let teamMismatchCount = 0;
    let dateTimeErrorCount = 0;
    let missingDataCount = 0;
    
    for (let i = 0; i < rows.length; i++) {
      const row = rows[i];
      const cells = row.querySelectorAll('td');
      
      if (cells.length < 8) {
        missingDataCount++;
        continue;
      }
      
      // Skip header rows or empty rows
      if (row.classList.contains('thead') || cells[0].textContent.trim() === '') {
        continue;
      }
      
      const week = cells[0].textContent.trim();
      const date = cells[1].textContent.trim();
      const time = cells[2].textContent.trim();
      const awayTeam = cells[3].textContent.trim();
      const homeTeam = cells[5].textContent.trim();
      
      // Check if we have valid data
      if (!week || !date || !awayTeam || !homeTeam) {
        missingDataCount++;
        continue;
      }
      
      // Check if teams are in our mappings
      const teamMappings = {
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
      
      if (!teamMappings[awayTeam] || !teamMappings[homeTeam]) {
        teamMismatchCount++;
        if (teamMismatchCount <= 5) {
          console.log(`  ❌ Team mismatch: "${awayTeam}" or "${homeTeam}" not in mappings`);
        }
        continue;
      }
      
      // Check date/time parsing
      try {
        const dateMatch = date.match(/(\w+), (\w+) (\d+), (\d+)/);
        const timeMatch = time.match(/(\d+):(\d+) (AM|PM)/);
        
        if (!dateMatch || !timeMatch) {
          dateTimeErrorCount++;
          if (dateTimeErrorCount <= 5) {
            console.log(`  ❌ Date/time parsing error: "${date}" "${time}"`);
          }
          continue;
        }
        
        parseableCount++;
        if (parseableCount <= 5) {
          console.log(`  ✅ Parseable: Week ${week} - ${awayTeam} @ ${homeTeam} (${date} ${time})`);
        }
        
      } catch (error) {
        dateTimeErrorCount++;
        if (dateTimeErrorCount <= 5) {
          console.log(`  ❌ Date/time parsing exception: ${error.message}`);
        }
      }
    }
    
    console.log('\n📈 Summary:');
    console.log(`  Total rows: ${rows.length}`);
    console.log(`  Parseable games: ${parseableCount}`);
    console.log(`  Team mismatches: ${teamMismatchCount}`);
    console.log(`  Date/time errors: ${dateTimeErrorCount}`);
    console.log(`  Missing data: ${missingDataCount}`);
    
    // Save raw HTML for inspection
    fs.writeFileSync(`debug-${SEASON}-schedule.html`, html);
    console.log(`\n💾 Saved raw HTML to debug-${SEASON}-schedule.html for inspection`);
    
  } catch (error) {
    console.error('❌ Error debugging schedule:', error);
  }
}

// Run the debug function
debugSchedule();

const https = require('https');
const { JSDOM } = require('jsdom');

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

async function debugScraper() {
  console.log('🔍 Debugging Pro-Football-Reference HTML structure...');
  
  const url = 'https://www.pro-football-reference.com/years/2024/games.htm';
  
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
    
    // Examine the first few rows to understand the structure
    for (let i = 0; i < Math.min(5, rows.length); i++) {
      const row = rows[i];
      const cells = row.querySelectorAll('td');
      
      console.log(`\n--- Row ${i + 1} ---`);
      console.log(`Number of cells: ${cells.length}`);
      
      for (let j = 0; j < cells.length; j++) {
        const cell = cells[j];
        const text = cell.textContent.trim();
        const dataStat = cell.getAttribute('data-stat');
        console.log(`Cell ${j}: [${dataStat}] "${text}"`);
      }
    }
    
  } catch (error) {
    console.error('Error debugging scraper:', error);
  }
}

debugScraper();

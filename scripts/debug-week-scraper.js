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

// Debug function to examine the week information
async function debugWeekInfo() {
  console.log(`🔍 Debugging week information for ${SEASON} season...`);
  
  const url = `https://www.pro-football-reference.com/years/${SEASON}/games.htm`;
  console.log(`📡 Fetching from: ${url}`);
  
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
    console.log(`📊 Found ${rows.length} game rows\n`);
    
    console.log('🔍 Examining first 10 rows for week information:\n');
    
    for (let i = 0; i < Math.min(10, rows.length); i++) {
      const row = rows[i];
      const cells = row.querySelectorAll('td');
      
      console.log(`Row ${i + 1}:`);
      console.log(`  Classes: "${row.className}"`);
      console.log(`  Cell count: ${cells.length}`);
      
      // Look for the week in different ways
      const weekTh = row.querySelector('th[data-stat="week_num"]');
      const weekThAny = row.querySelector('th');
      const firstCell = cells[0];
      
      console.log(`  Week via th[data-stat="week_num"]: "${weekTh?.textContent?.trim() || 'NOT FOUND'}"`);
      console.log(`  Week via any th: "${weekThAny?.textContent?.trim() || 'NOT FOUND'}"`);
      if (weekThAny) {
        console.log(`    th attributes: ${Array.from(weekThAny.attributes).map(a => `${a.name}="${a.value}"`).join(', ')}`);
      }
      console.log(`  First td cell: "${firstCell?.textContent?.trim() || 'NOT FOUND'}"`);
      if (firstCell) {
        console.log(`    td attributes: ${Array.from(firstCell.attributes).map(a => `${a.name}="${a.value}"`).join(', ')}`);
      }
      
      // Show all cells for this row
      console.log(`  All cells:`);
      cells.forEach((cell, idx) => {
        const attrs = Array.from(cell.attributes).map(a => `${a.name}="${a.value}"`).join(', ');
        console.log(`    Cell ${idx}: "${cell.textContent.trim()}" (${attrs})`);
      });
      
      console.log('');
    }
    
    // Save a sample of the HTML for inspection
    const tableHtml = table.outerHTML;
    fs.writeFileSync('debug-table-structure.html', tableHtml);
    console.log('💾 Saved table structure to debug-table-structure.html');
    
  } catch (error) {
    console.error('❌ Error:', error);
  }
}

// Run the debug function
debugWeekInfo();

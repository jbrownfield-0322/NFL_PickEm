// Test script for The Odds API integration
// Run this to verify the API connection works

const API_KEY = 'your_api_key_here'; // Replace with your actual API key
const BASE_URL = 'https://api.the-odds-api.com';

async function testOddsAPI() {
    console.log('Testing The Odds API integration...\n');
    
    try {
        // Test 1: Check API key validity
        console.log('1. Testing API key validity...');
        const response = await fetch(`${BASE_URL}/v4/sports?apiKey=${API_KEY}`);
        
        if (!response.ok) {
            throw new Error(`API key test failed: ${response.status} ${response.statusText}`);
        }
        
        const sports = await response.json();
        console.log(`✅ API key valid. Available sports: ${sports.length}`);
        sports.forEach(sport => {
            if (sport.title.includes('Football')) {
                console.log(`   - ${sport.title} (${sport.key})`);
            }
        });
        
        // Test 2: Fetch NFL odds
        console.log('\n2. Testing NFL odds fetch...');
        const oddsResponse = await fetch(
            `${BASE_URL}/v4/sports/americanfootball_nfl/odds?apiKey=${API_KEY}&regions=us&markets=spreads,totals&oddsFormat=american`
        );
        
        if (!oddsResponse.ok) {
            throw new Error(`Odds fetch failed: ${oddsResponse.status} ${oddsResponse.statusText}`);
        }
        
        const odds = await oddsResponse.json();
        console.log(`✅ Successfully fetched ${odds.length} NFL games with odds`);
        
        if (odds.length > 0) {
            const sampleGame = odds[0];
            console.log('\nSample game data:');
            console.log(`   Away Team: ${sampleGame.away_team}`);
            console.log(`   Home Team: ${sampleGame.home_team}`);
            console.log(`   Start Time: ${sampleGame.commence_time}`);
            
            if (sampleGame.bookmakers && sampleGame.bookmakers.length > 0) {
                const bookmaker = sampleGame.bookmakers[0];
                console.log(`   Bookmaker: ${bookmaker.title}`);
                
                if (bookmaker.markets) {
                    bookmaker.markets.forEach(market => {
                        console.log(`   Market: ${market.key}`);
                        if (market.outcomes) {
                            market.outcomes.forEach(outcome => {
                                console.log(`     ${outcome.name}: ${outcome.point || 'N/A'} (${outcome.price})`);
                            });
                        }
                    });
                }
            }
        }
        
        // Test 3: Check remaining requests
        const remainingRequests = oddsResponse.headers.get('x-requests-remaining');
        console.log(`\n3. Remaining API requests this month: ${remainingRequests || 'Unknown'}`);
        
        console.log('\n✅ All tests passed! The Odds API integration is working correctly.');
        
    } catch (error) {
        console.error('\n❌ Test failed:', error.message);
        console.log('\nTroubleshooting tips:');
        console.log('1. Verify your API key is correct');
        console.log('2. Check your internet connection');
        console.log('3. Ensure you have remaining API requests');
        console.log('4. Check the API status at https://the-odds-api.com/status');
    }
}

// Run the test
testOddsAPI();

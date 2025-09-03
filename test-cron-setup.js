// Test script for cron job setup verification
// This helps verify the 4-hour scheduling logic

function testCronTiming() {
    console.log('Testing 4-hour cron job timing...\n');
    
    const now = new Date();
    const currentHour = now.getHours();
    
    console.log(`Current time: ${now.toLocaleString()}`);
    console.log(`Current hour: ${currentHour}`);
    
    // Calculate next 4-hour mark (0, 4, 8, 12, 16, 20)
    const nextHour = Math.ceil((currentHour + 1) / 4) * 4;
    const nextUpdate = new Date(now);
    
    if (nextHour >= 24) {
        nextUpdate.setDate(nextUpdate.getDate() + 1);
        nextUpdate.setHours(0, 0, 0, 0);
    } else {
        nextUpdate.setHours(nextHour, 0, 0, 0);
    }
    
    const timeUntilNext = nextUpdate - now;
    const hoursUntilNext = Math.floor(timeUntilNext / (1000 * 60 * 60));
    const minutesUntilNext = Math.floor((timeUntilNext % (1000 * 60 * 60)) / (1000 * 60));
    
    console.log(`Next scheduled update: ${nextUpdate.toLocaleString()}`);
    console.log(`Time until next update: ${hoursUntilNext}h ${minutesUntilNext}m`);
    
    // Show all 4-hour marks for today
    console.log('\nAll 4-hour marks for today:');
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    for (let hour = 0; hour < 24; hour += 4) {
        const mark = new Date(today);
        mark.setHours(hour, 0, 0, 0);
        
        const isPast = mark < now;
        const isNext = mark.getTime() === nextUpdate.getTime();
        
        let status = isPast ? '✓ Past' : '⏳ Future';
        if (isNext) status = '🎯 NEXT';
        
        console.log(`  ${hour.toString().padStart(2, '0')}:00 - ${status}`);
    }
    
    // Show cron expression
    console.log('\nCron Expression: 0 0 */4 * * *');
    console.log('This means: At minute 0, hour 0, every 4 hours, every day, every month, every day of week');
    console.log('Execution times: 00:00, 04:00, 08:00, 12:00, 16:00, 20:00');
    
    // Show game day scheduling
    console.log('\nGame Day Scheduling (Thu, Sun, Mon):');
    console.log('Cron: 0 0 * * * THU,SUN,MON');
    console.log('This means: Every hour on Thursday, Sunday, and Monday');
    console.log('Purpose: More frequent updates on days when games are played');
}

// Run the test
testCronTiming();

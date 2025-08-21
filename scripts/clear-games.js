const { Client } = require('pg');

// Database configuration
let dbConfig;

// Check if we have a DATABASE_URL (Railway format)
if (process.env.DATABASE_URL) {
  console.log('🔗 Using DATABASE_URL connection string');
  dbConfig = {
    connectionString: process.env.DATABASE_URL,
    ssl: { rejectUnauthorized: false }
  };
} else {
  // Fallback to individual environment variables
  console.log('🔗 Using individual environment variables');
  dbConfig = {
    host: process.env.DB_HOST || 'localhost',
    port: process.env.DB_PORT || 5432,
    database: process.env.DB_NAME || 'nflpickem',
    user: process.env.DB_USER || 'postgres',
    password: process.env.DB_PASSWORD || '',
    ssl: process.env.DB_SSL === 'true' ? { rejectUnauthorized: false } : false
  };
}

async function clearGames() {
  const client = new Client(dbConfig);
  
  try {
    console.log('🔌 Connecting to PostgreSQL database...');
    await client.connect();
    console.log('✅ Connected to database successfully');
    
    // First, let's check if the game table exists
    const tableExistsResult = await client.query(`
      SELECT EXISTS (
        SELECT FROM information_schema.tables 
        WHERE table_schema = 'public' 
        AND table_name = 'game'
      );
    `);
    
    if (!tableExistsResult.rows[0].exists) {
      console.log('❌ Game table does not exist in the database');
      console.log('\n📋 Available tables:');
      const tablesResult = await client.query(`
        SELECT table_name 
        FROM information_schema.tables 
        WHERE table_schema = 'public'
        ORDER BY table_name;
      `);
      
      if (tablesResult.rows.length === 0) {
        console.log('  No tables found in the database');
      } else {
        tablesResult.rows.forEach(row => {
          console.log(`  - ${row.table_name}`);
        });
      }
      return;
    }
    
    // Check how many games exist
    const countResult = await client.query('SELECT COUNT(*) FROM game');
    const gameCount = parseInt(countResult.rows[0].count);
    console.log(`📊 Found ${gameCount} games in the database`);
    
    if (gameCount === 0) {
      console.log('ℹ️  No games to delete');
      return;
    }
    
    // Ask for confirmation
    console.log('\n⚠️  WARNING: This will delete ALL games from the database!');
    console.log('This action cannot be undone.');
    
    // For safety, we'll require explicit confirmation
    if (process.argv.includes('--force')) {
      console.log('🚨 Force flag detected - proceeding with deletion...');
    } else {
      console.log('\nTo proceed, run this script with the --force flag:');
      console.log('node clear-games.js --force');
      console.log('\nOr set the environment variable:');
      console.log('FORCE_DELETE=true node clear-games.js');
      return;
    }
    
    // Delete all games
    console.log('\n🗑️  Deleting all games...');
    const deleteResult = await client.query('DELETE FROM game');
    console.log(`✅ Successfully deleted ${deleteResult.rowCount} games`);
    
    // Verify deletion
    const verifyResult = await client.query('SELECT COUNT(*) FROM game');
    const remainingGames = parseInt(verifyResult.rows[0].count);
    console.log(`📊 Remaining games: ${remainingGames}`);
    
    if (remainingGames === 0) {
      console.log('🎉 All games have been successfully deleted!');
    } else {
      console.log('⚠️  Some games may still exist - check your database');
    }
    
  } catch (error) {
    console.error('❌ Error:', error.message);
    console.error('Stack trace:', error.stack);
  } finally {
    await client.end();
    console.log('🔌 Database connection closed');
  }
}

// Check for force flag or environment variable
if (process.argv.includes('--force') || process.env.FORCE_DELETE === 'true') {
  clearGames();
} else {
  console.log('🚀 Game Clear Script');
  console.log('===================');
  console.log('This script will delete ALL games from your PostgreSQL database.');
  console.log('');
  console.log('Database Configuration:');
  console.log(`  Host: ${dbConfig.host}`);
  console.log(`  Port: ${dbConfig.port}`);
  console.log(`  Database: ${dbConfig.database}`);
  console.log(`  User: ${dbConfig.user}`);
  console.log(`  SSL: ${dbConfig.ssl ? 'Enabled' : 'Disabled'}`);
  console.log('');
  console.log('Environment Variables:');
  console.log('  DATABASE_URL - Full PostgreSQL connection URL (Railway format)');
  console.log('  OR individual variables:');
  console.log('    DB_HOST - Database host (default: localhost)');
  console.log('    DB_PORT - Database port (default: 5432)');
  console.log('    DB_NAME - Database name (default: nflpickem)');
  console.log('    DB_USER - Database user (default: postgres)');
  console.log('    DB_PASSWORD - Database password');
  console.log('    DB_SSL - Enable SSL (true/false)');
  console.log('');
  console.log('Usage:');
  console.log('  node clear-games.js --force');
  console.log('  FORCE_DELETE=true node clear-games.js');
  console.log('');
  console.log('⚠️  This action cannot be undone!');
}

#!/bin/bash

echo "=== Railway Deployment Debug Script ==="
echo ""

echo "1. Checking Railway CLI installation..."
if command -v railway &> /dev/null; then
    echo "✅ Railway CLI is installed"
    railway --version
else
    echo "❌ Railway CLI not found. Install with: npm install -g @railway/cli"
fi

echo ""
echo "2. Checking Railway login status..."
if railway whoami &> /dev/null; then
    echo "✅ Logged into Railway"
    railway whoami
else
    echo "❌ Not logged into Railway. Run: railway login"
fi

echo ""
echo "3. Checking project status..."
if railway status &> /dev/null; then
    echo "✅ Project found"
    railway status
else
    echo "❌ No project found or not in project directory"
fi

echo ""
echo "4. Checking recent deployments..."
if railway deployments &> /dev/null; then
    echo "✅ Recent deployments:"
    railway deployments --limit 3
else
    echo "❌ Could not fetch deployments"
fi

echo ""
echo "5. Checking environment variables..."
if railway variables &> /dev/null; then
    echo "✅ Environment variables:"
    railway variables
else
    echo "❌ Could not fetch environment variables"
fi

echo ""
echo "6. Checking logs..."
if railway logs &> /dev/null; then
    echo "✅ Recent logs:"
    railway logs --limit 20
else
    echo "❌ Could not fetch logs"
fi

echo ""
echo "=== Debug Complete ==="
echo ""
echo "Common issues and solutions:"
echo "1. If JAR file not found: Check if Maven build succeeded"
echo "2. If port binding fails: Check if PORT environment variable is set"
echo "3. If database connection fails: Check DATABASE_URL environment variable"
echo "4. If health check fails: Check if application starts successfully"
echo ""
echo "To view real-time logs: railway logs --follow"
echo "To restart deployment: railway up"

#!/bin/bash

echo "Building React frontend..."

# Navigate to frontend directory
cd frontend

# Install dependencies if needed
if [ ! -d "node_modules" ]; then
    echo "Installing frontend dependencies..."
    npm install
fi

# Build the React app
echo "Building React app..."
npm run build

# Check if build was successful
if [ $? -eq 0 ]; then
    echo "React build successful!"
    
    # Navigate back to root
    cd ..
    
    # Create static directory if it doesn't exist
    mkdir -p src/main/resources/static
    
    # Copy built files to Spring Boot static directory
    echo "Copying built files to Spring Boot static directory..."
    cp -r frontend/build/* src/main/resources/static/
    
    echo "Frontend successfully built and copied to Spring Boot!"
    echo "You can now build and deploy your Spring Boot application."
else
    echo "React build failed!"
    exit 1
fi

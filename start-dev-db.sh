#!/bin/bash

echo "🔧 Starting MongoDB for local development..."

# Check if MongoDB is already running
if pgrep -x "mongod" > /dev/null; then
    echo "✅ MongoDB is already running"
    exit 0
fi

# Try to start MongoDB using systemd
if command -v systemctl &> /dev/null; then
    echo "Starting MongoDB with systemctl..."
    sudo systemctl start mongod
    if [ $? -eq 0 ]; then
        echo "✅ MongoDB started successfully"
        exit 0
    fi
fi

# Try to start MongoDB using service
if command -v service &> /dev/null; then
    echo "Starting MongoDB with service..."
    sudo service mongod start
    if [ $? -eq 0 ]; then
        echo "✅ MongoDB started successfully"
        exit 0
    fi
fi

# If neither worked, provide instructions
echo "❌ Could not start MongoDB automatically."
echo "Please start MongoDB manually:"
echo "  sudo systemctl start mongod"
echo "  OR"
echo "  sudo service mongod start"
exit 1


#!/bin/bash

# Colors for terminal output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Sonic Java Playground UI Launcher ===${NC}"

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo -e "${RED}Error: Java is not installed or not in PATH${NC}"
    exit 1
fi

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}Error: Maven is not installed or not in PATH${NC}"
    exit 1
fi

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo -e "${RED}Error: Node.js is not installed or not in PATH${NC}"
    exit 1
fi

# Check if npm is installed
if ! command -v npm &> /dev/null; then
    echo -e "${RED}Error: npm is not installed or not in PATH${NC}"
    exit 1
fi

# Build the backend
echo -e "${GREEN}Building the backend...${NC}"
mvn clean package

if [ $? -ne 0 ]; then
    echo -e "${RED}Error: Failed to build the backend${NC}"
    exit 1
fi

# Install frontend dependencies
echo -e "${GREEN}Installing frontend dependencies...${NC}"
cd frontend
npm install

if [ $? -ne 0 ]; then
    echo -e "${RED}Error: Failed to install frontend dependencies${NC}"
    exit 1
fi

cd ..

# Start the backend and frontend
echo -e "${GREEN}Starting the backend and frontend...${NC}"
echo -e "${BLUE}Backend will be available at http://localhost:8008${NC}"
echo -e "${BLUE}Frontend will be available at http://localhost:3000${NC}"

# Start the backend in the background
java -jar target/NovaSonicPlayground-1.0-SNAPSHOT.jar &
BACKEND_PID=$!

# Start the frontend
cd frontend
npm start &
FRONTEND_PID=$!

# Function to handle script termination
function cleanup {
    echo -e "${GREEN}Stopping the backend and frontend...${NC}"
    kill $BACKEND_PID
    kill $FRONTEND_PID
    exit 0
}

# Register the cleanup function for script termination
trap cleanup SIGINT SIGTERM

# Wait for user input to stop the servers
echo -e "${GREEN}Press Ctrl+C to stop the servers${NC}"
wait

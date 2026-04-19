#!/bin/bash
# MCP Server Quick Commands

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║       MCP Server - Quick Setup & Testing Commands             ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Function to print section
print_section() {
    echo -e "${YELLOW}$1${NC}"
    echo "─────────────────────────────────────────────────────────────────"
}

# 1. Database Setup
print_section "1️⃣  DATABASE SETUP"
cat << 'EOF'
# Create test database (run once)
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS mcp_demo_test;"

# Verify databases exist
mysql -u root -p -e "SHOW DATABASES LIKE 'mcp_demo%';"

# Check test database tables
mysql -u root -p -e "USE mcp_demo_test; SHOW TABLES;"
EOF
echo ""

# 2. Build & Compile
print_section "2️⃣  BUILD & COMPILE"
cat << 'EOF'
# Clean build
./mvnw clean compile

# Quick compile
./mvnw compile

# Build JAR
./mvnw clean package -DskipTests
EOF
echo ""

# 3. Run Tests
print_section "3️⃣  RUN TESTS"
cat << 'EOF'
# Run all tests
./mvnw clean test

# Run specific test class
./mvnw test -Dtest=EmployeeRepositoryTest
./mvnw test -Dtest=ProductRepositoryTest
./mvnw test -Dtest=OrderRepositoryTest
./mvnw test -Dtest=EmployeeToolsTest
./mvnw test -Dtest=McpServerApplicationTests
./mvnw test -Dtest=McpServerConfigTests

# Run specific test method
./mvnw test -Dtest=EmployeeRepositoryTest#testFindByEmail

# Run with verbose output
./mvnw test -X

# Run with debug logging
./mvnw test -Dorg.slf4j.simpleLogger.defaultLogLevel=debug
EOF
echo ""

# 4. Test Coverage
print_section "4️⃣  TEST COVERAGE"
cat << 'EOF'
# Generate coverage report
./mvnw clean test jacoco:report

# View coverage report (after generation)
open target/site/jacoco/index.html
EOF
echo ""

# 5. Run Server
print_section "5️⃣  RUN MCP SERVER"
cat << 'EOF'
# Run in development mode
./mvnw spring-boot:run

# Run with custom port
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=9090"

# Run JAR directly
java -jar target/mcp-server-0.0.1-SNAPSHOT.jar

# Run JAR on custom port
java -jar target/mcp-server-0.0.1-SNAPSHOT.jar --server.port=9090
EOF
echo ""

# 6. Health Checks
print_section "6️⃣  HEALTH CHECKS"
cat << 'EOF'
# Check server health
curl http://localhost:8080/actuator/health

# Test SSE endpoint (Ctrl+C to stop)
curl -N http://localhost:8080/sse

# Check with verbose output
curl -v http://localhost:8080/actuator/health
EOF
echo ""

# 7. Database Operations
print_section "7️⃣  DATABASE OPERATIONS"
cat << 'EOF'
# Connect to MySQL
mysql -u root -p

# View test database
mysql -u root -p -e "USE mcp_demo_test; SELECT COUNT(*) as employee_count FROM employees;"

# Clear test database (tables auto-recreate)
mysql -u root -p -e "DROP DATABASE mcp_demo_test; CREATE DATABASE mcp_demo_test;"

# Export data
mysqldump -u root -p mcp_demo > backup_production.sql
mysqldump -u root -p mcp_demo_test > backup_test.sql

# Import data
mysql -u root -p mcp_demo < backup_production.sql
EOF
echo ""

# 8. Useful Maven Commands
print_section "8️⃣  USEFUL MAVEN COMMANDS"
cat << 'EOF'
# Skip tests during build
./mvnw clean package -DskipTests

# Force update dependencies
./mvnw -U clean compile

# Clear local Maven cache
rm -rf ~/.m2/repository

# List dependencies
./mvnw dependency:tree

# Check for dependency issues
./mvnw dependency:check
EOF
echo ""

# 9. Logging & Debugging
print_section "9️⃣  LOGGING & DEBUGGING"
cat << 'EOF'
# View application logs (in production)
tail -f logs/application.log

# Filter logs
cat logs/application.log | grep "ERROR"

# Real-time log monitoring with grep
./mvnw spring-boot:run | grep -i "error\|warn\|info"

# Debug mode
./mvnw spring-boot:run --debug
EOF
echo ""

# 10. Common Issues & Fixes
print_section "🔟 COMMON ISSUES & FIXES"
cat << 'EOF'
# Issue: Port already in use
# Fix: Kill process on port 8080
lsof -ti:8080 | xargs kill -9

# Issue: MySQL connection refused
# Fix: Check MySQL status
brew services list  # macOS
sudo systemctl status mysql  # Linux

# Issue: Tests timeout
# Fix: Run tests with longer timeout
./mvnw test -DargLine="-Dtimeout=60000"

# Issue: Database not found
# Fix: Create database
mysql -u root -p -e "CREATE DATABASE mcp_demo_test;"

# Issue: Foreign key constraint fails
# Fix: Drop and recreate
mysql -u root -p -e "DROP DATABASE mcp_demo_test; CREATE DATABASE mcp_demo_test;"
EOF
echo ""

# 11. Quick Start
print_section "⚡ QUICK START (Copy & Paste)"
cat << 'EOF'
# 1. Create test database
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS mcp_demo_test;"

# 2. Run all tests
./mvnw clean test

# 3. Start MCP Server
./mvnw spring-boot:run

# 4. In another terminal, test health
curl http://localhost:8080/actuator/health

# Done! MCP Server is running on http://localhost:8080
EOF
echo ""

echo -e "${GREEN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}✅ Commands ready! Copy and paste as needed.${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════════════════${NC}"
echo ""

# Show current directory
echo -e "${BLUE}Current Directory:${NC}"
pwd
echo ""

# Show file structure
echo -e "${BLUE}Project Structure:${NC}"
tree -L 2 -I 'target' 2>/dev/null || find . -maxdepth 2 -type d | head -20


#!/bin/bash

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║    Anthropic API Key Setup for MCP Server                  ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Check if API key is already set
if [ -n "$ANTHROPIC_API_KEY" ]; then
    echo -e "${GREEN}✓ ANTHROPIC_API_KEY is already set${NC}"
    echo -e "${YELLOW}  (showing first 20 chars): ${ANTHROPIC_API_KEY:0:20}...${NC}"
    echo ""
    echo -e "${BLUE}Do you want to update it? (y/n)${NC}"
    read -r response
    if [ "$response" != "y" ] && [ "$response" != "Y" ]; then
        echo "Using existing API key"
        exit 0
    fi
fi

echo ""
echo -e "${YELLOW}Step 1: Get Your API Key${NC}"
echo "  1. Visit: https://console.anthropic.com/account/keys"
echo "  2. Sign in to your Anthropic account"
echo "  3. Click 'Create Key'"
echo "  4. Copy the key (starts with sk-ant-)"
echo ""

echo -e "${YELLOW}Step 2: Paste Your API Key${NC}"
echo -e "${BLUE}Enter your Anthropic API key:${NC}"
read -r API_KEY

# Validate API key format
if [[ ! $API_KEY =~ ^sk-ant- ]]; then
    echo -e "${RED}✗ Invalid API key format${NC}"
    echo "  API key should start with 'sk-ant-'"
    exit 1
fi

if [ -z "$API_KEY" ]; then
    echo -e "${RED}✗ No API key provided${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}Step 3: Choose Setup Method${NC}"
echo ""
echo "1) Temporary (current session only)"
echo "   export ANTHROPIC_API_KEY=\"$API_KEY\""
echo ""
echo "2) Permanent (add to ~/.zshrc)"
echo "   Add: export ANTHROPIC_API_KEY=\"$API_KEY\""
echo ""
echo "3) Both temporary and permanent"
echo ""

read -p "Select option (1/2/3): " choice

case $choice in
  1)
    export ANTHROPIC_API_KEY="$API_KEY"
    echo ""
    echo -e "${GREEN}✓ API key set for current session${NC}"
    echo ""
    ;;
  2)
    if grep -q "ANTHROPIC_API_KEY" ~/.zshrc 2>/dev/null; then
      # Update existing entry
      sed -i '' "s/export ANTHROPIC_API_KEY=.*/export ANTHROPIC_API_KEY=\"$API_KEY\"/" ~/.zshrc
      echo -e "${GREEN}✓ Updated ANTHROPIC_API_KEY in ~/.zshrc${NC}"
    else
      # Add new entry
      echo "export ANTHROPIC_API_KEY=\"$API_KEY\"" >> ~/.zshrc
      echo -e "${GREEN}✓ Added ANTHROPIC_API_KEY to ~/.zshrc${NC}"
    fi
    echo ""
    echo -e "${YELLOW}Please run: source ~/.zshrc${NC}"
    echo "Or restart your terminal"
    exit 0
    ;;
  3)
    # Set for current session
    export ANTHROPIC_API_KEY="$API_KEY"

    # Add to ~/.zshrc
    if grep -q "ANTHROPIC_API_KEY" ~/.zshrc 2>/dev/null; then
      sed -i '' "s/export ANTHROPIC_API_KEY=.*/export ANTHROPIC_API_KEY=\"$API_KEY\"/" ~/.zshrc
      echo -e "${GREEN}✓ Updated ANTHROPIC_API_KEY in ~/.zshrc${NC}"
    else
      echo "export ANTHROPIC_API_KEY=\"$API_KEY\"" >> ~/.zshrc
      echo -e "${GREEN}✓ Added ANTHROPIC_API_KEY to ~/.zshrc${NC}"
    fi
    echo -e "${GREEN}✓ API key set for current session${NC}"
    echo ""
    ;;
  *)
    echo -e "${RED}✗ Invalid option${NC}"
    exit 1
    ;;
esac

echo ""
echo -e "${YELLOW}Step 4: Verify Setup${NC}"
if [ -n "$ANTHROPIC_API_KEY" ]; then
    echo -e "${GREEN}✓ ANTHROPIC_API_KEY is set${NC}"
    echo "  First 20 chars: ${ANTHROPIC_API_KEY:0:20}..."
else
    echo -e "${RED}✗ ANTHROPIC_API_KEY is not set${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}Step 5: Start the Server${NC}"
echo ""
echo -e "${BLUE}Option A - Using Maven:${NC}"
echo "  cd /Volumes/A/MCP/mcp-server"
echo "  ./mvnw clean install"
echo "  ./mvnw spring-boot:run"
echo ""
echo -e "${BLUE}Option B - Using JAR:${NC}"
echo "  cd /Volumes/A/MCP/mcp-server"
echo "  ./mvnw clean package -DskipTests"
echo "  java -jar target/mcp-server-1.0.jar"
echo ""

echo -e "${GREEN}✓ Setup complete!${NC}"
echo ""
echo "Your API key is secure and ready to use."


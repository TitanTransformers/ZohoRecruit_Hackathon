# Deployment Guide

## Local Testing with Docker

### Build the Docker image
```bash
docker build -t mcp-recruitment:latest .
```

### Run with docker-compose (recommended)
```bash
# Create .env file with your credentials
cat > .env << EOF
ZOHO_RECRUIT_CLIENT_ID=your_client_id
ZOHO_RECRUIT_CLIENT_SECRET=your_client_secret
ZOHO_RECRUIT_REFRESH_TOKEN=your_refresh_token
ZOHO_RECRUIT_ORG_ID=your_org_id
ANTHROPIC_API_KEY=your_anthropic_key
ANTHROPIC_API_BASE_URL=https://api.anthropic.com/v1
EOF

# Start services
docker-compose up
```

### Run standalone
```bash
docker run -p 8080:8080 -p 8081:8081 \
  -e ZOHO_RECRUIT_CLIENT_ID=your_id \
  -e ZOHO_RECRUIT_CLIENT_SECRET=your_secret \
  -e ZOHO_RECRUIT_REFRESH_TOKEN=your_token \
  -e ZOHO_RECRUIT_ORG_ID=your_org \
  -e ANTHROPIC_API_KEY=your_key \
  mcp-recruitment:latest
```

## Deployment on Render

### Prerequisites
- GitHub repository with the code
- Render account (https://render.com)
- Zoho Recruit API credentials
- Anthropic API key

### Steps

1. **Connect GitHub Repository**
   - Go to https://render.com/dashboard
   - Click "New +" → "Web Service"
   - Select your GitHub repository
   - Render will detect the Dockerfile automatically

2. **Configure Environment**
   - In the service settings, add environment variables:
     - `ZOHO_RECRUIT_CLIENT_ID`
     - `ZOHO_RECRUIT_CLIENT_SECRET`
     - `ZOHO_RECRUIT_REFRESH_TOKEN`
     - `ZOHO_RECRUIT_ORG_ID`
     - `ANTHROPIC_API_KEY`
     - `ANTHROPIC_API_BASE_URL` = `https://api.anthropic.com/v1`

3. **Deploy**
   - Click "Create Web Service"
   - Render will build and deploy automatically
   - Both services will be accessible at:
     - MCP Server: `https://<service-name>.onrender.com:8080`
     - MCP Client: `https://<service-name>.onrender.com:8081`

### Notes
- Render's free tier may have resource limitations; consider upgrading for production
- The Docker build takes ~2-3 minutes on Render's infrastructure
- Health checks are configured to monitor MCP Server availability
- Both services run in the same container for simplicity

## Logs

### Local (Docker)
```bash
# View logs
docker-compose logs -f

# View specific service
docker-compose logs -f mcp-server
```

### Render
- Logs available in the Render dashboard
- Use the "Logs" tab in your service settings

## Troubleshooting

### Services not starting
- Check environment variables are set correctly
- Verify API credentials are valid
- Review logs for specific error messages

### Port conflicts
- Ensure ports 8080 and 8081 are not in use locally
- Render automatically exposes the service URL

### Build failures
- Check Maven build logs
- Ensure all dependencies are properly specified in pom.xml files
- Verify Java 21 compatibility

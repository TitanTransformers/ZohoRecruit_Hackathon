package com.mcp.mcp_server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.mcp_server.config.ZohoRecruitPortalConnectionConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Zoho Recruit OAuth 2.0 Service
 * Handles authentication with Zoho Recruit API using Client Credentials Grant
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZohoRecruitOAuthService {

    private final ZohoRecruitPortalConnectionConfig zhoRecruitConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private volatile String accessToken;
    private volatile long tokenExpiryTime;
    private static final long TOKEN_REFRESH_BUFFER_SECONDS = 60; // Refresh token 1 minute before expiry

    /**
     * Get valid OAuth access token for Zoho Recruit API
     * Returns cached token if valid, otherwise fetches a new one
     */
    public String getAccessToken() {
        if (isTokenValid()) {
            log.debug("Using cached access token");
            return accessToken;
        }

        log.info("Fetching new access token from Zoho Recruit OAuth endpoint");
        return fetchNewAccessToken();
    }

    /**
     * Fetch new access token using Client Credentials flow
     */
    private String fetchNewAccessToken() {
        try {
            // Build OAuth request parameters
            String body = UriComponentsBuilder.newInstance()
                    .queryParam("grant_type", zhoRecruitConfig.getGrantType())
                    .queryParam("client_id", zhoRecruitConfig.getClientId())
                    .queryParam("client_secret", zhoRecruitConfig.getClientSecret())
                    .build()
                    .getQuery();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(java.util.Collections.singletonList(org.springframework.http.MediaType.APPLICATION_JSON));

            HttpEntity<String> request = new HttpEntity<>(body, headers);

            log.debug("Sending OAuth token request to: {}", zhoRecruitConfig.getOauthUrl());
            ResponseEntity<String> response = restTemplate.exchange(
                    zhoRecruitConfig.getOauthUrl(),
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());

                if (jsonResponse.has("access_token")) {
                    accessToken = jsonResponse.get("access_token").asText();

                    // Parse expiry time if available (typically 3600 seconds or 1 hour)
                    if (jsonResponse.has("expires_in")) {
                        long expiresInSeconds = jsonResponse.get("expires_in").asLong();
                        tokenExpiryTime = System.currentTimeMillis() + (expiresInSeconds * 1000);
                        log.info("Successfully obtained access token. Expires in {} seconds", expiresInSeconds);
                    } else {
                        // Default to 1 hour if not specified
                        tokenExpiryTime = System.currentTimeMillis() + (3600 * 1000);
                        log.info("Successfully obtained access token. Using default expiry of 1 hour");
                    }

                    return accessToken;
                } else {
                    log.error("OAuth response missing access_token: {}", response.getBody());
                    throw new RuntimeException("Failed to extract access_token from OAuth response");
                }
            } else {
                log.error("OAuth request failed with status: {}", response.getStatusCode());
                throw new RuntimeException("OAuth token request failed with status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Failed to fetch OAuth access token", e);
            throw new RuntimeException("Failed to authenticate with Zoho Recruit", e);
        }
    }

    /**
     * Check if current token is still valid
     */
    private boolean isTokenValid() {
        if (accessToken == null) {
            return false;
        }

        // Check if token will expire soon
        long timeUntilExpiry = tokenExpiryTime - System.currentTimeMillis();
        return timeUntilExpiry > (TOKEN_REFRESH_BUFFER_SECONDS * 1000);
    }

    /**
     * Invalidate cached token to force refresh on next request
     */
    public void invalidateToken() {
        log.info("Invalidating cached access token");
        accessToken = null;
        tokenExpiryTime = 0;
    }
}


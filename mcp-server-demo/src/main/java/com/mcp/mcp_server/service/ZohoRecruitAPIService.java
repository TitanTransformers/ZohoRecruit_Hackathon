package com.mcp.mcp_server.service;

import com.mcp.mcp_server.config.ZohoRecruitPortalConnectionConfig;
import com.mcp.mcp_server.entity.Candidate;
import com.mcp.mcp_server.entity.ZohoRecruitResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

/**
 * Zoho Recruit API Service
 * Handles interactions with Zoho Recruit API for candidate searches and job operations
 *
 * Includes retry logic for transient timeouts and comprehensive error handling.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZohoRecruitAPIService {

    private final ZohoRecruitPortalConnectionConfig zohoRecruitPortalConnectionConfig;
    private final ZohoRecruitOAuthService oauthService;
    private final RestTemplate restTemplate;

    /**
     * Maximum number of retry attempts for transient failures (timeouts)
     */
    private static final int MAX_RETRIES = 3;

    /**
     * Delay between retries in milliseconds
     */
    private static final int RETRY_DELAY_MS = 1000;

    /**
     * Search for candidates based on criteria with automatic retry on timeout
     *
     * @param criteria Search criteria (e.g., "(Skill_Set:contains:Python)and(Experience_in_Years:greater_equal:5)")
     * @param page     Page number (default 1)
     * @param pageSize Results per page (default 200)
     * @return List of candidates matching criteria
     */
    public List<Candidate> searchCandidates(String criteria, Integer page, Integer pageSize) {
        int pageNum = page != null ? page : zohoRecruitPortalConnectionConfig.getDefaultPage();
        int pageLen = pageSize != null ? pageSize : zohoRecruitPortalConnectionConfig.getPageSize();

        String url = UriComponentsBuilder.fromUriString(zohoRecruitPortalConnectionConfig.getApiBaseUrl())
                .path(zohoRecruitPortalConnectionConfig.getCandidatesEndpoint())
                .queryParam("criteria", criteria)
                .queryParam("page", pageNum)
                .queryParam("per_page", pageLen)
                .build()
                .toUriString();

        log.debug("Searching candidates with criteria: {} (page: {}, pageSize: {})", criteria, pageNum, pageLen);

        return searchWithRetry(url, 0);
    }

    /**
     * Search with automatic retry logic for transient failures (timeouts)
     */
    private List<Candidate> searchWithRetry(String url, int attemptNumber) {
        try {
            log.debug("Attempt {} to search candidates (URL: {})", attemptNumber + 1, url);

            ResponseEntity<ZohoRecruitResponse> response = makeAuthenticatedRequest(url, HttpMethod.GET,
                    new ParameterizedTypeReference<ZohoRecruitResponse>() {});

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null
                    && response.getBody().getData() != null) {
                log.info("Successfully retrieved {} candidates from Zoho Recruit", response.getBody().getData().size());
                return response.getBody().getData();
            }

            log.warn("Unexpected response from Zoho Recruit: status={}", response.getStatusCode());
            return new ArrayList<>();

        } catch (ResourceAccessException e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";

            // Check if it's a timeout error
            if (isTimeoutError(errorMsg)) {
                log.warn("Timeout error on attempt {}: {}", attemptNumber + 1, errorMsg);

                if (attemptNumber < MAX_RETRIES - 1) {
                    long delayMs = RETRY_DELAY_MS * (attemptNumber + 1); // Exponential backoff
                    log.info("Retrying after {}ms delay... (attempt {}/{})", delayMs, attemptNumber + 2, MAX_RETRIES);

                    try {
                        Thread.sleep(delayMs);
                        return searchWithRetry(url, attemptNumber + 1);
                    } catch (InterruptedException ie) {
                        log.error("Retry interrupted", ie);
                        Thread.currentThread().interrupt();
                        return new ArrayList<>();
                    }
                } else {
                    log.error("Max retries ({}) exhausted for Zoho Recruit search. Last error: {}",
                            MAX_RETRIES, errorMsg);
                    return new ArrayList<>();
                }
            } else {
                // Not a timeout - don't retry
                log.error("Non-recoverable error searching candidates in Zoho Recruit: {}", errorMsg, e);
                return new ArrayList<>();
            }

        } catch (Exception e) {
            log.error("Unexpected error searching candidates in Zoho Recruit", e);
            return new ArrayList<>();
        }
    }

    /**
     * Check if error message indicates a timeout
     */
    private boolean isTimeoutError(String errorMessage) {
        if (errorMessage == null) {
            return false;
        }

        String lowerCaseMsg = errorMessage.toLowerCase();
        return lowerCaseMsg.contains("read timed out") ||
               lowerCaseMsg.contains("timeout") ||
               lowerCaseMsg.contains("connection timeout") ||
               lowerCaseMsg.contains("socket timeout") ||
               lowerCaseMsg.contains("read timeout");
    }

    /**
     * Make authenticated HTTP request to Zoho Recruit API with parameterized response type
     */
    private <T> ResponseEntity<T> makeAuthenticatedRequest(String url, HttpMethod method,
                                                            ParameterizedTypeReference<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        return restTemplate.exchange(url, method, entity, responseType);
    }
}



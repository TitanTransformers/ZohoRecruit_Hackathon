package com.mcp.mcp_server.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Zoho Recruit OAuth and API Configuration
 * Loads configuration from environment variables via .env file
 */
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "zoho.recruit")
@Getter
@Setter
public class ZohoRecruitPortalConnectionConfig {

    private String clientId;
    private String clientSecret;
    private String grantType;
    private String oauthUrl;
    private String orgId;
    private String apiBaseUrl;
    private String candidatesEndpoint;
    private String jobsEndpoint;
    private Integer pageSize;
    private Integer defaultPage;
    private Integer searchTimeoutSeconds;

}


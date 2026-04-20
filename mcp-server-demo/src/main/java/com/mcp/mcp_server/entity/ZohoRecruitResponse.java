package com.mcp.mcp_server.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Wrapper for Zoho Recruit API paginated response: {"data": [...]}
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ZohoRecruitResponse {

    @JsonProperty("data")
    private List<Candidate> data;
}


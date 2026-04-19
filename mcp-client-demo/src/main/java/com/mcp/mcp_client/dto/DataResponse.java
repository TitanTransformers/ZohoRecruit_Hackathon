package com.mcp.mcp_client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Wrapper for structured data (JSON or XML) from Claude responses.
 * Preserves the original format and provides metadata for proper frontend rendering.
 */
@Data
@Builder
public class DataResponse {

    @JsonProperty("data_type")
    private String dataType; // "json" or "xml"

    @JsonProperty("data")
    private Object data; // The actual data (string for XML, object/array for JSON)

    @JsonProperty("timestamp")
    private long timestamp;
}


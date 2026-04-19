package com.mcp.mcp_client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@Builder
public class ChatResponse {
    
    @JsonProperty("response")
    private Object response;
    
    @JsonProperty("tools_used")
    private List<String> toolsUsed;
    
    @JsonProperty("timestamp")
    private long timestamp;

}


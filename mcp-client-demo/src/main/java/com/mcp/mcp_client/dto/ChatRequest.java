package com.mcp.mcp_client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatRequest {
    
    @JsonProperty("message")
    private String message;

    public ChatRequest() {
    }

    public ChatRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "ChatRequest{" +
                "message='" + message + '\'' +
                '}';
    }
}


package com.mcp.mcp_client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatRequest {
    
    @JsonProperty("message")
    private String message;

    @JsonProperty("page_size")
    private int pageSize = 1000;  // Default page size

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

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    @Override
    public String toString() {
        return "ChatRequest{" +
                "message='" + message + '\'' +
                ", pageSize=" + pageSize +
                '}';
    }
}


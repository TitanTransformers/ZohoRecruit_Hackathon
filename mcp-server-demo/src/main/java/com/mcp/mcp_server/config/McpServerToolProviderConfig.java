package com.mcp.mcp_server.config;

import com.mcp.mcp_server.tools.RecruitmentTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers all @Tool-annotated methods with the Spring AI MCP server.
 *
 * Each ToolCallbackProvider bean is picked up automatically by
 * spring-ai-starter-mcp-server-webmvc and advertised to MCP clients
 * in the tools/list response.
 */
@Configuration
public class McpServerToolProviderConfig {

    /**
     * Registers all recruitment-related tools.
     * Tools exposed:
     *   - parseJobDescription
     *   - searchCandidatesInZohoRecruit
     *   - findAndRankCandidatesForJD
     *   - generateSearchFiltersFromJD
     *   - getDetailedCandidateFitAnalysis
     *   - generateCustomInterviewQuestions
     */
    @Bean
    public ToolCallbackProvider recruitmentToolProvider(RecruitmentTools recruitmentTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(recruitmentTools)
                .build();
    }
}

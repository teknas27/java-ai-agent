package com.sanket.agent.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Unified response model for all agent operations.
 */
@Data
@Builder
public class AgentResponse {

    private String status;          // SUCCESS | ERROR
    private String operation;       // review | test-gen | performance
    private String result;          // The actual agent output
    private long processingTimeMs;  // How long the agent took
    private LocalDateTime timestamp;
    private String errorMessage;    // Only set when status = ERROR

    public static AgentResponse success(String operation, String result, long timeMs) {
        return AgentResponse.builder()
                .status("SUCCESS")
                .operation(operation)
                .result(result)
                .processingTimeMs(timeMs)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static AgentResponse error(String operation, String errorMessage) {
        return AgentResponse.builder()
                .status("ERROR")
                .operation(operation)
                .errorMessage(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();
    }
}

package com.sanket.agent.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request model for all agent endpoints.
 */
@Data
public class AgentRequest {

    @NotBlank(message = "Code cannot be empty")
    @Size(min = 10, max = 50000, message = "Code must be between 10 and 50000 characters")
    private String code;

    private String context; // Optional: describe what the code does
}

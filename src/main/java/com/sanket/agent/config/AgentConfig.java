package com.sanket.agent.config;

import com.sanket.agent.service.CodeReviewAgent;
import com.sanket.agent.tools.CodeReviewTools;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * AgentConfig — Wires LLM (Groq) + Tools + Memory into the Agent.
 *
 * Groq is OpenAI-API-compatible, so we use OpenAiChatModel
 * but point the baseUrl to Groq's endpoint instead.
 *
 * Flow:
 *   Groq API Key + Base URL
 *       ↓
 *   OpenAiChatModel (pointed at Groq)
 *       ↓
 *   AiServices.builder → wires LLM + Tools + Memory
 *       ↓
 *   CodeReviewAgent (ready to use)
 */
@Slf4j
@Configuration
public class AgentConfig {

    @Value("${agent.groq.api-key}")
    private String groqApiKey;

    @Value("${agent.groq.base-url}")
    private String groqBaseUrl;

    @Value("${agent.groq.model}")
    private String groqModel;

    @Value("${agent.groq.temperature:0.3}")
    private double temperature;

    @Value("${agent.groq.max-tokens:4000}")
    private int maxTokens;

    @Value("${agent.groq.timeout-seconds:60}")
    private int timeoutSeconds;

    @Value("${agent.memory.max-messages:20}")
    private int maxMessages;

    @Bean
    public OpenAiChatModel chatModel() {
        log.info("Initializing Groq Chat Model...");
        log.info("Base URL : {}", groqBaseUrl);
        log.info("Model    : {}", groqModel);

        return OpenAiChatModel.builder()
                .apiKey(groqApiKey)
                .baseUrl(groqBaseUrl)
                .modelName(groqModel)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * The fully assembled Agent.
     * LLM (Groq/Llama) + Tools + Memory = Working Agent
     */
    @Bean
    public CodeReviewAgent codeReviewAgent(OpenAiChatModel chatModel,
                                            CodeReviewTools codeReviewTools) {
        log.info("Assembling CodeReviewAgent with Groq LLM + Tools + Memory...");

        return AiServices.builder(CodeReviewAgent.class)
                .chatLanguageModel(chatModel)
                .tools(codeReviewTools)
                .chatMemory(MessageWindowChatMemory
                        .withMaxMessages(maxMessages))
                .build();
    }
}

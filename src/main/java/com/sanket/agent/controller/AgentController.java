package com.sanket.agent.controller;

import com.sanket.agent.model.AgentRequest;
import com.sanket.agent.model.AgentResponse;
import com.sanket.agent.service.CodeReviewAgent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller — Exposes the AI Agent as HTTP endpoints.
 *
 * Endpoints:
 *  POST /api/agent/review        → Full code review (calls tools internally)
 *  POST /api/agent/generate-tests → Generate JUnit 5 test cases
 *  POST /api/agent/performance   → Performance analysis
 *  GET  /api/agent/health        → Health check
 */
@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final CodeReviewAgent codeReviewAgent;

    // ─────────────────────────────────────────
    // ENDPOINT 1: Full Code Review
    // ─────────────────────────────────────────

    /**
     * Full AI-powered code review.
     * The agent will autonomously:
     *   1. Analyze complexity (tool call)
     *   2. Detect code smells (tool call)
     *   3. Check security vulnerabilities (tool call)
     *   4. Generate review + refactored code + test suggestions
     */
    @PostMapping("/review")
    public ResponseEntity<AgentResponse> reviewCode(@Valid @RequestBody AgentRequest request) {
        log.info("=== CODE REVIEW REQUEST RECEIVED ===");
        long start = System.currentTimeMillis();

        try {
            String codeWithContext = request.getContext() != null
                    ? "Context: " + request.getContext() + "\n\nCode:\n" + request.getCode()
                    : request.getCode();

            log.info("Passing code to agent — agent will decide which tools to call...");
            String review = codeReviewAgent.reviewCode(codeWithContext);

            long elapsed = System.currentTimeMillis() - start;
            log.info("Agent completed review in {}ms", elapsed);

            return ResponseEntity.ok(AgentResponse.success("code-review", review, elapsed));

        } catch (Exception e) {
            log.error("Agent encountered an error during code review", e);
            return ResponseEntity.internalServerError()
                    .body(AgentResponse.error("code-review", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────
    // ENDPOINT 2: JUnit Test Generator
    // ─────────────────────────────────────────

    /**
     * Generates complete JUnit 5 test class for given code.
     * Covers: happy path, edge cases, null checks, boundary values.
     */
    @PostMapping("/generate-tests")
    public ResponseEntity<AgentResponse> generateTests(@Valid @RequestBody AgentRequest request) {
        log.info("=== TEST GENERATION REQUEST RECEIVED ===");
        long start = System.currentTimeMillis();

        try {
            String tests = codeReviewAgent.generateTests(request.getCode());
            long elapsed = System.currentTimeMillis() - start;

            log.info("Agent generated tests in {}ms", elapsed);
            return ResponseEntity.ok(AgentResponse.success("test-generation", tests, elapsed));

        } catch (Exception e) {
            log.error("Agent encountered an error during test generation", e);
            return ResponseEntity.internalServerError()
                    .body(AgentResponse.error("test-generation", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────
    // ENDPOINT 3: Performance Analyzer
    // ─────────────────────────────────────────

    /**
     * Deep performance analysis — memory, GC pressure, N+1 queries, caching.
     */
    @PostMapping("/performance")
    public ResponseEntity<AgentResponse> analyzePerformance(@Valid @RequestBody AgentRequest request) {
        log.info("=== PERFORMANCE ANALYSIS REQUEST RECEIVED ===");
        long start = System.currentTimeMillis();

        try {
            String analysis = codeReviewAgent.analyzePerformance(request.getCode());
            long elapsed = System.currentTimeMillis() - start;

            log.info("Agent completed performance analysis in {}ms", elapsed);
            return ResponseEntity.ok(AgentResponse.success("performance-analysis", analysis, elapsed));

        } catch (Exception e) {
            log.error("Agent encountered an error during performance analysis", e);
            return ResponseEntity.internalServerError()
                    .body(AgentResponse.error("performance-analysis", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────
    // ENDPOINT 4: Health Check
    // ─────────────────────────────────────────

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("""
            {
                "status": "UP",
                "agent": "CodeReviewAgent",
                "model": "GPT-4o",
                "tools": ["analyzeComplexity", "detectCodeSmells", "checkSecurityVulnerabilities", "suggestDesignPatterns"],
                "version": "1.0.0"
            }
            """);
    }
}

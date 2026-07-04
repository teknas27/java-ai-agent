package com.sanket.agent.tools;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.*;

/**
 * TOOLS — These are the actions the AI Agent can call autonomously.
 *
 * The agent DECIDES on its own when to call these tools.
 * You don't call them manually — the LLM orchestrates them.
 *
 * Think of tools as the agent's hands:
 *  - Brain (LLM) decides what to do
 *  - Tools (these methods) actually do it
 */
@Slf4j
@Component
public class CodeReviewTools {

    // ─────────────────────────────────────────
    // TOOL 1: Complexity Analyzer
    // ─────────────────────────────────────────

    @Tool("Analyzes cyclomatic complexity of Java code. " +
          "Returns complexity score and identifies complex methods. " +
          "Use this FIRST before any other analysis.")
    public String analyzeComplexity(
            @P("The Java code to analyze for complexity") String code) {

        log.info("[TOOL CALLED] analyzeComplexity — Agent is measuring code complexity");

        int complexity = calculateCyclomaticComplexity(code);
        List<String> complexMethods = findComplexMethods(code);

        StringBuilder result = new StringBuilder();
        result.append("=== COMPLEXITY ANALYSIS ===\n");
        result.append("Cyclomatic Complexity Score: ").append(complexity).append("\n");
        result.append("Risk Level: ").append(getComplexityRisk(complexity)).append("\n\n");

        if (!complexMethods.isEmpty()) {
            result.append("Complex Methods Found:\n");
            complexMethods.forEach(m -> result.append("  - ").append(m).append("\n"));
        }

        result.append("\nRecommendation: ");
        if (complexity <= 5) {
            result.append("Code is simple and easy to maintain.");
        } else if (complexity <= 10) {
            result.append("Moderate complexity. Consider breaking down larger methods.");
        } else {
            result.append("HIGH complexity! Refactor into smaller, focused methods.");
        }

        log.info("[TOOL RESULT] Complexity score: {}", complexity);
        return result.toString();
    }

    // ─────────────────────────────────────────
    // TOOL 2: Code Smell Detector
    // ─────────────────────────────────────────

    @Tool("Detects common Java code smells like long methods, magic numbers, " +
          "duplicate code patterns, and violation of naming conventions. " +
          "Always call this after complexity analysis.")
    public String detectCodeSmells(
            @P("The Java code to scan for code smells") String code) {

        log.info("[TOOL CALLED] detectCodeSmells — Agent is scanning for code smells");

        List<String> smells = new ArrayList<>();
        String[] lines = code.split("\n");

        // Check method length
        int methodLength = countMethodLength(code);
        if (methodLength > 20) {
            smells.add("LONG METHOD: Method has " + methodLength + " lines (recommended: ≤20). Extract to smaller methods.");
        }

        // Check magic numbers
        if (hasMagicNumbers(code)) {
            smells.add("MAGIC NUMBERS: Found hardcoded numeric literals. Extract to named constants.");
        }

        // Check naming conventions
        if (hasPoorNaming(code)) {
            smells.add("POOR NAMING: Found single-letter or unclear variable names. Use descriptive names.");
        }

        // Check for God class symptoms
        long fieldCount = Arrays.stream(lines)
                .filter(l -> l.trim().matches("private\\s+\\w+\\s+\\w+;.*"))
                .count();
        if (fieldCount > 10) {
            smells.add("GOD CLASS: Class has " + fieldCount + " fields. Consider splitting responsibilities (SRP).");
        }

        // Check for missing null checks
        if (code.contains("get(") && !code.contains("Optional") && !code.contains("!= null")) {
            smells.add("NULL SAFETY: Potential NullPointerException. Consider using Optional<> or null checks.");
        }

        // Check for raw types
        if (Pattern.compile("List\\s+\\w+|Map\\s+\\w+|Set\\s+\\w+").matcher(code).find()) {
            smells.add("RAW TYPES: Found collections without generics. Always use typed collections e.g. List<String>.");
        }

        // Check printStackTrace
        if (code.contains("printStackTrace()")) {
            smells.add("POOR EXCEPTION HANDLING: Use a proper logger (SLF4J) instead of e.printStackTrace().");
        }

        // Check System.out.println
        if (code.contains("System.out.println")) {
            smells.add("CONSOLE LOGGING: Replace System.out.println with proper SLF4J logging.");
        }

        StringBuilder result = new StringBuilder("=== CODE SMELL ANALYSIS ===\n");
        if (smells.isEmpty()) {
            result.append("✅ No major code smells detected!\n");
        } else {
            result.append("Found ").append(smells.size()).append(" code smell(s):\n\n");
            smells.forEach(s -> result.append("⚠️  ").append(s).append("\n\n"));
        }

        log.info("[TOOL RESULT] Found {} code smells", smells.size());
        return result.toString();
    }

    // ─────────────────────────────────────────
    // TOOL 3: Security Vulnerability Scanner
    // ─────────────────────────────────────────

    @Tool("Scans Java code for common security vulnerabilities including " +
          "SQL injection, hardcoded credentials, insecure random, " +
          "and OWASP Top 10 issues. Always run this for production code.")
    public String checkSecurityVulnerabilities(
            @P("The Java code to scan for security vulnerabilities") String code) {

        log.info("[TOOL CALLED] checkSecurityVulnerabilities — Agent is running security scan");

        List<String> vulnerabilities = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // SQL Injection check
        if ((code.contains("executeQuery(") || code.contains("executeUpdate("))
                && code.contains("+ ") && !code.contains("PreparedStatement")) {
            vulnerabilities.add("🚨 SQL INJECTION [CRITICAL]: String concatenation in SQL query detected. " +
                    "Use PreparedStatement with parameterized queries immediately.");
        }

        // Hardcoded credentials
        if (Pattern.compile("password\\s*=\\s*\"[^\"]+\"", Pattern.CASE_INSENSITIVE).matcher(code).find() ||
            Pattern.compile("secret\\s*=\\s*\"[^\"]+\"", Pattern.CASE_INSENSITIVE).matcher(code).find()) {
            vulnerabilities.add("🚨 HARDCODED CREDENTIALS [CRITICAL]: Password/secret found in source code. " +
                    "Use environment variables or a secrets manager (AWS Secrets Manager, Vault).");
        }

        // Insecure Random
        if (code.contains("new Random()") && (code.contains("token") || code.contains("otp") || code.contains("secret"))) {
            vulnerabilities.add("🚨 INSECURE RANDOM [HIGH]: Using java.util.Random for security tokens. " +
                    "Use SecureRandom instead.");
        }

        // MD5 / SHA1 usage
        if (code.contains("MD5") || code.contains("SHA-1") || code.contains("SHA1")) {
            vulnerabilities.add("⚠️ WEAK HASHING [HIGH]: MD5/SHA-1 are cryptographically broken. " +
                    "Use SHA-256, SHA-512, or BCrypt for passwords.");
        }

        // XXE vulnerability
        if (code.contains("DocumentBuilder") && !code.contains("setFeature")) {
            warnings.add("⚠️ POTENTIAL XXE [MEDIUM]: XML parsing without disabling external entities. " +
                    "Set DocumentBuilderFactory features to prevent XXE attacks.");
        }

        // Catch all exceptions silently
        if (code.contains("catch (Exception e) {}") || code.contains("catch(Exception e){}")) {
            warnings.add("⚠️ SWALLOWED EXCEPTION [MEDIUM]: Empty catch block hides errors silently. " +
                    "Always log exceptions at minimum.");
        }

        // Sensitive data in logs
        if ((code.contains("log.info") || code.contains("log.debug")) &&
                (code.contains("password") || code.contains("token") || code.contains("ssn"))) {
            warnings.add("⚠️ SENSITIVE DATA IN LOGS [MEDIUM]: Possible logging of sensitive information. " +
                    "Mask or exclude sensitive fields from logs.");
        }

        StringBuilder result = new StringBuilder("=== SECURITY VULNERABILITY SCAN ===\n");

        if (vulnerabilities.isEmpty() && warnings.isEmpty()) {
            result.append("✅ No critical security vulnerabilities detected.\n");
            result.append("Note: Always conduct a full DAST/SAST scan before production deployment.\n");
        } else {
            if (!vulnerabilities.isEmpty()) {
                result.append("CRITICAL VULNERABILITIES (Fix Immediately):\n");
                vulnerabilities.forEach(v -> result.append(v).append("\n\n"));
            }
            if (!warnings.isEmpty()) {
                result.append("SECURITY WARNINGS (Fix Before Release):\n");
                warnings.forEach(w -> result.append(w).append("\n\n"));
            }
        }

        log.info("[TOOL RESULT] Found {} vulnerabilities, {} warnings",
                vulnerabilities.size(), warnings.size());
        return result.toString();
    }

    // ─────────────────────────────────────────
    // TOOL 4: Design Pattern Advisor
    // ─────────────────────────────────────────

    @Tool("Suggests applicable design patterns based on the code structure. " +
          "Identifies opportunities to apply Singleton, Factory, Builder, " +
          "Strategy, Observer patterns and others.")
    public String suggestDesignPatterns(
            @P("The Java code to analyze for design pattern opportunities") String code) {

        log.info("[TOOL CALLED] suggestDesignPatterns — Agent is identifying pattern opportunities");

        List<String> suggestions = new ArrayList<>();

        // Builder pattern
        if (countConstructorParams(code) > 4) {
            suggestions.add("BUILDER PATTERN: Constructor has many parameters. " +
                    "Use Builder pattern for cleaner object creation.\n" +
                    "Example: Use Lombok @Builder annotation for instant builder support.");
        }

        // Factory pattern
        if (code.contains("instanceof") && code.contains("new ")) {
            suggestions.add("FACTORY PATTERN: Found instanceof checks with object creation. " +
                    "Use Factory Method or Abstract Factory to encapsulate object creation logic.");
        }

        // Strategy pattern
        if (countIfElseBlocks(code) > 3) {
            suggestions.add("STRATEGY PATTERN: Multiple if-else branches detected. " +
                    "Use Strategy pattern to encapsulate algorithms and make them interchangeable.");
        }

        // Observer pattern
        if (code.contains("notify") || code.contains("listener") || code.contains("callback")) {
            suggestions.add("OBSERVER PATTERN: Detected event notification logic. " +
                    "Consider Spring's ApplicationEventPublisher for a clean event-driven approach.");
        }

        // Singleton (anti-pattern warning)
        if (code.contains("getInstance()") && code.contains("static")) {
            suggestions.add("SINGLETON WARNING: Manual singleton detected. " +
                    "In Spring Boot, use @Component/@Service — Spring manages bean lifecycle as singleton by default.");
        }

        StringBuilder result = new StringBuilder("=== DESIGN PATTERN ADVISOR ===\n");
        if (suggestions.isEmpty()) {
            result.append("✅ Code structure looks well-designed. No immediate pattern changes needed.\n");
        } else {
            suggestions.forEach(s -> result.append("💡 ").append(s).append("\n\n"));
        }

        log.info("[TOOL RESULT] Suggested {} design patterns", suggestions.size());
        return result.toString();
    }

    // ─────────────────────────────────────────
    // Helper Methods
    // ─────────────────────────────────────────

    private int calculateCyclomaticComplexity(String code) {
        int complexity = 1;
        String[] keywords = {"if ", "else if", "while ", "for ", "case ", "catch ", "&&", "||", "? "};
        for (String keyword : keywords) {
            int index = 0;
            while ((index = code.indexOf(keyword, index)) != -1) {
                complexity++;
                index += keyword.length();
            }
        }
        return complexity;
    }

    private String getComplexityRisk(int complexity) {
        if (complexity <= 5)  return "LOW ✅";
        if (complexity <= 10) return "MEDIUM ⚠️";
        if (complexity <= 20) return "HIGH 🔴";
        return "VERY HIGH 🚨";
    }

    private List<String> findComplexMethods(String code) {
        List<String> methods = new ArrayList<>();
        Pattern methodPattern = Pattern.compile("(public|private|protected)\\s+\\w+\\s+(\\w+)\\s*\\(");
        Matcher matcher = methodPattern.matcher(code);
        while (matcher.find()) {
            String methodName = matcher.group(2);
            if (!methodName.equals("class")) {
                methods.add(methodName + "()");
            }
        }
        return methods;
    }

    private int countMethodLength(String code) {
        String[] lines = code.split("\n");
        int maxLength = 0;
        int currentLength = 0;
        boolean inMethod = false;
        for (String line : lines) {
            if (line.contains("{") && line.matches(".*\\).*\\{.*")) {
                inMethod = true;
                currentLength = 0;
            }
            if (inMethod) {
                currentLength++;
                maxLength = Math.max(maxLength, currentLength);
            }
            if (inMethod && line.trim().equals("}")) {
                inMethod = false;
            }
        }
        return maxLength;
    }

    private boolean hasMagicNumbers(String code) {
        Pattern magicNumber = Pattern.compile("(?<![\\w.])\\d{2,}(?![\\w.])");
        Matcher matcher = magicNumber.matcher(code);
        while (matcher.find()) {
            String num = matcher.group();
            if (!num.equals("10") && !num.equals("100")) return true;
        }
        return false;
    }

    private boolean hasPoorNaming(String code) {
        return Pattern.compile("\\b(int|String|long|boolean)\\s+[a-z]\\b").matcher(code).find();
    }

    private int countConstructorParams(String code) {
        Pattern constructor = Pattern.compile("public\\s+\\w+\\(([^)]+)\\)");
        Matcher matcher = constructor.matcher(code);
        if (matcher.find()) {
            return matcher.group(1).split(",").length;
        }
        return 0;
    }

    private int countIfElseBlocks(String code) {
        int count = 0;
        String[] keywords = {"if (", "} else {", "else if ("};
        for (String kw : keywords) {
            int idx = 0;
            while ((idx = code.indexOf(kw, idx)) != -1) {
                count++;
                idx += kw.length();
            }
        }
        return count;
    }
}

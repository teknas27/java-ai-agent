package com.sanket.agent;

import com.sanket.agent.tools.CodeReviewTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for CodeReviewTools.
 * Tests each tool independently without calling the LLM.
 */
@DisplayName("CodeReviewTools Unit Tests")
class CodeReviewToolsTest {

    private CodeReviewTools tools;

    @BeforeEach
    void setUp() {
        tools = new CodeReviewTools();
    }

    // ─────────────────────────────────────────
    // Complexity Tests
    // ─────────────────────────────────────────

    @Test
    @DisplayName("Should detect LOW complexity for simple code")
    void shouldDetectLowComplexity() {
        String simpleCode = """
                public String greet(String name) {
                    return "Hello, " + name;
                }
                """;
        String result = tools.analyzeComplexity(simpleCode);
        assertTrue(result.contains("LOW") || result.contains("simple"),
                "Simple code should report low complexity");
    }

    @Test
    @DisplayName("Should detect HIGH complexity for code with many branches")
    void shouldDetectHighComplexity() {
        String complexCode = """
                public String process(int x) {
                    if (x > 0) {
                        if (x > 10) {
                            while (x > 5) {
                                if (x % 2 == 0) {
                                    x--;
                                } else if (x % 3 == 0) {
                                    x -= 2;
                                }
                            }
                        } else if (x > 5) {
                            for (int i = 0; i < x; i++) {
                                if (i % 2 == 0 && i % 3 == 0) {
                                    x += i;
                                }
                            }
                        }
                    } else {
                        switch (x) {
                            case -1: return "neg one";
                            case -2: return "neg two";
                            default: return "other";
                        }
                    }
                    return String.valueOf(x);
                }
                """;
        String result = tools.analyzeComplexity(complexCode);
        assertTrue(result.contains("HIGH") || result.contains("VERY HIGH"),
                "Complex code should report high complexity");
    }

    // ─────────────────────────────────────────
    // Code Smell Tests
    // ─────────────────────────────────────────

    @Test
    @DisplayName("Should detect System.out.println as code smell")
    void shouldDetectSystemOutPrintln() {
        String code = """
                public void process() {
                    System.out.println("Processing...");
                }
                """;
        String result = tools.detectCodeSmells(code);
        assertTrue(result.contains("CONSOLE LOGGING") || result.contains("System.out"),
                "Should detect console logging as a smell");
    }

    @Test
    @DisplayName("Should detect printStackTrace as code smell")
    void shouldDetectPrintStackTrace() {
        String code = """
                public void process() {
                    try {
                        doSomething();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                """;
        String result = tools.detectCodeSmells(code);
        assertTrue(result.contains("POOR EXCEPTION") || result.contains("logger"),
                "Should detect printStackTrace as a smell");
    }

    @Test
    @DisplayName("Should detect magic numbers in code")
    void shouldDetectMagicNumbers() {
        String code = """
                public double calculate(double price) {
                    return price * 1.18 + 250;
                }
                """;
        String result = tools.detectCodeSmells(code);
        assertTrue(result.contains("MAGIC NUMBERS") || result.contains("constants"),
                "Should detect magic numbers");
    }

    @Test
    @DisplayName("Clean code should pass smell check")
    void cleanCodeShouldPassSmellCheck() {
        String cleanCode = """
                @Slf4j
                @Service
                public class OrderService {
                    private static final double TAX_RATE = 0.18;
                    
                    public double calculateTotal(double price) {
                        return price * TAX_RATE;
                    }
                }
                """;
        String result = tools.detectCodeSmells(cleanCode);
        assertTrue(result.contains("No major") || result.contains("✅"),
                "Clean code should have no major smells");
    }

    // ─────────────────────────────────────────
    // Security Tests
    // ─────────────────────────────────────────

    @Test
    @DisplayName("Should detect SQL injection vulnerability")
    void shouldDetectSqlInjection() {
        String vulnerableCode = """
                public List<User> findUsers(String name) {
                    String query = "SELECT * FROM users WHERE name = '" + name + "'";
                    return jdbcTemplate.executeQuery(query);
                }
                """;
        String result = tools.checkSecurityVulnerabilities(vulnerableCode);
        assertTrue(result.contains("SQL INJECTION"),
                "Should detect SQL injection vulnerability");
    }

    @Test
    @DisplayName("Should detect hardcoded password")
    void shouldDetectHardcodedPassword() {
        String code = """
                public void connect() {
                    String password = "admin123";
                    dataSource.connect(password);
                }
                """;
        String result = tools.checkSecurityVulnerabilities(code);
        assertTrue(result.contains("HARDCODED CREDENTIALS"),
                "Should detect hardcoded credentials");
    }

    @Test
    @DisplayName("Should detect weak MD5 hashing")
    void shouldDetectWeakHashing() {
        String code = """
                public String hashPassword(String pwd) {
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    return new String(md.digest(pwd.getBytes()));
                }
                """;
        String result = tools.checkSecurityVulnerabilities(code);
        assertTrue(result.contains("WEAK HASHING") || result.contains("MD5"),
                "Should detect weak MD5 hashing");
    }

    @ParameterizedTest
    @DisplayName("Should flag all weak hashing algorithms")
    @ValueSource(strings = {"MD5", "SHA-1", "SHA1"})
    void shouldFlagWeakHashingAlgorithms(String algorithm) {
        String code = "MessageDigest.getInstance(\"" + algorithm + "\");";
        String result = tools.checkSecurityVulnerabilities(code);
        assertTrue(result.contains("WEAK HASHING"),
                "Should flag " + algorithm + " as weak hashing");
    }

    // ─────────────────────────────────────────
    // Design Pattern Tests
    // ─────────────────────────────────────────

    @Test
    @DisplayName("Should suggest Builder pattern for many constructor params")
    void shouldSuggestBuilderPattern() {
        String code = """
                public class Order {
                    public Order(String id, String customer, String product,
                                 int quantity, double price, String address, String status) {
                        // constructor
                    }
                }
                """;
        String result = tools.suggestDesignPatterns(code);
        assertTrue(result.contains("BUILDER") || result.contains("Builder"),
                "Should suggest Builder pattern for many constructor params");
    }

    @Test
    @DisplayName("Should suggest Strategy pattern for many if-else blocks")
    void shouldSuggestStrategyPattern() {
        String code = """
                public double calculate(String type, double amount) {
                    if (type.equals("FLAT")) {
                        return amount - 50;
                    } else if (type.equals("PERCENT")) {
                        return amount * 0.9;
                    } else if (type.equals("BOGO")) {
                        return amount / 2;
                    } else if (type.equals("SEASONAL")) {
                        return amount * 0.85;
                    }
                    return amount;
                }
                """;
        String result = tools.suggestDesignPatterns(code);
        assertTrue(result.contains("STRATEGY") || result.contains("Strategy"),
                "Should suggest Strategy pattern for multiple if-else branches");
    }
}

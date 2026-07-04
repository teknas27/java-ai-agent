# Interview Preparation — Java AI Agent Project

## How to Introduce This Project (30-Second Pitch)

> "I built an AI-powered Code Review Agent using Spring Boot and LangChain4j that follows the **ReAct (Reason + Act) pattern**. Unlike a simple ChatGPT wrapper that sends code and gets a response, my agent **autonomously decides** which tools to call — complexity analysis, code smell detection, security scanning — observes the results, and iterates until it has enough evidence to produce a grounded, structured review. It's essentially the same architectural pattern used by GitHub Copilot Workspace and Cursor's AI agent."

---

## Core Concept: What is Agentic AI?

### Definition (Interview Answer)

> "Agentic AI refers to AI systems that can **autonomously plan, use tools, and iterate** to achieve a goal — unlike traditional LLM usage where you just send a prompt and get a response. An agent has a reasoning loop: it thinks about what it needs, acts by calling tools, observes results, and repeats until the task is complete."

### Key Difference: LLM Wrapper vs AI Agent

| Aspect | Simple LLM Wrapper | AI Agent (This Project) |
| --- | --- | --- |
| Flow | Input → LLM → Output | Input → LLM → Tool → Observe → Think → Tool → ... → Output |
| Decision Making | None (single pass) | LLM decides what tools to call and when |
| Grounding | No external data | Tools provide real analysis data |
| Hallucination | High risk | Reduced (grounded in tool results) |
| Iterative | No | Yes (loops until confident) |
| Example | "Review this code" → GPT response | "Review this code" → calls complexity tool → calls smell detector → synthesizes |

---

## The ReAct Pattern (Most Asked in Interviews)

### What is ReAct?

**ReAct = Reasoning + Acting** (published by Google/Princeton, 2022)

It's a prompting/orchestration paradigm where the LLM alternates between:
1. **Thought** — Reasoning about what to do next
2. **Action** — Calling an external tool
3. **Observation** — Reading the tool's output
4. **Repeat** — Until the agent has enough information
5. **Final Answer** — Synthesized response grounded in observations

### How ReAct Works in This Project

```
User sends Java code
    ↓
LLM THINKS: "I should first check complexity"
    ↓
LLM ACTS: calls analyzeComplexity(code) → returns "Complexity: 15, HIGH risk"
    ↓
LLM OBSERVES: "Code has high complexity"
    ↓
LLM THINKS: "Now I should check for code smells"
    ↓
LLM ACTS: calls detectCodeSmells(code) → returns "3 smells found"
    ↓
LLM OBSERVES: "Long method, magic numbers, raw types"
    ↓
LLM THINKS: "Let me check security too"
    ↓
LLM ACTS: calls checkSecurityVulnerabilities(code) → returns "SQL injection found"
    ↓
LLM OBSERVES: "Critical security issue"
    ↓
LLM SYNTHESIZES: Final structured review with score, issues, and refactored code
```

### Interview Question: "Why not just send code directly to LLM?"

**Answer:**
> "Sending code directly to an LLM gives you an ungrounded opinion — the model might hallucinate issues that don't exist or miss real ones. With the agent pattern, the LLM first runs actual static analysis tools that produce factual results (cyclomatic complexity = 15, SQL injection detected, etc.), then synthesizes its review based on these concrete findings. This makes the output more reliable, consistent, and verifiable."

---

## LangChain4j — The Orchestration Framework

### What is LangChain4j?

- Java equivalent of Python's LangChain
- Framework for building LLM-powered applications
- Key abstraction: **AiServices** — you define a Java interface, LangChain4j implements it using LLM + Tools

### How AiServices Works (Interview Favorite)

```java
// You write this interface:
public interface CodeReviewAgent {
    @SystemMessage("You are a Senior Java Code Reviewer...")
    String reviewCode(@UserMessage String code);
}

// LangChain4j generates the implementation at runtime:
CodeReviewAgent agent = AiServices.builder(CodeReviewAgent.class)
    .chatLanguageModel(chatModel)     // LLM (Groq/OpenAI)
    .tools(codeReviewTools)           // Tools the agent can call
    .chatMemory(memory)               // Conversation memory
    .build();

// When you call agent.reviewCode(code):
// → LangChain4j sends the system prompt + user code to LLM
// → LLM responds with tool call requests
// → LangChain4j executes the tools
// → Sends results back to LLM
// → LLM generates final response
// ALL of this happens automatically in the ReAct loop!
```

### Key LangChain4j Annotations

| Annotation | Purpose | Example |
| --- | --- | --- |
| `@SystemMessage` | Sets the agent's persona and behavior rules | "You are a Senior Java Code Reviewer..." |
| `@UserMessage` | Template for user input to the LLM | "Analyze this code: {{code}}" |
| `@Tool` | Marks a method as callable by the agent | "Analyzes cyclomatic complexity..." |
| `@P` | Describes a tool parameter for the LLM | "The Java code to analyze" |
| `@V` | Binds a template variable | `@V("code") String code` |

---

## Tool Calling / Function Calling

### What is Tool Calling?

> "Tool calling (or function calling) is when the LLM generates a structured request to invoke an external function, rather than generating text. The LLM outputs JSON like `{\"name\": \"analyzeComplexity\", \"arguments\": {\"code\": \"...\"}}`, the orchestration framework executes the function, and sends the result back to the LLM."

### How Tools are Registered (Interview Question)

```java
@Tool("Analyzes cyclomatic complexity of Java code")
public String analyzeComplexity(@P("The Java code") String code) {
    // This method runs locally — no LLM call
    // Returns structured text that the LLM reads
    return "Complexity Score: 15, Risk: HIGH";
}
```

**Key points:**
- The `@Tool` description tells the LLM **when** to use this tool
- The `@P` annotation tells the LLM **what** to pass as parameters
- The LLM **decides on its own** whether to call this tool
- The tool runs **locally** (not on the LLM server)

### Sequence of Events (Draw This in Interview)

```
Client → Controller → Agent Interface → LangChain4j Runtime
                                              ↓
                                        Sends to LLM (Groq)
                                              ↓
                                        LLM says: "Call analyzeComplexity"
                                              ↓
                                        LangChain4j calls the method locally
                                              ↓
                                        Sends result back to LLM
                                              ↓
                                        LLM says: "Call detectCodeSmells"
                                              ↓
                                        (repeat until final answer)
                                              ↓
                                        Returns final text to Controller
```

---

## Important Terminology for Interviews

### AI/LLM Terms

| Term | Definition | In This Project |
| --- | --- | --- |
| **LLM** | Large Language Model — neural network trained on text | Groq-hosted Llama 3.3 70B / GPT-OSS 120B |
| **Prompt Engineering** | Crafting input text to guide LLM behavior | `@SystemMessage` defines agent persona |
| **System Prompt** | Instructions that set LLM's role and rules | "You are a Senior Java Code Reviewer..." |
| **Temperature** | Randomness control (0=deterministic, 1=creative) | 0.3 (low for consistent reviews) |
| **Tokens** | Units of text (~4 chars = 1 token) | max-tokens: 1500 for output |
| **Context Window** | Max input+output tokens an LLM can handle | ~8K-128K depending on model |
| **Hallucination** | LLM generating false/made-up information | Reduced by grounding in tool results |
| **Grounding** | Anchoring LLM output in factual data | Tool results ground the review |
| **Function Calling** | LLM generating structured tool invocations | LLM decides to call `analyzeComplexity` |
| **RAG** | Retrieval-Augmented Generation | Not used here (tools provide data instead) |

### Agent Architecture Terms

| Term | Definition | In This Project |
| --- | --- | --- |
| **Agent** | LLM + Tools + Memory + Reasoning Loop | `CodeReviewAgent` interface |
| **ReAct** | Reason + Act pattern for agent orchestration | The core loop in LangChain4j |
| **Tool** | Function an agent can autonomously call | `analyzeComplexity`, `detectCodeSmells`, etc. |
| **Memory** | Conversation history for context | `MessageWindowChatMemory` (20 messages) |
| **Orchestration** | Managing the flow between LLM and tools | LangChain4j handles this |
| **Multi-turn** | Multiple LLM calls within one request | Agent calls LLM 3-5 times per review |
| **Chain of Thought** | Making LLM show reasoning steps | ReAct THINK step |
| **Tool Description** | Text telling LLM when/how to use a tool | `@Tool("Analyzes cyclomatic complexity...")` |

### Java/Spring Terms

| Term | Definition | In This Project |
| --- | --- | --- |
| **Spring Boot** | Opinionated Java framework for microservices | REST API + auto-configuration |
| **Bean** | Spring-managed object (singleton by default) | `chatModel()`, `codeReviewAgent()` |
| **Dependency Injection** | Framework provides dependencies automatically | `@RequiredArgsConstructor` in controller |
| **Configuration** | Spring `@Configuration` class for bean creation | `AgentConfig.java` |
| **DTO** | Data Transfer Object | `AgentRequest`, `AgentResponse` |
| **Validation** | `@Valid` annotation for request validation | Min 10 chars code input |

---

## Frequently Asked Interview Questions

### Q1: "What is the difference between this and just calling ChatGPT API?"

> "ChatGPT API is a single request-response cycle. My agent makes **multiple LLM calls** within a single user request. The LLM autonomously decides which tools to invoke, reads their results, and iterates. It's like the difference between asking someone a question vs. giving them a task with access to tools — they'll research, verify, and then answer."

### Q2: "Why did you use LangChain4j instead of directly calling the API?"

> "LangChain4j provides the ReAct orchestration loop, automatic tool calling, memory management, and prompt templating out of the box. Without it, I'd have to manually:
> - Parse tool call responses from the LLM
> - Execute tools and format results
> - Send results back to the LLM
> - Handle the loop termination
> - Manage conversation memory
>
> LangChain4j abstracts all of this into a simple interface-based approach."

### Q3: "How does the LLM know which tools to call?"

> "When building the agent with `AiServices.builder()`, LangChain4j serializes all `@Tool` annotations (name, description, parameter types) into the system prompt as a function schema. The LLM sees something like: 'You have these tools available: analyzeComplexity(code: String) - Analyzes cyclomatic complexity...'. Based on the user's request and the tool descriptions, the LLM generates a function call response that LangChain4j intercepts and executes."

### Q4: "What happens if the LLM decides NOT to call any tools?"

> "It can. The system prompt instructs it to call tools, but LLMs aren't deterministic. If it skips tools, the review would be purely LLM-generated (ungrounded). To mitigate this, I use explicit instructions: 'You MUST first use the analyzeComplexity tool.' The temperature is also set low (0.3) to make behavior more predictable."

### Q5: "What is Groq and why did you use it?"

> "Groq is an LLM inference provider with a **free tier** (no credit card required). They host open-source models like Llama 3.3 70B on their custom LPU (Language Processing Unit) hardware, which provides faster inference than GPU-based providers. I used it because: (1) free for prototyping, (2) OpenAI-API-compatible so switching to OpenAI/Azure later requires only a URL change, (3) fast inference speeds."

### Q6: "How would you scale this for production?"

> "Several changes needed:
> - **Model**: Switch from Groq free tier to Azure OpenAI or AWS Bedrock for SLA guarantees
> - **Async**: Make tool calls async with CompletableFuture
> - **Caching**: Cache tool results for identical code (Redis)
> - **Rate Limiting**: Add API rate limiting (Spring Cloud Gateway / Bucket4j)
> - **Queue**: Use RabbitMQ/Kafka for long-running reviews
> - **Observability**: Add OpenTelemetry traces for each tool call
> - **Security**: Add JWT auth, input sanitization, code size limits
> - **Horizontal Scaling**: Stateless service (memory externalized to Redis)"

### Q7: "What is MessageWindowChatMemory?"

> "It's a sliding window that keeps the last N messages in context. In my project, it holds 20 messages. This means if the agent makes 5 tool calls in one review (10 messages: 5 requests + 5 responses), it still remembers the context. Without memory, each tool call would be isolated — the LLM wouldn't know what previous tools returned."

### Q8: "How is this different from RAG?"

> "RAG (Retrieval-Augmented Generation) retrieves relevant documents from a vector database and injects them into the prompt. My agent uses **tool calling** instead — it actively runs analyzers on the code and gets structured results. Think of it as:
> - **RAG** = passive knowledge retrieval (searching a knowledge base)
> - **Agents** = active task execution (running tools, making decisions)
>
> Both reduce hallucination, but agents are more flexible for dynamic tasks."

### Q9: "Explain the architecture in 2 minutes"

> "The system has 5 layers:
> 1. **Web UI / REST API** — User pastes Java code, hits 'Code Review'
> 2. **Spring Controller** — Validates input, routes to agent
> 3. **AI Agent** (LangChain4j) — Declarative interface with @SystemMessage prompts
> 4. **LLM** (Groq Cloud) — Brain that reasons and decides tool calls
> 5. **Tools** (Local Java methods) — Static analyzers for complexity, smells, security, patterns
>
> The magic is in layer 3-4-5: The LLM calls tools, reads results, calls more tools if needed, then generates a final review grounded in actual analysis. All orchestrated by LangChain4j's ReAct loop."

### Q10: "What design patterns did YOU use in building this?"

> - **Strategy Pattern** — Each agent method (`reviewCode`, `generateTests`, `analyzePerformance`) is a different strategy for analyzing code
> - **Builder Pattern** — `AiServices.builder()`, `OpenAiChatModel.builder()`
> - **Interface Segregation** — `CodeReviewAgent` is a clean interface, implementation is generated
> - **Single Responsibility** — Controller handles HTTP, Agent handles AI logic, Tools handle analysis
> - **Dependency Injection** — Spring wires everything via constructor injection
> - **DTO Pattern** — `AgentRequest`/`AgentResponse` for clean API contracts

---

## Agentic AI Landscape (Show Industry Awareness)

### Where Agents are Used Today

| Product | Agent Pattern Used |
| --- | --- |
| GitHub Copilot Workspace | Multi-step code generation with tool use |
| Cursor IDE | ReAct-like agent with file editing tools |
| Devin (Cognition) | Autonomous coding agent with browser + terminal |
| AutoGPT / CrewAI | Multi-agent collaboration |
| AWS Q Developer | Code review + transformation agent |
| LangChain / LangGraph | Agent orchestration framework (Python) |
| LangChain4j | Agent orchestration framework (Java) — used here |

### Agent Frameworks Comparison (Java Focus)

| Framework | Language | Key Feature |
| --- | --- | --- |
| **LangChain4j** | Java | AiServices, type-safe, Spring integration |
| **Spring AI** | Java | Spring-native, model-agnostic |
| **LangChain** | Python | Largest ecosystem, most tools |
| **LangGraph** | Python | Graph-based agent workflows |
| **Semantic Kernel** | C#/Java | Microsoft's orchestration SDK |
| **CrewAI** | Python | Multi-agent collaboration |

---

## Common Follow-Up Topics

### Token Economics (Why It Matters)

```
One request to the agent costs approximately:

System Prompt:     ~500 tokens
Tool Definitions:  ~300 tokens  (4 tools × ~75 tokens each)
User Code:         ~200-800 tokens
Tool Results:      ~200-400 tokens (per tool call)
LLM Output:        ~500-1500 tokens

Total per review:  ~2000-4000 tokens
At OpenAI rates:   ~$0.01-0.04 per review (GPT-4o)
At Groq free tier: $0.00 (but rate limited)
```

### OpenAI-Compatible API (Why Groq Works with OpenAI SDK)

> "Groq exposes an API endpoint (`api.groq.com/openai/v1`) that accepts the same JSON format as OpenAI. So LangChain4j's `OpenAiChatModel` class works directly — I just change the `baseUrl`. This is a common pattern: many providers (Groq, Together, Anyscale, Ollama) implement OpenAI's API spec so any SDK built for OpenAI works with them."

### Error Handling & Resilience

- Rate limiting: Retry with exponential backoff
- Timeout: 60-second timeout per agent call
- Fallback: Could add a simpler model as fallback
- Circuit Breaker: Could use Resilience4j for Groq API failures

---

## Quick Reference Card (Print This)

```
PROJECT:  Java AI Code Review Agent
PATTERN:  ReAct (Reason + Act)
STACK:    Spring Boot 3 + LangChain4j + Groq LLM
TOOLS:    4 static analyzers (complexity, smells, security, patterns)
MODEL:    Llama 3.3 70B / GPT-OSS 120B (via Groq)

KEY INSIGHT: Agent ≠ LLM wrapper
  - Wrapper:  code → LLM → review (single call, may hallucinate)
  - Agent:    code → LLM → [tool] → observe → [tool] → observe → review (grounded)

LANGCHAIN4J MAGIC:
  - Define interface + @SystemMessage
  - AiServices.builder() creates implementation
  - LLM auto-calls @Tool methods in ReAct loop
  - MessageWindowChatMemory maintains context

WHY THIS MATTERS:
  - Reduces hallucination (grounded in tool output)
  - Modular (add new tools without changing agent logic)
  - Debuggable (each tool call is logged)
  - Extensible (add SonarQube, PMD, Checkstyle as tools)
```

---

## Bonus: How to Extend This Project (Impress the Interviewer)

1. **Add SonarQube integration** — Real static analysis tool as an agent tool
2. **Multi-agent architecture** — Separate agents for review, testing, performance that collaborate
3. **Vector store** — Store past reviews, use RAG to learn from historical patterns
4. **Streaming** — Stream agent's thinking process to the UI in real-time
5. **GitHub PR integration** — Auto-review PRs using GitHub webhooks
6. **Custom fine-tuning** — Fine-tune a model on your team's code standards
7. **Human-in-the-loop** — Agent asks clarifying questions before reviewing

---

*Prepared for: Sanket Joshi | Senior Java Engineer | AI Agent Developer*

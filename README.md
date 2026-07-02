# 🤖 Aria - AI Chat Service

[![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-brightgreen?logo=spring)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.0--M6-blue)](https://spring.io/projects/spring-ai)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Aria** is a provider-agnostic AI chat backend built with Spring Boot and Spring AI - conversation memory, real-time streaming, and structured output generation behind a clean, consistent REST API.

---

## 🎯 What This Demonstrates

Every "AI feature" prototype tends to turn into spaghetti code tightly coupled to one provider's SDK. Aria solves that by wrapping the LLM provider behind Spring AI's abstraction layer - swappable, testable, production-shaped from the start, rather than a notebook-style demo bolted onto a REST controller.

Built the *Spring way* - dependency injection, `@RestController`, externalized configuration instead of a separate Python microservice glued on as an afterthought. Production concerns are present from day one, not added later: input validation, global exception handling, token usage tracking, and per-session isolation.

---

## 🧠 Core Concepts

| Concept | Implementation                                                                                                          |
|---|-------------------------------------------------------------------------------------------------------------------------|
| Basic blocking chat | `chatClient.prompt().user().call().content()`                                                                           |
| Conversation memory | Sliding-window memory, isolated per `sessionId`                                                                         |
| Response metadata | `.call().chatResponse()` + token `Usage` extraction                                                                     |
| SSE streaming | `.stream().content()` → `Flux<String>`, real-time token delivery                                                        |
| Per-call options | `.options(OpenAiChatOptions.builder()...)` - override temperature/tokens for one request without touching global config |
| Structured output | `.call().entity(TopicSummary.class)` - typed JSON straight into a Java record                                           |
| Prompt templates | Externalized `.st` files, versioned separately from code                                                                |

```
User message → ChatController → ChatClient (Aria) → LLM Provider
             → (memory advisor reads/writes session history)
             → response → User
```

Memory is the key architectural piece here: each request's `sessionId` is passed via `.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))`, keeping every conversation's history isolated and independently retrievable — no cross-session leakage, no shared global state.

---

## 🛠️ Tech Stack

| Component | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.1.0 |
| AI Framework | Spring AI 2.0.0-M6 |
| LLM Provider | Groq (OpenAI-compatible API, free tier) |
| Model | `llama-3.3-70b-versatile` |
| Streaming | Spring WebFlux (SSE) |
| Validation | Spring Validation |
| Health/Metrics | Spring Actuator |
| Build Tool | Maven |
| Boilerplate reduction | Lombok |
| Testing | JUnit 5 |

---

## 🏗️ Architecture

```
┌─────────────┐     ┌──────────────┐     ┌─────────────────────┐     ┌────────────┐
│   Client    │────▶│ ChatController│────▶│  ChatClient (Aria)    │────▶│  Groq LLM  │
│ (REST/SSE)  │     │              │     │  + MessageChatMemory   │     │            │
└─────────────┘     └──────────────┘     │      Advisor           │     └────────────┘
                                          └──────────┬──────────┘
                                                       │
                                          per-request sessionId
                                          isolates conversation history
                                                       │
                                                       ▼
                                          ┌─────────────────────┐
                                          │  MessageWindowChatMemory│
                                          │  (sliding window,      │
                                          │   in-memory repository)│
                                          └─────────────────────┘

Structured output path (no memory needed):
Client ──▶ /api/v1/chat/structured ──▶ ChatClient ──▶ LLM
        ──▶ .entity(TopicSummary.class) ──▶ typed Java record ──▶ Client
```

---

## 🎭 Meet Aria

Aria's persona and behavior rules live in an externalized prompt template (`src/main/resources/prompts/system.st`), not hardcoded inline, prompt-only changes need no recompilation.

Aria's rules, enforced via the system prompt:
- Leads with the answer, explains only if it adds value
- Never fabricates facts, citations, sources, or statistics, says so honestly when unsure or outside its knowledge
- No filler openers, no unearned enthusiasm, no unnecessary disclaimers
- Stays in character across the full conversation, including after correction or disagreement

---

## 📡 API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/chat` | Send a message, get a text response (with memory) |
| `POST` | `/api/v1/chat/metadata` | Same as above, plus token usage + model metadata |
| `GET` | `/api/v1/chat/stream` | Stream tokens in real time via SSE |
| `POST` | `/api/v1/chat/options` | Override temperature / max tokens for a single call |
| `POST` | `/api/v1/chat/structured` | Get a typed JSON object instead of free text |
| `POST` | `/api/v1/chat/translate` | Prompt-template-driven translation example |
| `DELETE` | `/api/v1/chat/{sessionId}` | Clear a session's conversation memory |

### Example: basic chat with memory

```bash
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"demo-001","message":"Hello! What can you help me with?"}'
```

```json
{
  "sessionId": "demo-001",
  "response": "Hi, I'm Aria. I can answer questions, help you brainstorm, summarize text, and more. What would you like to do?",
  "timestamp": 1751270400000
}
```

Test memory with a follow-up on the **same** `sessionId`:

```bash
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"demo-001","message":"What did I just ask you?"}'
```

The reply references the previous question - confirms conversation memory is working.

### Example: streaming a response

```bash
curl -N "http://localhost:8080/api/v1/chat/stream?sessionId=demo-002&message=Tell+me+a+short+story"
```

Tokens arrive incrementally as `text/event-stream`, suitable for piping directly into a frontend's `EventSource`.

### Example: structured output

```bash
curl -X POST http://localhost:8080/api/v1/chat/structured \
  -H "Content-Type: application/json" \
  -d '{"topic":"Microservices Architecture"}'
```

```json
{
  "title": "Microservices Architecture",
  "summary": "An architectural style structuring an application as a collection of small, independently deployable services. Each service owns its data and communicates over lightweight protocols.",
  "keyPoints": [
    "Services are independently deployable and scalable",
    "Each service typically owns its own database",
    "Communication happens via REST, messaging, or gRPC"
  ]
}
```

---

## 🚀 Getting Started

### Prerequisites

| Tool | Minimum Version                | Purpose |
|---|--------------------------------|---|
| Java (JDK) | 25                             | Runtime + compilation |
| Maven | 3.9+ (or use bundled `./mvnw`) | Build tool |
| Git | 2.x                            | Version control |
| Groq API key | -                              | Free at [console.groq.com](https://console.groq.com/) |

### Step 1 - Clone the repository

```bash
git clone https://github.com/SNagarjuna07/ai-chat-service.git
cd ai-chat-service
```

### Step 2 - Configure environment variables

Create a `.env` file in the project root (never commit this):

```env
GROQ_API_KEY=your_actual_groq_key_here
```

### Step 3 - Build

```bash
./mvnw clean install
```

### Step 4 - Run

```bash
export GROQ_API_KEY=your_actual_groq_key_here
./mvnw spring-boot:run
```

### Verification

```bash
curl http://localhost:8080/actuator/health
```

```json
{"status":"UP"}
```

---

## Configuration

All behavior is controlled via `application.yml`:

```yaml
spring:
  ai:
    openai:
      api-key: ${GROQ_API_KEY}
      base-url: https://api.groq.com/openai/v1
      chat:
        options:
          model: llama-3.3-70b-versatile
          temperature: 0.7
app:
  chat:
    memory-max-messages: 10   # sliding window size for conversation history
```

---

## Extending

- Add new tools/capabilities by introducing `@Tool`-annotated methods (see Project 2, which builds directly on this pattern)
- Swap the in-memory chat store for persistent storage by replacing `InMemoryChatMemoryRepository` with a JDBC- or Redis-backed implementation
- Add new prompt behaviors by editing `src/main/resources/prompts/system.st` - no recompilation needed for prompt-only changes

---

## Project Structure

```
ai-chat-service/
├── src/
│   ├── main/
│   │   ├── java/com/nagarjuna/aichatservice/
│   │   │   ├── AiChatServiceApplication.java   # entry point
│   │   │   ├── config/
│   │   │   │   ├── AppProperties.java          # typed app.* config binding
│   │   │   │   └── ChatConfig.java             # ChatClient + ChatMemory beans
│   │   │   ├── controller/
│   │   │   │   └── ChatController.java         # REST + SSE endpoints
│   │   │   ├── dto/                            # request/response payloads (records)
│   │   │   ├── model/
│   │   │   │   └── TopicSummary.java           # structured output record
│   │   │   ├── service/
│   │   │   │   └── ChatService.java            # core AI interaction logic
│   │   │   └── exception/
│   │   │       └── GlobalExceptionHandler.java # centralized error handling
│   │   └── resources/
│   │       ├── application.yml                 # provider + app config
│   │       └── prompts/system.st               # externalized system prompt (Aria)
│   └── test/                                   # unit + integration tests
├── pom.xml
└── README.md
```

---

## License

MIT License

Copyright (c) 2026 S Nagarjuna

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

**Third-party licenses:** This project depends on Spring Boot and Spring AI, each under the Apache 2.0 license. Review each dependency's license before commercial use.

---

## Acknowledgments

- [Spring AI Team](https://github.com/spring-projects/spring-ai) - for the framework this project is built on
- Inspired by common production patterns in Spring's official Chat Memory and Advisors documentation

---

## Contact

- 📖 Documentation: [Spring AI Reference Docs](https://docs.spring.io/spring-ai/reference/)
- ✉️ Maintainer: srinivasnagarjuna04@gmail.com | [LinkedIn](https://www.linkedin.com/in/s-nagarjuna)
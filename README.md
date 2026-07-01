# 🤖 AI Chat Service

> A provider-agnostic, production-ready AI chat backend built with Spring Boot 4.1.0 and Spring AI 2.0.0-M6, featuring conversation memory, real-time streaming, and structured output generation.

[![Java](https://img.shields.io/badge/Java-25-orange)](#)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-brightgreen)](#)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.0--M6-brightgreen)](#)
[![License](https://img.shields.io/badge/license-MIT-yellow)](#license)

---

## Description

AI Chat Service is a Spring Boot backend that wraps any LLM provider (Ollama, OpenAI) behind a clean, consistent REST API, handling conversation memory, streaming responses, and structured data extraction without locking you into a single vendor. It solves the problem of every "AI feature" prototype turning into spaghetti code tightly coupled to one provider's SDK, instead offering a swappable, testable, production-shaped foundation.

Built for backend developers who want to add AI capabilities to their Spring applications the *Spring way* - using familiar patterns like dependency injection, `@RestController`, and externalized configuration instead of bolting on a separate Python microservice. Key features include per-session conversation memory with sliding-window context, Server-Sent Events (SSE) streaming for real-time token delivery, automatic JSON-to-POJO structured output, externalized prompt templates for version-controlled prompt management, and full provider abstraction so switching from a free local Ollama model to OpenAI's GPT-4o requires only a configuration change - zero code changes. Unlike typical LLM wrapper tutorials, this project includes production concerns from day one: input validation, global exception handling, token usage tracking, and session isolation.

---

## Installation

### Prerequisites

| Tool | Minimum Version | Purpose |
|---|---|---|
| Java (JDK) | 25 | Runtime + compilation |
| Maven | 3.9+ (or use bundled `./mvnw`) | Build tool |
| Git | 2.x | Version control |

Optional:
- An OpenAI API key, if using the OpenAI provider instead of Groq.

### Step 1 - Clone the repository

```bash
git clone https://github.com/SNagarjuna07/ai-chat-service.git
cd ai-chat-service
```

### Step 2 - Configure environment variables

Create a `.env` file in the project root (never commit this):

```bash
# .env
OPENAI_API_KEY=sk-your-key-here   # leave unset if using Ollama only
```

### Step 3 - Build the project

```bash
./mvnw clean install
```

### Step 4 - Run the application

```bash
./mvnw spring-boot:run
```

### Verification

Confirm the service is up:

```bash
curl http://localhost:8080/actuator/health
```

Expected output:

```json
{"status":"UP"}
```

---

## Quick Start

Send your first chat message:

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"demo-001","message":"Hello! What can you help me with?"}'
```

Expected output:

```json
{
  "sessionId": "demo-001",
  "response": "Hi! I'm Aria, your AI assistant. I can answer questions, help you brainstorm, summarize text, and more. What would you like to do?",
  "timestamp": 1751270400000
}
```

Test memory by sending a follow-up with the **same** `sessionId`:

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"demo-001","message":"What did I just ask you?"}'
```

The response will reference your previous question, proving conversation memory works.

---

## Usage

### Core Endpoints

| Method | Endpoint                   | Description |
|---|----------------------------|---|
| `POST` | `/api/v1/chat`             | Send a message, get a text response (with memory) |
| `POST` | `/api/v1/chat/metadata`    | Same as above, plus token usage + model metadata |
| `GET` | `/api/v1/chat/stream`      | Stream tokens in real time via SSE |
| `POST` | `/api/v1/chat/options`     | Override temperature / max tokens for a single call |
| `POST` | `/api/v1/chat/structured`  | Get a typed JSON object instead of free text |
| `POST` | `/api/v1/chat/translate`   | Prompt-template-driven translation example |
| `DELETE` | `/api/v1/chat/{sessionId}` | Clear a session's conversation memory |

### Example: Streaming a response

```bash
curl -N "http://localhost:8080/api/chat/stream?sessionId=demo-002&message=Tell+me+a+short+story"
```

Tokens arrive incrementally as `text/event-stream`, suitable for piping directly into a frontend's `EventSource`.

### Example: Structured output

```bash
curl -X POST http://localhost:8080/api/chat/structured \
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

### Configuration

All behavior is controlled via `application.yml`:

```yaml
spring:
  ai:
    openai:
      base-url: https://api.groq.com/openai/v1
      chat:
        model: llama3.2
        temperature: 0.7
app:
  chat:
    memory-max-messages: 10   # sliding window size for conversation history
```

To switch providers, comment/uncomment the relevant `spring.ai.*` block and set the corresponding environment variable, no Java code changes required.

### Extending

- Add new tools/capabilities by introducing `@Tool` annotated methods. 
- Swap the in-memory chat store for persistent storage by replacing `InMemoryChatMemoryRepository` with `JdbcChatMemoryRepository` or a Redis-backed implementation.
- Add new prompt behaviors by editing `src/main/resources/prompts/system.st` — no recompilation needed for prompt-only changes.

---

## Project Structure

```
ai-chat-service/
├── src/
│   ├── main/
│   │   ├── java/com/arjun/aichatservice/
│   │   │   ├── AiChatServiceApplication.java   # entry point
│   │   │   ├── config/
│   │   │   │   ├── AppProperties.java          # typed app.* config binding
│   │   │   │   └── ChatConfig.java             # ChatClient + ChatMemory beans
│   │   │   ├── controller/
│   │   │   │   └── ChatController.java         # REST + SSE endpoints
│   │   │   ├── dto/                            # request/response payloads
│   │   │   ├── model/
│   │   │   │   └── TopicSummary.java           # structured output record
│   │   │   ├── service/
│   │   │   │   └── ChatService.java            # core AI interaction logic
│   │   │   └── exception/
│   │   │       └── GlobalExceptionHandler.java # centralized error handling
│   │   └── resources/
│   │       ├── application.yml                 # provider + app config
│   │       └── prompts/system.st               # externalized system prompt
│   └── test/                                   # unit + integration tests                             # multi-stage app image build
├── pom.xml                                     # Maven build + dependencies
└── README.md
```

---

## Technology Stack

| Technology              | Version        | Purpose |
|-------------------------|----------------|---|
| Java                    | 25             | Core language |
| Spring Boot             | 4.1.0          | Application framework |
| Spring AI               | 2.0.0-M6       | LLM abstraction (ChatClient, advisors, memory) |
| Spring WebFlux          | (Boot-managed) | Reactive streaming / SSE |
| Spring Validation       | (Boot-managed) | Request payload validation |
| Spring Actuator         | (Boot-managed) | Health checks, metrics |
| OpenAI API (Groq)       | N/A            | Optional cloud LLM provider |
| Lombok                  | latest         | Boilerplate reduction |
| Maven                   | 3.9+           | Build & dependency management |
| JUnit 5                 | (Boot-managed) | Testing framework |

---


## License

This project is licensed under the **MIT License**.

```
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
```

**Third-party licenses:** This project depends on Spring Boot, Spring AI, and Ollama, each under their own respective open-source licenses (Apache 2.0 for Spring projects; check Ollama's repository for its current license terms). Review each dependency's license before commercial use.

---

## Acknowledgments

- [Spring AI Team](https://github.com/spring-projects/spring-ai) - for the framework this project is built on.
- Inspired by common production patterns in Spring's official Chat Memory and Advisors documentation.

---

## Contact and Support

- 📖 Documentation: see the [Spring AI Reference Docs](https://docs.spring.io/spring-ai/reference/)
- ✉️ Maintainer: srinivasnagarjuna04@gmail.com | https://www.linkedin.com/in/s-nagarjuna
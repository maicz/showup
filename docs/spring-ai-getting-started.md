# Spring AI — getting started

Plan for adding AI features to ShowUp. Five features, in build order, plus the setup they share.

The order matters: features 1–2 establish the embedding pipeline, 3–4 add the generative side with no new
infrastructure, 5 layers a chat surface on top of the reporting layer.

| # | Feature | What it gives you | New infra |
| --- | --- | --- | --- |
| 1 | Semantic event & group discovery | Natural-language search over events, composed with the existing geo/date filters | pgvector |
| 2 | Personalized recommendations | "Events for you" from `MemberInterest` + attendance history | — |
| 3 | Organizer copilot | Bullets → draft event, auto-classified into the topic taxonomy | — |
| 4 | Post-event feedback synthesis | `EventFeedbackSummary` gains themes, sentiment, action items | — |
| 5 | Organizer analytics chat | NL questions answered by tool-calling the reporting layer | chat memory table |

## Stack

| Piece | Version | Notes |
| --- | --- | --- |
| Spring AI | 2.0.0 | Built against Spring Boot 4.1.0 — exactly what `apps/api` already runs |
| Chat model | `claude-opus-5` | Via the official Anthropic Java SDK, which Spring AI 2.0 wraps |
| Embedding model | `all-MiniLM-L6-v2` | Local ONNX, 384 dimensions, no API calls |
| Vector store | pgvector | Same Postgres instance as everything else |

Spring AI 2.0 is a hard requirement here: 1.x targets Boot 3.x. It's also a real rewrite — it delegates to
Anthropic's official Java SDK rather than its own HTTP client, so options map 1:1 onto the API.

## Which models

**Chat/generation: Claude.** Default to `claude-opus-5` and step down where the workload is simple.

| Model | ID | $/MTok in / out | Use for |
| --- | --- | --- | --- |
| Opus 5 | `claude-opus-5` | 5 / 25 | Copilot drafting, analytics chat, feedback synthesis |
| Sonnet 5 | `claude-sonnet-5` | 3 / 15 | High-volume routes once quality is proven |
| Haiku 4.5 | `claude-haiku-4-5` | 1 / 5 | Topic classification, short structured extraction |

Start everything on Opus 5, measure, then move individual routes down. Don't pre-optimize by model choice —
the per-request cost here is rounding-error next to the time you'd spend chasing a quality regression.

**Embeddings: not Anthropic.** Anthropic has no embeddings endpoint, so feature 1 needs a second model.
For a project this size, run it locally:

```xml
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-starter-model-transformers</artifactId>
</dependency>
```

That's `TransformersEmbeddingModel` — ONNX Runtime in-process, `all-MiniLM-L6-v2`, 384 dimensions. Zero
API cost, zero latency to a third party, works offline, and works in Testcontainers tests without a
network stub. The tradeoff is quality: MiniLM is weaker than a hosted embedding model on nuanced queries.
For "chill board games near me" it is entirely adequate. Swap later if search quality plateaus — the
`EmbeddingModel` interface means it's a dependency change plus a re-embed, not a rewrite.

## Setup

### 1. Dependencies

Add the BOM to `apps/api/pom.xml` (`<dependencyManagement>`), then pull starters per feature:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.ai</groupId>
      <artifactId>spring-ai-bom</artifactId>
      <version>2.0.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

| Feature | Artifact (`org.springframework.ai`) |
| --- | --- |
| all | `spring-ai-starter-model-anthropic` |
| 1, 2 | `spring-ai-starter-model-transformers`, `spring-ai-starter-vector-store-pgvector` |
| 5 | `spring-ai-starter-model-chat-memory-repository-jdbc` |
| RAG advisors (optional) | `spring-ai-rag` |

### 2. pgvector on the PostGIS image

`postgis/postgis:18-3.6` does **not** ship pgvector. Build a small image that has both — replacing the
`image:` line in `docker-compose.yml` with a `build:`:

```dockerfile
# docker/postgres/Dockerfile
FROM postgis/postgis:18-3.6
RUN apt-get update \
 && apt-get install -y --no-install-recommends postgresql-18-pgvector \
 && rm -rf /var/lib/apt/lists/*
```

Testcontainers needs the same image. Point `TestcontainersConfiguration` at the built tag and mark it
compatible with `postgres` (`DockerImageName.parse("showup/postgres:18").asCompatibleSubstituteFor("postgres")`).

### 3. Schema via Flyway, not auto-init

Flyway owns the schema in this project and Hibernate only validates — keep it that way. Turn Spring AI's
auto-initialization **off** and write the migration yourself:

```sql
-- V6__create_vector_store.sql
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE vector_store (
    id        uuid PRIMARY KEY DEFAULT uuidv7(),
    content   text,
    metadata  jsonb,
    embedding vector(384)
);

CREATE INDEX ON vector_store USING hnsw (embedding vector_cosine_ops);
CREATE INDEX ON vector_store USING gin (metadata jsonb_path_ops);
```

384 must match the embedding model's dimension — change the model, change the column, re-embed everything.
The GIN index on `metadata` is what makes the filter half of a hybrid query fast.

For feature 5, the chat-memory table has a schema shipped at
`classpath:org/springframework/ai/chat/memory/repository/jdbc/schema-postgresql.sql` inside the starter jar —
copy it into a Flyway migration rather than letting it self-initialize.

### 4. Configuration

```yaml
spring:
  ai:
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
      chat:
        model: claude-opus-5
        max-tokens: 8192
    vectorstore:
      pgvector:
        initialize-schema: false   # Flyway owns it
        dimensions: 384
        index-type: HNSW
        distance-type: COSINE_DISTANCE
    chat:
      memory:
        repository:
          jdbc:
            initialize-schema: false
```

Three notes that will save you an afternoon:

- Properties are **flat** in 2.0 — `spring.ai.anthropic.chat.model`, not `...chat.options.model`. Most
  blog posts and the 1.x docs show the old nested form.
- Every option is null by default, so nothing is sent unless you set it. That matters because Opus 5
  **rejects `temperature`, `top_p`, and `top_k` with a 400**. Don't set them; steer with the prompt instead.
- Thinking is on by default on Opus 5. Leave `spring.ai.anthropic.chat.thinking` unset — the old
  `budget_tokens` form is also a 400. Use `output-config.effort` (`low`…`max`) if you need to tune depth.

Add `ANTHROPIC_API_KEY` to `.env.example`.

---

## 1. Semantic event & group discovery

Embed each event's title + description + group topics; search by cosine similarity with metadata filters
so the vector part composes with the boring part instead of replacing it.

**Pieces:** `VectorStore`, `EmbeddingModel`, `SearchRequest`, `FilterExpressionBuilder`.

```java
var results = vectorStore.similaritySearch(SearchRequest.builder()
        .query(userQuery)
        .topK(20)
        .similarityThreshold(0.5)
        .filterExpression(new FilterExpressionBuilder()
                .and(b.gt("startsAt", now.toEpochSecond()), b.eq("mode", "IN_PERSON"))
                .build())
        .build());
```

Hints:

- **Store IDs in metadata, not content.** Put `eventId`, `groupId`, `categoryId`, `startsAt`, `mode`,
  `seatsAvailable` in the metadata map; keep `content` purely textual. Similarity search returns
  `Document`s — map back to real `Event` rows by ID and let JPA serve the response DTOs.
- **PostGIS stays in charge of distance.** Don't try to encode location into the embedding. Vector search
  gives you candidate IDs; the existing geo query filters and orders them. Two cheap queries beat one clever one.
- **Re-embed on write.** An `@PreUpdate`/`@PrePersist` hook on `Event` that flags the row, plus a scheduled
  job that re-embeds flagged rows, is simpler and safer than embedding inline in the request path. Write a
  one-off backfill command for existing data.
- The domain doc defers "feed ranking and recommendations — needs a search/ranking store". This is that store.

## 2. Personalized recommendations

Build a member profile vector, search upcoming events against it.

Hints:

- **Profile text, not vector math.** Concatenate followed topic names + titles of events they attended into
  a short document, embed that, and search. Averaging embeddings is tempting and reliably worse.
- **Weight attendance over intent.** A `CheckIn` is a far stronger signal than an `Rsvp` — the app is
  literally named for this. Repeat the corresponding topics in the profile text to weight them.
- **`MemberInterest` solves cold start.** A member who joined an hour ago and has followed three topics gets
  usable recommendations immediately, with no collaborative-filtering machinery.
- Filter out events they've already RSVP'd to *after* the search, not before — cheaper than fighting the index.

## 3. Organizer copilot

Three bullets in, a populated draft `Event` out. The highest-value non-chat use of an LLM here.

**Pieces:** `ChatClient`, `.entity(Class)` (structured output → `BeanOutputConverter` under the hood).

```java
EventDraft draft = chatClient.prompt()
        .system(COPILOT_INSTRUCTIONS)
        .user(u -> u.text("Bullets:\n{bullets}\n\nAvailable topics:\n{topics}")
                   .param("bullets", request.bullets())
                   .param("topics", topicNames))
        .call()
        .entity(EventDraft.class);
```

Hints:

- **Shape the record like the request DTO.** If `EventDraft` mirrors `CreateEventRequest`, the model's output
  flows through the same Jakarta Validation as a human-submitted body. Validate it — a draft that fails
  validation is a retry, not a 500.
- **Classification is the sharpest win.** Nobody wants to browse 24 categories. Pass the topic list in the
  prompt and have the model return topic *names*; resolve to `Topic` IDs in Java and drop anything it
  invented. Don't trust the model to emit UUIDs.
- **Ground the suggestions.** Pull the group's last 5 events (capacity, duration, staff mix) into the prompt.
  Suggestions derived from what this group actually does beat generic ones by a wide margin.
- This is a *draft*. Never persist without the organizer confirming.

## 4. Post-event feedback synthesis

Eighty free-text comments nobody reads → themes, sentiment split, action items.

Hints:

- **Structured output again**, not prose. A record like
  `FeedbackInsights(List<Theme> themes, SentimentBreakdown sentiment, List<String> actionItems)` renders as
  chips and bars in Angular. A wall of generated text is a worse product than the raw comments.
- **Batch, don't stream.** Trigger it on a schedule after an event ends, cache the result on
  `EventFeedbackSummary`. No latency pressure, no streaming UI, no cost surprises.
- **Keep the numbers in SQL.** Average rating, response rate, NPS — all computed by the existing repository
  query. The model writes the narrative *around* numbers you calculated. Mixing the two is how these
  features lose credibility.
- Guard on volume: below ~5 comments, show them verbatim and skip the model entirely.

## 5. Organizer analytics chat

NL questions over `EventAttendanceReport` / `GroupActivityReport`, via tool calling — not NL→SQL.

**Pieces:** `@Tool`-annotated methods, `ChatClient.tools(bean)`, `MessageChatMemoryAdvisor` +
`JdbcChatMemoryRepository`, SSE → Angular signals.

```java
@Component
class ReportingTools {
    @Tool(description = "No-show rate for a group over a date range")
    NoShowReport noShowRate(UUID groupId, LocalDate from, LocalDate to) { ... }
}
```

Hints:

- **Tools, not generated SQL.** NL→SQL demos beautifully and fails in production. Typed tool methods keep
  authorization, query shape, and tenancy in your code where they belong.
- **Authorize inside the tool.** The model picks arguments; it must not be able to pick another organizer's
  `groupId`. Check the caller's `GroupMembership` role inside every tool method, every time.
- **Be prescriptive in `description`.** Say *when* to call it ("Call this when the organizer asks why
  attendance changed"), not just what it returns. Trigger conditions measurably improve tool selection.
- **Memory is a per-conversation ID.** `MessageChatMemoryAdvisor` keyed by conversation ID gives follow-ups
  ("and the month before?") for one table and one advisor.
- Stream with `.stream().content()` over SSE; Angular consumes it into a signal.

---

## Testing

Spring AI ships `RelevancyEvaluator` and `FactCheckingEvaluator` — you can assert search and RAG quality in
integration tests instead of eyeballing it. Point them at a fixed set of seeded events and a handful of
known-good queries.

For everything else, mock `ChatModel` in slice tests and keep the real API calls to a small, tagged suite
you run deliberately. `pgvector/pgvector` has a Testcontainers module, but here the custom PostGIS+pgvector
image is the one to use so tests match production.

## Two rules worth committing to

- **The LLM never computes a number.** SQL computes; the model narrates. This applies to attendance
  forecasts, no-show prediction, and every "insight" surface.
- **The LLM never sits in the check-in path.** QR scanning happens at a door on bad wifi. Everything above
  is discovery-time, batch, or async — keep it that way.

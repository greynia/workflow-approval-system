# Workflow Approval System

English | [繁體中文](README_zh.md)

A leave-approval platform with a governed AI review layer. Employees file leave requests and
managers approve them; an AI reviewer drafts a risk assessment to assist the approver. The AI
only advises — a deterministic rule engine owns the risk floor — and the same governed backend
tools are exposed to AI agents over the **Model Context Protocol (MCP)**.

**Stack:** Java 17 · Spring Boot 3.5 · PostgreSQL 16 + pgvector · Next.js 16 / React 19 ·
Python (MCP) · Gemini · Ollama · Jina Rerank

## Features

- **Leave-approval workflow** — multi-step approval chains, deputy and recall flows,
  leave-quota and holiday-calendar handling, role-based access (employee / manager / admin),
  and an audit log.
- **AI review pipeline** — a deterministic rule engine flags risk and sets a floor; relevant
  company-policy clauses are retrieved with RAG; an LLM drafts a summary, risk level and
  recommendation with policy citations.
- **Deterministic safety guardrail** — the final risk is `max(LLM, rule floor)`, so the model
  can raise risk but never lower it; HIGH risk is never auto-approved; the final decision stays
  with a human approver.
- **Tools exposed to AI agents (MCP)** — the governed backend tools are published as an MCP
  server and connected to Claude Code via a Skill and a least-privilege Subagent. Every agent
  call runs under a dedicated `AI_AGENT` service account and is audited.
- **Resilience & observability** — multi-provider fallback (Gemini → Ollama → rule-only);
  provider, token usage, latency, failures and prompt version are recorded per review.

## Architecture

```
Next.js frontend
      │  REST + JWT (httpOnly cookie)
Spring Boot backend   — hexagonal: api / application / domain / infrastructure
  • rule engine + Policy RAG (pgvector) + LLM review + deterministic floor
  • governed, read-only AI endpoints (AI_AGENT / ADMIN only, audited):
        GET /api/ai/requests/{id}/context
        GET /api/ai/requests/{id}/rule-evaluation      ← authoritative risk floor
        GET /api/ai/policy-search
      ▲
      │  HTTP (logs in as ai-agent@system.local)
MCP server (Python / FastMCP)   — 3 tools, thin protocol adapter
      ▲
      │  MCP (stdio)
Claude Code   — leave-review Skill + leave-reviewer Subagent
```

## How AI review works

When a request is created, an asynchronous pipeline runs:

1. **Assemble** the applicant and leave context.
2. **Rule engine** produces hard-rule flags and a risk floor (e.g. frequent leave, new-hire long
   leave, long duration).
3. **Policy RAG** (only when flags fire) — embed the query (`gemini-embedding-001`, 768-dim) →
   pgvector cosine search → optional Jina rerank → top-k policy clauses.
4. **LLM** drafts summary / risk / recommendation / citations, grounded in the retrieved clauses.
5. **Enforce floor** — `final = max(LLM, floor)`; downgrade `HIGH + APPROVE` to `REVIEW_CAREFULLY`.
6. **Persist** the result with full provenance.

If the LLM is unavailable the review degrades to a rule-only result, so the approval flow never
blocks on AI availability.

## Connecting tools to AI agents (MCP)

The three read-only endpoints are wrapped by an MCP server (`agent/`) so any MCP-capable agent
can call them as tools:

| File | Purpose |
|---|---|
| `agent/src/agent/mcp_server.py` | MCP server (FastMCP) exposing `evaluate_rules` / `get_request_context` / `search_policy` |
| `.mcp.json` | registers the server with Claude Code (cross-platform via `uv`) |
| `.claude/skills/leave-review/SKILL.md` | a Skill describing the governed review procedure |
| `.claude/agents/leave-reviewer.md` | a Subagent with a least-privilege tool allowlist |

The MCP server is a thin adapter and holds no business logic — the rule engine and the
authoritative risk floor live only in the backend.

## Tech stack

**Backend** — Java 17, Spring Boot 3.5 (Web, Security, Data JPA, Validation, Actuator, Cache),
JWT via httpOnly cookie + refresh tokens, Flyway, PostgreSQL 16 + pgvector (hibernate-vector),
langchain4j (Gemini, Jina rerank), Caffeine, MapStruct, Lombok, springdoc OpenAPI.

**Frontend** — Next.js 16, React 19, TypeScript, Tailwind CSS, Zustand, TanStack Query,
React Hook Form + Zod, next-intl (en / zh-TW), Storybook.

**Agent** — Python ≥ 3.11, MCP (FastMCP), pydantic, httpx, LangGraph; managed with `uv`.

**AI providers** — Gemini (`gemini-2.5-flash-lite` review, `gemini-embedding-001` embeddings),
Ollama (`llama3.1:8b`, local fallback), Jina (`jina-reranker-v2-base-multilingual`).

## Getting started

```bash
# 1. Config — copy the examples and fill in DB credentials (+ optional Gemini key)
cp backend/.env.example backend/.env
cp backend/src/main/resources/application-local.properties.example \
   backend/src/main/resources/application-local.properties

# 2. Database (PostgreSQL 16 + pgvector)
cd backend && docker compose up -d

# 3. Backend (Flyway migrates on startup)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# 4. Frontend
cd frontend && npm install && npm run dev
```

To drive the tools from Claude Code, install [`uv`](https://docs.astral.sh/uv/), sync the agent
environment, and approve the MCP server:

```bash
uv --directory agent sync
cp agent/.env.example agent/.env       # set AGENT_PASSWORD
claude                                 # from the repo root, run /mcp to approve "workflow"
```

## Roadmap

- A programmatic LangGraph agent over the same MCP tools.
- Agent-run tracing with token / cost attribution.
- A scenario-based guardrail evaluation suite.
- A hosted demo.

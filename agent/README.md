# agent/ — Governed LLM Agent (LangGraph + MCP)

Python agent that turns the leave-approval system's existing `AI_AGENT` tool
surface into an autonomous, governed review agent.

The agent (Gemini, via LangGraph ReAct) decides which tools to call, but it is
sandwiched between two **deterministic nodes** so it can never bypass safety:

```
START → evaluate_rules (always) → agent (LLM + MCP tools) → enforce_floor (always) → submit → END
        ─────────── authoritative floor ───────────────────  final = max(agent, floor)
```

Full design: [`../docs/02_ai_agent_langgraph_mcp_plan.md`](../docs/02_ai_agent_langgraph_mcp_plan.md)

## Setup

```bash
cd agent
python -m venv .venv && . .venv/Scripts/activate    # Windows; use .venv/bin/activate on *nix
pip install -e ".[dev]"
cp .env.example .env                                # then fill AGENT_PASSWORD + GEMINI_API_KEY
```

For tools or contributors that expect a traditional requirements file, the
equivalent command is:

```bash
pip install -r requirements.txt
```

`pyproject.toml` remains the dependency source of truth; `requirements.txt`
delegates to it so the package lists cannot drift.

`AGENT_PASSWORD` = the `ai-agent@system.local` password, which equals the
`huang.yating@example.com` (EMP007) demo password (same bcrypt hash). Recover it
from `docs/01_development_roadmap.md` via git history, or reset it with a migration.

## Run

```bash
# Phase 0 — connectivity (no LLM): login as AI_AGENT, call the 3 tool endpoints
python -m agent.smoke --request-id 1

# Phase 1 — MCP server (stdio); inspect the tools
python -m agent.mcp_server
npx @modelcontextprotocol/inspector python -m agent.mcp_server

# Unit tests for the deterministic safety gate (no backend needed)
pytest tests

# Phase 2+ — full agent review (stub until the graph is wired)
python -m agent.run_review --request-id 1
```

> `policy-search` only exists if the backend has a Gemini API key configured
> (`@ConditionalOnProperty app.ai.gemini.api-key`); otherwise that one tool 404s.

## Layout & status

```
src/agent/
  config.py            settings from .env                         [ready]
  schema.py            enums + DraftReview/FinalReview             [ready]
  backend_client.py    AI_AGENT login + governed tool calls        [ready]
  mcp_server.py        FastMCP: get_context/evaluate_rules/policy   [ready]
  smoke.py             Phase 0 connectivity check                  [ready]
  observability.py     trajectory / token / latency                [ready]
  nodes/
    evaluate_rules.py  deterministic #1 (authoritative floor)      [ready]
    enforce_floor.py   deterministic #3 (max + downgrade + I6)     [ready]
    agent_node.py      Gemini ReAct loop                           [Phase 2 stub]
    submit.py          emit (v1) / POST draft-review (v2)          [v1 ready / v2 Phase 5]
  tools.py             MCP -> LangChain tools                      [Phase 2 stub]
  graph.py             StateGraph wiring (ReviewState is real)     [Phase 2 stub]
  run_review.py        full review CLI                             [Phase 2 stub]
tests/                 deterministic-gate unit tests               [ready]
evals/                 scenario guardrail eval                     [Phase 4 scaffold]
```

The deterministic backbone (`evaluate_rules`, `enforce_floor`) and its tests are
already real — the safety guarantees do not depend on any unfinished agent code.

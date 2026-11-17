# agent/ — Governed LLM Agent (LangGraph + MCP)

Python agent that turns the leave-approval system's existing `AI_AGENT` tool
surface into an autonomous, governed review agent.

The agent (Gemini, built with LangChain `create_agent`) decides which tools to call,
but it is one node in a LangGraph `StateGraph`, sandwiched between two
**deterministic nodes** so it can never bypass safety:

```
START → evaluate_rules (always) → agent (LLM + MCP tools) → enforce_floor (always) → submit → END
        ─────────── authoritative floor ───────────────────  final = max(agent, floor)
```

The edges are fixed, so the order is guaranteed by the graph rather than by the
prompt. The agent is offered two read tools (`get_request_context`,
`search_policy`); rule evaluation is not on its menu. If the agent raises, times
out, loops past its step limit or answers in the wrong shape, the review falls
back to the rule-only result.

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

# Unit tests: the safety gate, the graph with a misbehaving fake agent, and the
# agent wiring with a scripted model (no backend, no API key)
pytest tests

# Phase 2 — full agent review (needs the backend, AGENT_PASSWORD and GEMINI_API_KEY)
python -m agent.run_review --request-id 1

# Phase 4 — the same invariants on real runs; scenarios without a request_id are skipped
pytest evals/test_guardrails.py
```

> `policy-search` only exists if the backend has a Gemini API key configured
> (`@ConditionalOnProperty app.ai.gemini.api-key`); otherwise that one tool 404s.

## Retrieval eval

`evals/retrieval_eval.py` scores the policy search itself (which sections come back and in
what order), not the LLM's answer. The questions in `evals/retrieval_questions.zh.yaml` mirror
what actually reaches the search: every single-rule query the built-in review can build, plus
paraphrased queries an external agent might send through `search_policy`. It reports Hit@1,
Hit@3 and MRR per question type.

```bash
# Keyword (BM25) baseline — no backend, no API key
python evals/retrieval_eval.py --mode keyword --save evals/results/keyword.json

# The real search — needs a backend started with a Gemini key (plus a Jina key for rerank)
python evals/retrieval_eval.py --mode backend --save evals/results/vector.json

# Side-by-side comparison of saved runs
python evals/retrieval_eval.py --compare evals/results/keyword.json evals/results/vector.json
```

Status: only the keyword baseline has been run ([`evals/results/keyword.md`](evals/results/keyword.md)).
The backend mode has not been run against a live backend yet, so there are no numbers for the
vector or reranked search.

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
    agent_node.py      Gemini tool loop (create_agent)             [wired, not run live]
    submit.py          emit (v1) / POST draft-review (v2)          [v1 ready / v2 Phase 5]
  tools.py             MCP -> LangChain tools (one stdio session)  [ready]
  graph.py             StateGraph: 4 nodes, fixed edges            [ready]
  run_review.py        full review CLI                             [wired, not run live]
tests/                 gate + graph guardrails + agent wiring      [ready]
evals/                 scenario guardrail eval (real model)        [written, request_ids not filled]
```

What is and is not verified:

- **Verified without a model** (`pytest tests`, runs in CI): with a fake agent that
  answers LOW / APPROVE, throws, hangs or returns garbage, the final review is never
  below the rule floor and degrades to the rule-only result; rules always run before
  the agent; the MCP server starts over stdio and the agent is offered only its two
  read tools; the `create_agent` loop is read back correctly using a scripted model.
- **Not yet verified**: a run with a real Gemini model against a live backend. Whether
  the model reliably calls `search_policy` and cites a source when flags are present is
  what `evals/test_guardrails.py` measures once `scenarios.yaml` has request ids.

"""Agent node — the LLM-driven ReAct loop (Gemini over MCP tools).  [Phase 2 — STUB]

Produces a DraftReview. It is sandwiched between the two deterministic nodes, so
its output is only ever a *suggestion*; enforce_floor has the final say.

Intended implementation (Phase 2):

    from langchain_google_genai import ChatGoogleGenerativeAI
    from langgraph.prebuilt import create_react_agent

    model = ChatGoogleGenerativeAI(model=settings.gemini_model,
                                   google_api_key=settings.gemini_api_key)
    agent = create_react_agent(model, tools, prompt=SYSTEM_PROMPT)

System-prompt contract:
  - Goal: produce a DraftReview (summary / suggested_risk / suggested_recommendation
    / recommendation_reason / risk_reasons / policy_citations).
  - rule_flags are provided in context; when flags are present you MUST call
    search_policy and cite at least one source.
  - Call get_request_context when you need more detail.
  - You do NOT set the risk floor — the system guarantees it.
"""

from __future__ import annotations

SYSTEM_PROMPT = (
    "You are a leave-approval review assistant. Use the provided tools to draft a "
    "review. The deterministic rule engine owns the risk floor; you only advise. "
    "When risk flags are present you MUST call search_policy and cite a source."
)


def build_agent(tools: list):  # noqa: ANN201 - returns a compiled LangGraph agent
    raise NotImplementedError(
        "Phase 2: build the ReAct agent (Gemini + MCP tools). "
        "See docs/02_ai_agent_langgraph_mcp_plan.md §6."
    )

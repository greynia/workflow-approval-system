"""LangGraph assembly: the deterministic backbone with the agent in the middle.

    START
      -> evaluate_rules   (deterministic, always)   writes rule_flags + rule_floor
      -> agent            (LLM ReAct over MCP tools) writes draft
      -> enforce_floor    (deterministic, always)    writes final = max(draft, floor)
      -> submit
      -> END

[Phase 2 — wiring is a STUB. The ReviewState below is the real shared state.]
"""

from __future__ import annotations

from typing import Annotated, TypedDict

from .schema import DraftReview, FinalReview, HardRuleFlag, RiskLevel


class ReviewState(TypedDict, total=False):
    request_id: int
    locale: str
    rule_flags: list[HardRuleFlag]
    rule_floor: RiskLevel
    messages: Annotated[list, "agent conversation / tool turns"]
    draft: DraftReview | None
    final: FinalReview


def build_graph():  # noqa: ANN201 - returns a compiled StateGraph
    raise NotImplementedError(
        "Phase 2: wire evaluate_rules -> agent -> enforce_floor -> submit into a "
        "StateGraph. See docs/02_ai_agent_langgraph_mcp_plan.md §6."
    )

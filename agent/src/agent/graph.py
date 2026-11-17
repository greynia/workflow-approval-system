"""LangGraph assembly: the deterministic backbone with the agent in the middle.

    START
      -> evaluate_rules   (deterministic, always)   writes rule_flags + rule_floor
      -> agent            (LLM tool loop over MCP)   writes draft (or None)
      -> enforce_floor    (deterministic, always)    writes final = max(draft, floor)
      -> submit
      -> END

The edges are fixed: there is no path from START to END that skips
evaluate_rules or enforce_floor, and the agent node has no say over where the
graph goes next. Whatever the agent does — a low suggestion, an exception, a
timeout, a malformed answer — the review still ends in enforce_floor.
"""

from __future__ import annotations

import asyncio
from typing import TypedDict

from langgraph.graph import END, START, StateGraph

from .backend_client import BackendClient
from .nodes import evaluate_rules as evaluate_rules_impl
from .nodes.agent_node import AgentOutcome, AgentRunner
from .nodes.enforce_floor import enforce_floor
from .nodes.submit import submit_local
from .observability import Trajectory, timed
from .schema import DraftReview, FinalReview, HardRuleFlag, RiskLevel

# Wall-clock budget for the whole agent node. Past this the review falls back to
# the rule-only result instead of making the approver wait.
AGENT_TIMEOUT_SECONDS = 60.0


class ReviewState(TypedDict, total=False):
    request_id: int
    locale: str
    rule_flags: list[HardRuleFlag]
    rule_floor: RiskLevel
    draft: DraftReview | None
    agent_error: str | None
    final: FinalReview
    trajectory: Trajectory


def build_graph(
    client: BackendClient,
    run_agent: AgentRunner,
    *,
    agent_timeout_seconds: float = AGENT_TIMEOUT_SECONDS,
):  # noqa: ANN201 - returns a compiled StateGraph
    """Wire the four nodes. `run_agent` is the only non-deterministic part."""

    def evaluate_rules_node(state: ReviewState) -> dict:
        trajectory = Trajectory(request_id=state["request_id"])
        with timed() as t:
            evaluation = evaluate_rules_impl.run(
                client, state["request_id"], state.get("locale", "en")
            )
        trajectory.record("evaluate_rules", t.ms, ok=True)
        return {
            "rule_flags": evaluation.flags,
            "rule_floor": evaluation.floor,
            "trajectory": trajectory,
        }

    async def agent_node(state: ReviewState) -> dict:
        trajectory = state["trajectory"]
        try:
            outcome: AgentOutcome = await asyncio.wait_for(
                run_agent(dict(state)), timeout=agent_timeout_seconds
            )
        except Exception as exc:  # noqa: BLE001 - any agent failure means "no draft" (I6)
            return {"draft": None, "agent_error": f"{type(exc).__name__}: {exc}"}

        for name, ok in outcome.tool_calls:
            trajectory.record(name, None, ok=ok)
        trajectory.input_tokens += outcome.input_tokens
        trajectory.output_tokens += outcome.output_tokens

        draft = outcome.draft if isinstance(outcome.draft, DraftReview) else None
        error = None if draft is not None else "agent returned no valid DraftReview"
        return {"draft": draft, "agent_error": error, "trajectory": trajectory}

    def enforce_floor_node(state: ReviewState) -> dict:
        final = enforce_floor(
            state.get("draft"),
            state["rule_flags"],
            state["rule_floor"],
            locale=state.get("locale", "en"),
        )
        return {"final": final}

    def submit_node(state: ReviewState) -> dict:
        return {"final": submit_local(state["final"])}

    graph = StateGraph(ReviewState)
    graph.add_node("evaluate_rules", evaluate_rules_node)
    graph.add_node("agent", agent_node)
    graph.add_node("enforce_floor", enforce_floor_node)
    graph.add_node("submit", submit_node)

    graph.add_edge(START, "evaluate_rules")
    graph.add_edge("evaluate_rules", "agent")
    graph.add_edge("agent", "enforce_floor")
    graph.add_edge("enforce_floor", "submit")
    graph.add_edge("submit", END)
    return graph.compile()

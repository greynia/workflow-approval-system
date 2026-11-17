"""Scenario guardrail eval — runs the full agent (real model) against a live backend.

Each test asserts an invariant from docs/02_ai_agent_langgraph_mcp_plan.md §3 on a
real run. A scenario is skipped until its `request_id` is filled in scenarios.yaml,
and the whole file is skipped without GEMINI_API_KEY / AGENT_PASSWORD, so CI (which
has neither) stays green on the unit tests alone. The same invariants are proven
without a model in tests/test_graph_guardrails.py.
"""

import asyncio
from functools import lru_cache
from pathlib import Path

import pytest
import yaml

from agent.config import get_settings
from agent.run_review import run_review
from agent.schema import RiskLevel

_SCENARIOS = yaml.safe_load((Path(__file__).parent / "scenarios.yaml").read_text("utf-8"))[
    "scenarios"
]
_ORDER = {RiskLevel.LOW: 0, RiskLevel.MEDIUM: 1, RiskLevel.HIGH: 2}


@lru_cache(maxsize=None)
def _run(request_id: int) -> dict:
    """One real review per request, shared by the tests (each run costs model calls)."""
    return asyncio.run(run_review(request_id))


def _state_for(scenario: dict) -> dict:
    if scenario["request_id"] is None:
        pytest.skip("scenario has no request_id yet (fill it from the demo seed)")
    settings = get_settings()
    if not (settings.gemini_api_key and settings.agent_password):
        pytest.skip("needs GEMINI_API_KEY and AGENT_PASSWORD in .env, and a running backend")
    return _run(scenario["request_id"])


@pytest.mark.parametrize("scenario", _SCENARIOS, ids=lambda s: s["name"])
def test_final_risk_never_below_floor(scenario):
    """I1: the final risk is always >= the rule floor."""
    state = _state_for(scenario)

    assert state["rule_floor"].value == scenario["expect_floor"]
    assert _ORDER[state["final"].risk_level] >= _ORDER[state["rule_floor"]]


@pytest.mark.parametrize("scenario", _SCENARIOS, ids=lambda s: s["name"])
def test_evaluate_rules_always_in_trajectory(scenario):
    """I2: rule evaluation always happens, before anything the agent does."""
    state = _state_for(scenario)

    assert state["trajectory"].tool_sequence[0] == "evaluate_rules"


@pytest.mark.parametrize("scenario", _SCENARIOS, ids=lambda s: s["name"])
def test_policy_cited_when_flagged(scenario):
    """When risk flags are present, the agent searches policy and cites a source."""
    state = _state_for(scenario)
    if not scenario["expect_policy_cited"]:
        pytest.skip("scenario has no risk flags")

    assert state["final"].is_fallback is False, state.get("agent_error")
    assert "search_policy" in state["trajectory"].tool_sequence
    assert len(state["final"].policy_citations) >= 1

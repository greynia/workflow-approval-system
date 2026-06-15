"""Scenario guardrail eval (Phase 4) — runs the full agent against a live backend.

Skipped until the Phase 2 graph exists and a backend + seeded data are available.
Each test asserts an invariant from docs/02_ai_agent_langgraph_mcp_plan.md §3.
"""

from pathlib import Path

import pytest
import yaml

pytestmark = pytest.mark.skip(reason="Phase 4: needs running backend + Phase 2 graph")

_SCENARIOS = yaml.safe_load((Path(__file__).parent / "scenarios.yaml").read_text("utf-8"))[
    "scenarios"
]


@pytest.mark.parametrize("scenario", _SCENARIOS, ids=lambda s: s["name"])
def test_final_risk_never_below_floor(scenario):
    """I1: the final risk is always >= the rule floor."""
    ...


@pytest.mark.parametrize("scenario", _SCENARIOS, ids=lambda s: s["name"])
def test_evaluate_rules_always_in_trajectory(scenario):
    """I2: rule evaluation always happens (deterministic backbone)."""
    ...


@pytest.mark.parametrize("scenario", _SCENARIOS, ids=lambda s: s["name"])
def test_policy_cited_when_flagged(scenario):
    """When risk flags are present, the agent cites at least one policy source."""
    ...

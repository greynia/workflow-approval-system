"""Guardrail tests for the assembled graph. No backend, no LLM, no API key.

The agent node is replaced by a fake that misbehaves on purpose. These prove the
invariants hold because of how the graph is wired, not because the model behaves:

  I1  final risk is never below the rule floor
  I2  rule evaluation always runs, and runs before the agent
  I5  HIGH risk is never auto-approved
  I6  any agent failure degrades to the rule-only result
"""

import asyncio

import pytest

from agent.graph import build_graph
from agent.nodes.agent_node import AgentOutcome
from agent.schema import DraftReview, Recommendation, RiskLevel

HIGH_FLAG = {
    "code": "HIGH_FREQUENCY",
    "level": "HIGH",
    "humanReadable": "Frequent leave in last 30 days",
}


class FakeBackend:
    """Stands in for BackendClient: returns a fixed rule evaluation."""

    def __init__(self, risk: str = "HIGH", flags: list[dict] | None = None) -> None:
        self.risk = risk
        self.flags = [HIGH_FLAG] if flags is None else flags
        self.events: list[str] = []

    def evaluate_rules(self, request_id: int, locale: str = "en") -> dict:
        self.events.append("evaluate_rules")
        return {"riskLevel": self.risk, "flags": self.flags}


def _draft(risk: RiskLevel, rec: Recommendation) -> DraftReview:
    return DraftReview(summary="draft", suggested_risk=risk, suggested_recommendation=rec)


def _agent_returning(draft, backend: FakeBackend | None = None):
    async def run_agent(state: dict) -> AgentOutcome:
        if backend is not None:
            backend.events.append("agent")
        return AgentOutcome(draft=draft)

    return run_agent


def _review(backend: FakeBackend, run_agent, **kwargs) -> dict:
    graph = build_graph(backend, run_agent, **kwargs)
    return asyncio.run(graph.ainvoke({"request_id": 1, "locale": "en"}))


def test_agent_cannot_lower_risk_below_the_rule_floor():
    # I1 + I5: the agent says LOW / APPROVE, the rules say HIGH.
    agent = _agent_returning(_draft(RiskLevel.LOW, Recommendation.APPROVE))
    final = _review(FakeBackend(risk="HIGH"), agent)["final"]

    assert final.risk_level == RiskLevel.HIGH
    assert final.recommendation != Recommendation.APPROVE
    assert final.is_fallback is False


def test_agent_may_raise_risk_above_the_floor():
    agent = _agent_returning(_draft(RiskLevel.HIGH, Recommendation.ESCALATE))
    final = _review(FakeBackend(risk="LOW", flags=[]), agent)["final"]

    assert final.risk_level == RiskLevel.HIGH
    assert final.recommendation == Recommendation.ESCALATE


def test_agent_exception_falls_back_to_rules():
    # I6: the agent blows up -> rule-only review, flagged as a fallback.
    async def broken_agent(state: dict) -> AgentOutcome:
        raise RuntimeError("model unavailable")

    state = _review(FakeBackend(risk="HIGH"), broken_agent)

    assert state["final"].is_fallback is True
    assert state["final"].risk_level == RiskLevel.HIGH
    assert "model unavailable" in state["agent_error"]


def test_agent_timeout_falls_back_to_rules():
    # I6: a hanging agent is cut off; the approver still gets the rule-only review.
    async def hanging_agent(state: dict) -> AgentOutcome:
        await asyncio.sleep(30)
        return AgentOutcome(draft=_draft(RiskLevel.LOW, Recommendation.APPROVE))

    state = _review(FakeBackend(risk="MEDIUM"), hanging_agent, agent_timeout_seconds=0.05)

    assert state["final"].is_fallback is True
    assert state["final"].risk_level == RiskLevel.MEDIUM
    assert state["agent_error"].startswith("TimeoutError")


@pytest.mark.parametrize("bad_draft", [None, {"suggested_risk": "LOW"}, "approve it"])
def test_agent_without_a_valid_draft_falls_back_to_rules(bad_draft):
    # I6: no answer, or an answer in the wrong shape, is treated as "no draft".
    state = _review(FakeBackend(risk="HIGH"), _agent_returning(bad_draft))

    assert state["final"].is_fallback is True
    assert state["final"].risk_level == RiskLevel.HIGH
    assert state["agent_error"]


def test_rules_always_run_before_the_agent():
    # I2: the agent starts with the floor already decided; it cannot skip the rules.
    backend = FakeBackend(risk="HIGH")
    seen: dict = {}

    async def recording_agent(state: dict) -> AgentOutcome:
        backend.events.append("agent")
        seen.update(state)
        return AgentOutcome(
            draft=_draft(RiskLevel.HIGH, Recommendation.REVIEW_CAREFULLY),
            tool_calls=[("search_policy", True)],
            input_tokens=120,
            output_tokens=30,
        )

    state = _review(backend, recording_agent)

    assert backend.events == ["evaluate_rules", "agent"]
    assert seen["rule_floor"] == RiskLevel.HIGH
    assert [f.code for f in seen["rule_flags"]] == ["HIGH_FREQUENCY"]
    assert state["trajectory"].tool_sequence == ["evaluate_rules", "search_policy"]
    assert state["trajectory"].input_tokens == 120
    assert state["trajectory"].output_tokens == 30


def test_no_review_is_produced_when_rule_evaluation_fails():
    # Without an authoritative floor there is nothing safe to return: fail loudly
    # instead of letting the agent's opinion stand in for the rules.
    class DownBackend(FakeBackend):
        def evaluate_rules(self, request_id: int, locale: str = "en") -> dict:
            raise ConnectionError("backend unreachable")

    agent = _agent_returning(_draft(RiskLevel.LOW, Recommendation.APPROVE))
    with pytest.raises(ConnectionError):
        _review(DownBackend(), agent)

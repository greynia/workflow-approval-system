"""Unit tests for the deterministic safety gate. No backend / no LLM required.

These prove the invariants that make the agent safe (I1, I5, I6).
Run: pytest agent/tests
"""

from agent.nodes.enforce_floor import enforce_floor
from agent.schema import DraftReview, HardRuleFlag, Recommendation, RiskLevel


def _draft(risk: RiskLevel, rec: Recommendation) -> DraftReview:
    return DraftReview(summary="draft", suggested_risk=risk, suggested_recommendation=rec)


def _flag(text: str = "Frequent leave in last 30 days") -> HardRuleFlag:
    return HardRuleFlag(code="HIGH_FREQUENCY", level=RiskLevel.HIGH, human_readable=text)


def test_floor_raises_risk_when_agent_underrates():
    # I1: agent says LOW, rules say HIGH -> final must be HIGH
    final = enforce_floor(_draft(RiskLevel.LOW, Recommendation.APPROVE), [_flag()], RiskLevel.HIGH)
    assert final.risk_level == RiskLevel.HIGH


def test_floor_keeps_higher_agent_risk():
    # max(): agent HIGH, floor LOW -> HIGH
    final = enforce_floor(
        _draft(RiskLevel.HIGH, Recommendation.REVIEW_CAREFULLY), [], RiskLevel.LOW
    )
    assert final.risk_level == RiskLevel.HIGH


def test_high_risk_approve_is_downgraded():
    # I5: HIGH + APPROVE -> REVIEW_CAREFULLY
    final = enforce_floor(_draft(RiskLevel.LOW, Recommendation.APPROVE), [_flag()], RiskLevel.HIGH)
    assert final.recommendation == Recommendation.REVIEW_CAREFULLY


def test_rule_reasons_listed_before_agent_reasons():
    draft = _draft(RiskLevel.MEDIUM, Recommendation.REVIEW_CAREFULLY)
    draft.risk_reasons = ["agent note"]
    final = enforce_floor(draft, [_flag("Frequent leave")], RiskLevel.MEDIUM)
    assert final.risk_reasons[0] == "Frequent leave"
    assert "agent note" in final.risk_reasons


def test_none_draft_falls_back_to_pure_rules():
    # I6: agent unavailable -> deterministic rule-only result
    final = enforce_floor(None, [_flag()], RiskLevel.HIGH)
    assert final.is_fallback is True
    assert final.risk_level == RiskLevel.HIGH
    assert final.recommendation == Recommendation.REVIEW_CAREFULLY

"""Deterministic node #3 — ALWAYS runs after the agent. The safety gate.

Direct port of the backend's `AiReviewOrchestrator.enforceHardRuleFloor`.
This is NOT handed to the LLM. It enforces the invariants:

  I1  final_risk = max(agent_suggestion, rule_floor)   -> agent can only raise risk
  I5  HIGH + APPROVE  -> REVIEW_CAREFULLY
  I6  draft is None/invalid -> fall back to a pure rule-based result

(Phase 5 / v2: this logic moves back to the backend behind POST /draft-review so
there is a single source of truth — see docs/02_ai_agent_langgraph_mcp_plan.md.)
"""

from __future__ import annotations

import re

from ..schema import (
    DraftReview,
    FinalReview,
    HardRuleFlag,
    PolicyCitation,
    Recommendation,
    RiskLevel,
    max_risk,
)


def enforce_floor(
    draft: DraftReview | None,
    flags: list[HardRuleFlag],
    floor: RiskLevel,
    *,
    locale: str = "en",
    policy_citations: list[PolicyCitation] | None = None,
) -> FinalReview:
    if draft is None:
        return _pure_rule_result(flags, floor, locale)

    final_risk = max_risk(draft.suggested_risk, floor)  # I1

    recommendation = draft.suggested_recommendation
    if final_risk == RiskLevel.HIGH and recommendation == Recommendation.APPROVE:
        recommendation = Recommendation.REVIEW_CAREFULLY  # I5

    return FinalReview(
        summary=draft.summary,
        risk_level=final_risk,
        risk_reasons=_merge_reasons(flags, draft.risk_reasons),
        recommendation=recommendation,
        recommendation_reason=draft.recommendation_reason,
        policy_citations=policy_citations or draft.policy_citations,
        is_fallback=False,
    )


def _pure_rule_result(
    flags: list[HardRuleFlag], floor: RiskLevel, locale: str
) -> FinalReview:
    """I6: agent unavailable/invalid -> deterministic rule-only review."""
    recommendation = (
        Recommendation.APPROVE if floor == RiskLevel.LOW else Recommendation.REVIEW_CAREFULLY
    )
    zh = locale.lower().startswith("zh")
    summary = (
        "AI 文字分析暫時不可用，以下依系統規則產生審核摘要。"
        if zh
        else "AI text analysis is temporarily unavailable; this review was generated from system rules."
    )
    return FinalReview(
        summary=summary,
        risk_level=floor,
        risk_reasons=[f.human_readable for f in flags],
        recommendation=recommendation,
        recommendation_reason="",
        policy_citations=[],
        is_fallback=True,
    )


def _merge_reasons(flags: list[HardRuleFlag], ai_reasons: list[str]) -> list[str]:
    """Rule flags first, then AI reasons; dedup on a normalized key (mirrors Java)."""
    seen: set[str] = set()
    out: list[str] = []

    def add(reason: str | None) -> None:
        if not reason or not reason.strip():
            return
        key = re.sub(r"[\s\W]+", "", reason.lower())
        if key not in seen:
            seen.add(key)
            out.append(reason)

    for flag in flags:
        add(flag.human_readable)
    for reason in ai_reasons or []:
        add(reason)
    return out

"""Domain types mirrored from the backend enums + agent I/O models.

Enum values intentionally match the Java backend so payloads round-trip:
  RiskLevel:       LOW < MEDIUM < HIGH
  Recommendation:  APPROVE / REVIEW_CAREFULLY / ESCALATE / INSUFFICIENT_INFORMATION
"""

from __future__ import annotations

from enum import Enum

from pydantic import BaseModel, Field


class RiskLevel(str, Enum):
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"


class Recommendation(str, Enum):
    APPROVE = "APPROVE"
    REVIEW_CAREFULLY = "REVIEW_CAREFULLY"
    ESCALATE = "ESCALATE"
    INSUFFICIENT_INFORMATION = "INSUFFICIENT_INFORMATION"


_RISK_ORDER = {RiskLevel.LOW: 0, RiskLevel.MEDIUM: 1, RiskLevel.HIGH: 2}


def max_risk(a: RiskLevel, b: RiskLevel) -> RiskLevel:
    """Return the higher of two risk levels (mirrors Java RiskLevel.compareTo)."""
    return a if _RISK_ORDER[a] >= _RISK_ORDER[b] else b


class HardRuleFlag(BaseModel):
    code: str
    level: RiskLevel
    human_readable: str = Field(alias="humanReadable")

    model_config = {"populate_by_name": True}


class PolicyCitation(BaseModel):
    section: str | None = None
    source: str | None = None
    content: str | None = None
    score: float | None = None


class DraftReview(BaseModel):
    """The agent's *suggestion*. Never the final word — enforce_floor clamps it."""

    summary: str
    suggested_risk: RiskLevel
    suggested_recommendation: Recommendation
    recommendation_reason: str = ""
    risk_reasons: list[str] = Field(default_factory=list)
    policy_citations: list[PolicyCitation] = Field(default_factory=list)


class FinalReview(BaseModel):
    """Result after the deterministic rule-floor has been enforced."""

    summary: str
    risk_level: RiskLevel
    risk_reasons: list[str] = Field(default_factory=list)
    recommendation: Recommendation
    recommendation_reason: str = ""
    policy_citations: list[PolicyCitation] = Field(default_factory=list)
    is_fallback: bool = False

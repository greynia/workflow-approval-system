"""Deterministic node #1 — ALWAYS runs before the agent.

Calls the backend rule engine and returns the authoritative hard-rule flags and
risk floor. This guarantees the floor exists regardless of what the agent does
later (invariant I2: rule evaluation is not an agent decision).
"""

from __future__ import annotations

from dataclasses import dataclass

from ..backend_client import BackendClient
from ..schema import HardRuleFlag, RiskLevel


@dataclass
class RuleEvaluation:
    flags: list[HardRuleFlag]
    floor: RiskLevel  # backend returns highestRiskLevel(flags) as `riskLevel`


def run(client: BackendClient, request_id: int, locale: str = "en") -> RuleEvaluation:
    payload = client.evaluate_rules(request_id, locale=locale)
    flags = [HardRuleFlag.model_validate(f) for f in payload.get("flags", [])]
    floor = RiskLevel(payload.get("riskLevel", "LOW"))
    return RuleEvaluation(flags=flags, floor=floor)

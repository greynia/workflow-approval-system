"""Submit node — emit / persist the final review.  [v1 local · v2 backend]

v1: return the FinalReview (CLI prints it).
v2 (Phase 5): POST it to /api/ai/requests/{id}/draft-review so the BACKEND re-runs
enforceHardRuleFloor server-side and persists — making the floor unbypassable and
keeping a single source of truth (invariants I4, I7).
"""

from __future__ import annotations

from ..backend_client import BackendClient
from ..schema import FinalReview


def submit_local(final: FinalReview) -> FinalReview:
    return final


def submit_to_backend(client: BackendClient, request_id: int, final: FinalReview) -> dict:
    # Phase 5: backend re-enforces the floor before persisting.
    return client.submit_draft_review(request_id, final.model_dump(mode="json"))

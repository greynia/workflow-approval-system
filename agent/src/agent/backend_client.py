"""HTTP client that authenticates as the AI_AGENT service account and calls the
governed `/api/ai/**` tool endpoints.

Auth: the backend issues a JWT delivered via an httpOnly cookie (`workflow-token`).
`httpx.Client` keeps a cookie jar, so after one login the cookie rides along
automatically. On a 401 we re-login once.
"""

from __future__ import annotations

import httpx

from .config import Settings, get_settings


class BackendClient:
    def __init__(self, settings: Settings | None = None) -> None:
        self.settings = settings or get_settings()
        self._client = httpx.Client(
            base_url=self.settings.backend_base_url,
            timeout=self.settings.request_timeout_seconds,
        )
        self._logged_in = False

    # --- auth ---
    def login(self) -> None:
        resp = self._client.post(
            "/api/auth/login",
            json={
                "email": self.settings.agent_email,
                "password": self.settings.agent_password,
                "preferredLocale": self.settings.agent_preferred_locale,
            },
        )
        resp.raise_for_status()  # the workflow-token cookie is now in the jar
        self._logged_in = True

    def _ensure_login(self) -> None:
        if not self._logged_in:
            self.login()

    def _get(self, path: str, params: dict | None = None) -> dict:
        self._ensure_login()
        resp = self._client.get(path, params=params)
        if resp.status_code == 401:
            self.login()  # token likely expired → retry once
            resp = self._client.get(path, params=params)
        resp.raise_for_status()
        return resp.json()

    # --- governed AI tools (AI_AGENT role) ---
    def get_request_context(self, request_id: int) -> dict:
        return self._get(f"/api/ai/requests/{request_id}/context")

    def evaluate_rules(self, request_id: int, locale: str = "en") -> dict:
        return self._get(
            f"/api/ai/requests/{request_id}/rule-evaluation", params={"locale": locale}
        )

    def search_policy(
        self, query: str, top_k: int | None = None, locale: str | None = None
    ) -> dict:
        params: dict[str, object] = {"query": query}
        if top_k is not None:
            params["topK"] = top_k
        if locale is not None:
            params["locale"] = locale
        return self._get("/api/ai/policy-search", params=params)

    # --- v2 (Phase 5): let the backend re-enforce the floor and persist ---
    def submit_draft_review(self, request_id: int, draft: dict) -> dict:
        self._ensure_login()
        resp = self._client.post(f"/api/ai/requests/{request_id}/draft-review", json=draft)
        resp.raise_for_status()
        return resp.json()

    # --- lifecycle ---
    def close(self) -> None:
        self._client.close()

    def __enter__(self) -> "BackendClient":
        return self

    def __exit__(self, *exc: object) -> None:
        self.close()

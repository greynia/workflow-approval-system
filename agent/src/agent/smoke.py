"""Phase 0 connectivity check.

Logs in as the AI_AGENT service account and calls all three governed endpoints,
proving the Python side can reach the backend before any LLM wiring exists.

Usage:  python -m agent.smoke --request-id 1
"""

from __future__ import annotations

import argparse
import json

from .backend_client import BackendClient


def _show(label: str, payload: object) -> None:
    print(f"\n=== {label} ===")
    print(json.dumps(payload, ensure_ascii=False, indent=2))


def main() -> None:
    parser = argparse.ArgumentParser(description="AI_AGENT backend connectivity check")
    parser.add_argument("--request-id", type=int, required=True)
    parser.add_argument("--query", default="annual leave frequency policy")
    parser.add_argument("--locale", default="en", choices=["en", "zh-TW"])
    args = parser.parse_args()

    with BackendClient() as client:
        client.login()
        print("login OK (AI_AGENT cookie acquired)")

        _show("context", client.get_request_context(args.request_id))
        _show("rule-evaluation", client.evaluate_rules(args.request_id, locale=args.locale))

        # policy-search only exists if the backend has a Gemini key configured.
        try:
            _show("policy-search", client.search_policy(args.query, locale=args.locale))
        except Exception as exc:  # noqa: BLE001 - smoke test, surface anything
            print(f"\npolicy-search skipped/failed: {exc}")


if __name__ == "__main__":
    main()
